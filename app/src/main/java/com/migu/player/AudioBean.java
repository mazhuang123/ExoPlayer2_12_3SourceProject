package com.migu.player;

import com.google.gson.Gson;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/8/4 11:44
 * @Description 文件描述：
 */

public class AudioBean {
    private transient byte[] resultBytes;
    private String content;
    private String sid;
    private String voiceName = "";
    public byte[] getResultBytes() {
        return resultBytes;
    }

    public void setResultBytes(byte[] resultBytes) {
        this.resultBytes = resultBytes;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
