package com.miniproject.horseracing;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class RacingActivity extends AppCompatActivity implements View.OnClickListener {
    Handler handler=new Handler();
    int finishTime = 6000;
    Button startBtn, resetBtn, logoutBtn;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_racing);

        logoutBtn = findViewById(R.id.btnLogout);
        logoutBtn.setOnClickListener(this);

        startBtn = findViewById(R.id.btnStart);
        startBtn.setOnClickListener(this);

        resetBtn = findViewById(R.id.BtnReset);
        resetBtn.setOnClickListener(this);
    }

    void onMusicStart(int delay){
        MediaPlayer music = MediaPlayer.create(RacingActivity.this, R.raw.horse_sound);
        music.start();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("replay", "replay: Success");
                music.stop();
            }
        }, delay);
    }

    void onRacingStart(){
        onMusicStart(finishTime);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
            onRacingFinish();
            }
        },finishTime);
    }

    void onRacingFinish(){
        Log.d("Finish", "onRacingFinish: Success");
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("horse","HeHe");
        intent.putExtra("reward",50);
        startActivity(intent);
    }

    void onReset(){
        Log.d("ResetBtnReset", "onReset: Success");
    }

    void onLogout(){
        Intent intent = new Intent(this,  MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btnStart){
            onRacingStart();
        }else if(view.getId() == R.id.btnLogout){
            onLogout();
        }else if(view.getId() == R.id.BtnReset){
            onReset();
        }
    }
}
