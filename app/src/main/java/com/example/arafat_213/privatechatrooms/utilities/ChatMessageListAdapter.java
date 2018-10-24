package com.example.arafat_213.privatechatrooms.utilities;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.arafat_213.privatechatrooms.R;
import com.example.arafat_213.privatechatrooms.models.ChatMessage;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

public class ChatMessageListAdapter extends ArrayAdapter<ChatMessage> {

    private static final String TAG = "ChatMessageListAdapter";

    private int mLayoutResource;
    private Context mContext;

    public ChatMessageListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<ChatMessage> objects) {
        super(context, resource, objects);
        mContext = context;
        mLayoutResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mLayoutResource, parent, false);
            holder = new ViewHolder();

            holder.name = convertView.findViewById(R.id.name);
            holder.message = convertView.findViewById(R.id.message);
            holder.mProfileImage = convertView.findViewById(R.id.profile_image);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.name.setText("");
            holder.message.setText("");
        }

        try {
            //set the message
            holder.message.setText(getItem(position).getMessage());

            ImageLoader.getInstance().displayImage(getItem(position).getProfile_image(),
                    holder.mProfileImage);
            holder.name.setText(getItem(position).getName());


        } catch (NullPointerException e) {
            Log.e(TAG, "getView: NullPointerException: ", e.getCause());
        }

        return convertView;
    }

    public static class ViewHolder {
        TextView name, message;
        ImageView mProfileImage;
    }

}

















