package com.example.arafat_213.privatechatrooms;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    private static final String DOMAIN_NAME = "gmail.com";

    //widgets
    private EditText mEmail, mPassword, mConfirmPassword;
    private Button mRegister;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        mEmail = findViewById(R.id.input_email);
        mPassword = findViewById(R.id.input_password);
        mConfirmPassword = findViewById(R.id.input_confirm_password);
        mRegister = findViewById(R.id.btn_register);
        mProgressBar = findViewById(R.id.progressBar);

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isEmpty(mEmail.getText().toString())
                        && !isEmpty(mPassword.getText().toString())
                        && !isEmpty(mConfirmPassword.getText().toString())) {
                    if (isValidDomain(mEmail.getText().toString())) {
                        if (doPasswordsMatch(mPassword.getText().toString(), mConfirmPassword.getText().toString())) {
                            //Initiate registration task
                            registerNewEmail(
                                    mEmail.getText().toString(),
                                    mPassword.getText().toString());
                        } else {
                            Toast.makeText(getApplicationContext(), "Passwords don't match", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Please register with official mail", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "You did't fill all the details", Toast.LENGTH_SHORT).show();
                }
            }
        });

        hideSoftKeyboard();
    }


    private void registerNewEmail(String email, String password) {
        showDialog();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail: onComplete");
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: AuthState: " + FirebaseAuth.getInstance().getCurrentUser().getUid());

                            //send email verificaiton
                            sendVerificationEmail();

                            FirebaseAuth.getInstance().signOut();

                            redirectLoginScreen();
                        } else if (!task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Registration failed", Toast.LENGTH_SHORT).show();
                        }
                        hideDialog();
                    }
                });
    }

    private void redirectLoginScreen() {
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }

    private boolean isValidDomain(String email) {
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        Log.d(TAG, "user domain: " + domain);
        return domain.equals(DOMAIN_NAME);
    }

    private boolean doPasswordsMatch(String s1, String s2) {
        return s1.equals(s2);
    }

    /**
     * Return true if the @param is null
     *
     * @param string
     * @return boolean
     */
    private boolean isEmpty(String string) {
        return string.equals("");
    }


    private void showDialog() {
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void hideDialog() {
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
        ----------------------------- Firebase ---------------------------------
     */

    private void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(getApplicationContext(), "Sent verification Email", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Couldn't send verification Email", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


}
