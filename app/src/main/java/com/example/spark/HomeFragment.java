package com.example.spark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Setup Action Buttons
        FloatingActionButton btnRewind = view.findViewById(R.id.btn_rewind);
        FloatingActionButton btnDislike = view.findViewById(R.id.btn_dislike);
        FloatingActionButton btnLike = view.findViewById(R.id.btn_like);

        btnRewind.setOnClickListener(v -> Toast.makeText(getContext(), "Rewind clicked", Toast.LENGTH_SHORT).show());
        btnDislike.setOnClickListener(v -> Toast.makeText(getContext(), "Disliked!", Toast.LENGTH_SHORT).show());
        btnLike.setOnClickListener(v -> Toast.makeText(getContext(), "Liked!", Toast.LENGTH_SHORT).show());

        return view;
    }
}