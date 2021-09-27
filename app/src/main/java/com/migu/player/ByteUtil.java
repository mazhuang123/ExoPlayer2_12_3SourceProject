package com.migu.player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/6/29 17:25
 * @Description 文件描述：
 */

public class ByteUtil {

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }
    public static byte[] transferByte(ArrayList<byte[]> arrayList) {
        try {
            int audioDataLen = 0;
            for(byte[] dataArray : arrayList){
                audioDataLen+=dataArray.length;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] headByte = writeHeader(audioDataLen);
            byteArrayOutputStream.write(headByte);
            for (byte[] tempData : arrayList) {
                byteArrayOutputStream.write(tempData);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static byte[] writeHeader(long dataLength){
        long sampleRate = 16000;
        int channels = 1;
        long pcmDataLen = dataLength;
        long wavDataLen = pcmDataLen + 44;
        long bitsPerChannel = 16;
        long byteRate = sampleRate * channels * (bitsPerChannel >> 3);

        byte[] header = new byte[44];

        // RIFF/WAVE header
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte)(wavDataLen & 0xff);
        header[5] = (byte)((wavDataLen >> 8) & 0xff);
        header[6] = (byte)((wavDataLen >> 16) & 0xff);
        header[7] = (byte)((wavDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // 'fmt' chunk
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // 4 bytes, size of 'fmt' chunk
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // format
        header[20] = 1; // framesPerPacket, pcm格式为1
        header[21] = 0;
        // channels
        header[22] = (byte)channels;
        header[23] = 0;
        // sampleRate
        header[24] = (byte)(sampleRate & 0xff);
        header[25] = (byte)((sampleRate >> 8) & 0xff);
        header[26] = (byte)((sampleRate >> 16) & 0xff);
        header[27] = (byte)((sampleRate >> 24) & 0xff);
        // byteRate
        header[28] = (byte)(byteRate & 0xff);
        header[29] = (byte)((byteRate >> 8) & 0xff);
        header[30] = (byte)((byteRate >> 16) & 0xff);
        header[31] = (byte)((byteRate >> 24) & 0xff);
        // block align
        header[32] = (byte) (channels * (bitsPerChannel >> 3)); // bytesPerFrame
        header[33] = 0;
        // bits per sample
        header[34] = 16;
        header[35] = 0;
        // data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte)(pcmDataLen & 0xff);
        header[41] = (byte)((pcmDataLen >> 8) & 0xff);
        header[42] = (byte)((pcmDataLen >> 16) & 0xff);
        header[43] = (byte)((pcmDataLen >> 24) & 0xff);
        return header;

    }
}
