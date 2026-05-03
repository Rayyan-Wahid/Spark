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

public class LikeRequestAdapter extends RecyclerView.Adapter<LikeRequestAdapter.ViewHolder> {

    private List<ProfileModel> profileList;
    private OnLikeActionListener listener;

    public interface OnLikeActionListener {
        void onAccept(ProfileModel profile);
        void onDecline(ProfileModel profile);
    }

    public LikeRequestAdapter(List<ProfileModel> profileList, OnLikeActionListener listener) {
        this.profileList = profileList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_like_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfileModel profile = profileList.get(position);
        holder.tvName.setText(profile.getName() + ", " + profile.getAge());
        holder.tvBio.setText(profile.getBio());

        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(profile.getProfileImageUrl())
                    .circleCrop()
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.heart);
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(profile);
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) listener.onDecline(profile);
        });
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile, btnAccept, btnDecline;
        TextView tvName, tvBio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_like_profile);
            tvName = itemView.findViewById(R.id.tv_like_name);
            tvBio = itemView.findViewById(R.id.tv_like_bio);
            btnAccept = itemView.findViewById(R.id.btn_like_accept);
            btnDecline = itemView.findViewById(R.id.btn_like_decline);
        }
    }
}
