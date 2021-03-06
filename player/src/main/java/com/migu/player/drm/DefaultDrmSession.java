/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.migu.player.drm;

import android.annotation.SuppressLint;
import android.media.NotProvisionedException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Pair;

import com.migu.player.C;
import com.migu.player.drm.DrmInitData.SchemeData;
import com.migu.player.drm.ExoMediaDrm.KeyRequest;
import com.migu.player.drm.ExoMediaDrm.ProvisionRequest;
import com.migu.player.source.LoadEventInfo;
import com.migu.player.source.MediaLoadData;
import com.migu.player.upstream.LoadErrorHandlingPolicy;
import com.migu.player.upstream.LoadErrorHandlingPolicy.LoadErrorInfo;
import com.migu.player.util.Assertions;
import com.migu.player.util.Consumer;
import com.migu.player.util.CopyOnWriteMultiset;
import com.migu.player.util.Log;
import com.migu.player.util.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.migu.player.util.Assertions.checkState;
import static java.lang.Math.min;


/** A {@link DrmSession} that supports playbacks using {@link ExoMediaDrm}. */
@RequiresApi(18)
/* package */ class DefaultDrmSession implements DrmSession {

  /** Thrown when an unexpected exception or error is thrown during provisioning or key requests. */
  public static final class UnexpectedDrmSessionException extends IOException {

    public UnexpectedDrmSessionException(@Nullable Throwable cause) {
      super(cause);
    }
  }

  /** Manages provisioning requests. */
  public interface ProvisioningManager {

    /**
     * Called when a session requires provisioning. The manager <em>may</em> call {@link
     * #provision()} to have this session perform the provisioning operation. The manager
     * <em>will</em> call {@link DefaultDrmSession#onProvisionCompleted()} when provisioning has
     * completed, or {@link DefaultDrmSession#onProvisionError} if provisioning fails.
     *
     * @param session The session.
     */
    void provisionRequired(DefaultDrmSession session);

    /**
     * Called by a session when it fails to perform a provisioning operation.
     *
     * @param error The error that occurred.
     */
    void onProvisionError(Exception error);

    /** Called by a session when it successfully completes a provisioning operation. */
    void onProvisionCompleted();
  }

  /** Callback to be notified when the reference count of this session changes. */
  public interface ReferenceCountListener {

    /**
     * Called when the internal reference count of this session is incremented.
     *
     * @param session This session.
     * @param newReferenceCount The reference count after being incremented.
     */
    void onReferenceCountIncremented(DefaultDrmSession session, int newReferenceCount);

    /**
     * Called when the internal reference count of this session is decremented.
     *
     * <p>{@code newReferenceCount == 0} indicates this session is in {@link #STATE_RELEASED}.
     *
     * @param session This session.
     * @param newReferenceCount The reference count after being decremented.
     */
    void onReferenceCountDecremented(DefaultDrmSession session, int newReferenceCount);
  }

  private static final String TAG = "DefaultDrmSession";

  private static final int MSG_PROVISION = 0;
  private static final int MSG_KEYS = 1;
  private static final int MAX_LICENSE_DURATION_TO_RENEW_SECONDS = 60;

  /** The DRM scheme datas, or null if this session uses offline keys. */
  @Nullable public final List<SchemeData> schemeDatas;

  private final ExoMediaDrm mediaDrm;
  private final ProvisioningManager provisioningManager;
  private final ReferenceCountListener referenceCountListener;
  private final @DefaultDrmSessionManager.Mode int mode;
  private final boolean playClearSamplesWithoutKeys;
  private final boolean isPlaceholderSession;
  private final HashMap<String, String> keyRequestParameters;
  private final CopyOnWriteMultiset<DrmSessionEventListener.EventDispatcher> eventDispatchers;
  private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;

  /* package */ final MediaDrmCallback callback;
  /* package */ final UUID uuid;
  /* package */ final ResponseHandler responseHandler;

  private @DrmSession.State int state;
  private int referenceCount;
  @Nullable private HandlerThread requestHandlerThread;
  @Nullable private RequestHandler requestHandler;
  @Nullable private ExoMediaCrypto mediaCrypto;
  @Nullable private DrmSessionException lastException;
  @Nullable private byte[] sessionId;
  private byte  [] offlineLicenseKeySetId;

  @Nullable private KeyRequest currentKeyRequest;
  @Nullable private ProvisionRequest currentProvisionRequest;

  /**
   * Instantiates a new DRM session.
   *
   * @param uuid The UUID of the drm scheme.
   * @param mediaDrm The media DRM.
   * @param provisioningManager The manager for provisioning.
   * @param referenceCountListener The {@link ReferenceCountListener}.
   * @param schemeDatas DRM scheme datas for this session, or null if an {@code
   *     offlineLicenseKeySetId} is provided or if {@code isPlaceholderSession} is true.
   * @param mode The DRM mode. Ignored if {@code isPlaceholderSession} is true.
   * @param isPlaceholderSession Whether this session is not expected to acquire any keys.
   * @param offlineLicenseKeySetId The offline license key set identifier, or null when not using
   *     offline keys.
   * @param keyRequestParameters Key request parameters.
   * @param callback The media DRM callback.
   * @param playbackLooper The playback looper.
   * @param loadErrorHandlingPolicy The {@link LoadErrorHandlingPolicy} for key and provisioning
   *     requests.
   */
  public DefaultDrmSession(
      UUID uuid,
      ExoMediaDrm mediaDrm,
      ProvisioningManager provisioningManager,
      ReferenceCountListener referenceCountListener,
      @Nullable List<SchemeData> schemeDatas,
      @DefaultDrmSessionManager.Mode int mode,
      boolean playClearSamplesWithoutKeys,
      boolean isPlaceholderSession,
      @Nullable byte[] offlineLicenseKeySetId,
      HashMap<String, String> keyRequestParameters,
      MediaDrmCallback callback,
      Looper playbackLooper,
      LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
    if (mode == DefaultDrmSessionManager.MODE_QUERY
        || mode == DefaultDrmSessionManager.MODE_RELEASE) {
      Assertions.checkNotNull(offlineLicenseKeySetId);
    }
    this.uuid = uuid;
    this.provisioningManager = provisioningManager;
    this.referenceCountListener = referenceCountListener;
    this.mediaDrm = mediaDrm;
    this.mode = mode;
    this.playClearSamplesWithoutKeys = playClearSamplesWithoutKeys;
    this.isPlaceholderSession = isPlaceholderSession;
    if (offlineLicenseKeySetId != null) {
      this.offlineLicenseKeySetId = offlineLicenseKeySetId;
      this.schemeDatas = null;
    } else {
      this.schemeDatas = Collections.unmodifiableList(Assertions.checkNotNull(schemeDatas));
    }
    this.keyRequestParameters = keyRequestParameters;
    this.callback = callback;
    this.eventDispatchers = new CopyOnWriteMultiset<>();
    this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
    state = STATE_OPENING;
    responseHandler = new ResponseHandler(playbackLooper);
  }

  public boolean hasSessionId(byte[] sessionId) {
    return Arrays.equals(this.sessionId, sessionId);
  }

  public void onMediaDrmEvent(int what) {
    switch (what) {
      case ExoMediaDrm.EVENT_KEY_REQUIRED:
        onKeysRequired();
        break;
      default:
        break;
    }
  }

  // Provisioning implementation.

  public void provision() {
    currentProvisionRequest = mediaDrm.getProvisionRequest();
    Util.castNonNull(requestHandler)
        .post(
            MSG_PROVISION,
            Assertions.checkNotNull(currentProvisionRequest),
            /* allowRetry= */ true);
  }

  public void onProvisionCompleted() {
    if (openInternal(false)) {
      doLicense(true);
    }
  }

  public void onProvisionError(Exception error) {
    onError(error);
  }

  // DrmSession implementation.

  @Override
  @DrmSession.State
  public final int getState() {
    return state;
  }

  @Override
  public boolean playClearSamplesWithoutKeys() {
    return playClearSamplesWithoutKeys;
  }

  @Override
  public final @Nullable DrmSessionException getError() {
    return state == STATE_ERROR ? lastException : null;
  }

  @Override
  public final @Nullable ExoMediaCrypto getMediaCrypto() {
    return mediaCrypto;
  }

  @Override
  @Nullable
  public Map<String, String> queryKeyStatus() {
    return sessionId == null ? null : mediaDrm.queryKeyStatus(sessionId);
  }

  @Override
  @Nullable
  public byte[] getOfflineLicenseKeySetId() {
    return offlineLicenseKeySetId;
  }

  @Override
  public void acquire(@Nullable DrmSessionEventListener.EventDispatcher eventDispatcher) {
    checkState(referenceCount >= 0);
    if (eventDispatcher != null) {
      eventDispatchers.add(eventDispatcher);
    }
    if (++referenceCount == 1) {
      checkState(state == STATE_OPENING);
      requestHandlerThread = new HandlerThread("ExoPlayer:DrmRequestHandler");
      requestHandlerThread.start();
      requestHandler = new RequestHandler(requestHandlerThread.getLooper());
      if (openInternal(true)) {
        doLicense(true);
      }
    } else if (eventDispatcher != null && isOpen()) {
      // If the session is already open then send the acquire event only to the provided dispatcher.
      // TODO: Add a parameter to onDrmSessionAcquired to indicate whether the session is being
      // re-used or not.
      eventDispatcher.drmSessionAcquired();
    }
    referenceCountListener.onReferenceCountIncremented(this, referenceCount);
  }

  @Override
  public void release(@Nullable DrmSessionEventListener.EventDispatcher eventDispatcher) {
    checkState(referenceCount > 0);
    if (--referenceCount == 0) {
      // Assigning null to various non-null variables for clean-up.
      state = STATE_RELEASED;
      Util.castNonNull(responseHandler).removeCallbacksAndMessages(null);
      Util.castNonNull(requestHandler).removeCallbacksAndMessages(null);
      requestHandler = null;
      Util.castNonNull(requestHandlerThread).quit();
      requestHandlerThread = null;
      mediaCrypto = null;
      lastException = null;
      currentKeyRequest = null;
      currentProvisionRequest = null;
      if (sessionId != null) {
        mediaDrm.closeSession(sessionId);
        sessionId = null;
      }
      dispatchEvent(new Consumer<DrmSessionEventListener.EventDispatcher>() {
          @Override
          public void accept(DrmSessionEventListener.EventDispatcher eventDispatcher) {
              eventDispatcher.drmSessionReleased();
          }
      });
//      dispatchEvent(DrmSessionEventListener.EventDispatcher::drmSessionReleased);
    }
    if (eventDispatcher != null) {
      if (isOpen()) {
        // If the session is still open then send the release event only to the provided dispatcher
        // before removing it.
        eventDispatcher.drmSessionReleased();
      }
      eventDispatchers.remove(eventDispatcher);
    }
    referenceCountListener.onReferenceCountDecremented(this, referenceCount);
  }

  // Internal methods.

  /**
   * Try to open a session, do provisioning if necessary.
   *
   * @param allowProvisioning if provisioning is allowed, set this to false when calling from
   *     processing provision response.
   * @return true on success, false otherwise.
   */
  private boolean openInternal(boolean allowProvisioning) {
    if (isOpen()) {
      // Already opened
      return true;
    }

    try {
      sessionId = mediaDrm.openSession();
      mediaCrypto = mediaDrm.createMediaCrypto(sessionId);
      dispatchEvent(new Consumer<DrmSessionEventListener.EventDispatcher>() {
          @Override
          public void accept(DrmSessionEventListener.EventDispatcher eventDispatcher) {
              eventDispatcher.drmSessionAcquired();
          }
      });
//      dispatchEvent(DrmSessionEventListener.EventDispatcher::drmSessionAcquired);
      state = STATE_OPENED;
      Assertions.checkNotNull(sessionId);
      return true;
    } catch (NotProvisionedException e) {
      if (allowProvisioning) {
        provisioningManager.provisionRequired(this);
      } else {
        onError(e);
      }
    } catch (Exception e) {
      onError(e);
    }

    return false;
  }

  private void onProvisionResponse(Object request, Object response) {
    if (request != currentProvisionRequest || (state != STATE_OPENING && !isOpen())) {
      // This event is stale.
      return;
    }
    currentProvisionRequest = null;

    if (response instanceof Exception) {
      provisioningManager.onProvisionError((Exception) response);
      return;
    }

    try {
      mediaDrm.provideProvisionResponse((byte[]) response);
    } catch (Exception e) {
      provisioningManager.onProvisionError(e);
      return;
    }

    provisioningManager.onProvisionCompleted();
  }

  private void doLicense(boolean allowRetry) {
    if (isPlaceholderSession) {
      return;
    }
    byte[] sessionId = Util.castNonNull(this.sessionId);
    switch (mode) {
      case DefaultDrmSessionManager.MODE_PLAYBACK:
      case DefaultDrmSessionManager.MODE_QUERY:
        if (offlineLicenseKeySetId == null) {
          postKeyRequest(sessionId, ExoMediaDrm.KEY_TYPE_STREAMING, allowRetry);
        } else if (state == STATE_OPENED_WITH_KEYS || restoreKeys()) {
          long licenseDurationRemainingSec = getLicenseDurationRemainingSec();
          if (mode == DefaultDrmSessionManager.MODE_PLAYBACK
              && licenseDurationRemainingSec <= MAX_LICENSE_DURATION_TO_RENEW_SECONDS) {
            Log.d(
                TAG,
                "Offline license has expired or will expire soon. "
                    + "Remaining seconds: "
                    + licenseDurationRemainingSec);
            postKeyRequest(sessionId, ExoMediaDrm.KEY_TYPE_OFFLINE, allowRetry);
          } else if (licenseDurationRemainingSec <= 0) {
            onError(new KeysExpiredException());
          } else {
            state = STATE_OPENED_WITH_KEYS;
//            dispatchEvent(DrmSessionEventListener.EventDispatcher::drmKeysRestored);
            dispatchEvent(new Consumer<DrmSessionEventListener.EventDispatcher>() {
                @Override
                public void accept(DrmSessionEventListener.EventDispatcher eventDispatcher) {
                    eventDispatcher.drmKeysRestored();
                }
            });
          }
        }
        break;
      case DefaultDrmSessionManager.MODE_DOWNLOAD:
        if (offlineLicenseKeySetId == null || restoreKeys()) {
          postKeyRequest(sessionId, ExoMediaDrm.KEY_TYPE_OFFLINE, allowRetry);
        }
        break;
      case DefaultDrmSessionManager.MODE_RELEASE:
        Assertions.checkNotNull(offlineLicenseKeySetId);
        Assertions.checkNotNull(this.sessionId);
        // It's not necessary to restore the key before releasing it but this serves as a good
        // fast-failure check.
        if (restoreKeys()) {
          postKeyRequest(offlineLicenseKeySetId, ExoMediaDrm.KEY_TYPE_RELEASE, allowRetry);
        }
        break;
      default:
        break;
    }
  }

  private boolean restoreKeys() {
    try {
      mediaDrm.restoreKeys(sessionId, offlineLicenseKeySetId);
      return true;
    } catch (Exception e) {
      Log.e(TAG, "Error trying to restore keys.", e);
      onError(e);
    }
    return false;
  }

  private long getLicenseDurationRemainingSec() {
    if (!C.WIDEVINE_UUID.equals(uuid)) {
      return Long.MAX_VALUE;
    }
    Pair<Long, Long> pair =
        Assertions.checkNotNull(WidevineUtil.getLicenseDurationRemainingSec(this));
    return min(pair.first, pair.second);
  }

  private void postKeyRequest(byte[] scope, int type, boolean allowRetry) {
    try {
      currentKeyRequest = mediaDrm.getKeyRequest(scope, schemeDatas, type, keyRequestParameters);
      Util.castNonNull(requestHandler)
          .post(MSG_KEYS, Assertions.checkNotNull(currentKeyRequest), allowRetry);
    } catch (Exception e) {
      onKeysError(e);
    }
  }

  private void onKeyResponse(Object request, Object response) {
    if (request != currentKeyRequest || !isOpen()) {
      // This event is stale.
      return;
    }
    currentKeyRequest = null;

    if (response instanceof Exception) {
      onKeysError((Exception) response);
      return;
    }

    try {
      byte[] responseData = (byte[]) response;
      if (mode == DefaultDrmSessionManager.MODE_RELEASE) {
        mediaDrm.provideKeyResponse(Util.castNonNull(offlineLicenseKeySetId), responseData);
//        dispatchEvent(DrmSessionEventListener.EventDispatcher::drmKeysRemoved);
        dispatchEvent(new Consumer<DrmSessionEventListener.EventDispatcher>() {
            @Override
            public void accept(DrmSessionEventListener.EventDispatcher eventDispatcher) {
                eventDispatcher.drmKeysRemoved();
            }
        });
      } else {
        byte[] keySetId = mediaDrm.provideKeyResponse(sessionId, responseData);
        if ((mode == DefaultDrmSessionManager.MODE_DOWNLOAD
                || (mode == DefaultDrmSessionManager.MODE_PLAYBACK
                    && offlineLicenseKeySetId != null))
            && keySetId != null
            && keySetId.length != 0) {
          offlineLicenseKeySetId = keySetId;
        }
        state = STATE_OPENED_WITH_KEYS;
//        dispatchEvent(DrmSessionEventListener.EventDispatcher::drmKeysLoaded);
        dispatchEvent(new Consumer<DrmSessionEventListener.EventDispatcher>() {
            @Override
            public void accept(DrmSessionEventListener.EventDispatcher eventDispatcher) {
                eventDispatcher.drmKeysLoaded();
            }
        });
      }
    } catch (Exception e) {
      onKeysError(e);
    }
  }

  private void onKeysRequired() {
    if (mode == DefaultDrmSessionManager.MODE_PLAYBACK && state == STATE_OPENED_WITH_KEYS) {
      Util.castNonNull(sessionId);
      doLicense(/* allowRetry= */ false);
    }
  }

  private void onKeysError(Exception e) {
    if (e instanceof NotProvisionedException) {
      provisioningManager.provisionRequired(this);
    } else {
      onError(e);
    }
  }

  private void onError(final Exception e) {
    lastException = new DrmSessionException(e);
//    dispatchEvent(eventDispatcher -> eventDispatcher.drmSessionManagerError(e));
    dispatchEvent(new Consumer<DrmSessionEventListener.EventDispatcher>() {
        @Override
        public void accept(DrmSessionEventListener.EventDispatcher eventDispatcher) {
            eventDispatcher.drmSessionManagerError(e);
        }
    });
    if (state != STATE_OPENED_WITH_KEYS) {
      state = STATE_ERROR;
    }
  }

  @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
  private boolean isOpen() {
    return state == STATE_OPENED || state == STATE_OPENED_WITH_KEYS;
  }

  private void dispatchEvent(Consumer<DrmSessionEventListener.EventDispatcher> event) {
    for (DrmSessionEventListener.EventDispatcher eventDispatcher : eventDispatchers.elementSet()) {
      event.accept(eventDispatcher);
    }
  }

  // Internal classes.

  @SuppressLint("HandlerLeak")
  private class ResponseHandler extends Handler {

    public ResponseHandler(Looper looper) {
      super(looper);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMessage(Message msg) {
      Pair<Object, Object> requestAndResponse = (Pair<Object, Object>) msg.obj;
      Object request = requestAndResponse.first;
      Object response = requestAndResponse.second;
      switch (msg.what) {
        case MSG_PROVISION:
          onProvisionResponse(request, response);
          break;
        case MSG_KEYS:
          onKeyResponse(request, response);
          break;
        default:
          break;
      }
    }
  }

  @SuppressLint("HandlerLeak")
  private class RequestHandler extends Handler {

    public RequestHandler(Looper backgroundLooper) {
      super(backgroundLooper);
    }

    void post(int what, Object request, boolean allowRetry) {
      RequestTask requestTask =
          new RequestTask(
              LoadEventInfo.getNewId(),
              allowRetry,
              /* startTimeMs= */ SystemClock.elapsedRealtime(),
              request);
      obtainMessage(what, requestTask).sendToTarget();
    }

    @Override
    public void handleMessage(Message msg) {
      RequestTask requestTask = (RequestTask) msg.obj;
      Object response;
      try {
        switch (msg.what) {
          case MSG_PROVISION:
            response =
                callback.executeProvisionRequest(uuid, (ProvisionRequest) requestTask.request);
            break;
          case MSG_KEYS:
            response = callback.executeKeyRequest(uuid, (KeyRequest) requestTask.request);
            break;
          default:
            throw new RuntimeException();
        }
      } catch (MediaDrmCallbackException e) {
        if (maybeRetryRequest(msg, e)) {
          return;
        }
        response = e;
      } catch (Exception e) {
        Log.w(TAG, "Key/provisioning request produced an unexpected exception. Not retrying.", e);
        response = e;
      }
      loadErrorHandlingPolicy.onLoadTaskConcluded(requestTask.taskId);
      responseHandler
          .obtainMessage(msg.what, Pair.create(requestTask.request, response))
          .sendToTarget();
    }

    private boolean maybeRetryRequest(Message originalMsg, MediaDrmCallbackException exception) {
      RequestTask requestTask = (RequestTask) originalMsg.obj;
      if (!requestTask.allowRetry) {
        return false;
      }
      requestTask.errorCount++;
      if (requestTask.errorCount
          > loadErrorHandlingPolicy.getMinimumLoadableRetryCount(C.DATA_TYPE_DRM)) {
        return false;
      }
      LoadEventInfo loadEventInfo =
          new LoadEventInfo(
              requestTask.taskId,
              exception.dataSpec,
              exception.uriAfterRedirects,
              exception.responseHeaders,
              SystemClock.elapsedRealtime(),
              /* loadDurationMs= */ SystemClock.elapsedRealtime() - requestTask.startTimeMs,
              exception.bytesLoaded);
      MediaLoadData mediaLoadData = new MediaLoadData(C.DATA_TYPE_DRM);
      IOException loadErrorCause =
          exception.getCause() instanceof IOException
              ? (IOException) exception.getCause()
              : new UnexpectedDrmSessionException(exception.getCause());
      long retryDelayMs =
          loadErrorHandlingPolicy.getRetryDelayMsFor(
              new LoadErrorInfo(
                  loadEventInfo, mediaLoadData, loadErrorCause, requestTask.errorCount));
      if (retryDelayMs == C.TIME_UNSET) {
        // The error is fatal.
        return false;
      }
      sendMessageDelayed(Message.obtain(originalMsg), retryDelayMs);
      return true;
    }
  }

  private static final class RequestTask {

    public final long taskId;
    public final boolean allowRetry;
    public final long startTimeMs;
    public final Object request;
    public int errorCount;

    public RequestTask(long taskId, boolean allowRetry, long startTimeMs, Object request) {
      this.taskId = taskId;
      this.allowRetry = allowRetry;
      this.startTimeMs = startTimeMs;
      this.request = request;
    }
  }
}
