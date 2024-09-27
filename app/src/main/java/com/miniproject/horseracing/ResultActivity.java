package com.miniproject.horseracing;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {
    TextView tvWinningHorse, tvHorse1Result, tvHorse2Result, tvHorse3Result;
    TextView tvBalance, tvReward, tvHorse1Bet, tvHorse2Bet, tvHorse3Bet;
    Button btnPlayAgain, btnReset;
    ImageView imgHorse1, imgHorse2, imgHorse3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        tvWinningHorse = findViewById(R.id.tvWinningHorse);
        tvHorse1Result = findViewById(R.id.tvHorse1Result);
        tvHorse2Result = findViewById(R.id.tvHorse2Result);
        tvHorse3Result = findViewById(R.id.tvHorse3Result);
        tvBalance = findViewById(R.id.tvBalance);
        tvReward = findViewById(R.id.tvReward);
        btnPlayAgain = findViewById(R.id.btnPlayAgain);
        btnReset = findViewById(R.id.btnReset);

        tvHorse1Bet = findViewById(R.id.tvHorse1Bet);
        tvHorse2Bet = findViewById(R.id.tvHorse2Bet);
        tvHorse3Bet = findViewById(R.id.tvHorse3Bet);

        imgHorse1 = findViewById(R.id.imgHorse1);
        imgHorse2 = findViewById(R.id.imgHorse2);
        imgHorse3 = findViewById(R.id.imgHorse3);

        int[] standings = getIntent().getIntArrayExtra("standings");
        double balanceValue = getIntent().getDoubleExtra("balance", 0);
        double bet1 = getIntent().getDoubleExtra("BET1", 0);
        double bet2 = getIntent().getDoubleExtra("BET2", 0);
        double bet3 = getIntent().getDoubleExtra("BET3", 0);
        double total = getIntent().getDoubleExtra("total", 0);
        Log.d(TAG, "balanceValue: " + balanceValue);
        Log.d(TAG, "BET1: " + bet1);
        Log.d(TAG, "BET2: " + bet2);
        Log.d(TAG, "bet3: " + bet3);

        tvBalance.setText(String.format("%.0f", balanceValue));
        tvReward.setText(String.format("%.0f", total));

        if (standings[0] == 0) {
            tvWinningHorse.setText("Horse 1 Wins!");
            imgHorse1.setImageResource(R.drawable.horse_2);
            imgHorse2.setImageResource(R.drawable.horse_1);
            imgHorse3.setImageResource(R.drawable.horse_3);
            tvHorse2Bet.setText(String.format("%.0f", bet1));
            tvHorse1Bet.setText(String.format("%.0f", bet2));
            tvHorse3Bet.setText(String.format("%.0f", bet3));
        } else if (standings[1] == 0) {
            tvWinningHorse.setText("Horse 2 Wins!");
            imgHorse1.setImageResource(R.drawable.horse_1);
            imgHorse2.setImageResource(R.drawable.horse_2);
            imgHorse3.setImageResource(R.drawable.horse_3);
            tvHorse2Bet.setText(String.format("%.0f", bet2));
            tvHorse1Bet.setText(String.format("%.0f", bet1));
            tvHorse3Bet.setText(String.format("%.0f", bet3));
        } else if (standings[2] == 0) {
            tvWinningHorse.setText("Horse 3 Wins!");
            imgHorse1.setImageResource(R.drawable.horse_1);
            imgHorse2.setImageResource(R.drawable.horse_3);
            imgHorse3.setImageResource(R.drawable.horse_2);
            tvHorse2Bet.setText(String.format("%.0f", bet3));
            tvHorse1Bet.setText(String.format("%.0f", bet1));
            tvHorse3Bet.setText(String.format("%.0f", bet2));
        }

        btnPlayAgain.setOnClickListener(v -> {
            finish();
        });
        btnReset.setOnClickListener(v -> {
            finish();
        });
    }
}
