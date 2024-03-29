package com.example.tinderproject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BtnDisLikeActivity extends AppCompatActivity {

    private static final String TAG = "BtnDisLikeActivity";
    private static final int ACTIVITY_NUM = 1;
    private Context mContext = BtnDisLikeActivity.this;
    private ImageView dislike;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btn_dis_like);

        setupTopNavigationView();
        dislike = findViewById(R.id.dislike);

        Intent intent = getIntent();
        String profileUrl = intent.getStringExtra("url");
        switch (profileUrl){
            case "default":
                Glide.with(mContext).load(R.drawable.profile).into(dislike);
                break;
            default:
                Glide.with(mContext).load(profileUrl).into(dislike);
                break;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Thread.sleep(1000);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
                Intent mainIntent = new Intent(BtnDisLikeActivity.this, MainActivity.class);
                startActivity(mainIntent);
            }
        }).start();
    }

    private void setupTopNavigationView() {
        BottomNavigationView txEx = findViewById(R.id.topNavViewBar);
        TopNavigationViewHelper.setupTopNavigationView(txEx);;
        TopNavigationViewHelper.enableNavigation(mContext, txEx);;
        Menu menu = txEx.getMenu();
        MenuItem menuItem = menu.getItem(ACTIVITY_NUM);
        menuItem.setChecked(true);
    }
}