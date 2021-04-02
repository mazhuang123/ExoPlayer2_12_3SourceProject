/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.support.annotation.Nullable;

import com.google.common.primitives.Ints;
import com.migu.player.MediaItem;
import com.migu.player.drm.DefaultDrmSessionManager;
import com.migu.player.drm.DrmSessionManager;
import com.migu.player.drm.FrameworkMediaDrm;
import com.migu.player.drm.HttpMediaDrmCallback;
import com.migu.player.upstream.DefaultHttpDataSourceFactory;
import com.migu.player.upstream.HttpDataSource;
import com.migu.player.util.Assertions;
import com.migu.player.util.Util;

import java.util.Map;

import static com.migu.player.ExoPlayerLibraryInfo.DEFAULT_USER_AGENT;
import static com.migu.player.drm.DefaultDrmSessionManager.MODE_PLAYBACK;

/** A helper to create a {@link DrmSessionManager} from a {@link MediaItem}. */
public final class MediaSourceDrmHelper {

  @Nullable private HttpDataSource.Factory drmHttpDataSourceFactory;
  @Nullable private String userAgent;

  /**
   * Sets the {@link HttpDataSource.Factory} to be used for creating {@link HttpMediaDrmCallback
   * HttpMediaDrmCallbacks} which executes key and provisioning requests over HTTP. If {@code null}
   * is passed the {@link DefaultHttpDataSourceFactory} is used.
   *
   * @param drmHttpDataSourceFactory The HTTP data source factory or {@code null} to use {@link
   *     DefaultHttpDataSourceFactory}.
   */
  public void setDrmHttpDataSourceFactory(
      @Nullable HttpDataSource.Factory drmHttpDataSourceFactory) {
    this.drmHttpDataSourceFactory = drmHttpDataSourceFactory;
  }

  /**
   * Sets the optional user agent to be used for DRM requests.
   *
   * <p>In case a factory has been set by {@link
   * #setDrmHttpDataSourceFactory(HttpDataSource.Factory)}, this user agent is ignored.
   *
   * @param userAgent The user agent to be used for DRM requests.
   */
  public void setDrmUserAgent(@Nullable String userAgent) {
    this.userAgent = userAgent;
  }

  /** Creates a {@link DrmSessionManager} for the given media item. */
  public DrmSessionManager create(MediaItem mediaItem) {
    Assertions.checkNotNull(mediaItem.playbackProperties);
    @Nullable
    MediaItem.DrmConfiguration drmConfiguration = mediaItem.playbackProperties.drmConfiguration;
    if (drmConfiguration == null || Util.SDK_INT < 18) {
      return DrmSessionManager.getDummyDrmSessionManager();
    }
    HttpDataSource.Factory dataSourceFactory =
        drmHttpDataSourceFactory != null
            ? drmHttpDataSourceFactory
            : new DefaultHttpDataSourceFactory(userAgent != null ? userAgent : DEFAULT_USER_AGENT);
    HttpMediaDrmCallback httpDrmCallback =
        new HttpMediaDrmCallback(
            drmConfiguration.licenseUri == null ? null : drmConfiguration.licenseUri.toString(),
            drmConfiguration.forceDefaultLicenseUri,
            dataSourceFactory);
    for (Map.Entry<String, String> entry : drmConfiguration.requestHeaders.entrySet()) {
      httpDrmCallback.setKeyRequestProperty(entry.getKey(), entry.getValue());
    }
    DefaultDrmSessionManager drmSessionManager =
        new DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(
                drmConfiguration.uuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .setMultiSession(drmConfiguration.multiSession)
            .setPlayClearSamplesWithoutKeys(drmConfiguration.playClearContentWithoutKey)
            .setUseDrmSessionsForClearContent(Ints.toArray(drmConfiguration.sessionForClearTypes))
            .build(httpDrmCallback);
    drmSessionManager.setMode(MODE_PLAYBACK, drmConfiguration.getKeySetId());
    return drmSessionManager;
  }
}
