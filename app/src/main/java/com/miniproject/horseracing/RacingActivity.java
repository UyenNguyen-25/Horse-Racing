package com.miniproject.horseracing;

import android.content.Intent;
import android.icu.math.BigDecimal;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class RacingActivity extends AppCompatActivity {
    EditText betHorse1;
    EditText betHorse2;
    EditText betHorse3;
    private double bet1 = 0;
    private double bet2 = 0;
    private double bet3 = 0;
    private final String TAG = "RacingActivity";

    private final double ODDS = 1;
    private BigDecimal balance = BigDecimal.valueOf(100);

    SeekBar[] horses = new SeekBar[3];
    private boolean isRacing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_racing);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnLogout = findViewById(R.id.btnLogout);

        horses[0] = findViewById(R.id.seekBar1);
        horses[1] = findViewById(R.id.seekBar2);
        horses[2] = findViewById(R.id.seekBar3);
        for (int i = 0; i < HORSE_COUNT; i++) {
            horses[i].setProgress(0, false);
        }

        btnStart.setOnClickListener(this::startRace);
        btnReset.setOnClickListener(this::resetRace);

        // TODO: Handle betting
    }

    final int HORSE_COUNT = 3;
    final int INIT_SPEED_MIN = 1;
    final int INIT_SPEED_MAX = 3;
    final int MIN_SPEED = 2;
    final int MAX_SPEED = 5;
    final int MAX_SPEED_CHANGE = 2;
    final int CYCLE_LENGTH = 100; // millisecond(s)
    final int SPEED_CHANGE_BREAKPOINT_1 = 4; // cycle(s)
    final int SPEED_CHANGE_BREAKPOINT_2 = 15; // cycle(s)

    final float[] CHANCE_BIAS = new float[]{-0.2f, 0f, 0.2f};
    final int[] SPEED_BIAS = new int[]{0, 0, 1};

    private final int[] progress = new int[3];
    private final int[] standings = new int[3]; // standings[horse] = place
    private final int[] speed = new int[3];
    private final byte[] lastSpeedChange = new byte[3];

    private final Random random = new Random();
    private Timer timer;
    private TimerTask raceTask;

    class RaceTask extends TimerTask {

        @Override
        public void run() {
            int lastChange;
            float chance;
            int bias;
            int speedChange;
            for (int i = 0; i < HORSE_COUNT; i++) {
                // Increment lastChanged
                lastChange = ++lastSpeedChange[i];

                // Randomly decides whether to change speed; if true, reset lastChanged
                // Probability:
                //      (..1st_breakpoint) = 0%
                //      [1st_breakpoint,2nd_breakpoint) = 0..100%
                //      [2nd_breakpoint..) = 100%
                if (lastChange < SPEED_CHANGE_BREAKPOINT_1) {
                    chance = 0;
                } else if (lastChange < SPEED_CHANGE_BREAKPOINT_2) {
                    chance = 1 - (float) (SPEED_CHANGE_BREAKPOINT_2 - lastChange) / (SPEED_CHANGE_BREAKPOINT_2 - SPEED_CHANGE_BREAKPOINT_1);
                } else {
                    chance = 1;
                }
                if (random.nextFloat() < chance + CHANCE_BIAS[standings[i] - 1]) {
                    lastSpeedChange[i] = 0;
                    // Rubberband mechanics: If behind, more likely to speed up
                    bias = SPEED_BIAS[standings[i] - 1];
                    // Actually change speed here
                    speedChange = (random.nextInt(2 * MAX_SPEED_CHANGE + 1) - MAX_SPEED_CHANGE + bias);
                    speed[i] += speedChange;
                    if (speed[i] < MIN_SPEED) speed[i] = MIN_SPEED;
                    if (speed[i] > MAX_SPEED) speed[i] = MAX_SPEED;
                    Log.d(TAG, "Horse " + (i + 1) + " has changed speed!"
                            + " : Change=" + speedChange + " Bias=" + bias);
                }

                // Increase progress by speed
                progress[i] += speed[i];
                updateStandings();
                Log.d(TAG, "Horse " + (i + 1) + " : Standing=" + standings[i]
                        + " Progress=" + progress[i] + " Speed=" + speed[i]);
            }

            // Animate the horses
            for (int i = 0; i < HORSE_COUNT; i++) {
                horses[i].setProgress(progress[i], true);
            }

            // Determine if there's a winner
            boolean hasWinner = false;
            for (int i = 0; i < HORSE_COUNT; i++) {
                if (progress[i] >= 100) {
                    hasWinner = true;
                    break;
                    // TODO: Show results
                }
            }

            if (hasWinner && isRacing) {
                stopRace();
            }
        }

        private void updateStandings() {
            int[] tempStandings = new int[HORSE_COUNT];
            for (int i = 0; i < HORSE_COUNT; i++) {
                tempStandings[i] = i;
            }

            for (int i = 0; i < HORSE_COUNT - 1; i++) {
                for (int j = 0; j < HORSE_COUNT - i - 1; j++) {
                    if (progress[tempStandings[j]] < progress[tempStandings[j + 1]]) {
                        int temp = tempStandings[j];
                        tempStandings[j] = tempStandings[j + 1];
                        tempStandings[j + 1] = temp;
                    }
                }
            }

            for (int i = 0; i < HORSE_COUNT; i++) {
                standings[tempStandings[i]] = i + 1;
            }
        }


        private void setStandings(int... s) {
            System.arraycopy(s, 0, standings, 0, 3);
        }
    }

    void startRace(View view) {
        if (isRacing || timer != null) {
            return;
        }
        isRacing = true;
        Log.d(TAG, "Race started.");

        betHorse1 = findViewById(R.id.betHorse1);
        betHorse2 = findViewById(R.id.betHorse2);
        betHorse3 = findViewById(R.id.betHorse3);

        if (((CheckBox) findViewById(R.id.checkBox1)).isChecked()) {
            bet1 = Double.parseDouble(betHorse1.getText().toString());
            Log.d(TAG, "bet1: " + bet1);
        }
        if (((CheckBox) findViewById(R.id.checkBox2)).isChecked()) {
            bet2 = Double.parseDouble(betHorse2.getText().toString());
            Log.d(TAG, "bet2: " + bet2);
        }
        if (((CheckBox) findViewById(R.id.checkBox3)).isChecked()) {
            bet3 = Double.parseDouble(betHorse3.getText().toString());
            Log.d(TAG, "bet3: " + bet3);
        }

        Log.d(TAG, "Bets placed: Horse 1 = " + bet1 + ", Horse 2 = " + bet2 + ", Horse 3 = " + bet3);

        initRace();
        for (int i = 0; i < HORSE_COUNT; i++) {
            Log.d(TAG, "Start speed for Horse" + (i + 1) + ": " + speed[i]);
        }
        raceTask = new RaceTask();
        timer = new Timer();
        timer.schedule(raceTask, 0, CYCLE_LENGTH);
    }

    private void initRace() {
        for (int i = 0; i < HORSE_COUNT; i++) {
            progress[i] = 0;
            standings[i] = 2;
            speed[i] = random.nextInt(INIT_SPEED_MAX - INIT_SPEED_MIN + 1) + INIT_SPEED_MIN;
            lastSpeedChange[i] = 0;
            horses[i].setProgress(0, true);
        }
    }

    private void stopRace() {
        isRacing = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (raceTask != null) {
            raceTask.cancel();
            raceTask = null;
        }

        double totalWinnings = 0;
        double totalLoss = 0;

        if (standings[0] == 1) {
            totalWinnings += bet1 * ODDS;
        } else {
            totalLoss += bet1;
        }
        if (standings[1] == 1) {
            totalWinnings += bet2 * ODDS;
        } else {
            totalLoss += bet2;
        }
        if (standings[2] == 1) {
            totalWinnings += bet3 * ODDS;
        } else {
            totalLoss += bet3;
        }

        Log.d(TAG, "totalWinnings: " + totalWinnings);
        Log.d(TAG, "totalLoss: " + totalLoss);

        double total = totalWinnings - totalLoss;
        balance = balance.add(BigDecimal.valueOf(total));

        Intent intent = new Intent(RacingActivity.this, ResultActivity.class);
        intent.putExtra("standings", standings);
        intent.putExtra("balance", balance.doubleValue());
        intent.putExtra("BET1", bet1);
        intent.putExtra("BET2", bet2);
        intent.putExtra("BET3", bet3);
        intent.putExtra("odds", ODDS);
        intent.putExtra("total", total);
        startActivity(intent);
    }

    void resetRace(View view) {
        stopRace();
        initRace();
    }
}
