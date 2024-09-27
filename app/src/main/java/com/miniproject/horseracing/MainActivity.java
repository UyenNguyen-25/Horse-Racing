package com.miniproject.horseracing;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import model.Account;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    public final static String ERROR_MESSAGE = "Please enter full fill";

    EditText  usernameTxt, passwordTxt;
    Button signInBtn;
    TextView usernameErr, passwordErr;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        usernameTxt =findViewById(R.id.UsernameEditText);
        passwordTxt =findViewById(R.id.PasswordEditText);
        usernameErr = findViewById(R.id.usernameError);
        passwordErr = findViewById(R.id.passwordError);
        signInBtn = findViewById(R.id.LoginButton);
        signInBtn.setOnClickListener(this);
//        Account acc = new Account("sa", "1");
    }

    boolean checkInput() {
        boolean isValidate = true;
        if (TextUtils.isEmpty(usernameTxt.getText().toString()) ) {
            usernameErr.setText(ERROR_MESSAGE);
            usernameErr.setVisibility(View.VISIBLE);
            isValidate = false;
        }else {
            usernameErr.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(passwordTxt.getText().toString()) ) {
            passwordErr.setText(ERROR_MESSAGE);
            passwordErr.setVisibility(View.VISIBLE);
            isValidate = false;
        }else {
            passwordErr.setVisibility(View.GONE);
        };
        return isValidate;
    }

    void signIn (){
        if (!checkInput()){
            return;
        }
        Log.d("Success", "signIn: sign in success");
        Intent intent = new Intent(MainActivity.this, RacingActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.LoginButton){
            signIn();
        }
    }
}