package com.miniproject.horseracing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import model.Account;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    EditText  usernameTxt, passwordTxt;
    Button signInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        usernameTxt =findViewById(R.id.UsernameEditText);
        passwordTxt =findViewById(R.id.PasswordEditText);
        signInBtn = findViewById(R.id.LoginButton);
        signInBtn.setOnClickListener(this);
//        Account acc = new Account("sa", "1");
    }

    boolean checkInput() {
        if (TextUtils.isEmpty(usernameTxt.getText().toString()) || TextUtils.isEmpty(passwordTxt.getText().toString())) {
            return false;
        };
        return true;
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