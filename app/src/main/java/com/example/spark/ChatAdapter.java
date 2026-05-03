package com.example.spark;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatModel> chatList;
    private Context context;

    public ChatAdapter(List<ChatModel> chatList) {
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);

        holder.tvName.setText(chat.getName() + (chat.getAge() > 0 ? ", " + chat.getAge() : ""));
        holder.tvLastMessage.setText(chat.getLastMessage());
        holder.unreadIndicator.setVisibility(chat.isUnread() ? View.VISIBLE : View.GONE);

        if (chat.getProfileImageUrl() != null && !chat.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(chat.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.heart)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.heart);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("OTHER_USER_ID", chat.getOtherUserId());
            intent.putExtra("OTHER_USER_NAME", chat.getName());
            intent.putExtra("OTHER_USER_IMAGE", chat.getProfileImageUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvLastMessage;
        View unreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_conv_profile);
            tvName = itemView.findViewById(R.id.tv_conv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_conv_last_message);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }
}
