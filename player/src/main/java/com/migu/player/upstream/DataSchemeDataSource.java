/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.migu.player.upstream;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.google.common.base.Charsets;
import com.migu.player.C;
import com.migu.player.ParserException;
import com.migu.player.util.Util;

import java.io.IOException;
import java.net.URLDecoder;

import static com.migu.player.util.Util.castNonNull;
import static java.lang.Math.min;

/** A {@link DataSource} for reading data URLs, as defined by RFC 2397. */
public final class DataSchemeDataSource extends BaseDataSource {

  public static final String SCHEME_DATA = "data";

  @Nullable private DataSpec dataSpec;
  @Nullable private byte[] data;
  private int endPosition;
  private int readPosition;

  public DataSchemeDataSource() {
    super(/* isNetwork= */ false);
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    transferInitializing(dataSpec);
    this.dataSpec = dataSpec;
    readPosition = (int) dataSpec.position;
    Uri uri = dataSpec.uri;
    String scheme = uri.getScheme();
    if (!SCHEME_DATA.equals(scheme)) {
      throw new ParserException("Unsupported scheme: " + scheme);
    }
    String[] uriParts = Util.split(uri.getSchemeSpecificPart(), ",");
    if (uriParts.length != 2) {
      throw new ParserException("Unexpected URI format: " + uri);
    }
    String dataString = uriParts[1];
    if (uriParts[0].contains(";base64")) {
      try {
        // TODO(internal: b/169937045): Consider passing Base64.URL_SAFE flag.
        data = Base64.decode(dataString, /* flags= */ Base64.DEFAULT);
      } catch (IllegalArgumentException e) {
        throw new ParserException("Error while parsing Base64 encoded string: " + dataString, e);
      }
    } else {
      // TODO: Add support for other charsets.
      data = Util.getUtf8Bytes(URLDecoder.decode(dataString, Charsets.US_ASCII.name()));
    }
    endPosition =
        dataSpec.length != C.LENGTH_UNSET ? (int) dataSpec.length + readPosition : data.length;
    if (endPosition > data.length || readPosition > endPosition) {
      data = null;
      throw new DataSourceException(DataSourceException.POSITION_OUT_OF_RANGE);
    }
    transferStarted(dataSpec);
    return (long) endPosition - readPosition;
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) {
    if (readLength == 0) {
      return 0;
    }
    int remainingBytes = endPosition - readPosition;
    if (remainingBytes == 0) {
      return C.RESULT_END_OF_INPUT;
    }
    readLength = min(readLength, remainingBytes);
    System.arraycopy(castNonNull(data), readPosition, buffer, offset, readLength);
    readPosition += readLength;
    bytesTransferred(readLength);
    return readLength;
  }

  @Override
  @Nullable
  public Uri getUri() {
    return dataSpec != null ? dataSpec.uri : null;
  }

  @Override
  public void close() {
    if (data != null) {
      data = null;
      transferEnded();
    }
    dataSpec = null;
  }
}
