package com.example.spark;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DiscoverFragment extends Fragment {

    private RecyclerView rvDiscover;
    private DiscoverAdapter adapter;
    private List<ProfileModel> profileList;
    private List<ProfileModel> filteredList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private EditText etSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        mAuth = FirebaseAuth.getInstance();
        rvDiscover = view.findViewById(R.id.rv_discover);
        rvDiscover.setLayoutManager(new GridLayoutManager(getContext(), 2));

        etSearch = view.findViewById(R.id.et_discover_search);

        profileList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new DiscoverAdapter(filteredList);
        rvDiscover.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("users");
        fetchProfiles();

        setupSearch();

        return view;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProfiles(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProfiles(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(profileList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (ProfileModel profile : profileList) {
                // Search by name
                if (profile.getName() != null && profile.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(profile);
                    continue;
                }
                
                // Search by interests
                if (profile.getInterests() != null) {
                    for (String interest : profile.getInterests()) {
                        if (interest.toLowerCase().contains(lowerCaseQuery)) {
                            filteredList.add(profile);
                            break;
                        }
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchProfiles() {
        if (mAuth.getCurrentUser() == null) return;
        String myId = mAuth.getCurrentUser().getUid();

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                profileList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    ProfileModel profile = postSnapshot.getValue(ProfileModel.class);
                    // Filter: Must be completed profile AND NOT the current logged-in user
                    if (profile != null && profile.isProfileCompleted() && !profile.getId().equals(myId)) {
                        profileList.add(profile);
                    }
                }
                // Update filtered list based on current search or show all
                filterProfiles(etSearch.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}