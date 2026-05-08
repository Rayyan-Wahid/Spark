package com.example.spark;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

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


        holder.chipGroupInterests.removeAllViews();
        if (profile.getInterests() != null) {
            int max = Math.min(profile.getInterests().size(), 2);
            for (int i = 0; i < max; i++) {
                Chip chip = new Chip(holder.itemView.getContext());
                String text = profile.getInterests().get(i);
                // Truncate long text so both chips fit in one line
                if (text.length() > 8) text = text.substring(0, 7) + "…";
                chip.setText(text);
                chip.setChipBackgroundColorResource(R.color.pink_primary);
                chip.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
                chip.setChipStrokeWidth(0f);
                chip.setTextSize(10f);
                chip.setClickable(false);
                chip.setFocusable(false);
                chip.setEnsureMinTouchTargetSize(false);
                chip.setChipMinHeight(0f);
                holder.chipGroupInterests.addView(chip);
            }
        }


        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ViewProfileActivity.class);
            intent.putExtra("USER_ID", profile.getId());
            v.getContext().startActivity(intent);
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
        ChipGroup chipGroupInterests;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvNameAge = itemView.findViewById(R.id.tv_name_age);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            chipGroupInterests = itemView.findViewById(R.id.chip_group_interests);
        }
    }
}