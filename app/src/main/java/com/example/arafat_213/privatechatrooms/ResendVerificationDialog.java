package com.example.arafat_213.privatechatrooms;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arafat_213.privatechatrooms.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResendVerificationDialog extends DialogFragment {

    private static final String TAG = "ResendVerificationDialo";

    private EditText mConfirmEmail, mConfirmPassword;

    private TextView confirmDialog, cancelDialog;

    private Context mContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_resend_verification, container, false);

        mContext = getActivity();
        mConfirmEmail = view.findViewById(R.id.confirm_email);
        mConfirmPassword = view.findViewById(R.id.confirm_password);

        confirmDialog = view.findViewById(R.id.dialogConfirm);
        confirmDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: attempting to resend verification email");

                if (!isEmpty(mConfirmEmail.getText().toString())
                        && !isEmpty(mConfirmPassword.getText().toString())) {
                    //temporarily authenticate and resend verification email
                    authenticanteAndResendEmail(
                            mConfirmEmail.getText().toString(),
                            mConfirmPassword.getText().toString()
                    );
                } else {
                    Toast.makeText(mContext, "All fields must be filled out", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cancelDialog = view.findViewById(R.id.dialogCancel);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().dismiss();
            }
        });

        return view;
    }


    /**
     * reauthenticate so we can send a verification email again
     *
     * @param email
     * @param password
     */

    private void authenticanteAndResendEmail(String email, String password) {
        AuthCredential credential = EmailAuthProvider
                .getCredential(email, password);
        FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: re-authenticate success");
                    sendVerificationEmail();
                    FirebaseAuth.getInstance().signOut();
                    if(getDialog() != null)
                        getDialog().dismiss();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(mContext, "Invalid credentials", Toast.LENGTH_SHORT).show();
                getDialog().dismiss();
            }
        });

    }


    private void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(mContext, "Verification Email sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mContext, "Could not sent verification email", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


    /**
     * Return true if the @param is null
     *
     * @param string
     * @return
     */
    private boolean isEmpty(String string) {
        return string.equals("");
    }
}
