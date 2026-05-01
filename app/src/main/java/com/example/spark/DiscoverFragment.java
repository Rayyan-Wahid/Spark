package com.example.spark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DiscoverFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        RecyclerView rvDiscover = view.findViewById(R.id.rv_discover);
        rvDiscover.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<ProfileModel> profileList = new ArrayList<>();
        profileList.add(new ProfileModel("Sarah", 26, "2 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Marcus", 28, "5 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Emma", 23, "1 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Leo", 25, "3 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Mia", 22, "4 mi", R.drawable.heart));
        profileList.add(new ProfileModel("James", 27, "6 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Chloe", 24, "2 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Ryan", 29, "8 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Sophie", 21, "1 mi", R.drawable.heart));
        profileList.add(new ProfileModel("Alex", 26, "5 mi", R.drawable.heart));

        DiscoverAdapter adapter = new DiscoverAdapter(profileList);
        rvDiscover.setAdapter(adapter);

        return view;
    }
}