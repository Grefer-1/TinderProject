package com.example.tinderproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private Button mRegister;
    private ProgressBar spinner;
    private EditText mEmail, mPassword, mName, mBudget;

    private RadioGroup mRadioGroup;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthStateListener;
    private String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        spinner = (ProgressBar) findViewById(R.id.pBar);
        spinner.setVisibility(View.GONE);
        TextView existing = (TextView) findViewById(R.id.existing);
        firebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                spinner.setVisibility(View.VISIBLE);
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null && user.isEmailVerified()){
                    Intent i = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(i);
                    finish();
                    spinner.setVisibility(View.GONE);
                    return;
                }
                spinner.setVisibility(View.GONE);
            }
        };

        existing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(i);
                finish();
                return;
            }
        });

        mRegister = (Button) findViewById(R.id.register);
        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mName = (EditText) findViewById(R.id.name);
        final CheckBox checkBox = (CheckBox) findViewById(R.id.checkbox1);
        TextView textView = (TextView) findViewById(R.id.TextView2);

        checkBox.setText("");
        textView.setText(Html.fromHtml("I have read agree to the " +
                "<a href = 'https://www.blogger.com/blog/post/edit/2191961955848249063/7821114277933759467'> Term & Conditions</a>"));
        textView.setClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinner.setVisibility(View.VISIBLE);

                final String email = mEmail.getText().toString();
                final String password = mPassword.getText().toString();
                final String name = mName.getText().toString();
                final Boolean tnc = checkBox.isChecked();

                if(checkInputs(email,password,name,tnc)){
                    mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(!task.isSuccessful()){
                                Toast.makeText(RegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }else{
                                mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(RegisterActivity.this, "Registration success." + "Please verify your email", Toast.LENGTH_SHORT).show();
                                            String userId = mAuth.getCurrentUser().getUid();
                                            DatabaseReference currentUserdb = FirebaseDatabase.getInstance().getReference()
                                                    .child("users").child(userId);
                                            Map userInfo = new HashMap<>();
                                            userInfo.put("name", name);
                                            userInfo.put("profileImageUrl", "default");

                                            currentUserdb.updateChildren(userInfo);

                                            mEmail.setText("");
                                            mName.setText("");
                                            mPassword.setText("");
                                            Intent i = new Intent(RegisterActivity.this, Choose_Login_And_Reg.class);
                                            startActivity(i);
                                            finish();
                                            return;
                                        }
                                        else {
                                            Toast.makeText(RegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
                spinner.setVisibility(View.GONE);
            }
        });
    }

    private boolean checkInputs(String email, String password, String username, Boolean tnc) {
        if(email.equals("") || username.equals("") || password.equals("")){
            Toast.makeText(this, "All fields must be filled out", Toast.LENGTH_SHORT).show();
            return false;
        }
        if(!email.matches(emailPattern)){
            Toast.makeText(this, "Invalid Email, Please try again", Toast.LENGTH_SHORT).show();
            return false;
        }
        if(!tnc){
            Toast.makeText(this, "Please accept Terms and Condition", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthStateListener);
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(RegisterActivity.this, Choose_Login_And_Reg.class);
        startActivity(i);
        finish();
    }
}