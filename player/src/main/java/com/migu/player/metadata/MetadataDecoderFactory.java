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
package com.migu.player.metadata;

import android.support.annotation.Nullable;

import com.migu.player.Format;
import com.migu.player.metadata.dvbsi.AppInfoTableDecoder;
import com.migu.player.metadata.emsg.EventMessageDecoder;
import com.migu.player.metadata.icy.IcyDecoder;
import com.migu.player.metadata.id3.Id3Decoder;
import com.migu.player.metadata.scte35.SpliceInfoDecoder;
import com.migu.player.util.MimeTypes;

/**
 * A factory for {@link MetadataDecoder} instances.
 */
public interface MetadataDecoderFactory {

  /**
   * Returns whether the factory is able to instantiate a {@link MetadataDecoder} for the given
   * {@link Format}.
   *
   * @param format The {@link Format}.
   * @return Whether the factory can instantiate a suitable {@link MetadataDecoder}.
   */
  boolean supportsFormat(Format format);

  /**
   * Creates a {@link MetadataDecoder} for the given {@link Format}.
   *
   * @param format The {@link Format}.
   * @return A new {@link MetadataDecoder}.
   * @throws IllegalArgumentException If the {@link Format} is not supported.
   */
  MetadataDecoder createDecoder(Format format);

  /**
   * Default {@link MetadataDecoder} implementation.
   *
   * <p>The formats supported by this factory are:
   *
   * <ul>
   *   <li>ID3 ({@link Id3Decoder})
   *   <li>EMSG ({@link EventMessageDecoder})
   *   <li>SCTE-35 ({@link SpliceInfoDecoder})
   *   <li>ICY ({@link IcyDecoder})
   * </ul>
   */
  MetadataDecoderFactory DEFAULT =
      new MetadataDecoderFactory() {

        @Override
        public boolean supportsFormat(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          return MimeTypes.APPLICATION_ID3.equals(mimeType)
              || MimeTypes.APPLICATION_EMSG.equals(mimeType)
              || MimeTypes.APPLICATION_SCTE35.equals(mimeType)
              || MimeTypes.APPLICATION_ICY.equals(mimeType)
              || MimeTypes.APPLICATION_AIT.equals(mimeType);
        }

        @Override
        public MetadataDecoder createDecoder(Format format) {
          @Nullable String mimeType = format.sampleMimeType;
          if (mimeType != null) {
            switch (mimeType) {
              case MimeTypes.APPLICATION_ID3:
                return new Id3Decoder();
              case MimeTypes.APPLICATION_EMSG:
                return new EventMessageDecoder();
              case MimeTypes.APPLICATION_SCTE35:
                return new SpliceInfoDecoder();
              case MimeTypes.APPLICATION_ICY:
                return new IcyDecoder();
              case MimeTypes.APPLICATION_AIT:
                return new AppInfoTableDecoder();
              default:
                break;
            }
          }
          throw new IllegalArgumentException(
              "Attempted to create decoder for unsupported MIME type: " + mimeType);
        }
      };
}
