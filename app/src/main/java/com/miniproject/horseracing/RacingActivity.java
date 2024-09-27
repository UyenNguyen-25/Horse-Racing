package com.miniproject.horseracing;

import android.app.Dialog;
import android.icu.math.BigDecimal;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class RacingActivity extends AppCompatActivity {
    private final String TAG = "RacingActivity";

    private final double ODDS = 2;
    private BigDecimal balance = BigDecimal.valueOf(100);

    final int HORSE_COUNT = 3;
    SeekBar[] horses = new SeekBar[HORSE_COUNT];
    private boolean isRacing;
    List<Integer> horseBetPlace = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_racing);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnAddBalance = findViewById(R.id.btnAddBalance);
        horses[0] = findViewById(R.id.seekBar1);
        horses[1] = findViewById(R.id.seekBar2);
        horses[2] = findViewById(R.id.seekBar3);
        for (int i = 0; i < HORSE_COUNT; i++) {
            horses[i].setProgress(0, false);
        }

        btnStart.setOnClickListener(this::startRace);
        btnReset.setOnClickListener(this::resetRace);

        // Handle Add Balance
        btnAddBalance.setOnClickListener(view -> {
            showAddMenu();
        });

        // TODO: Handle betting
    }

    final int INIT_SPEED_MIN = 1;
    final int INIT_SPEED_MAX = 3;
    final int MIN_SPEED = 2;
    final int MAX_SPEED = 5;
    final int MAX_SPEED_CHANGE = 2;
    final int CYCLE_LENGTH = 100; // millisecond(s)
    final int SPEED_CHANGE_BREAKPOINT_1 = 4; // cycle(s)
    final int SPEED_CHANGE_BREAKPOINT_2 = 15; // cycle(s)

    // FIXME: Maybe have an algorithm initialize these dynamically based on the number of horses?
    final float[] CHANCE_BIAS = new float[]{-0.2f, 0f, 0.2f};
    final int[] SPEED_BIAS = new int[]{-1, 0, 1};

    private final int[] progress = new int[HORSE_COUNT];
    private final int[] standings = new int[HORSE_COUNT];// standings[horse] = place
    private final int[] horseBet = new int[HORSE_COUNT];
    private final int[] speed = new int[HORSE_COUNT];
    private final byte[] lastSpeedChange = new byte[HORSE_COUNT];

    private final Random random = new Random();
    private Timer timer;
    private TimerTask raceTask;

    private void showAddMenu() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_balance_popup);
        TextView balanceTextView = dialog.findViewById(R.id.balanceTextView);
        final EditText addAmountEditText = dialog.findViewById(R.id.addAmount);
        final TextView selectedBalanceTextView = dialog.findViewById(R.id.selectedBalanceTextView);
        Button btnAdd = dialog.findViewById(R.id.btnAddMoney);
        Button closeButton = dialog.findViewById(R.id.close_button);

        balanceTextView.setText("Current Balance: $" + balance.toString());

        selectedBalanceTextView.setText("Selected Amount: $0");

        btnAdd.setOnClickListener(view -> {
            Log.d("TAG", "Add button clicked!");
            String amountStr = addAmountEditText.getText().toString();

            if (!amountStr.isEmpty()) {
                try {
                    BigDecimal amount = new BigDecimal(amountStr);
                    balance.add(amount);
                    Log.d("TAG", String.valueOf(balance));
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            }
        });

        closeButton.setOnClickListener(view -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    private void getCheckedHorses() {
        for (int i = 0; i < HORSE_COUNT; i++) {
            CheckBox checkBox = findViewById(getCheckBoxId(i));
            if (checkBox.isChecked()) {
                horseBet[i] = 1;
            } else {
                horseBet[i] = 0;
            }
        }
    }

    private int getCheckBoxId(int index) {
        switch (index) {
            case 0:
                return R.id.checkBox1;
            case 1:
                return R.id.checkBox2;
            case 2:
                return R.id.checkBox3;
            default:
                return -1;
        }
    }
    private int getBetAmount(int horseIndex) {
        EditText editText;
        switch (horseIndex) {
            case 0:
                editText = findViewById(R.id.editTextNumber);
                break;
            case 1:
                editText = findViewById(R.id.editTextNumber2);
                break;
            case 2:
                editText = findViewById(R.id.editTextNumber3);
                break;
            default:
                return 0;
        }
        String betAmountStr = editText.getText().toString();
        try {
            return Integer.parseInt(betAmountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Vui lòng nhập số tiền cược hợp lệ", Toast.LENGTH_SHORT).show();
            return 0;
        }
    }
    private BigDecimal calculateWinnings(int winningHorseId) {
        int betAmount = getBetAmount(winningHorseId);
        BigDecimal winnings = BigDecimal.valueOf(betAmount).multiply(BigDecimal.valueOf(2));
        balance = balance.add(winnings);
        return winnings;
    }
    private int getTotalBetAmount() {
        int totalBet = 0;
        for (int i = 0; i < HORSE_COUNT; i++) {
            if (horseBet[i] == 1) {
                totalBet += getBetAmount(i);
            }
        }
        return totalBet;
    }

    private boolean checkValidateBalance(int totalBetAmount) {
        BigDecimal totalBet = BigDecimal.valueOf(totalBetAmount);
        if (totalBet.compareTo(balance) <= 0) {
            System.out.println("Bạn có đủ tiền để đặt cược.");
            return true;
        } else {
            System.out.println("Số dư không đủ.");
            return false;
        }
    }

    class RaceTask extends TimerTask {

        private int cycle = 0;

        @Override
        public void run() {
            Log.d(TAG, "--- CYCLE " + ++cycle + " ---");

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
                if (random.nextFloat() < chance + CHANCE_BIAS[standings[i]]) {
                    lastSpeedChange[i] = 0;
                    // Rubberband mechanics: If behind, more likely to speed up
                    bias = SPEED_BIAS[standings[i]];
                    // Actually change speed here
                    speedChange = (random.nextInt(2 * MAX_SPEED_CHANGE + 1) - MAX_SPEED_CHANGE + bias);
                    speed[i] += speedChange;
                    if (speed[i] < MIN_SPEED) speed[i] = MIN_SPEED;
                    if (speed[i] > MAX_SPEED) speed[i] = MAX_SPEED;
                    Log.d(TAG, "Horse " + (i + 1) + " has changed speed!"
                            + " - Change=" + speedChange + " Bias=" + bias);
                }

                // Increase progress by speed
                progress[i] += speed[i];
                if (progress[i] > 100) progress[i] = 100;
            }
            updateStandings();

            // Animate the horses
            for (int i = 0; i < HORSE_COUNT; i++) {
                horses[i].setProgress(progress[i], true);
                Log.d(TAG, "Horse " + (i + 1) + " : Standing=" + (standings[i] + 1)
                        + " Progress=" + progress[i] + " Speed=" + speed[i]);
            }

            // Determine if there's a winner
            for (int i = 0; i < HORSE_COUNT; i++) {
                if (progress[i] >= 100) {
                    stopRace();
                    // TODO: Show results
                }
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
    }
    public int getWinningHorseId(int[] standings) {
        int minStanding = standings[0];
        int winningHorseId = 0;
        for (int i = 1; i < standings.length; i++) {
            if (standings[i] < minStanding) {
                minStanding = standings[i];
                winningHorseId = i;
            }
        }
        return winningHorseId;
    }

    void startRace(View view) {
        if (isRacing || timer != null) {
            return;
        }

        int totalBetAmount = getTotalBetAmount();
        if (!checkValidateBalance(totalBetAmount)) {
            balance = balance.subtract(BigDecimal.valueOf(totalBetAmount));
            isRacing = true;
            Log.d(TAG, "Race started.");
            initRace();
            for (int i = 0; i < HORSE_COUNT; i++) {
                Log.d(TAG, "Start speed for Horse" + (i + 1) + ": " + speed[i]);
            }
            raceTask = new RaceTask();
            timer = new Timer();
            timer.schedule(raceTask, 0, CYCLE_LENGTH);
        } else {
            Toast.makeText(this, "Vui lòng nhập số tiền cược hợp lệ", Toast.LENGTH_SHORT).show();
        }


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
        int winningHorseId = getWinningHorseId(standings);
        if (winningHorseId != -1) {
            // Calculate winnings only if there's a winner
            calculateWinnings(winningHorseId);
            Log.d("TAG","Ok u win");
            for (int i = 0; i < standings.length; i++) {
                System.out.println("standings[" + i + "]: " + standings[i]);
            }
        } else {
            Log.d("TAG","error when calculate winner");
        }

    }

    void resetRace(View view) {
        stopRace();
        initRace();
    }
}
