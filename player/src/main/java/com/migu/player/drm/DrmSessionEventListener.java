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
package com.migu.player.drm;

import android.os.Handler;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;

import com.migu.player.Player;
import com.migu.player.source.MediaSource.MediaPeriodId;
import com.migu.player.util.Assertions;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.migu.player.util.Util.postOrRun;

/** Listener of {@link DrmSessionManager} events. */
public interface DrmSessionEventListener {

  /**
   * Called each time a drm session is acquired.
   *
   * @param windowIndex The window index in the timeline this media period belongs to.
   * @param mediaPeriodId The {@link MediaPeriodId} associated with the drm session.
   */
  default void onDrmSessionAcquired(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {}

  /**
   * Called each time keys are loaded.
   *
   * @param windowIndex The window index in the timeline this media period belongs to.
   * @param mediaPeriodId The {@link MediaPeriodId} associated with the drm session.
   */
  default void onDrmKeysLoaded(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {}

  /**
   * Called when a drm error occurs.
   *
   * <p>This method being called does not indicate that playback has failed, or that it will fail.
   * The player may be able to recover from the error and continue. Hence applications should
   * <em>not</em> implement this method to display a user visible error or initiate an application
   * level retry ({@link Player.EventListener#onPlayerError} is the appropriate place to implement
   * such behavior). This method is called to provide the application with an opportunity to log the
   * error if it wishes to do so.
   *
   * @param windowIndex The window index in the timeline this media period belongs to.
   * @param mediaPeriodId The {@link MediaPeriodId} associated with the drm session.
   * @param error The corresponding exception.
   */
  default void onDrmSessionManagerError(
          int windowIndex, @Nullable MediaPeriodId mediaPeriodId, Exception error) {}

  /**
   * Called each time offline keys are restored.
   *
   * @param windowIndex The window index in the timeline this media period belongs to.
   * @param mediaPeriodId The {@link MediaPeriodId} associated with the drm session.
   */
  default void onDrmKeysRestored(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {}

  /**
   * Called each time offline keys are removed.
   *
   * @param windowIndex The window index in the timeline this media period belongs to.
   * @param mediaPeriodId The {@link MediaPeriodId} associated with the drm session.
   */
  default void onDrmKeysRemoved(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {}

  /**
   * Called each time a drm session is released.
   *
   * @param windowIndex The window index in the timeline this media period belongs to.
   * @param mediaPeriodId The {@link MediaPeriodId} associated with the drm session.
   */
  default void onDrmSessionReleased(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {}

  /** Dispatches events to {@link DrmSessionEventListener DrmSessionEventListeners}. */
  class EventDispatcher {

    /** The timeline window index reported with the events. */
    public final int windowIndex;
    /** The {@link MediaPeriodId} reported with the events. */
    @Nullable public final MediaPeriodId mediaPeriodId;

    private final CopyOnWriteArrayList<ListenerAndHandler> listenerAndHandlers;

    /** Creates an event dispatcher. */
    public EventDispatcher() {
      this(
          /* listenerAndHandlers= */ new CopyOnWriteArrayList<>(),
          /* windowIndex= */ 0,
          /* mediaPeriodId= */ null);
    }

    private EventDispatcher(
        CopyOnWriteArrayList<ListenerAndHandler> listenerAndHandlers,
        int windowIndex,
        @Nullable MediaPeriodId mediaPeriodId) {
      this.listenerAndHandlers = listenerAndHandlers;
      this.windowIndex = windowIndex;
      this.mediaPeriodId = mediaPeriodId;
    }

    /**
     * Creates a view of the event dispatcher with the provided window index and media period id.
     *
     * @param windowIndex The timeline window index to be reported with the events.
     * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events.
     * @return A view of the event dispatcher with the pre-configured parameters.
     */
    @CheckResult
    public EventDispatcher withParameters(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
      return new EventDispatcher(listenerAndHandlers, windowIndex, mediaPeriodId);
    }

    /**
     * Adds a listener to the event dispatcher.
     *
     * @param handler A handler on the which listener events will be posted.
     * @param eventListener The listener to be added.
     */
    public void addEventListener(Handler handler, DrmSessionEventListener eventListener) {
      Assertions.checkNotNull(handler);
      Assertions.checkNotNull(eventListener);
      listenerAndHandlers.add(new ListenerAndHandler(handler, eventListener));
    }

    /**
     * Removes a listener from the event dispatcher.
     *
     * @param eventListener The listener to be removed.
     */
    public void removeEventListener(DrmSessionEventListener eventListener) {
      for (ListenerAndHandler listenerAndHandler : listenerAndHandlers) {
        if (listenerAndHandler.listener == eventListener) {
          listenerAndHandlers.remove(listenerAndHandler);
        }
      }
    }

    /** Dispatches {@link #onDrmSessionAcquired(int, MediaPeriodId)}. */
    public void drmSessionAcquired() {
      for (ListenerAndHandler listenerAndHandler : listenerAndHandlers) {
        DrmSessionEventListener listener = listenerAndHandler.listener;
        postOrRun(listenerAndHandler.handler, new Runnable() {
                    @Override
                    public void run() {
                        listener.onDrmSessionAcquired(windowIndex, mediaPeriodId);
                    }
                });
      }
    }

    /** Dispatches {@link #onDrmKeysLoaded(int, MediaPeriodId)}. */
    public void drmKeysLoaded() {
      for (ListenerAndHandler listenerAndHandler : listenerAndHandlers) {
        DrmSessionEventListener listener = listenerAndHandler.listener;
        postOrRun(listenerAndHandler.handler, new Runnable() {
            @Override
            public void run() {
                listener.onDrmKeysLoaded(windowIndex, mediaPeriodId);
            }
        });
      }
    }

    /** Dispatches {@link #onDrmSessionManagerError(int, MediaPeriodId, Exception)}. */
    public void drmSessionManagerError(Exception error) {
      for (ListenerAndHandler listenerAndHandler : listenerAndHandlers) {
        DrmSessionEventListener listener = listenerAndHandler.listener;
        postOrRun(listenerAndHandler.handler, new Runnable() {
            @Override
            public void run() {
                listener.onDrmSessionManagerError(windowIndex, mediaPeriodId, error);
            }
        });
      }
    }

    /** Dispatches {@link #onDrmKeysRestored(int, MediaPeriodId)}. */
    public void drmKeysRestored() {
      for (ListenerAndHandler listenerAndHandler : listenerAndHandlers) {
        DrmSessionEventListener listener = listenerAndHandler.listener;
          postOrRun(listenerAndHandler.handler, new Runnable() {
              @Override
              public void run() {
                  listener.onDrmKeysRestored(windowIndex, mediaPeriodId);
              }
          });
      }
    }

    /** Dispatches {@link #onDrmKeysRemoved(int, MediaPeriodId)}. */
    public void drmKeysRemoved() {
      for (ListenerAndHandler listenerAndHandler : listenerAndHandlers) {
        DrmSessionEventListener listener = listenerAndHandler.listener;
          postOrRun(listenerAndHandler.handler, new Runnable() {
              @Override
              public void run() {
                  listener.onDrmKeysRemoved(windowIndex, mediaPeriodId);
              }
          });
      }
    }

    /** Dispatches {@link #onDrmSessionReleased(int, MediaPeriodId)}. */
    public void drmSessionReleased() {
      for (ListenerAndHandler listenerAndHandler : listenerAndHandlers) {
        DrmSessionEventListener listener = listenerAndHandler.listener;
          postOrRun(listenerAndHandler.handler, new Runnable() {
              @Override
              public void run() {
                  listener.onDrmSessionReleased(windowIndex, mediaPeriodId);
              }
          });
      }
    }

    private static final class ListenerAndHandler {

      public Handler handler;
      public DrmSessionEventListener listener;

      public ListenerAndHandler(Handler handler, DrmSessionEventListener listener) {
        this.handler = handler;
        this.listener = listener;
      }
    }
  }
}
