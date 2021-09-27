package com.migu.player;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/8/30 17:39
 * @Description 文件描述：
 */

public class ContentAdapter extends BaseAdapter {
    private Context context;
    private List<String> contentList;
    public ContentAdapter(Context context) {
        this.context = context;
        contentList = new ArrayList<>();
    }
    public void setData(List<String> contentList){
        this.contentList.clear();
        this.contentList.addAll(contentList);
        notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return contentList.size();
    }

    @Override
    public Object getItem(int i) {
        return contentList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        TextView textView = new TextView(context);
        textView.setText(contentList.get(i));
        return textView;
    }
}
