package com.example.tinderproject.Chat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tinderproject.Matches.MatchesActivity;
import com.example.tinderproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mChatAdapter;
    private RecyclerView.LayoutManager mChatLayoutManager;

    private EditText mSendEditText;
    private ImageButton mBack;

    private ImageButton mSendButton;
    private String notification;
    private String currentUserID, matchId, chatId;
    private String matchName, matchGive, matchNeed, matchBudget, matchProfile;
    private String lastMessage, lastTimeStamp;
    private String message, createdByUser, isSeen, messageId, currentUserName;
    private Boolean currentUserBoolean;
    ValueEventListener seenListener;
    DatabaseReference mDatabaseUser, mDatabaseChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        matchId = getIntent().getExtras().getString("matchId");
        matchName = getIntent().getExtras().getString("matchName");
        matchGive = getIntent().getExtras().getString("give");
        matchNeed = getIntent().getExtras().getString("need");
        matchBudget = getIntent().getExtras().getString("budget");
        matchProfile = getIntent().getExtras().getString("profile");
        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabaseUser = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(currentUserID).child("connections").child("matches").child(matchId).child("ChatId");
        mDatabaseChat = FirebaseDatabase.getInstance().getReference().child("Chat");

        getChatId();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setFocusable(false);
        mChatLayoutManager = new LinearLayoutManager(ChatActivity.this);
        mRecyclerView.setLayoutManager(mChatLayoutManager);
        mChatAdapter = new ChatAdapter(getDataSetChat(), ChatActivity.this);
        mRecyclerView.setAdapter(mChatAdapter);

        mSendEditText = findViewById(R.id.message);
        mBack = findViewById(R.id.chatBack);

        mSendButton = findViewById(R.id.send);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        mRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mRecyclerView.smoothScrollToPosition(mRecyclerView.getAdapter().getItemCount() - 1);
                        }
                    }, 100);
                }
            }
        });

        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatActivity.this, MatchesActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });

        Toolbar toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(currentUserID);
        Map onChat = new HashMap();
        onChat.put("onChat", matchId);
        reference.updateChildren(onChat);

        DatabaseReference current = FirebaseDatabase.getInstance().getReference("Users")
                .child(matchId).child("connections").child("matches").child(currentUserID);
        Map lastSeen = new HashMap();
        lastSeen.put("lastSeen", "false");
        current.updateChildren(lastSeen);
    }

    private void sendMessage() {
        final String sendMessageText = mSendEditText.getText().toString();
        long now = System.currentTimeMillis();
        String timeStamp = Long.toString(now);

        if (!sendMessageText.isEmpty()) {
            DatabaseReference newMessageDb = mDatabaseChat.push();

            Map newMessage = new HashMap();
            newMessage.put("createByUser", currentUserID);
            newMessage.put("text", sendMessageText);
            newMessage.put("timeStamp", timeStamp);
            newMessage.put("seen", false);

            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        if (snapshot.child("name").exists())
                            currentUserName = snapshot.child("name").getValue().toString();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

            lastMessage = sendMessageText;
            lastTimeStamp = timeStamp;
            updateLastMessage();
            seenMessage(sendMessageText);
            newMessageDb.setValue(newMessage);
        }
        mSendEditText.setText(null);
    }

    private void updateLastMessage() {
        DatabaseReference currUserDb = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID)
                .child("connections").child("matches").child(matchId);
        DatabaseReference matchDb = FirebaseDatabase.getInstance().getReference().child("Users").child(matchId)
                .child("connections").child("matches").child(currentUserID);

        Map lastMessageMap = new HashMap();
        lastMessageMap.put("lastMessage", lastMessage);
        Map lastTimestampMap = new HashMap();
        lastTimestampMap.put("lastTimeStamp", lastTimeStamp);

        Map lastSeen = new HashMap();
        lastSeen.put("lastSeen", "true");
        currUserDb.updateChildren(lastSeen);
        currUserDb.updateChildren(lastMessageMap);
        currUserDb.updateChildren(lastTimestampMap);

        matchDb.updateChildren(lastMessageMap);
        matchDb.updateChildren(lastTimestampMap);
    }

    private ArrayList<ChatObject> resultsChat = new ArrayList<>();

    private List<ChatObject> getDataSetChat() {
        return resultsChat;
    }

    private void getChatId() {
        mDatabaseUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    chatId = snapshot.getValue().toString();
                    mDatabaseChat = mDatabaseChat.child(chatId);
                    getChatMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getChatMessage() {
        mDatabaseChat.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()) {
                    messageId = null;
                    message = null;
                    createdByUser = null;
                    isSeen = null;
                    if (snapshot.child("text").getValue() != null) {
                        message = snapshot.child("text").getValue().toString();
                    }
                    if (snapshot.child("createByUser").getValue() != null) {
                        createdByUser = snapshot.child("createByUser").getValue().toString();
                    }
                    if (snapshot.child("seen").getValue() != null) {
                        isSeen = snapshot.child("seen").getValue().toString();
                    } else
                        isSeen = "true";

                    messageId = snapshot.getKey().toString();
                    if (message != null && createdByUser != null) {
                        currentUserBoolean = false;
                        if (createdByUser.equals(currentUserID)) {
                            currentUserBoolean = true;
                        }
                        ChatObject newMessage = null;
                        if (isSeen.equals("false")) {
                            if (!currentUserBoolean) {
                                isSeen = "true";

                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Chat")
                                        .child(chatId).child(messageId);

                                Map seenInfo = new HashMap();
                                seenInfo.put("seen", "true");
                                reference.updateChildren(seenInfo);

                                newMessage = new ChatObject(message, currentUserBoolean, true);
                            }
                            else {
                                newMessage = new ChatObject(message, currentUserBoolean, false);
                            }
                        } else
                            newMessage = new ChatObject(message, currentUserBoolean, true);

                        DatabaseReference usersInChat = FirebaseDatabase.getInstance().getReference().child("Chat").child(matchId);

                        resultsChat.add(newMessage);
                        mChatAdapter.notifyDataSetChanged();
                        if (mRecyclerView.getAdapter() != null && resultsChat.size() > 0)
                            mRecyclerView.smoothScrollToPosition(resultsChat.size() - 1);
                        else
                            Toast.makeText(ChatActivity.this, "Chat Empty.", Toast.LENGTH_SHORT).show();
                    }
                    if (snapshot.child("text").getValue() != null) {
                        message = snapshot.child("text").getValue().toString();
                    }
                    if (snapshot.child("text").getValue() != null) {
                        message = snapshot.child("text").getValue().toString();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onPause() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(currentUserID);
        Map onChat = new HashMap();
        onChat.put("onChat", "None");
        reference.updateChildren(onChat);
        super.onPause();
    }

    @Override
    protected void onStop() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(currentUserID);
        Map onChat = new HashMap();
        onChat.put("onChat", "None");
        reference.updateChildren(onChat);
        super.onStop();
    }

    private void seenMessage(final String text) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(matchId);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("onChat").exists()) {
                        if (snapshot.child("notificationKey").exists())
                            notification = snapshot.child("notificationKey").getValue().toString();
                        else
                            notification = "";

                        if (!snapshot.child("onChat").getValue().toString().equals(currentUserID)) {
                            new SendNotification(text, "New message from: " + currentUserName, notification,
                                    "activityTpBeOpened", "MatchesActivity");
                        } else {
                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Users")
                                    .child(currentUserID).child("connections").child("matches").child(matchId);
                            Map seenInfo = new HashMap();
                            seenInfo.put("lastSeen", "false");
                            reference.updateChildren(seenInfo);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    public void showProfile(View view) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.item_profile, null);

        TextView name = (TextView) popupView.findViewById(R.id.name);
        ImageView image = (ImageView) popupView.findViewById(R.id.image);
        TextView budget = (TextView) popupView.findViewById(R.id.budget);
        ImageView mNeedImage = (ImageView) popupView.findViewById(R.id.needImage);
        ImageView mGiveImage = (ImageView) popupView.findViewById(R.id.giveImage);

        name.setText(matchName);
        budget.setText(matchBudget);

        if (matchNeed.equals("Netflix"))
            mNeedImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.netflix));
        else if (matchNeed.equals("Hulu"))
            mNeedImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.hulu));
        else if (matchNeed.equals("Vudu"))
            mNeedImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.vudu));
        else if (matchNeed.equals("HBO Now"))
            mNeedImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.hbo));
        else if (matchNeed.equals("Youtube Originals"))
            mNeedImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.youtube));
        else
            mNeedImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.none));

        if (matchNeed.equals("Netflix"))
            mGiveImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.netflix));
        else if (matchNeed.equals("Hulu"))
            mGiveImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.hulu));
        else if (matchNeed.equals("Vudu"))
            mGiveImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.vudu));
        else if (matchNeed.equals("HBO Now"))
            mGiveImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.hbo));
        else if (matchNeed.equals("Youtube Originals"))
            mGiveImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.youtube));
        else
            mGiveImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.none));

        switch (matchProfile) {
            case "default" :
                Glide.with(popupView.getContext()).load(R.drawable.profile).into(image);
                break;
            default:
                Glide.with(popupView.getContext()).clear(image);
                Glide.with(popupView.getContext()).load(matchProfile).into(image);
                break;
        }

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.MATCH_PARENT;

        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        hideSoftKeyBoard();

        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    private void hideSoftKeyBoard() {
        InputMethodManager inm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (inm.isAcceptingText()) {
            inm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.unmatch) {
            new AlertDialog.Builder(ChatActivity.this)
                    .setTitle("Unmatch")
                    .setMessage("Are you sure you want to unmatch?")
                    .setPositiveButton("Unmatch", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            deleteMatch(matchId);
                            Intent intent = new Intent(ChatActivity.this, MatchesActivity.class);
                            startActivity(intent);
                            finish();
                            Toast.makeText(ChatActivity.this, "Unmatch successful", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Dismiss", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else if (item.getItemId() == R.id.viewProfile) {
            showProfile(findViewById(R.id.content));
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteMatch(String matchId) {
        DatabaseReference matchId_in_UserId_dbReference = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(currentUserID).child("connections").child("matches").child(matchId);
        DatabaseReference userId_in_matchId_dbReference = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(matchId).child("connections").child("matches").child(currentUserID);
        DatabaseReference yeps_in_matchId_dbReference = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(matchId).child("connections").child("yeps").child(currentUserID);
        DatabaseReference yeps_in_UserId_dbReference = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(currentUserID).child("connections").child("yeps").child(matchId);

        DatabaseReference matchId_chat_dbReference = FirebaseDatabase.getInstance().getReference().child("Chat").child(chatId);

        matchId_chat_dbReference.removeValue();
        matchId_in_UserId_dbReference.removeValue();
        userId_in_matchId_dbReference.removeValue();
        yeps_in_matchId_dbReference.removeValue();
        yeps_in_UserId_dbReference.removeValue();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        return;
    }
}