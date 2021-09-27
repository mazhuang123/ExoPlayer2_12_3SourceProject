package com.migu.player;

import android.text.TextUtils;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/7/1 15:38
 * @Description 文件描述：
 */

public class XttsStringUtil {
    public static String getUUID(String content) {
        if(content.length()<=8){
            return content;
        }
        return content.substring(0,8);
//        return UUID.nameUUIDFromBytes((content).getBytes()).toString().replaceAll("-", "");
    }
    /**
     * 判断某个句子中是否只有标点符号，返回true代表该句子全是标点符号
     * @param text
     * @return
     */
    public static boolean isAllMark(String text){
        String tempContent = text;
        tempContent = tempContent.replaceAll("\\u00a0","");//去除所有空格
        tempContent = tempContent.replaceAll("\\u0020","");
        tempContent = tempContent.replaceAll("\\u3000","");
        tempContent = tempContent.replaceAll("\\p{P}", "");
        return tempContent.length() == 0;
    }
    public static int removeMark(String tempContent){
        if(TextUtils.isEmpty(tempContent)){
            return 0;
        }
        tempContent = tempContent.replaceAll("\\u00a0","");//去除所有空格
        tempContent = tempContent.replaceAll("\\u0020","");
        tempContent = tempContent.replaceAll("\\u3000","");
        tempContent = tempContent.replaceAll("\\p{P}", "");
        return tempContent.length();
    }
}
