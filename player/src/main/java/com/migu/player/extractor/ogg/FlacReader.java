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
package com.migu.player.extractor.ogg;

import android.support.annotation.Nullable;

import com.migu.player.extractor.ExtractorInput;
import com.migu.player.extractor.FlacFrameReader;
import com.migu.player.extractor.FlacMetadataReader;
import com.migu.player.extractor.FlacSeekTableSeekMap;
import com.migu.player.extractor.FlacStreamMetadata;
import com.migu.player.extractor.FlacStreamMetadata.SeekTable;
import com.migu.player.extractor.SeekMap;
import com.migu.player.util.Assertions;
import com.migu.player.util.FlacConstants;
import com.migu.player.util.ParsableByteArray;
import com.migu.player.util.Util;

import java.util.Arrays;

/**
 * {@link StreamReader} to extract Flac data out of Ogg byte stream.
 */
/* package */ final class FlacReader extends StreamReader {

  private static final byte AUDIO_PACKET_TYPE = (byte) 0xFF;

  private static final int FRAME_HEADER_SAMPLE_NUMBER_OFFSET = 4;

  @Nullable private FlacStreamMetadata streamMetadata;
  @Nullable private FlacOggSeeker flacOggSeeker;

  public static boolean verifyBitstreamType(ParsableByteArray data) {
    return data.bytesLeft() >= 5 && data.readUnsignedByte() == 0x7F && // packet type
        data.readUnsignedInt() == 0x464C4143; // ASCII signature "FLAC"
  }

  @Override
  protected void reset(boolean headerData) {
    super.reset(headerData);
    if (headerData) {
      streamMetadata = null;
      flacOggSeeker = null;
    }
  }

  private static boolean isAudioPacket(byte[] data) {
    return data[0] == AUDIO_PACKET_TYPE;
  }

  @Override
  protected long preparePayload(ParsableByteArray packet) {
    if (!isAudioPacket(packet.getData())) {
      return -1;
    }
    return getFlacFrameBlockSize(packet);
  }

  @Override
  protected boolean readHeaders(ParsableByteArray packet, long position, SetupData setupData) {
    byte[] data = packet.getData();
    @Nullable FlacStreamMetadata streamMetadata = this.streamMetadata;
    if (streamMetadata == null) {
      streamMetadata = new FlacStreamMetadata(data, 17);
      this.streamMetadata = streamMetadata;
      byte[] metadata = Arrays.copyOfRange(data, 9, packet.limit());
      setupData.format = streamMetadata.getFormat(metadata, /* id3Metadata= */ null);
    } else if ((data[0] & 0x7F) == FlacConstants.METADATA_TYPE_SEEK_TABLE) {
      SeekTable seekTable = FlacMetadataReader.readSeekTableMetadataBlock(packet);
      streamMetadata = streamMetadata.copyWithSeekTable(seekTable);
      this.streamMetadata = streamMetadata;
      flacOggSeeker = new FlacOggSeeker(streamMetadata, seekTable);
    } else if (isAudioPacket(data)) {
      if (flacOggSeeker != null) {
        flacOggSeeker.setFirstFrameOffset(position);
        setupData.oggSeeker = flacOggSeeker;
      }
      return false;
    }
    return true;
  }

  private int getFlacFrameBlockSize(ParsableByteArray packet) {
    int blockSizeKey = (packet.getData()[2] & 0xFF) >> 4;
    if (blockSizeKey == 6 || blockSizeKey == 7) {
      // Skip the sample number.
      packet.skipBytes(FRAME_HEADER_SAMPLE_NUMBER_OFFSET);
      packet.readUtf8EncodedLong();
    }
    int result = FlacFrameReader.readFrameBlockSizeSamplesFromKey(packet, blockSizeKey);
    packet.setPosition(0);
    return result;
  }

  private static final class FlacOggSeeker implements OggSeeker {

    private FlacStreamMetadata streamMetadata;
    private SeekTable seekTable;
    private long firstFrameOffset;
    private long pendingSeekGranule;

    public FlacOggSeeker(FlacStreamMetadata streamMetadata, SeekTable seekTable) {
      this.streamMetadata = streamMetadata;
      this.seekTable = seekTable;
      firstFrameOffset = -1;
      pendingSeekGranule = -1;
    }

    public void setFirstFrameOffset(long firstFrameOffset) {
      this.firstFrameOffset = firstFrameOffset;
    }

    @Override
    public long read(ExtractorInput input) {
      if (pendingSeekGranule >= 0) {
        long result = -(pendingSeekGranule + 2);
        pendingSeekGranule = -1;
        return result;
      }
      return -1;
    }

    @Override
    public void startSeek(long targetGranule) {
      long[] seekPointGranules = seekTable.pointSampleNumbers;
      int index =
          Util.binarySearchFloor(
              seekPointGranules, targetGranule, /* inclusive= */ true, /* stayInBounds= */ true);
      pendingSeekGranule = seekPointGranules[index];
    }

    @Override
    public SeekMap createSeekMap() {
      Assertions.checkState(firstFrameOffset != -1);
      return new FlacSeekTableSeekMap(streamMetadata, firstFrameOffset);
    }

  }

}
