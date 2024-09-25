package com.miniproject.horseracing;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import model.Account;

public class RacingActivity extends AppCompatActivity {
    Account loggedInAccount;
    private int selectedBalance =0, musicPlayPosition, totalBetAmount;
    final Dialog dialog = new Dialog(this);
    int blackHorseBet = 0, whiteHorseBet = 0, redHorseBet = 0, greenHorseBet = 0;
    MediaPlayer mediaPlayer;
    Button startButton, playButton, showBalance, resetButton;
    ObjectAnimator horseAnimator;
    ImageView blackHorse, whiteHorse, redHorse, greenHorse;
    CheckBox blackCheckBox,whiteCheckBox, redCheckBox, greenCheckBox;
    EditText blackBetAmount,whiteBetAmount, redBetAmount, greenBetAmount;
    List<CheckBox> horseBetplace = new ArrayList<>();
    Map<ImageView, Long> finishingTimes = new HashMap<>();
    Button addBalanceButton = dialog.findViewById(R.id.addBalanceButton);
    private float[] initialXPositions = new float[4];
    private float[] initialYPositions = new float[4];

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_racing);
        blackHorse = findViewById(R.id.imageView1);
        whiteHorse = findViewById(R.id.imageView2);
        redHorse = findViewById(R.id.imageView3);
        greenHorse = findViewById(R.id.imageView4);
//        showBalance = findViewById(R.id.showBalanceBtn);
        Button controlMusic = findViewById(R.id.buttonControlMusic);
        Button logoutButton = findViewById(R.id.btnLogout);
        mediaPlayer = MediaPlayer.create(this, R.raw.Instinct);
        resetButton = findViewById(R.id.BtnReset);
        loggedInAccount = (Account) getIntent().getSerializableExtra("loggedInAccount");
        if (!mediaPlayer.isPlaying())
        {
            mediaPlayer.start();
            mediaPlayer.setLooping(true);
        }
        // Pause music
        controlMusic.setOnClickListener(view -> {
            String musicStatus = (String) controlMusic.getText();
            switch (musicStatus) {
                case "Pause music" :
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        musicPlayPosition = mediaPlayer.getCurrentPosition();
                    }
                    controlMusic.setText("Play music");
                    break;
                case "Play music":
                    if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                        mediaPlayer.seekTo(musicPlayPosition);
                        mediaPlayer.start();
                    }
                    controlMusic.setText("Pause music");
                    break;
            }
        });
        showBalance.setOnClickListener(view -> {
            addBalancePopup();
        });
        // Logout
        logoutButton.setOnClickListener(view -> {
            Intent intent = new Intent(RacingActivity.this, MainActivity.class);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            startActivity(intent);
        });
        //set position
        initialXPositions[0] = blackHorse.getX();
        initialXPositions[1] = whiteHorse.getX();
        initialXPositions[2] = redHorse.getX();
        initialXPositions[3] = greenHorse.getX();

        //Reset Btn
        resetButton.setOnClickListener(view -> {
            blackHorse.setTranslationX(initialXPositions[0]);
            whiteHorse.setTranslationX(initialXPositions[1]);
            redHorse.setTranslationX(initialXPositions[2]);
            greenHorse.setTranslationX(initialXPositions[3]);


            finishingTimes.clear(); // Clear the finishingTimes Map when race finished
            blackHorseBet = 0; // Reset the bet money to 0 for all cars
            whiteHorseBet = 0;
            greenHorseBet = 0;
            redHorseBet = 0;
            horseBetplace.clear();
        });
        startButton.setOnClickListener(view -> {
            playButton.setEnabled(true); // Enable play button after user confirms race start
        });

        // Play Button
        playButton.setOnClickListener(view -> {
            // Reset positions and finishing times before starting a new race
            blackHorse.setTranslationX(initialXPositions[0]);
            whiteHorse.setTranslationX(initialXPositions[1]);
            redHorse.setTranslationX(initialXPositions[2]);
            greenHorse.setTranslationX(initialXPositions[3]);
            finishingTimes.clear();

            // Move each horse with random speed
            totalBetAmount = blackHorseBet + whiteHorseBet + greenHorseBet + redHorseBet;

            // Validate the bet money must be <= user balance
            if (totalBetAmount > loggedInAccount.getBalance()) {
                Toast.makeText(RacingActivity.this, "Your balance is not enough", Toast.LENGTH_SHORT).show();
            } else {
                moveHorseWithRandomSpeed(blackHorse);
                moveHorseWithRandomSpeed(whiteHorse);
                moveHorseWithRandomSpeed(redHorse);
                moveHorseWithRandomSpeed(greenHorse);
                //Minus totalBetAmount when race started
                int leftBalance = loggedInAccount.getBalance() - totalBetAmount;
                loggedInAccount.setBalance(leftBalance);
                showBalance.setText("Balance: " + leftBalance);
                Toast.makeText(RacingActivity.this, "Race started", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            playButton.setEnabled(false); // Disable play button after starting the race
        });

    }

    private void addBalancePopup() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_balance_popup);
        TextView balanceTextView = dialog.findViewById(R.id.balanceTextView);
        final SeekBar addBalanceSeekBar = dialog.findViewById(R.id.addBalanceSeekBar);
        final TextView selectedBalanceTextView = dialog.findViewById(R.id.selectedBalanceTextView);
        Button addBalanceButton = dialog.findViewById(R.id.addBalanceButton);
        Button closeButton = dialog.findViewById(R.id.close_button);

        balanceTextView.setText("Current Balance: $" + loggedInAccount.getBalance());
        addBalanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedBalance = progress; // Update the selected balance
                selectedBalanceTextView.setText("Selected Amount: $" + selectedBalance);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Add balance
        addBalanceButton.setOnClickListener(view -> {
            loggedInAccount.setBalance(loggedInAccount.getBalance() + selectedBalance);
            balanceTextView.setText("Current Balance: $" + loggedInAccount.getBalance());
            showBalance.setText("Balance: " + loggedInAccount.getBalance());
        });

        //Close popup + update balance
        closeButton.setOnClickListener(view -> {
            showBalance.setText("Balance: " + loggedInAccount.getBalance());
            dialog.dismiss();
        });
        dialog.show();


    }
    private void moveHorseWithRandomSpeed(ImageView horse) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        horseAnimator = ObjectAnimator.ofFloat(horse, "translationX",-screenWidth - horse.getWidth() - 5);
        horseAnimator.setDuration(new Random().nextInt(30000 - 20000 + 1) + 20000);
        horseAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animator) {
                long currentTime = System.currentTimeMillis();
                finishingTimes.put(horse, currentTime);
                if (finishingTimes.size() == 4) {
                    List<ImageView> sortedHorses = new ArrayList<>(finishingTimes.keySet());
                    Collections.sort(sortedHorses, (Horse1, Horse2) ->
                            Long.compare(finishingTimes.get(Horse1), finishingTimes.get(Horse2)));
//                    showFinishingOrder(sortedHorses);
                }
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animator) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animator) {

            }
        });
        horseAnimator.start();
    }
    private int calculateWinningMoney(List<ImageView> sortedCar) {
        int winningMoney = 0;

        for (CheckBox c : horseBetplace) {
            for (ImageView img : sortedCar) {
                if (returnImgViewBaseOnCheckbox(c).getTag().equals(img.getTag())) {
                    if (sortedCar.indexOf(img) == 0) { // 1st car
                        winningMoney += returnMoneyBaseOnCheckBox(c) * 2 ;
                        break;
                    } else if (sortedCar.indexOf(img) == 1) {
                        winningMoney += (int) Math.round(returnMoneyBaseOnCheckBox(c) * 1.5); // 2nd car
                        break;
                    } else {
                        winningMoney += 0; // 3rd and 4th cars
                        break;
                    }
                }
            }
        }
        return winningMoney;
    }
    private ImageView returnImgViewBaseOnCheckbox(CheckBox checkBox) {
        ImageView imageView;
        if (getResources().getResourceEntryName(checkBox.getId()).equals("checkBox1")) {
            imageView = findViewById(R.id.imageView1);
        } else if (getResources().getResourceEntryName(checkBox.getId()).equals("checkBox2")) {
            imageView = findViewById(R.id.imageView2);
        } else if (getResources().getResourceEntryName(checkBox.getId()).equals("checkBox3")) {
            imageView = findViewById(R.id.imageView3);
        } else {
            imageView = findViewById(R.id.imageView4);
        }
        return imageView;
    }
    private int returnMoneyBaseOnCheckBox(CheckBox checkBox) {
        int betMoney = 0;
        if (getResources().getResourceEntryName(checkBox.getId()).equals("checkBox1")) {
            betMoney = blackHorseBet;
        } else if (getResources().getResourceEntryName(checkBox.getId()).equals("checkBox2")) {
            betMoney = whiteHorseBet;
        } else if (getResources().getResourceEntryName(checkBox.getId()).equals("checkBox3")) {
            betMoney = redHorseBet;
        } else {
            betMoney = greenHorseBet;
        }
        return betMoney;
    }

//    private void showFinishingOrder(List<ImageView> sortedHorses) {
//        View dialogView = getLayoutInflater().inflate(R.layout.activity_result, null);
////        TextView orderTextView = dialogView.findViewById(R.id.finishingOrder);
//        StringBuilder orderText = new StringBuilder("Finishing Order:\n");
//
//        List<ImageView> finishHorseImg = new ArrayList<>();
////        finishHorseImg.add(dialogView.findViewById(R.id.firstHorseImg));
////        finishHorseImg.add(dialogView.findViewById(R.id.secondHorseImg));
////        finishHorseImg.add(dialogView.findViewById(R.id.thirdHorseImg));
////        finishHorseImg.add(dialogView.findViewById(R.id.fouthHorseImg));
//
//        ArrayList<TextView> finishHorseText = new ArrayList<>();
////        finishHorseText.add(dialogView.findViewById(R.id.firstHorseText));
////        finishHorseText.add(dialogView.findViewById(R.id.secondHorseText));
////        finishHorseText.add(dialogView.findViewById(R.id.thirdHorseText));
////        finishHorseText.add(dialogView.findViewById(R.id.fouthHorseText));
//
//        for (int i = 0; i < sortedHorses.size(); i++) {
//            finishHorseImg.get(i).setImageDrawable(sortedHorses.get(i).getDrawable());
//            finishHorseText.get(i).setText(sortedHorses.get(i).getTag().toString());
//        }
//
//        orderTextView.setText(orderText.toString());
//        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.TransparentDialog);
//        builder.setView(dialogView);
//        AlertDialog dialog = builder.create();
//        dialog.show();
//        Toast.makeText(WelcomeActivity.this, "Win money: " + calculateWinningMoney(sortedHorses), Toast.LENGTH_SHORT).show();
//        TextView summaryResult = dialogView.findViewById(R.id.textViewResult);
//        if (calculateWinningMoney(sortedHorses) > totalBetAmount) {
//            int moneyResult = calculateWinningMoney(sortedHorses) - totalBetAmount;
//            summaryResult.setText("Congratulation, you won " + moneyResult + "$");
//            summaryResult.setTextColor(Color.parseColor("#51F349"));
//        } else {
//            int moneyResult = totalBetAmount - calculateWinningMoney(sortedHorses);
//            summaryResult.setText("Unfortunately, you lose " + moneyResult + "$");
//            summaryResult.setTextColor(Color.parseColor("#F51425"));
//        }
//        // Sum user balance to winning money after the race finished
//        loggedInAccount.setBalance(loggedInAccount.getBalance() + calculateWinningMoney(sortedHorses));
//        showBalance.setText("Balance: " + loggedInAccount.getBalance());
//
//        // After the race finished, confirm the result
//        Button confirmReslutButton = dialogView.findViewById(R.id.confirmResultBtn);
//        confirmReslutButton.setOnClickListener(view -> {
//            dialog.dismiss();
//        });
//    }
}
