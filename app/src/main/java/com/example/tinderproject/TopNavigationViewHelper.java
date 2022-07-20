package com.example.tinderproject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.example.tinderproject.Matches.MatchesActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class TopNavigationViewHelper {
    private static final String TAG = "TopNavigationViewHelper";

    public static void setupTopNavigationView(BottomNavigationView tv) {
        Log.d(TAG, "setupTopNavigationView: setting up navigationview");
    }

    public static void enableNavigation(final Context context, BottomNavigationView view) {
        view.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.ic_profile:
                        Intent i = new Intent(context, SettingActivity.class);
                        context.startActivity(i);
                        break;
                    case R.id.ic_matched:
                        Intent ii = new Intent(context, MatchesActivity.class);
                        context.startActivity(ii);
                        break;
                }
                return false;
            }
        });
    }
}
