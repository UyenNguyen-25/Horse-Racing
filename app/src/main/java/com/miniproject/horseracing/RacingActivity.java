package com.miniproject.horseracing;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.icu.math.BigDecimal;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class RacingActivity extends AppCompatActivity {
    private final String TAG = "RacingActivity";

    final int HORSE_COUNT = 3;

    private double[] bets = new double[HORSE_COUNT];
//    private double bet1 = 0;
//    private double bet2 = 0;
//    private double bet3 = 0;

    private TextView moneyResult;
    private final double ODDS = 2;
    private BigDecimal balance = BigDecimal.valueOf(100);
    private BigDecimal oldBalance = BigDecimal.valueOf(0);

    SeekBar[] horses = new SeekBar[HORSE_COUNT];

    public RacingActivity() {
        sound = null;
    }

    private enum RaceState {
        READY,
        ONGOING,
        COMPLETED
    }

    ;
    private RaceState raceState;

    MediaPlayer sound;


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
    private final int[] standings = new int[HORSE_COUNT];// standings[horse] = place
    private final int[] horseBet = new int[HORSE_COUNT];
    private final int[] speed = new int[HORSE_COUNT];
    private final byte[] lastSpeedChange = new byte[HORSE_COUNT];

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_racing);
        moneyResult = findViewById(R.id.tvBalance);
        Button btnStart = findViewById(R.id.btnStart);
        Button btnReset = findViewById(R.id.btnReset);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnAddBalance = findViewById(R.id.btnAddBalance);
        horses[0] = findViewById(R.id.seekBar1);
        horses[1] = findViewById(R.id.seekBar2);
        horses[2] = findViewById(R.id.seekBar3);
        for (int i = 0; i < HORSE_COUNT; i++) {
            horses[i].setProgress(0, false);
            horses[i].setOnTouchListener((v, e) -> true);
        }
        resetRace();
//        btnStart.setOnClickListener(this::startRace);
        btnStart.setOnClickListener(view -> {
            oldBalance = balance;
            int totalBetAmount = getTotalBetAmount();
            System.out.println(totalBetAmount);
            if (checkValidateBalance(totalBetAmount)) {
                BigDecimal bet = new BigDecimal(totalBetAmount);
                balance = balance.subtract(bet);
                System.out.println("After bet:" + balance);
                runOnUiThread(() -> {
                    moneyResult.setText(balance.toString());
                });
                startRace();

            } else {
                Toast.makeText(this, "Bạn không đủ tiền để đặt cược.", Toast.LENGTH_SHORT).show();
            }
        });

        btnReset.setOnClickListener(this::resetRace);
        initRace();

        // Handle Add Balance
        btnAddBalance.setOnClickListener(view -> {
            showAddMenu();
        });
        btnLogout.setOnClickListener(this::logOut);


    }

    private final Random random = new Random();
    private Timer timer;
    private TimerTask raceTask;

    private void showAddMenu() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_balance_popup);
        TextView balanceTextView = dialog.findViewById(R.id.balanceTextView);
        final EditText addAmountEditText = dialog.findViewById(R.id.addAmount);
        Button btnAdd = dialog.findViewById(R.id.btnAddMoney);
        balanceTextView.setText("Current Balance: " + balance.toString() + " $");
        Button closeButton = dialog.findViewById(R.id.close_button);
        btnAdd.setOnClickListener(view -> {
            Log.d("TAG", "Add button clicked!");
            String amountStr = addAmountEditText.getText().toString();
            if (!amountStr.isEmpty()) {
                try {
                    BigDecimal amount = new BigDecimal(amountStr);
                    balance = balance.add(amount);
                    System.out.println("Updated balance: " + balance); // Check balance value here
                    // Update UI element (if applicable)
                    runOnUiThread(() -> {
                        balanceTextView.setText("Current Balance: " + balance.toString() + " $");

                    });

                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });

        closeButton.setOnClickListener(view -> {
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialog1 -> {
            moneyResult.setText(balance.toString());
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

    private void resetCheckedHorse() {
        for (int i = 0; i < HORSE_COUNT; i++) {
            CheckBox checkBox = findViewById(getCheckBoxId(i));
            checkBox.setChecked(false);
            resetBet(i);
        }
        balance = oldBalance;
        runOnUiThread(() -> {
            moneyResult.setText(balance.toString());
        });
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
                editText = findViewById(R.id.betHorse1);
                break;
            case 1:
                editText = findViewById(R.id.betHorse2);
                break;
            case 2:
                editText = findViewById(R.id.betHorse3);
                break;
            default:
                return 0;
        }
        if(horseBet[horseIndex] != 1) {
            return 0;
        }
        try {
            String betAmountStr = editText.getText().toString();
            if(betAmountStr.isEmpty()) {
                return 0;
            }

            Log.d(TAG, "getBetAmount: " + betAmountStr);
            return Integer.parseInt(betAmountStr);
        } catch (NumberFormatException e) {
            System.out.println("the text show when have error");
            return 0;
        }
    }

    private BigDecimal calculateWinnings(int winningHorseId) {
        Log.d(TAG, "calculateWinnings: " + winningHorseId);
        int betAmount = getBetAmount(winningHorseId);
        if (betAmount == 0) {
            return null;
        } else {
            BigDecimal winnings = BigDecimal.valueOf(betAmount).multiply(BigDecimal.valueOf(2));
            balance = balance.add(winnings);
            return winnings;
        }
    }

    private int getTotalBetAmount() {
        int totalBet = 0;
        getCheckedHorses();
        for (int i = 0; i < HORSE_COUNT; i++) {
            if (horseBet[i] == 1) {
                Log.d(TAG, "getTotalBetAmount: " + i);
                double bet = getBetAmount(i);
                totalBet += bet;
                bets[i] = bet;
            } else bets[i] = 0;
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
                stopSound();
                showResults();
                // Payout
                int winningHorseId = invStandings[0];
                if (winningHorseId != -1) {
                    calculateWinnings(winningHorseId);


//            for (int i = 0; i < standings.length; i++) {
//                System.out.println("standings[" + i + "]: " + standings[i]);
//            }
                    // Show results dialog
                    double total = balance.subtract(oldBalance).doubleValue();
                    Intent intent = new Intent(RacingActivity.this, ResultActivity.class);
                    intent.putExtra("standings", standings);
                    intent.putExtra("balance", balance.doubleValue());
                    intent.putExtra("BET1", bets[0]);
                    intent.putExtra("BET2", bets[1]);
                    intent.putExtra("BET3", bets[2]);
                    intent.putExtra("total", total);
                    startActivity(intent);
                    initRace();
                } else {
                    Log.d("TAG", "error when calculate winner");
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
            runOnUiThread(() -> {
                moneyResult.setText(balance.toString());
            });
        }
    }

    void startSound() {
        sound = MediaPlayer.create(this, R.raw.horse_sound);
        sound.start();
    }

    void stopSound() {
        sound.stop();
    }

//    public int getWinningHorseId(int[] standings) {
//        int minStanding = standings[0];
//        int winningHorseId = 0;
//        for (int i = 1; i < standings.length; i++) {
//            if (standings[i] < minStanding) {
//                minStanding = standings[i];
//                winningHorseId = i;
//            }
//        }
//        return winningHorseId;
//    }

    void startRace() {
        if (raceState != RaceState.READY) return;
        raceState = RaceState.ONGOING;
        Log.d(TAG, "Race started.");

        for (int i = 0; i < HORSE_COUNT; i++) {
            Log.d(TAG, "Start speed for Horse" + (i + 1) + ": " + speed[i]);
        }
        raceTask = new RaceTask();
        timer = new Timer();
        timer.schedule(raceTask, 0, CYCLE_LENGTH);
        startSound();
    }

    private void resetBet(int i){
        EditText editText;
            if (i == 0) {
                editText = findViewById(R.id.betHorse1);
            } else if (i == 1) {
                editText = findViewById(R.id.betHorse2);
            } else{
                editText = findViewById(R.id.betHorse3);
            }
            editText.setText("");
            editText.setHint("0.0");

    }

    private void initRace() {
        for (int i = 0; i < HORSE_COUNT; i++) {
            progress[i] = 0;
            standings[i] = 1;
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
        stopSound();
    }

    void resetRace(View view) {
        stopRace();
        initRace();
        resetCheckedHorse();
        stopSound();
    }

    void resetRace(){
        Intent intent = getIntent();
        String action = intent.getStringExtra("action");

        initRace();

        if(Objects.equals(action, "refresh") || action == null){
            for (int i = 0; i < HORSE_COUNT; i++) {
                CheckBox checkBox = findViewById(getCheckBoxId(i));
                checkBox.setChecked(false);
                resetBet(i);
            }
            runOnUiThread(() -> {
                moneyResult.setText(balance.toString());
            });
            return;
        }
        resetCheckedHorse();
    }

    void logOut(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
