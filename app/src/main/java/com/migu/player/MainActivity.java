package com.migu.player;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private String content = "";
    private ExoByteListPlayer exoByteListPlayer;
    OfflineSynthesizer offlineSynthesizer;
    private List<String> contentList;
    private ContentAdapter contentAdapter;
    List<Sentence> sentenceList;
    private int synIndex;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exo_player_list_layout);
        offlineSynthesizer = new OfflineSynthesizer();
        content = getResources().getString(R.string.text_1);
        sentenceList = new TTSTextProcessor().startBreak(content);
        findViewById(R.id.start_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSyn(0);
            }
        });
        findViewById(R.id.repeat_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exoByteListPlayer.justPlay();
            }
        });
        initPlayer();
        initSyn();
        initAdapter();
        split();
    }
    private void startSyn(int index){
        if (index >= contentList.size()) {
            return;
        }
        String content = contentList.get(index);
        AudioBean audioBean = new AudioBean();
        audioBean.setSid(UUID.randomUUID().toString());
        audioBean.setContent(content);
        offlineSynthesizer.startAudioBean(audioBean, new MySynthesizerListener() {
            @Override
            public void onRequestSuccess(String content, byte[] resultByte) {
                exoByteListPlayer.prepareDataSource(resultByte,content);
                if(synIndex == 0){
                    exoByteListPlayer.justPlay();
                }
                synIndex++;
                startSyn(synIndex);
            }
        });
    }
    private void initSyn() {
        offlineSynthesizer = new OfflineSynthesizer();
    }

    private void split() {
        contentList.clear();
        for(int i = 0;i<sentenceList.size();i++){
            String text = "("+i+")"+sentenceList.get(i).getContent();
            contentList.add(text);
        }
        contentAdapter.setData(contentList);
    }
    private void initAdapter() {
        contentList = new ArrayList<>();
        ListView contentListView = findViewById(R.id.offline_list_view);
        contentAdapter = new ContentAdapter(this);
        contentListView.setAdapter(contentAdapter);
    }
    public void initPlayer(){
        exoByteListPlayer = new ExoByteListPlayer();
    }
}
