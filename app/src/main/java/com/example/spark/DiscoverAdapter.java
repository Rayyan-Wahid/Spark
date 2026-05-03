package com.example.spark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.ViewHolder> {
    private List<ProfileModel> profileList;
    private OnProfileActionListener actionListener;

    public interface OnProfileActionListener {
        void onLike(ProfileModel profile);
        void onProfileClick(ProfileModel profile);
    }

    public DiscoverAdapter(List<ProfileModel> profileList) {
        this.profileList = profileList;
    }

    public DiscoverAdapter(List<ProfileModel> profileList, OnProfileActionListener actionListener) {
        this.profileList = profileList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discover_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileModel profile = profileList.get(position);
        holder.tvNameAge.setText(profile.getName() + ", " + profile.getAge());
        holder.tvDistance.setText(profile.getDistance() != null ? profile.getDistance() : "Near you");
        
        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(profile.getProfileImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.heart)
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(profile.getImageResId() != 0 ? profile.getImageResId() : R.drawable.heart);
        }

        holder.btnLike.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onLike(profile);
        });

        holder.itemView.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onProfileClick(profile);
        });
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvNameAge, tvDistance;
        View btnLike;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvNameAge = itemView.findViewById(R.id.tv_name_age);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            btnLike = itemView.findViewById(R.id.btn_like_container);
        }
    }
}