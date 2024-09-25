package com.miniproject.horseracing;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        Intent intent = getIntent();
        String horse = intent.getStringExtra("horse");
        Log.d("winner", "winner:"+horse);
        int reward = intent.getIntExtra("reward",0);
        Log.d("reward", "reward:"+reward);
    }
}
