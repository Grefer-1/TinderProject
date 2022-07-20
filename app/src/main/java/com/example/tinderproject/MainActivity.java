package com.example.tinderproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tinderproject.Cards.arrayAdapter;
import com.example.tinderproject.Cards.cards;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.onesignal.OSNotificationAction;
import com.onesignal.OSNotificationOpenedResult;
import com.onesignal.OneSignal;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.dimorinny.showcasecard.ShowCaseView;
import ru.dimorinny.showcasecard.position.ShowCasePosition;
import ru.dimorinny.showcasecard.position.ViewPosition;
import ru.dimorinny.showcasecard.radius.Radius;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private cards cards_data[];
    private com.example.tinderproject.Cards.arrayAdapter arrayAdapter;
    private int i;
    private String tag;
    private FirebaseAuth mAuth;
    private ProgressBar spinner;
    boolean firstStart;
    private String currentUId, notification, sendMessageText;
    private boolean activityStarted;
    private DatabaseReference usersDb;

    ListView listView;
    List<cards> rowItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityStarted = true;
        setContentView(R.layout.activity_main);
        spinner = (ProgressBar)findViewById(R.id.pBar);
        spinner.setVisibility(View.GONE);
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        firstStart = prefs.getBoolean("firstStart", true);
        setupTopNavigationView();

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }

        tag = "MainActivity";
        usersDb = FirebaseDatabase.getInstance().getReference().child("Users");

        mAuth = FirebaseAuth.getInstance();
        if(mAuth != null && mAuth.getCurrentUser() != null)
            currentUId = mAuth.getCurrentUser().getUid();
        else{
            Log.d(tag, "Authorization failed");
            Toast.makeText(getApplicationContext(), "Auth failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        OneSignal.initWithContext(this);
        OneSignal.setAppId("e1e1e37f-6861-4cc6-a6f1-500de6a4ae9a");

        Log.d(tag, "onCreate " + currentUId);

        checkUserSex();

        rowItems = new ArrayList<cards>();

        arrayAdapter = new arrayAdapter(this, R.layout.item, rowItems);


        final SwipeFlingAdapterView flingContainer = (SwipeFlingAdapterView) findViewById(R.id.frame);

        flingContainer.setAdapter(arrayAdapter);
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                Log.d(tag, "removed object!");
                rowItems.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {

                cards obj = (cards) dataObject;
                String userId = obj.getUserId();
                usersDb.child(userId).child("connections").child("nope").child(currentUId).setValue(true);
                Toast.makeText(MainActivity.this, "Left", Toast.LENGTH_SHORT).show();

                //Display a banner when no cards are available to display
                TextView tv = (TextView)findViewById(R.id.noCardsBanner);
                if(rowItems.size() == 0) {
                    tv.setVisibility(View.VISIBLE);
                } else {
                    tv.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                cards obj = (cards) dataObject;
                String userId = obj.getUserId();
                usersDb.child(userId).child("connections").child("yeps").child(currentUId).setValue(true);
                isConnectionMatch(userId);
                Toast.makeText(MainActivity.this, "Right", Toast.LENGTH_SHORT).show();

                //Display a banner when no cards are available to display
                TextView tv = (TextView)findViewById(R.id.noCardsBanner);
                if(rowItems.size() == 0) {
                    tv.setVisibility(View.VISIBLE);
                } else {
                    tv.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
            }

            @Override
            public void onScroll(float scrollProgressPercent) {
                View view = flingContainer.getSelectedView();
                view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);

            }
        });


        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                Toast.makeText(MainActivity.this, "Item Clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupTopNavigationView(){
        BottomNavigationView txEx = findViewById(R.id.topNavViewBar);
        TopNavigationViewHelper.setupTopNavigationView(txEx);;
        TopNavigationViewHelper.enableNavigation(MainActivity.this, txEx);;
        Menu menu = txEx.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);

        View profile_view = findViewById(R.id.ic_profile);
        View matches_view = findViewById(R.id.ic_matched);

        if(firstStart){
            showToolTip_profile(new ViewPosition(profile_view));
            showToolTip_matches(new ViewPosition(matches_view));
        }

        SharedPreferences newPref = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = newPref.edit();
        editor.putBoolean("firstStart", false);
        editor.apply();
    }

    private void showToolTip_profile(ShowCasePosition position) {
        new ShowCaseView.Builder(MainActivity.this)
                .withTypedPosition(position)
                .withTypedRadius(new Radius(186F))
                .withContent("First time upload your profile picture and click on Confirm other wise your app will not work")
                .build()
                .show(MainActivity.this);
    }

    private void showToolTip_matches(ShowCasePosition position) {
        new ShowCaseView.Builder(MainActivity.this)
                .withTypedPosition(position)
                .withTypedRadius(new Radius(186F))
                .withContent("Find you matches and begin chatting!")
                .build()
                .show(MainActivity.this);
    }

    public void DislikeBtn(View v) {
        if (rowItems.size() != 0) {
            cards card_item = rowItems.get(0);
            String userId = card_item.getUserId();
            usersDb.child(userId).child("connections").child("nope").child(currentUId).setValue(true);
            Toast.makeText(MainActivity.this, "Left", Toast.LENGTH_SHORT).show();

            rowItems.remove(0);
            arrayAdapter.notifyDataSetChanged();

            //Display a banner when no cards are available to display
            TextView tv = (TextView)findViewById(R.id.noCardsBanner);
            if(rowItems.size() == 0) {
                tv.setVisibility(View.VISIBLE);
            } else {
                tv.setVisibility(View.INVISIBLE);
            }

            Intent btnClick = new Intent(MainActivity.this, BtnDisLikeActivity.class);
            btnClick.putExtra("url", card_item.getProfileImageUrl());
            startActivity(btnClick);

        }
    }

    public void LikeBtn(View v) {
        if (rowItems.size() != 0) {
            cards card_item = rowItems.get(0);
            String userId = card_item.getUserId();
            //check matches
            usersDb.child(userId).child("connections").child("yeps").child(currentUId).setValue(true);
            isConnectionMatch(userId);
            Toast.makeText(MainActivity.this, "Right", Toast.LENGTH_SHORT).show();

            rowItems.remove(0);
            arrayAdapter.notifyDataSetChanged();


            //Display a banner when no cards are available to display
            TextView tv = (TextView)findViewById(R.id.noCardsBanner);
            if(rowItems.size() == 0) {
                tv.setVisibility(View.VISIBLE);
            } else {
                tv.setVisibility(View.INVISIBLE);
            }

            Intent btnClick = new Intent(MainActivity.this, BtnLikeActivity.class);
            btnClick.putExtra("url", card_item.getProfileImageUrl());
            startActivity(btnClick);

        }
    }

    private void isConnectionMatch(final String userId) {
        DatabaseReference currentUserConnectionsDb = usersDb.child(currentUId).child("connections").child("yeps").child(userId);
        usersDb.child(currentUId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                    sendMessageText = dataSnapshot.getValue().toString();
                else
                    sendMessageText = "";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        if(!currentUId.equals(userId)) {
            currentUserConnectionsDb.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        Toast.makeText(MainActivity.this, "" +
                                "New Connection", Toast.LENGTH_LONG).show();

                        String key = FirebaseDatabase.getInstance().getReference().child("Chat").push().getKey();
                        Map mapLastTimeStamp = new HashMap<>();
                        long now  = System.currentTimeMillis();
                        String timeStamp = Long.toString(now);
                        mapLastTimeStamp.put("lastTimeStamp", timeStamp);

                        usersDb.child(dataSnapshot.getKey()).child("connections").child("matches").child(currentUId).child("ChatId").setValue(key);
                        usersDb.child(dataSnapshot.getKey()).child("connections").child("matches").child(currentUId).updateChildren(mapLastTimeStamp);

                        usersDb.child(currentUId).child("connections").child("matches").child(dataSnapshot.getKey()).child("ChatId").setValue(key);
                        usersDb.child(currentUId).child("connections").child("matches").child(dataSnapshot.getKey()).updateChildren(mapLastTimeStamp);

                        notification = " ";

                        DatabaseReference notificationID = FirebaseDatabase.getInstance().getReference().child("Users").child(userId).child("notificationKey");
                        notificationID.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if(snapshot.exists()) {
                                    notification = snapshot.getValue().toString();
                                    Log.d("sendChat", notification);
                                    new SendNotification("You have a new match!", "", notification, null, null );
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                            }
                        });

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }

            });
        }
    }

    private String userNeed, userGive;
    private String oppositeUserNeed, oppositeUserGive;
    public void checkUserSex(){

        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userDb = usersDb.child(user.getUid());

        userDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Log.d("CardSearch", dataSnapshot.toString());

                if (dataSnapshot.exists()){
                    if (dataSnapshot.child("need").getValue() != null){
                        // Log.d("CardSearch", "exists coloumn called");

                        userNeed = dataSnapshot.child("need").getValue().toString();
                        userGive = dataSnapshot.child("give").getValue().toString();
                        //  Log.d("CardSearch", "datachange called");

                        oppositeUserGive = userNeed;
                        oppositeUserNeed = userGive;
                        getOppositeSexUsers(oppositeUserGive, oppositeUserNeed);
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void getOppositeSexUsers(final String oppositeUserGive, final String oppositeUserNeed){
        usersDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && !dataSnapshot.getKey().equals(currentUId)) {

                    if (dataSnapshot.child("give").exists() && dataSnapshot.child("need").exists() && !dataSnapshot.child("connections").child("nope").hasChild(currentUId) && !dataSnapshot.child("connections").child("yeps").hasChild(currentUId) && dataSnapshot.child("give").getValue().toString().equals(oppositeUserGive) && dataSnapshot.child("need").getValue().toString().equals(oppositeUserNeed)) {
                        String profileImageUrl = "default";
                        if (!dataSnapshot.child("profileImageUrl").getValue().equals("default")) {
                            profileImageUrl = dataSnapshot.child("profileImageUrl").getValue().toString();
                        }
                        cards item = new cards(dataSnapshot.getKey(), dataSnapshot.child("name").getValue().toString(), profileImageUrl, dataSnapshot.child("need").getValue().toString(), dataSnapshot.child("give").getValue().toString(), dataSnapshot.child("budget").getValue().toString());
                        rowItems.add(item);
                        arrayAdapter.notifyDataSetChanged();
                    }
                    else if( dataSnapshot.child("give").exists() && !dataSnapshot.child("connections").child("nope").hasChild(currentUId) && !dataSnapshot.child("connections").child("yeps").hasChild(currentUId) && dataSnapshot.child("give").getValue().toString().equals(oppositeUserGive)){
                        String profileImageUrl = "default";
                        if (!dataSnapshot.child("profileImageUrl").getValue().equals("default")) {
                            profileImageUrl = dataSnapshot.child("profileImageUrl").getValue().toString();
                        }
                        cards item = new cards(dataSnapshot.getKey(), dataSnapshot.child("name").getValue().toString(), profileImageUrl, dataSnapshot.child("need").getValue().toString(), dataSnapshot.child("give").getValue().toString(),  dataSnapshot.child("budget").getValue().toString());
                        rowItems.add(item);
                        arrayAdapter.notifyDataSetChanged();
                    }
                    else if( dataSnapshot.child("need").exists() && !dataSnapshot.child("connections").child("nope").hasChild(currentUId) && !dataSnapshot.child("connections").child("yeps").hasChild(currentUId) && dataSnapshot.child("need").getValue().toString().equals(oppositeUserNeed)){
                        String profileImageUrl = "default";
                        if (!dataSnapshot.child("profileImageUrl").getValue().equals("default")) {
                            profileImageUrl = dataSnapshot.child("profileImageUrl").getValue().toString();
                        }
                        cards item = new cards(dataSnapshot.getKey(), dataSnapshot.child("name").getValue().toString(), profileImageUrl, dataSnapshot.child("need").getValue().toString(), dataSnapshot.child("give").getValue().toString(),  dataSnapshot.child("budget").getValue().toString());
                        rowItems.add(item);
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
                //spinner.setVisibility(View.GONE);


                //Display a banner when no cards are available to display
                TextView tv = (TextView)findViewById(R.id.noCardsBanner);
                if(rowItems.size() == 0) {
                    tv.setVisibility(View.VISIBLE);
                } else {
                    tv.setVisibility(View.INVISIBLE);
                }


            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        return;
    }

    public class NotificationOpenedHandler implements OneSignal.OSNotificationOpenedHandler {
        private Context mContext;
        public NotificationOpenedHandler(Context context) {
            mContext = context;
        }
        @Override
        public void notificationOpened(OSNotificationOpenedResult result) {
            OSNotificationAction.ActionType actionType = result.getAction().getType();
            JSONObject data = result.getNotification().toJSONObject();
            String activityToBeOpened;

            if (data != null)
            {
                activityToBeOpened = data.optString("activityToBeOpened", null);
                if (activityToBeOpened != null && activityToBeOpened.equals("MatchesActivity"))
                {
                    try {
                        Intent intent3 = new Intent(getApplicationContext(), SettingActivity.class);
                        intent3.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);

                        Log.i("OneSignal", "customkey set with value: " + activityToBeOpened);
                        mContext.startActivity(intent3);
                    }
                    catch (Throwable t){
                        t.printStackTrace();
                    }
                }
            }
        }
    }
}