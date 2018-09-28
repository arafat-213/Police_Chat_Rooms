package com.example.arafat_213.privatechatrooms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arafat_213.privatechatrooms.models.User;
import com.example.arafat_213.privatechatrooms.utilities.FilePaths;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SettingsActivity extends AppCompatActivity implements ChangePhotoDialog.OnPhotoReceivedListener {

    private static final String TAG = "SettingsActivity";

    private static final String DOMAIN_NAME = "gmail.com";

    private static final int REQUEST_CODE = 1234; // random number

    private static final double MB_THRESHOLD = 5.0;

    private static final double MB = 1000000.0;
    private FirebaseAuth.AuthStateListener mAuthListener;
    //widgets
    private EditText mEmail, mCurrentPassword, mName, mPhone;
    private ImageView mProfileImage;
    //vars
    private boolean mStoragePermissions;
    private Uri mSelectedImgUri;
    private Button mSave;
    private ProgressBar mProgressBar;
    private TextView mResetPasswordLink;
    private Bitmap mSelectedImgBitmap;
    private byte[] mBytes;
    private double progress;

    // convert from bitmap to byte array
    public static byte[] getBytesFromBitmap(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    @Override
    public void getImagePath(Uri imgPath) {
        if (!imgPath.toString().equals("")) {
            mSelectedImgUri = imgPath;
            mSelectedImgBitmap = null;
            Log.d(TAG, "getImagePath: got the image uri: " + mSelectedImgUri);
            ImageLoader.getInstance().displayImage(imgPath.toString(), mProfileImage);
        }
    }

    @Override
    public void getImageBitmap(Bitmap imgBitmap) {
        if (imgBitmap != null) {
            mSelectedImgBitmap = imgBitmap;
            mSelectedImgUri = null;
            Log.d(TAG, "getImageBitmap: got the image bitmap " + mSelectedImgBitmap);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mEmail = findViewById(R.id.input_email);
        mCurrentPassword = findViewById(R.id.input_password);
        mName = findViewById(R.id.input_name);
        mPhone = findViewById(R.id.input_phone);
        mProfileImage = findViewById(R.id.profile_image);
        mSave = findViewById(R.id.btn_save);
        mProgressBar = findViewById(R.id.progressBar);
        mResetPasswordLink = findViewById(R.id.change_password);

        verifyStoragePermissions();

        setupFirebaseAuth();

        setCurrentEmail();

        init();

        mResetPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: sending password reset link");

                /*
                ------ Reset Password Link -----
                */
                sendResetPasswordLink();
            }
        });

        hideSoftKeyboard();
    }

    private void init() {
        getUserAccountData();
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //make sure email and current password fields are filled
                if (!isEmpty(mEmail.getText().toString())
                        && !isEmpty(mCurrentPassword.getText().toString())) {

                    /*
                    ------ Change Email Task -----
                     */
                    //if the current email doesn't equal what's in the EditText field then attempt
                    //to edit
                    if (!FirebaseAuth.getInstance().getCurrentUser().getEmail().equals(
                            mEmail.getText().toString()
                    )) {

                        //verify that user is changing to an official email address
                        if (isValidDomain(mEmail.getText().toString())) {
                            editUserEmail();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Invalid Domain", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(SettingsActivity.this, "Email and Current Password Fields Must be Filled to Save", Toast.LENGTH_SHORT).show();
                    }
                }



                /*
                ------ METHOD 1 for changing database data (proper way in this scenario) -----
                 */

                DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                /*
                ------ Change Name -----
                 */
                if (!mName.getText().toString().equals("")) {
                    reference.child(getString(R.string.dbnode_users))
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getString(R.string.field_name))
                            .setValue(mName.getText().toString());
                }

            /*
                ------ Change Phone Number -----
                 */

                if (!mPhone.getText().toString().trim().equals("")) {
                    reference.child(getString(R.string.dbnode_users))
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .child(getString(R.string.field_phone))
                            .setValue(mPhone.getText().toString());
                }

         /*
                ------ Upload the New Photo -----
                 */
                if (mSelectedImgUri != null) {
                    uploadNewPhoto(mSelectedImgUri);
                } else if (mSelectedImgBitmap != null) {
                    uploadNewPhoto(mSelectedImgBitmap);
                }

                Toast.makeText(SettingsActivity.this, "saved", Toast.LENGTH_SHORT).show();

            }


        });

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: started");
                if (mStoragePermissions) {
                    ChangePhotoDialog dialog = new ChangePhotoDialog();
                    dialog.show(getSupportFragmentManager(), getString(R.string.dialog_change_photo));
                } else {
                    verifyStoragePermissions();
                }

            }
        });

    }

    private void verifyStoragePermissions() {
        Log.d(TAG, "verifyStoragePermissions: asking user for permissions");
        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(),
                permissions[2]) == PackageManager.PERMISSION_GRANTED) {
            mStoragePermissions = true;
        } else {
            ActivityCompat.requestPermissions(
                    SettingsActivity.this,
                    permissions,
                    REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: request code : " + requestCode);
        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: user has allowed permissions to access " + permissions[0]);
                }
                break;
        }
    }

    // Gets user data from database to display on edit profile section widgets
    private void getUserAccountData() {

        /*
        ---------- Query method 1 --------
        orderByKey() is used when when we want to search user by their unique "Key"
         */

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query1 = reference.child(getString(R.string.dbnode_users))
                .orderByKey()
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    User user = singleSnapshot.getValue(User.class);
                    Log.d(TAG, "onDataChange: Query method 1 found user: " + user.toString());

                    //user is here. Now show name and phone no data into widgets
                    mName.setText(user.getName());
                    mPhone.setText(user.getPhone());

                    ImageLoader.getInstance().displayImage(user.getProfile_image(), mProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        /*
        -------- Query method 2 --------
         */
        Query query2 = reference.child(getString(R.string.dbnode_users))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());

        query2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    User user = singleSnapshot.getValue(User.class);
                    Log.d(TAG, "onDataChange: Query method 2, found user: " + user.toString());

                    mName.setText(user.getName());
                    mPhone.setText(user.getPhone());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Uploads a new profile photo to Firebase Storage using a @param ***imageUri***
     *
     * @param imageUri
     */
    public void uploadNewPhoto(Uri imageUri) {
        /*
            upload a new profile photo to firebase storage
         */
        Log.d(TAG, "uploadNewPhoto: uploading new photo to firebase storage");

        //Only accept image sizes that are compressed to under 5MB. If thats not possible
        //then do not allow image to be uploaded
        BackgroundImageResize resize = new BackgroundImageResize(null);
        resize.execute(imageUri);
    }

    /**
     * Uploads a new profile photo to Firebase Storage using a @param ***imageBitmap***
     *
     * @param imageBitmap
     */
    public void uploadNewPhoto(Bitmap imageBitmap) {
            /*
            upload a new profile photo to firebase storage
         */
        Log.d(TAG, "uploadNewPhoto: uploading new profile photo to firebase storage.");

        //Only accept image sizes that are compressed to under 5MB. If thats not possible
        //then do not allow image to be uploaded
        BackgroundImageResize resize = new BackgroundImageResize(imageBitmap);
        Uri uri = null;
        resize.execute(uri);
    }

    public void executeUploadTask() {
        showDialog();
        FilePaths filePaths = new FilePaths();
        //specify where the photo will be stored
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(filePaths.FIREBASE_IMAGE_STORAGE + "/" + FirebaseAuth.getInstance().getCurrentUser().getUid()
                        + "/profile_image"); //just replace old image with the new one

        if (mBytes.length / MB < MB_THRESHOLD) {
            // Create file metadata including the content type
            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpg")
                    .setContentLanguage("en")
                    .setCustomMetadata("Arfee's special metadata", "Hahaha jk")
                    .setCustomMetadata("Ye kya hai?", "Ye DP hai")
                    .build();

            //if the image size is valid then we can submit to database
            UploadTask uploadTask = null;
            uploadTask = storageReference.putBytes(mBytes, metadata);
            Log.d(TAG, "executeUploadTask: is file even uploaded?");
            //uploadTask = storageReference.putBytes(mBytes); //without metadata

            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return storageReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri firebaseURL = task.getResult();
                        Toast.makeText(SettingsActivity.this, "Upload Success", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onSuccess: firebase download url : " + firebaseURL.toString());
                        FirebaseDatabase.getInstance().getReference()
                                .child(getString(R.string.dbnode_users))
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .child(getString(R.string.field_profile_image))
                                .setValue(firebaseURL.toString());
                    }
                    hideDialog();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(SettingsActivity.this, "could not upload photo", Toast.LENGTH_SHORT).show();
                    hideDialog();
                }
            });

            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double currentProgress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if (currentProgress > (progress + 15)) {
                        progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "onProgress: Upload is " + progress + "% done");
                        Toast.makeText(SettingsActivity.this, progress + "%", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            Toast.makeText(this, "Image is too Large", Toast.LENGTH_SHORT).show();
        }


    }

    /**
     * 1) doinBackground takes an imageUri and returns the byte array after compression
     * 2) onPostExecute will print the % compression to the log once finished
     */

    public class BackgroundImageResize extends AsyncTask<Uri, Integer, byte[]> {

        private Bitmap mBitmap;

        public BackgroundImageResize(Bitmap bitmap) {
            if (bitmap != null)
                this.mBitmap = bitmap;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog();
            Toast.makeText(SettingsActivity.this, "Compressing image", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected byte[] doInBackground(Uri... uris) {
            Log.d(TAG, "doInBackground: started");

            if (mBitmap == null) {
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(SettingsActivity.this.getContentResolver(), uris[0]);
                    Log.d(TAG, "doInBackground: bitmap size: megabytes: " + mBitmap.getByteCount() / MB + " MB");
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground: IOException: ", e.getCause());
                }
            }

            byte[] bytes = null;
            for (int i = 1; i < 11; i++) {
                if (i == 10) {
                    Toast.makeText(SettingsActivity.this, "Image is too large", Toast.LENGTH_SHORT).show();
                    break;
                }
                bytes = getBytesFromBitmap(mBitmap, 100 / i);
                Log.d(TAG, "doInBackground: megabytes: (" + (11 - i) + "0%) " + bytes.length / MB + " MB");
                if (bytes.length / MB < MB_THRESHOLD) {
                    return bytes;
                }
            }
            return bytes;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            super.onPostExecute(bytes);
            hideDialog();
            mBytes = bytes;
            // execute the upload task
            executeUploadTask();
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        checkAuthenticationState();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null)
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
    }

    // Fetches the user email from firebase and sets the value of @param Edittext mEmail
    private void setCurrentEmail() {
        Log.d(TAG, "setCurrentEmail: setting current email to EditText field");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            Log.d(TAG, "setCurrentEmail: user is NOT null.");

            String email = user.getEmail();

            Log.d(TAG, "setCurrentEmail: got the email: " + email);

            mEmail.setText(email);
        }
    }


    /*
                ----------------------------- Firebase setup ---------------------------------
             */
    private void setupFirebaseAuth() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged: signed in" + user.getUid());
                } else {
                    Log.d(TAG, "onAuthStateChanged: signed out");
                    Toast.makeText(SettingsActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        };
    }

    private void checkAuthenticationState() {
        Log.d(TAG, "checkAuthenticationState: checking authentication state.");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.d(TAG, "checkAuthenticationState: user is null, sending back to log in screen.");

            Intent intent = new Intent(
                    SettingsActivity.this,
                    LoginActivity.class
            );
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );
            startActivity(intent);
            finish();
        } else {
            Log.d(TAG, "checkAuthenticationState: user is authenticated");
        }
    }

    private void sendResetPasswordLink() {
        FirebaseAuth.getInstance().sendPasswordResetEmail(
                FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Password Reset Email sent.");
                            Toast.makeText(SettingsActivity.this, "Sent Password Reset Link to Email",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "onComplete: No user associated with that email.");
                            Toast.makeText(SettingsActivity.this, "No User Associated with that Email.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }

    private void editUserEmail() {
        // Get auth credentials from the user for re-authentication. The example below shows
        // email and password credentials but there are multiple possible providers,
        // such as GoogleAuthProvider or FacebookAuthProvider.

        showDialog();

        AuthCredential credential = EmailAuthProvider
                .getCredential(FirebaseAuth.getInstance().getCurrentUser().getEmail(), mCurrentPassword.getText().toString());
        Log.d(TAG, "editUserEmail: reauthenticating with:  \n email " + FirebaseAuth.getInstance().getCurrentUser().getEmail()
                + " \n passowrd: " + mCurrentPassword.getText().toString());


        FirebaseAuth.getInstance().getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: reauthenticate success.");

                            //make sure the domain is valid
                            if (isValidDomain(mEmail.getText().toString())) {

                                ///////////////////now check to see if the email is not already present in the database
                                FirebaseAuth.getInstance().fetchSignInMethodsForEmail(mEmail.getText().toString()).addOnCompleteListener(
                                        new OnCompleteListener<SignInMethodQueryResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {

                                                if (task.isSuccessful()) {
                                                    ///////// getProviders().size() will return size 1 if email ID is in use.

                                                    Log.d(TAG, "onComplete: RESULT: " + task.getResult().getSignInMethods().size());
                                                    if (task.getResult().getSignInMethods().size() == 1) {
                                                        Log.d(TAG, "onComplete: That email is already in use.");
                                                        hideDialog();
                                                        Toast.makeText(SettingsActivity.this, "That email is already in use", Toast.LENGTH_SHORT).show();

                                                    } else {
                                                        Log.d(TAG, "onComplete: That email is available.");

                                                        /////////////////////add new email
                                                        FirebaseAuth.getInstance().getCurrentUser().updateEmail(mEmail.getText().toString())
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            Log.d(TAG, "onComplete: User email address updated.");
                                                                            Toast.makeText(SettingsActivity.this, "Updated email", Toast.LENGTH_SHORT).show();
                                                                            sendVerificationEmail();
                                                                            FirebaseAuth.getInstance().signOut();
                                                                        } else {
                                                                            Log.d(TAG, "onComplete: Could not update email.");
                                                                            Toast.makeText(SettingsActivity.this, "unable to update email", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                        hideDialog();
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        hideDialog();
                                                                        Toast.makeText(SettingsActivity.this, "unable to update email", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    }

                                                }
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                hideDialog();
                                                Toast.makeText(SettingsActivity.this, "unable to update email", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(SettingsActivity.this, "you must use a company email", Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Log.d(TAG, "onComplete: Incorrect Password");
                            Toast.makeText(SettingsActivity.this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                            hideDialog();
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideDialog();
                        Toast.makeText(SettingsActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(SettingsActivity.this, "Sent Verification Email", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Could not send verification Email", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {

        }
    }

    /**
     * Returns True if the user's email contains '@tabian.ca'
     *
     * @param email
     * @return
     */
    private boolean isValidDomain(String email) {
        Log.d(TAG, "isValidDomain: verifying email has correct domain: " + email);
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        Log.d(TAG, "isValidDomain: users domain: " + domain);
        return domain.equals(DOMAIN_NAME);
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
