package com.example.tinderproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class Choose_Login_And_Reg extends AppCompatActivity {

    private Button mLogin, mRegister;
    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_login_and_reg);

        spinner = (ProgressBar) findViewById(R.id.pBar);
        spinner.setVisibility(View.GONE);

        mLogin = (Button) findViewById(R.id.login);
        mRegister = (Button) findViewById(R.id.register);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Choose_Login_And_Reg.this, LoginActivity.class);
                startActivity(i);
            }
        });

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Choose_Login_And_Reg.this, RegisterActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    public void onBackPressed(){
        finishAffinity();
        finish();
    }
}