package com.example.tinderproject.Matches;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tinderproject.SplashScreenActivity;
import com.example.tinderproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MatchesActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mMatchesAdapter;
    private RecyclerView.LayoutManager mMatchesLayoutManager;
    private ImageButton mBack;
    private DatabaseReference current;
    private ValueEventListener listen;
    private HashMap<String, Integer> mList = new HashMap<>();
    private String currentUserId, mLastTimeStamp, mLastMessage, lastSeen;
    DatabaseReference mCurrUserIdInsideMatchConnections, mCheckLastSeen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matches);
        //mBack = findViewById(R.id.matchesBack);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setHasFixedSize(true);
        mMatchesLayoutManager = new LinearLayoutManager(MatchesActivity.this);
        mRecyclerView.setLayoutManager(mMatchesLayoutManager);
        mMatchesAdapter = new MatchesAdapter(getDataSetMatches(), MatchesActivity.this);
        mRecyclerView.setAdapter(mMatchesAdapter);

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MatchesActivity.this, SplashScreenActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        getUserMatchId();
        mLastMessage = mLastTimeStamp = lastSeen = "";
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void getLastMessageInfo(DatabaseReference userDb) {
        mCurrUserIdInsideMatchConnections = userDb.child("connections").child("matches").child(currentUserId);

        mCurrUserIdInsideMatchConnections.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("lastMessage").getValue() != null && snapshot.child("lastTimeStamp").getValue() != null
                    && snapshot.child("lastSend").getValue() != null) {
                        mLastMessage = snapshot.child("lastMessage").getValue().toString();
                        mLastTimeStamp = snapshot.child("lastTimeStamp").getValue().toString();
                        lastSeen = snapshot.child("lastSend").getValue().toString();
                    } else {
                        mLastMessage = "Start Chatting now";
                        mLastTimeStamp = "";
                        lastSeen = "true";
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getUserMatchId() {
        Query sortedMatchesByLastTimeStamp = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId).child("connections").child("matches")
                .orderByChild("lastTimeStamp");

        sortedMatchesByLastTimeStamp.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot match: snapshot.getChildren()) {
                        FetchMatchInformation(match.getKey(), match.child("ChatId").toString());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void FetchMatchInformation(final String key, final String chatId) {
        DatabaseReference userDb = FirebaseDatabase.getInstance().getReference().child("Users").child(key);
        getLastMessageInfo(userDb);

        userDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String userId = snapshot.getKey();
                    String name = "";
                    String profileImageUrl = "";
                    String need = "";
                    String give = "";
                    String budget = "";
                    final String lastMessage = "";
                    String lastTimeStamp = "";

                    if (snapshot.child("name").getValue() != null) {
                        name = snapshot.child("name").getValue().toString();
                    }
                    if (snapshot.child("profileImageUrl").getValue() != null) {
                        profileImageUrl = snapshot.child("profileImageUrl").getValue().toString();
                    }
                    if (snapshot.child("need").getValue() != null) {
                        need = snapshot.child("need").getValue().toString();
                    }
                    if (snapshot.child("give").getValue() != null) {
                        give = snapshot.child("give").getValue().toString();
                    }
                    if (snapshot.child("budget").getValue() != null) {
                        budget = snapshot.child("budget").getValue().toString();
                    }

                    String miliSec = mLastTimeStamp;
                    Long now;
                    try {
                        now = Long.parseLong(miliSec);
                        lastTimeStamp = convertMilliToRelative(now);
                        String[] arrOfStr = lastTimeStamp.split(",");
                        mLastTimeStamp = arrOfStr[0];
                    } catch (Exception e) {}

                    MatchesObject obj = new MatchesObject(userId, name, profileImageUrl, need, give, budget
                    , mLastMessage, mLastTimeStamp, chatId, lastMessage);
                    if (mList.containsKey(chatId)) {
                        int key = mList.get(chatId);
                        resultsMatches.set(resultsMatches.size() - key, obj);
                    } else {
                        resultsMatches.add(0, obj);
                        mList.put(chatId, resultsMatches.size());
                    }
                    mMatchesAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String convertMilliToRelative(Long now) {
        String time = DateUtils.getRelativeDateTimeString(this, now, DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString();
        return time;
    }

    private ArrayList<MatchesObject> resultsMatches = new ArrayList<>();
    private List<MatchesObject> getDataSetMatches() {
        return resultsMatches;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        return;
    }

}