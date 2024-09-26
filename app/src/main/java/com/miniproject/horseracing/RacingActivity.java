package com.miniproject.horseracing;

import android.annotation.SuppressLint;
import android.icu.math.BigDecimal;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class RacingActivity extends AppCompatActivity {
    private final String TAG = "RacingActivity";

    private final double ODDS = 2;
    private BigDecimal balance = BigDecimal.valueOf(100);

    final int HORSE_COUNT = 3;
    SeekBar[] horses = new SeekBar[HORSE_COUNT];

    private enum RaceState {
        READY,
        ONGOING,
        COMPLETED
    }

    ;
    private RaceState raceState;

    @SuppressLint("ClickableViewAccessibility")
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
            horses[i].setOnTouchListener((v, e) -> true);
        }

        btnStart.setOnClickListener(this::startRace);
        btnReset.setOnClickListener(this::resetRace);
        initRace();

        // TODO: Handle betting
    }

    // NOTE: If an INIT_SPEED is outside the range of [MIN_SPEED, MAX_SPEED],
    // the horse's speed will be clamped the first time it attempts to change speed.
    // This is an intended behavior meant to emulate "starting speed burst/slump".
    final int INIT_SPEED_MIN = 1;
    final int INIT_SPEED_MAX = 6;
    final int MIN_SPEED = 2;
    final int MAX_SPEED = 5;
    final int SPEED_CHANGE_RANGE = 2;
    final int FINAL_STRETCH_POINT = 85;

    final int CYCLE_LENGTH = 100; // millisecond(s)
    final int SPEED_CHANGE_BREAKPOINT_1 = 4; // cycle(s)
    final int SPEED_CHANGE_BREAKPOINT_2 = 15; // cycle(s)

    // FIXME: Maybe have an algorithm initialize these dynamically based on the number of horses?
    final float[] CHANCE_BIAS = new float[]{0.2f, 0f, 0.2f};
    final int[] SPEED_BIAS = new int[]{-1, 0, 1};

    private final int[] progress = new int[HORSE_COUNT];
    private final int[] standings = new int[HORSE_COUNT]; // standings[horse] = place
    private final int[] speed = new int[HORSE_COUNT];
    private final byte[] lastSpeedChange = new byte[HORSE_COUNT];

    private final Random random = new Random();
    private Timer timer;
    private TimerTask raceTask;

    class RaceTask extends TimerTask {

        private int cycle = 0;
        private int finished = 0;

        @Override
        public void run() {
            Log.d(TAG, "--- CYCLE " + ++cycle + " ---");

            int lastChange;
            float chance;
            int bias;
            int speedChange;
            for (int i = 0; i < HORSE_COUNT; i++) {
                // Check if this horse is still in the race
                if (progress[i] < 100) {
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
                    if (random.nextFloat() < chance + CHANCE_BIAS[standings[i]]) {
                        lastSpeedChange[i] = 0;
                        // Rubberband mechanics: If behind, more likely to speed up
                        bias = SPEED_BIAS[standings[i]];
                        speedChange = (random.nextInt(2 * SPEED_CHANGE_RANGE + 1) - SPEED_CHANGE_RANGE + bias);
                        // No slowing down in the final stretch
                        if (speedChange < 0 && progress[i] >= FINAL_STRETCH_POINT) speedChange = 0;

                        speed[i] += speedChange;
                        if (speed[i] < MIN_SPEED) speed[i] = MIN_SPEED;
                        if (speed[i] > MAX_SPEED) speed[i] = MAX_SPEED;
                        Log.d(TAG, "Horse " + (i + 1) + " has changed speed!"
                                + " - Change=" + speedChange + " Bias=" + bias);
                    }

                    // Increase progress by speed
                    progress[i] += speed[i];
                    if (progress[i] >= 100) {
                        progress[i] = 100;
                        finished++;
                        Log.d(TAG, "Horse " + (i + 1) + " has finished! "
                                + (HORSE_COUNT - finished) + " more still in the race.");
                    }
                }
            }

            updateStandings();

            // Animate the horses
            for (int i = 0; i < HORSE_COUNT; i++) {
                horses[i].setProgress(progress[i], true);
                Log.d(TAG, "Horse " + (i + 1) + " : Standing=" + (standings[i] + 1)
                        + " Progress=" + progress[i] + " Speed=" + speed[i]);
            }

            // Determine if there's a winner
            if (finished == HORSE_COUNT) {
                raceState = RaceState.COMPLETED;
                stopRace();
                showResults();
                // TODO: Show results dialog
                // TODO: Payout
            }
        }

        private final Integer[] invStandings = new Integer[HORSE_COUNT]; // invStandings[place] = horse

        {
            for (int i = 0; i < HORSE_COUNT; i++) {
                invStandings[i] = i;
            }
        }

        private void updateStandings() {
            Arrays.sort(invStandings, (o1, o2) -> progress[o2] - progress[o1]);
            for (int i = 0; i < HORSE_COUNT; i++) {
                standings[invStandings[i]] = i;
            }
        }

        private void showResults() {
            String ordinalSuffix;
            Log.d(TAG, "===== RESULTS =====");
            for (int i = 0; i < HORSE_COUNT; i++) {
                if (i == 0) {
                    ordinalSuffix = "st";
                } else if (i == 1) {
                    ordinalSuffix = "nd";
                } else if (i == 2) {
                    ordinalSuffix = "rd";
                } else ordinalSuffix = "th";
                Log.d(TAG, (i + 1) + ordinalSuffix + " Place : Horse " + (invStandings[i] + 1));
            }
            Log.d(TAG, "===================");
        }
    }

    void startRace(View view) {
        if (raceState != RaceState.READY) return;
        raceState = RaceState.ONGOING;
        Log.d(TAG, "Race started.");

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
        raceState = RaceState.READY;
    }

    private void stopRace() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (raceTask != null) {
            raceTask.cancel();
            raceTask = null;
        }
    }

    void resetRace(View view) {
        stopRace();
        initRace();
    }
}
