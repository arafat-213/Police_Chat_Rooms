package com.example.arafat_213.privatechatrooms;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // widgets
    private EditText mEmail, mPassword;
    private ProgressBar mProgressBar;
    private Button signIn;
    private TextView register, resetPassword, resendEmailVerification;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mEmail = findViewById(R.id.email);
        mPassword = findViewById(R.id.password);
        mProgressBar = findViewById(R.id.progressBar);

        setupFirebaseAuth();
        signIn = findViewById(R.id.email_sign_in_button);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isEmpty(mEmail.getText().toString())
                        && !isEmpty(mPassword.getText().toString())){
                    Log.d(TAG, "onClick: attempting to authenticate");

                    showDialog();

                    FirebaseAuth.getInstance().signInWithEmailAndPassword(
                            mEmail.getText().toString(),
                            mPassword.getText().toString()
                    ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                hideDialog();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                            hideDialog();
                        }
                    });
                } else{
                    Toast.makeText(getApplicationContext(), "You didn't fill all details", Toast.LENGTH_SHORT).show();
                }

            }
        });

        register = findViewById(R.id.link_register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                if(intent != null)
                    startActivity(intent);

            }
        });

        resetPassword = findViewById(R.id.forgot_password);
        resetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PasswordResetDialog passwordResetDialog = new PasswordResetDialog();
                passwordResetDialog.show(getSupportFragmentManager(), "dialog_password_reset");
            }
        });


        resendEmailVerification = findViewById(R.id.resend_verification_email);
        resendEmailVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ResendVerificationDialog resendVerificationDialog = new ResendVerificationDialog();
                resendVerificationDialog.show(getSupportFragmentManager(), "dialog_resend_email_verification");
            }
        });
    }


    private boolean isEmpty(String string){
        return string.equals("");
    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        if(mProgressBar.getVisibility() == View.VISIBLE)
            mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mAuthListener!=null)
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
    }

    /*
        ----------------------------- Firebase setup ---------------------------------
     */

    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started.");
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    if(user.isEmailVerified()){
                        Log.d(TAG, "setupFirebaseAuth: onAuthStateChanged: signed in");
                        Toast.makeText(getApplicationContext(), "Authenticate with: "+user.getEmail(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else{
                        Toast.makeText(getApplicationContext(), "Email is not verified, check your inbox", Toast.LENGTH_SHORT).show();
                        FirebaseAuth.getInstance().signOut();
                    }

                } else {
                    Log.d(TAG, "setupFirebaseAuth: onAuthStateChanged: signed out");
                }
            }
        };
    }
}
