package com.miniproject.horseracing;

import android.icu.math.BigDecimal;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class RacingActivity extends AppCompatActivity {
    private final String TAG = "RacingActivity";

    private final double ODDS = 2;
    private BigDecimal balance = BigDecimal.valueOf(100);

    SeekBar horse1, horse2, horse3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_racing);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnLogout = findViewById(R.id.btnLogout);

        horse1 = findViewById(R.id.seekBar1);
        horse2 = findViewById(R.id.seekBar2);
        horse3 = findViewById(R.id.seekBar3);

        horse1.setProgress(0, false);
        horse2.setProgress(0, false);
        horse3.setProgress(0, false);

        btnStart.setOnClickListener(this::startRace);

        // TODO: Handle betting
    }

    final int INIT_SPEED = 2;
    final int MIN_SPEED = 1;
    final int MAX_SPEED = 4;
    final int MAX_SPEED_CHANGE = 2;
    final int CYCLE_LENGTH = 200; // millisecond(s)
    final int SPEED_CHANGE_BREAKPOINT_1 = 400 / CYCLE_LENGTH; // cycle(s)
    final int SPEED_CHANGE_BREAKPOINT_2 = 800 / CYCLE_LENGTH; // cycle(s)
    final int[] SPEED_BIAS = new int[]{0, 1, 2};

    private final int[] progress = new int[3];
    private final int[] speed = new int[3];
    private final int[] standings = new int[3]; // standings[horse] = place
    private final byte[] lastSpeedChange = new byte[3];

    private final Random random = new Random();
    private Timer timer;
    private final TimerTask timerTask = new TimerTask() {

        @Override
        public void run() {
            updateStandings();
            int lastChange;
            float chance;
            int bias;
            for (int i = 0; i < 3; i++) {
                // Increment lastChanged
                lastChange = ++lastSpeedChange[i];

                // Randomly decides whether to change speed; if true, reset lastChanged
                // Probability: [0-1s] = 0%; [1-3s] = 0..100%; >3s = 100%
                if (lastChange < SPEED_CHANGE_BREAKPOINT_1) {
                    chance = 0;
                } else if (lastChange < SPEED_CHANGE_BREAKPOINT_2) {
                    chance = 1 - (float) (SPEED_CHANGE_BREAKPOINT_2 - lastChange) / (SPEED_CHANGE_BREAKPOINT_2 - SPEED_CHANGE_BREAKPOINT_1);
                } else {
                    chance = 1;
                }
                if (random.nextFloat() < chance) {
                    // Rubberband mechanics: If behind, more likely to speed up
                    bias = SPEED_BIAS[standings[i] - 1];
                    // Actually change speed here
                    speed[i] += (random.nextInt(2 * MAX_SPEED_CHANGE + 1) - MAX_SPEED_CHANGE + bias);
                    Log.d(TAG, "Horse " + i + " : Speed=" + speed[i] + " Bias=" + bias);


                    if (speed[i] < MIN_SPEED) speed[i] = MIN_SPEED;
                    if (speed[i] > MAX_SPEED) speed[i] = MAX_SPEED;
                }

                // Increase progress by speed
                progress[i] += speed[i];
                Log.d(TAG, "Horse " + i + " : Progress=" + progress[i]);
            }

            horse1.setProgress(progress[0], true);
            horse2.setProgress(progress[1], true);
            horse3.setProgress(progress[2], true);

            for (int i = 0; i < 3; i++) {
                if (progress[i] >= 100) {
                    stopRace();
                    // TODO: Show results
                }
            }
        }

        private void updateStandings() {
            // FIXME: Is there a better way?
            if (progress[0] > progress[1]) {
                if (progress[1] > progress[2]) {
                    setStandings(1, 2, 3);
                } else if (progress[2] > progress[0]) {
                    setStandings(2, 3, 1);
                } else {
                    setStandings(1, 3, 2);
                }
            } else { // [0] < [1]
                if (progress[2] < progress[0]) {
                    setStandings(2, 1, 3);
                } else if (progress[2] > progress[1]) {
                    setStandings(3, 2, 1);
                } else {
                    setStandings(3, 1, 2);
                }
            }
        }

        private void setStandings(int... s) {
            System.arraycopy(s, 0, standings, 0, 3);
        }
    };

    private void startRace(View view) {
        if (timer != null) {
            return;
        }

        Log.d(TAG, "Race started.");
        for (int i = 0; i < 3; i++) {
            speed[i] = INIT_SPEED;
            Log.d(TAG, "Speed " + (i + 1) + ": " + speed[i]);
        }
        timer = new Timer();
        timer.schedule(timerTask, 0, 100);
    }

    private void stopRace() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
