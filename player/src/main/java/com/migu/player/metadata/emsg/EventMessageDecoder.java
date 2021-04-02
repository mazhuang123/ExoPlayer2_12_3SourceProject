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
package com.migu.player.metadata.emsg;

import com.migu.player.metadata.Metadata;
import com.migu.player.metadata.MetadataInputBuffer;
import com.migu.player.metadata.SimpleMetadataDecoder;
import com.migu.player.util.Assertions;
import com.migu.player.util.ParsableByteArray;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Decodes data encoded by {@link EventMessageEncoder}. */
public final class EventMessageDecoder extends SimpleMetadataDecoder {

  @Override
  @SuppressWarnings("ByteBufferBackingArray") // Buffer validated by SimpleMetadataDecoder.decode
  protected Metadata decode(MetadataInputBuffer inputBuffer, ByteBuffer buffer) {
    return new Metadata(decode(new ParsableByteArray(buffer.array(), buffer.limit())));
  }

  public EventMessage decode(ParsableByteArray emsgData) {
    String schemeIdUri = Assertions.checkNotNull(emsgData.readNullTerminatedString());
    String value = Assertions.checkNotNull(emsgData.readNullTerminatedString());
    long durationMs = emsgData.readUnsignedInt();
    long id = emsgData.readUnsignedInt();
    byte[] messageData =
        Arrays.copyOfRange(emsgData.getData(), emsgData.getPosition(), emsgData.limit());
    return new EventMessage(schemeIdUri, value, durationMs, id, messageData);
  }
}
