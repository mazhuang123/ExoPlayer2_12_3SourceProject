/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.migu.player.source;

import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;

import com.migu.player.Timeline;
import com.migu.player.drm.DrmSessionEventListener;
import com.migu.player.upstream.TransferListener;
import com.migu.player.util.Assertions;
import com.migu.player.util.Util;

import java.io.IOException;
import java.util.HashMap;

/**
 * Composite {@link MediaSource} consisting of multiple child sources.
 *
 * @param <T> The type of the id used to identify prepared child sources.
 */
public abstract class CompositeMediaSource<T> extends BaseMediaSource {

  private final HashMap<T, MediaSourceAndListener> childSources;

  @Nullable private Handler eventHandler;
  @Nullable private TransferListener mediaTransferListener;

  /** Creates composite media source without child sources. */
  protected CompositeMediaSource() {
    childSources = new HashMap<>();
  }

  @Override
  @CallSuper
  protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
    this.mediaTransferListener = mediaTransferListener;
    eventHandler = Util.createHandlerForCurrentLooper();
  }

  @Override
  @CallSuper
  public void maybeThrowSourceInfoRefreshError() throws IOException {
    for (MediaSourceAndListener childSource : childSources.values()) {
      childSource.mediaSource.maybeThrowSourceInfoRefreshError();
    }
  }

  @Override
  @CallSuper
  protected void enableInternal() {
    for (MediaSourceAndListener childSource : childSources.values()) {
      childSource.mediaSource.enable(childSource.caller);
    }
  }

  @Override
  @CallSuper
  protected void disableInternal() {
    for (MediaSourceAndListener childSource : childSources.values()) {
      childSource.mediaSource.disable(childSource.caller);
    }
  }

  @Override
  @CallSuper
  protected void releaseSourceInternal() {
    for (MediaSourceAndListener childSource : childSources.values()) {
      childSource.mediaSource.releaseSource(childSource.caller);
      childSource.mediaSource.removeEventListener(childSource.eventListener);
    }
    childSources.clear();
  }

  /**
   * Called when the source info of a child source has been refreshed.
   *
   * @param id The unique id used to prepare the child source.
   * @param mediaSource The child source whose source info has been refreshed.
   * @param timeline The timeline of the child source.
   */
  protected abstract void onChildSourceInfoRefreshed(
       T id, MediaSource mediaSource, Timeline timeline);

  /**
   * Prepares a child source.
   *
   * <p>{@link #onChildSourceInfoRefreshed(Object, MediaSource, Timeline)} will be called when the
   * child source updates its timeline with the same {@code id} passed to this method.
   *
   * <p>Any child sources that aren't explicitly released with {@link #releaseChildSource(Object)}
   * will be released in {@link #releaseSourceInternal()}.
   *
   * @param id A unique id to identify the child source preparation. Null is allowed as an id.
   * @param mediaSource The child {@link MediaSource}.
   */
  protected final void prepareChildSource( T id, MediaSource mediaSource) {
    Assertions.checkArgument(!childSources.containsKey(id));
    MediaSourceCaller caller =
        (source, timeline) -> onChildSourceInfoRefreshed(id, source, timeline);
    ForwardingEventListener eventListener = new ForwardingEventListener(id);
    childSources.put(id, new MediaSourceAndListener(mediaSource, caller, eventListener));
    mediaSource.addEventListener(Assertions.checkNotNull(eventHandler), eventListener);
    mediaSource.addDrmEventListener(Assertions.checkNotNull(eventHandler), eventListener);
    mediaSource.prepareSource(caller, mediaTransferListener);
    if (!isEnabled()) {
      mediaSource.disable(caller);
    }
  }

  /**
   * Enables a child source.
   *
   * @param id The unique id used to prepare the child source.
   */
  protected final void enableChildSource( T id) {
    MediaSourceAndListener enabledChild = Assertions.checkNotNull(childSources.get(id));
    enabledChild.mediaSource.enable(enabledChild.caller);
  }

  /**
   * Disables a child source.
   *
   * @param id The unique id used to prepare the child source.
   */
  protected final void disableChildSource( T id) {
    MediaSourceAndListener disabledChild = Assertions.checkNotNull(childSources.get(id));
    disabledChild.mediaSource.disable(disabledChild.caller);
  }

  /**
   * Releases a child source.
   *
   * @param id The unique id used to prepare the child source.
   */
  protected final void releaseChildSource( T id) {
    MediaSourceAndListener removedChild = Assertions.checkNotNull(childSources.remove(id));
    removedChild.mediaSource.releaseSource(removedChild.caller);
    removedChild.mediaSource.removeEventListener(removedChild.eventListener);
  }

  /**
   * Returns the window index in the composite source corresponding to the specified window index in
   * a child source. The default implementation does not change the window index.
   *
   * @param id The unique id used to prepare the child source.
   * @param windowIndex A window index of the child source.
   * @return The corresponding window index in the composite source.
   */
  protected int getWindowIndexForChildWindowIndex( T id, int windowIndex) {
    return windowIndex;
  }

  /**
   * Returns the {@link MediaPeriodId} in the composite source corresponding to the specified {@link
   * MediaPeriodId} in a child source. The default implementation does not change the media period
   * id.
   *
   * @param id The unique id used to prepare the child source.
   * @param mediaPeriodId A {@link MediaPeriodId} of the child source.
   * @return The corresponding {@link MediaPeriodId} in the composite source. Null if no
   *     corresponding media period id can be determined.
   */
  @Nullable
  protected MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(
       T id, MediaPeriodId mediaPeriodId) {
    return mediaPeriodId;
  }

  /**
   * Returns the media time in the composite source corresponding to the specified media time in a
   * child source. The default implementation does not change the media time.
   *
   * @param id The unique id used to prepare the child source.
   * @param mediaTimeMs A media time of the child source, in milliseconds.
   * @return The corresponding media time in the composite source, in milliseconds.
   */
  protected long getMediaTimeForChildMediaTime( T id, long mediaTimeMs) {
    return mediaTimeMs;
  }

  private static final class MediaSourceAndListener {

    public final MediaSource mediaSource;
    public final MediaSourceCaller caller;
    public final MediaSourceEventListener eventListener;

    public MediaSourceAndListener(
        MediaSource mediaSource, MediaSourceCaller caller, MediaSourceEventListener eventListener) {
      this.mediaSource = mediaSource;
      this.caller = caller;
      this.eventListener = eventListener;
    }
  }

  private final class ForwardingEventListener
      implements MediaSourceEventListener, DrmSessionEventListener {

     private final T id;
    private MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher;
    private DrmSessionEventListener.EventDispatcher drmEventDispatcher;

    public ForwardingEventListener( T id) {
      this.mediaSourceEventDispatcher = createEventDispatcher(/* mediaPeriodId= */ null);
      this.drmEventDispatcher = createDrmEventDispatcher(/* mediaPeriodId= */ null);
      this.id = id;
    }

    // MediaSourceEventListener implementation

    @Override
    public void onLoadStarted(
        int windowIndex,
        @Nullable MediaPeriodId mediaPeriodId,
        LoadEventInfo loadEventData,
        MediaLoadData mediaLoadData) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        mediaSourceEventDispatcher.loadStarted(
            loadEventData, maybeUpdateMediaLoadData(mediaLoadData));
      }
    }

    @Override
    public void onLoadCompleted(
        int windowIndex,
        @Nullable MediaPeriodId mediaPeriodId,
        LoadEventInfo loadEventData,
        MediaLoadData mediaLoadData) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        mediaSourceEventDispatcher.loadCompleted(
            loadEventData, maybeUpdateMediaLoadData(mediaLoadData));
      }
    }

    @Override
    public void onLoadCanceled(
        int windowIndex,
        @Nullable MediaPeriodId mediaPeriodId,
        LoadEventInfo loadEventData,
        MediaLoadData mediaLoadData) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        mediaSourceEventDispatcher.loadCanceled(
            loadEventData, maybeUpdateMediaLoadData(mediaLoadData));
      }
    }

    @Override
    public void onLoadError(
        int windowIndex,
        @Nullable MediaPeriodId mediaPeriodId,
        LoadEventInfo loadEventData,
        MediaLoadData mediaLoadData,
        IOException error,
        boolean wasCanceled) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        mediaSourceEventDispatcher.loadError(
            loadEventData, maybeUpdateMediaLoadData(mediaLoadData), error, wasCanceled);
      }
    }

    @Override
    public void onUpstreamDiscarded(
        int windowIndex, @Nullable MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        mediaSourceEventDispatcher.upstreamDiscarded(maybeUpdateMediaLoadData(mediaLoadData));
      }
    }

    @Override
    public void onDownstreamFormatChanged(
        int windowIndex, @Nullable MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        mediaSourceEventDispatcher.downstreamFormatChanged(maybeUpdateMediaLoadData(mediaLoadData));
      }
    }

    // DrmSessionEventListener implementation

    @Override
    public void onDrmSessionAcquired(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        drmEventDispatcher.drmSessionAcquired();
      }
    }

    @Override
    public void onDrmKeysLoaded(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        drmEventDispatcher.drmKeysLoaded();
      }
    }

    @Override
    public void onDrmSessionManagerError(
        int windowIndex, @Nullable MediaPeriodId mediaPeriodId, Exception error) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        drmEventDispatcher.drmSessionManagerError(error);
      }
    }

    @Override
    public void onDrmKeysRestored(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        drmEventDispatcher.drmKeysRestored();
      }
    }

    @Override
    public void onDrmKeysRemoved(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        drmEventDispatcher.drmKeysRemoved();
      }
    }

    @Override
    public void onDrmSessionReleased(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
      if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
        drmEventDispatcher.drmSessionReleased();
      }
    }

    /** Updates the event dispatcher and returns whether the event should be dispatched. */
    private boolean maybeUpdateEventDispatcher(
        int childWindowIndex, @Nullable MediaPeriodId childMediaPeriodId) {
      @Nullable MediaPeriodId mediaPeriodId = null;
      if (childMediaPeriodId != null) {
        mediaPeriodId = getMediaPeriodIdForChildMediaPeriodId(id, childMediaPeriodId);
        if (mediaPeriodId == null) {
          // Media period not found. Ignore event.
          return false;
        }
      }
      int windowIndex = getWindowIndexForChildWindowIndex(id, childWindowIndex);
      if (mediaSourceEventDispatcher.windowIndex != windowIndex
          || !Util.areEqual(mediaSourceEventDispatcher.mediaPeriodId, mediaPeriodId)) {
        mediaSourceEventDispatcher =
            createEventDispatcher(windowIndex, mediaPeriodId, /* mediaTimeOffsetMs= */ 0);
      }
      if (drmEventDispatcher.windowIndex != windowIndex
          || !Util.areEqual(drmEventDispatcher.mediaPeriodId, mediaPeriodId)) {
        drmEventDispatcher = createDrmEventDispatcher(windowIndex, mediaPeriodId);
      }
      return true;
    }

    private MediaLoadData maybeUpdateMediaLoadData(MediaLoadData mediaLoadData) {
      long mediaStartTimeMs = getMediaTimeForChildMediaTime(id, mediaLoadData.mediaStartTimeMs);
      long mediaEndTimeMs = getMediaTimeForChildMediaTime(id, mediaLoadData.mediaEndTimeMs);
      if (mediaStartTimeMs == mediaLoadData.mediaStartTimeMs
          && mediaEndTimeMs == mediaLoadData.mediaEndTimeMs) {
        return mediaLoadData;
      }
      return new MediaLoadData(
          mediaLoadData.dataType,
          mediaLoadData.trackType,
          mediaLoadData.trackFormat,
          mediaLoadData.trackSelectionReason,
          mediaLoadData.trackSelectionData,
          mediaStartTimeMs,
          mediaEndTimeMs);
    }
  }
}
