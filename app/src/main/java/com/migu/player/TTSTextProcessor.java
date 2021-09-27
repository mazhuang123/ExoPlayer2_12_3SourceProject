package com.migu.player;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2020/12/21 14:30
 * @Description 文件描述：https://blog.csdn.net/expect521/article/details/107107802
 */
public class TTSTextProcessor {
    private int MAX_SIZE = 150;//一句最多字符数量,超过此字符数要做拆分处理
    private static final int MIN_SIZE = 20;//一句最少的字符数量,低于此字符数,不做处理
    private static final int SENTENCE_UNIT = 4;//要把句子几等分
    private static final int FIRST_SENTENCE_MAX_SIZE = 20;
    private static final int FIRST_SENTENCE_MIN_SIZE = 5;
    private static final int FIRST_SENTENCE_UNIT = 2;//需要将第一句几等分
    protected static final String SENTENCE_BREAK = "([\r\n]+)|((\\.)+$)|([\\.，。；：？！…～—,;:?!．]+[）}】」～’》”\\)〉｝>］\" \t\f]*([\r\n]+)?)|(\\.+[）}】」～’》”\\)〉｝>］\" \t\f]+([\r\n]+)?)";
    protected static final String SENTENCE_BREAK_LONG = "([\r\n]+)|((\\.)+$)|([。；？！～;?!]+[）}】」～’》”\\)〉｝>］\" \t\f]*([\r\n]+)?)|(\\.+[）}】」～’》”\\)〉｝>］\" \t\f]+([\r\n]+)?)";
    protected static final Pattern Sentence_PATTER = Pattern.compile(SENTENCE_BREAK);
    protected static final Pattern Sentence_PATTER_LONG = Pattern.compile(SENTENCE_BREAK_LONG);

    public TTSTextProcessor() {
    }


    public List<Sentence> startBreak(String content){
        List<Sentence> resultList = new ArrayList<>();
        int realLength = XttsStringUtil.removeMark(content);
        if(realLength<20){//去除文本中的所有标点符号后文本长度如果小于20
            Sentence sentence = new Sentence(0,content.length()-1,content.length(),false,content);
            resultList.add(sentence);
            return resultList;
        }
        List<Sentence> sentenceList = breakSentence(content, false);
        if(sentenceList.size()==1){//出现拆分结果为1的情况有两种:1.全是文本,一个标点符号都没有 2.全是标点符号,一个文本都没有
            String firstContent = sentenceList.get(0).getContent();
            if(XttsStringUtil.isAllMark(firstContent)){
                return sentenceList;
            }
            if(firstContent.length() > MAX_SIZE){
                resultList = splitUnit(sentenceList.get(0),2);
                return resultList;
            }
            return sentenceList;
        }
        //走到这里说明拆分的集合大小肯定大于1 是文本和标点符号掺杂在一起
        //就需要对第一句单独处理,挨个拼接,直到文本长度大于10为止,
        // 当然,没有对拼接后的文本做最大长度判断,所以有可能出现拼接后文本长度有一两百字的情况
        //此为极端情况,需要耗时实属正常

        StringBuilder stringBuilder =  new StringBuilder();
        for(int i = 0;i<sentenceList.size();i++){
            Sentence currentSentence = sentenceList.get(i);
            String currentSentenceContent = currentSentence.getContent();
            stringBuilder.append(currentSentenceContent);
            if(!isNeedAppend(stringBuilder.toString())){
                Sentence resultFirstSentence = new Sentence(0,currentSentence.getTagIndex(),currentSentence.getEndIndex(),false,stringBuilder.toString());
                sentenceList.add(resultFirstSentence);
                resultList.add(0,resultFirstSentence);
                break;
            }
        }
        //再对除第一句剩余的部分做分割处理
        Sentence lastSentence = resultList.get(resultList.size() - 1);
        int firstSentenceEndIndex = lastSentence.getEndIndex();
        //有可能出现拼接完第一句后,后续就没有文本了,这里就要做下判断
        if(firstSentenceEndIndex>=content.length()){
            return resultList;
        }
        String leftContent = content.substring(firstSentenceEndIndex);
        List<Sentence> leftSentences = startBreakByEndPoint(leftContent);
        reloadIndex(firstSentenceEndIndex,leftSentences);
        Sentence resultLastSentence = leftSentences.get(leftSentences.size()-1);
        resultList.addAll(leftSentences);
        if(XttsStringUtil.isAllMark(resultLastSentence.getContent())){ //针对按句号分割后,出现最后一句是个逗号的情况,比如"天气。,"这样就会拆成两句"天气。"和","
            Sentence resultLastSecondSentence = leftSentences.get(leftSentences.size()-2);
            StringBuilder stringBuilder1 = new StringBuilder();
            stringBuilder1.append(resultLastSecondSentence.getContent());
            stringBuilder1.append(resultLastSentence.getContent());
            int startIndex = resultLastSecondSentence.getStartIndex();
            int tagIndex = resultLastSentence.getTagIndex();
            int endIndex = resultLastSentence.getEndIndex();
            Sentence sentence = new Sentence(startIndex,tagIndex,endIndex,true,stringBuilder1.toString());
            resultList.remove(resultLastSentence);
            resultList.remove(resultLastSecondSentence);
            resultList.add(sentence);
        }
        return resultList;
    }
    private boolean isNeedAppend(String content){
        int tempContentLength = XttsStringUtil.removeMark(content);
        return XttsStringUtil.isAllMark(content) || tempContentLength<10;
    }

    /**
     * 以句号分割句子
     * @param text
     * @return
     */
    public List<Sentence> startBreakByEndPoint(String text){
        List<Sentence> resultList = new ArrayList<>();
        List<Sentence> endPointSentenceList= breakSentence(text,true);
        for(int i=0;i<endPointSentenceList.size();i++){
            Sentence sentence = endPointSentenceList.get(i);
            if(sentence.getContent().length()<20){
                resultList.add(sentence);
                continue;
            }
            List<Sentence> sentenceList = dealLongSentence(sentence);
            resultList.addAll(sentenceList);
        }
        return resultList;
    }

    /**
     * 将集合中 全是标点符号的分句移除
     * @param sentenceList
     */
    private void deleteAllMarkFromList(List<Sentence> sentenceList){
        Iterator<Sentence> sentenceIterator = sentenceList.iterator();
        while (sentenceIterator.hasNext()) {
            Sentence sentence = sentenceIterator.next();
            if (XttsStringUtil.isAllMark(sentence.getContent())) {
                sentenceIterator.remove();
            }
        }
    }
    /**
     * 对每一句进行处理,超过规定字符数时:直接对半分
     *
     * @return
     */
    private List<Sentence> dealLongSentence(Sentence firstSentence) {
        List<Sentence> resultSentenceList = new ArrayList<>();
        if (firstSentence.getContent().length() > MAX_SIZE) {
            List<Sentence> sentences41List = splitUnit(firstSentence,2);
            resultSentenceList.addAll(sentences41List);
        } else {
            resultSentenceList.add(firstSentence);
        }
        return resultSentenceList;
    }

    /**
     * 给每一个句子的索引重新复制:在原先基础上加上startIndex
     * @param startIndex
     * @param firstSentenceList
     */
    private static void reloadIndex(int startIndex, List<Sentence> firstSentenceList) {
        for(Sentence sentence : firstSentenceList){
            sentence.startIndex+=startIndex;
            sentence.endIndex+=startIndex;
            sentence.tagIndex+=startIndex;
        }
    }

    /**
     * 对没有标点符号的长整句做1/4切割
     *
     * @param sentence
     * @return
     */
    private List<Sentence> splitUnit(Sentence sentence,int unitNum) {
        List<Sentence> resultSentenceList = new ArrayList<>();
        String content = sentence.getContent();
        int length = content.length();
        int unitLength = length / unitNum;
        int leftLength = length % unitNum;
        int tempStartIndex = sentence.getStartIndex();
        String tempContent = null;
        Sentence tempSentence = null;
        for (int i = 0; i < unitNum; i++) {
            int tempEndIndex = tempStartIndex + unitLength;
            int tempTagIndex = tempEndIndex - 1;
            int startSubIndex = i * unitLength;
            int stopSubIndex = (i + 1) * unitLength;
            tempContent = content.substring(startSubIndex, stopSubIndex);
            tempSentence = new Sentence(tempStartIndex, tempTagIndex, tempEndIndex, false, tempContent);
            tempStartIndex = tempEndIndex;
            if (i == unitNum-1) {
                tempEndIndex = tempStartIndex + leftLength;
                tempTagIndex = tempEndIndex - 1;
                tempContent = content.substring(startSubIndex);
                tempSentence = new Sentence(tempStartIndex, tempTagIndex, tempEndIndex, false, tempContent);
            }
            resultSentenceList.add(tempSentence);
        }
        return resultSentenceList;
    }

    /**
     * 根据正则表达式拆分句子
     *
     * @param content    文本内容
     * @param isEndPoint 是否以句号拆分
     * @return
     */
    public List<Sentence> breakSentence(String content, boolean isEndPoint) {
        int limit = 150;
        List<Sentence> sentences = new ArrayList<>();
        if (content != null && content.length() > 0) {
            sentences = new ArrayList<>();
            Matcher Sentence_MATCHER;
            if (isEndPoint) {
                Sentence_MATCHER = Sentence_PATTER_LONG.matcher(content);
            } else {
                Sentence_MATCHER = Sentence_PATTER.matcher(content);
            }
            int contentSize = content.length();
            int startIndex = 0;

            while (true) {
                int tagIndex;
                int endIndex;
                while (Sentence_MATCHER.find()) {
                    int sentenceStart = Sentence_MATCHER.start();
                    int sentenceEnd = Sentence_MATCHER.end();
                    if (limit > 0 && sentenceEnd - sentenceStart >= limit) {
                        tagIndex = sentenceStart;

                        for (endIndex = sentenceStart + limit; tagIndex < sentenceEnd; endIndex += limit) {
                            if (endIndex > sentenceEnd) {
                                endIndex = sentenceEnd;
                            }

                            String sentence = content.substring(startIndex, endIndex);
                            sentences.add(new Sentence(startIndex, tagIndex, endIndex, true, sentence));
                            startIndex = endIndex;
                            tagIndex = endIndex;
                        }
                    } else {
                        sentences.add(new Sentence(startIndex, sentenceStart, sentenceEnd,
                                true, content.substring(startIndex, sentenceEnd)));
                        startIndex = sentenceEnd;
                    }
                }

                if (startIndex < contentSize) {
                    if (limit > 0 && contentSize - startIndex >= limit) {
                        tagIndex = startIndex;
                        endIndex = startIndex + limit;

                        for (boolean isSentenceEnd = true; tagIndex < contentSize; endIndex += limit) {
                            if (endIndex > contentSize) {
                                endIndex = contentSize;
                                isSentenceEnd = false;
                            }

                            String sentence = content.substring(startIndex, endIndex);
                            sentences.add(new Sentence(startIndex, tagIndex, endIndex, isSentenceEnd, sentence));
                            startIndex = endIndex;
                            tagIndex = endIndex;
                        }
                    } else {
                        sentences.add(new Sentence(startIndex, startIndex, contentSize, false, content.substring(startIndex)));
                    }
                }
                break;
            }
        }
        return sentences;
    }



}

