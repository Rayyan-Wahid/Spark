package com.example.spark;

import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DiscoverFragment extends Fragment {

    private static final String TAG = "DiscoverFragment";
    private double currentRadius = 50.0;
    private boolean isNearMe = false;
    private Double currentLat = null;
    private Double currentLng = null;
    private com.google.android.material.button.MaterialButton btnNearMe;

    private RecyclerView rvDiscover;
    private DiscoverAdapter adapter;
    private List<ProfileModel> profileList;
    private List<ProfileModel> filteredList;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private EditText etSearch;
    private ChipGroup chipGroupGenderFilter;
    private String selectedGenderFilter = "Non-binary"; // Non-binary = show all

    private GeoQuery geoQuery;
    private ValueEventListener myLocationListener;
    private DatabaseReference myUserRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return view;

        rvDiscover = view.findViewById(R.id.rv_discover);
        rvDiscover.setLayoutManager(new GridLayoutManager(getContext(), 2));
        etSearch = view.findViewById(R.id.et_discover_search);
        chipGroupGenderFilter = view.findViewById(R.id.chip_group_gender_filter);
        btnNearMe = view.findViewById(R.id.btn_near_me);
        btnNearMe.setOnClickListener(v -> {
            isNearMe = !isNearMe;
            currentRadius = isNearMe ? 20.0 : 50.0;
            btnNearMe.setText(isNearMe ? "All (50km)" : "Near me");
            if (currentLat != null && currentLng != null && mAuth.getCurrentUser() != null) {
                runGeoQuery(mAuth.getCurrentUser().getUid(), currentLat, currentLng);
            }
        });

        chipGroupGenderFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedGenderFilter = "Non-binary"; // fallback = show all
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_filter_male) selectedGenderFilter = "Male";
                else if (id == R.id.chip_filter_female) selectedGenderFilter = "Female";
                else selectedGenderFilter = "Non-binary";
            }
            filterProfiles(etSearch != null ? etSearch.getText().toString() : "");
        });

        profileList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new DiscoverAdapter(filteredList);
        rvDiscover.setAdapter(adapter);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        loadNearbyProfiles();
        setupSearch();

        return view;
    }

    private void loadNearbyProfiles() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        myUserRef = mDatabase.child("users").child(myUid);
        myLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                Double lat = snapshot.child("latitude").getValue(Double.class);
                Double lng = snapshot.child("longitude").getValue(Double.class);

                // Treat 0.0 as "not set" since primitive defaults to 0
                if (lat == null || lng == null || (lat == 0.0 && lng == 0.0)) {
                    Log.w(TAG, "My location not set in Firebase yet — falling back to load all profiles.");
                    fallbackLoadAllProfiles(myUid);
                    return;
                }
                currentLat = lat;
                currentLng = lng;
                runGeoQuery(myUid, lat, lng);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read my location: " + error.getMessage());
                // Still try to load profiles on error
                if (isAdded() && mAuth.getCurrentUser() != null) {
                    fallbackLoadAllProfiles(mAuth.getCurrentUser().getUid());
                }
            }
        };
        myUserRef.addValueEventListener(myLocationListener);
    }

    private void runGeoQuery(String myUid, double myLat, double myLng) {
        if (!isAdded()) return;

        // Cancel any previous query
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }

        DatabaseReference geoFireRef = mDatabase.getRoot().child("geofire");
        GeoFire geoFire = new GeoFire(geoFireRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(myLat, myLng), currentRadius);

        profileList.clear();
        filteredList.clear();
        adapter.notifyDataSetChanged();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String uid, GeoLocation location) {
                if (!isAdded()) return;
                if (uid.equals(myUid)) return; // Skip myself

                mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        ProfileModel profile = snapshot.getValue(ProfileModel.class);
                        if (profile == null || !profile.isProfileCompleted()) return;

                        float[] results = new float[1];
                        Location.distanceBetween(myLat, myLng,
                                location.latitude, location.longitude, results);
                        int distKm = Math.round(results[0] / 1000f);
                        profile.setDistance(String.format(Locale.getDefault(), "%d km away", distKm));

                        // Avoid duplicates
                        for (ProfileModel p : profileList) {
                            if (p.getId() != null && p.getId().equals(profile.getId())) return;
                        }
                        profileList.add(profile);
                        filterProfiles(etSearch != null ? etSearch.getText().toString() : "");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to load profile for uid=" + uid + ": " + error.getMessage());
                    }
                });
            }

            @Override
            public void onKeyExited(String uid) {
                if (!isAdded()) return;
                profileList.removeIf(p -> uid.equals(p.getId()));
                filterProfiles(etSearch != null ? etSearch.getText().toString() : "");
            }

            @Override
            public void onKeyMoved(String uid, GeoLocation location) {}

            @Override
            public void onGeoQueryReady() {
                Log.d(TAG, "GeoQuery ready — " + profileList.size() + " nearby profiles found.");
                // If GeoFire returned nothing, fall back to loading all profiles
                if (isAdded() && profileList.isEmpty()) {
                    Log.w(TAG, "GeoQuery returned 0 results, falling back to all profiles.");
                    fallbackLoadAllProfiles(myUid);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e(TAG, "GeoQuery error: " + error.getMessage());
                // Fallback on error too
                if (isAdded()) fallbackLoadAllProfiles(myUid);
            }
        });
    }

    private void fallbackLoadAllProfiles(String myUid) {
        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                profileList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ProfileModel profile = ds.getValue(ProfileModel.class);
                    if (profile != null && profile.isProfileCompleted()
                            && !myUid.equals(profile.getId())) {
                        profileList.add(profile);
                    }
                }
                filterProfiles(etSearch != null ? etSearch.getText().toString() : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Fallback load cancelled: " + error.getMessage());
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProfiles(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterProfiles(String query) {
        filteredList.clear();
        String q = query.toLowerCase().trim();
        for (ProfileModel profile : profileList) {
            // Gender filter:
            // Male chip → only profiles with gender "Male"
            // Female chip → only profiles with gender "Female"
            // Non-binary chip → show ALL (Male + Female + Prefer not to say + X + null)
            if (selectedGenderFilter.equals("Male")) {
                if (!"Male".equals(profile.getGender())) continue;
            } else if (selectedGenderFilter.equals("Female")) {
                if (!"Female".equals(profile.getGender())) continue;
            }
            // Non-binary = no gender filtering, show everyone
            if (!q.isEmpty()) {
                boolean nameMatch = profile.getName() != null
                        && profile.getName().toLowerCase().contains(q);
                boolean interestMatch = false;
                if (profile.getInterests() != null) {
                    for (String interest : profile.getInterests()) {
                        if (interest.toLowerCase().contains(q)) {
                            interestMatch = true;
                            break;
                        }
                    }
                }
                if (!nameMatch && !interestMatch) continue;
            }
            filteredList.add(profile);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
            geoQuery = null;
        }
        if (myUserRef != null && myLocationListener != null) {
            myUserRef.removeEventListener(myLocationListener);
        }
    }
}