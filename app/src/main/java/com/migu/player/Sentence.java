package com.migu.player;

public class Sentence {
    private boolean isSuccess;
    public int startIndex;
    public int endIndex;//最后一个字的索引基础上加1 与下一句的startIndex相等
    public int tagIndex;//最后一个字的索引,比如一句有7个字 则tagIndex为6
    public boolean isSentenceEnd;
    public int contentCount = 0;
    public String mContent;
    public Sentence(int startIndex, int tagIndex, int endIndex, boolean isSentenceEnd, String content) {
        this.tagIndex = tagIndex;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.isSentenceEnd = isSentenceEnd;
        this.mContent = content;
        if (this.mContent != null) {
            this.contentCount = this.mContent.length();
        }
    }
    public boolean getIsSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getContent() {
        return mContent;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public int getTagIndex() {
        return tagIndex;
    }

    public void setTagIndex(int tagIndex) {
        this.tagIndex = tagIndex;
    }
}
