package com.example.spark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NewMatchAdapter extends RecyclerView.Adapter<NewMatchAdapter.ViewHolder> {
    private List<ProfileModel> matchList;
    private OnMatchClickListener listener;

    public interface OnMatchClickListener {
        void onShowProfile(ProfileModel profile);
        void onStartMessaging(ProfileModel profile);
    }

    public NewMatchAdapter(List<ProfileModel> matchList, OnMatchClickListener listener) {
        this.matchList = matchList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_new_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileModel match = matchList.get(position);
        holder.tvName.setText(match.getName());
        
        if (match.getProfileImageUrl() != null && !match.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(match.getProfileImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.heart)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(match.getImageResId() != 0 ? match.getImageResId() : R.drawable.heart);
        }

        holder.itemView.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(holder.itemView.getContext(), v);
            popup.getMenu().add("Show Profile");
            popup.getMenu().add("Start Messaging");
            
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Show Profile")) {
                    listener.onShowProfile(match);
                } else if (item.getTitle().equals("Start Messaging")) {
                    listener.onStartMessaging(match);
                }
                return true;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return matchList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_match_profile);
            tvName = itemView.findViewById(R.id.tv_match_name);
        }
    }
}