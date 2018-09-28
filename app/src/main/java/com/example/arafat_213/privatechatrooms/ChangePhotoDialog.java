package com.example.arafat_213.privatechatrooms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ChangePhotoDialog extends DialogFragment {

    private static final String TAG = "ChangeDialog";

    private static final int PICK_FILE_REQUEST_CODE = 2103; //def not a random number :P

    private static final int CAMERA_REQUEST_CODE = 1998; // this toooooo xD
    OnPhotoReceivedListener mOnPhotoReceived;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_changephoto, container, false);

        TextView selectPhoto = view.findViewById(R.id.dialogChoosePhoto);
        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: accessing phone memory");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
            }
        });

        TextView takePhoto = view.findViewById(R.id.dialogOpenCamera);
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: starting a camera");
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        });
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /*
        Results when selecting new image from phone memory
         */
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri selectedImgUri = data.getData();
            Log.d(TAG, "onActivityResult: image: " + selectedImgUri);

            //send the bitmap and fragment to the interface
            mOnPhotoReceived.getImagePath(selectedImgUri);
            if (getDialog() != null)
                getDialog().dismiss();
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "onActivityResult: taking a photo");

            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            mOnPhotoReceived.getImageBitmap(bitmap);

            if (getDialog() != null)
                getDialog().dismiss();
        }
    }

    @Override
    public void onAttach(Context context) {
        try {
            mOnPhotoReceived = (OnPhotoReceivedListener) getActivity();
        } catch (ClassCastException e) {
            Log.d(TAG, "onAttach: ClassCastException " + e.getCause());
        }
        super.onAttach(context);
    }

    public interface OnPhotoReceivedListener {
        void getImagePath(Uri imgPath);

        void getImageBitmap(Bitmap imgBitmap);
    }
}
