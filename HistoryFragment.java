package com.hiketracker.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hiketracker.data.repository.HikeRepository;
import com.hiketracker.databinding.FragmentHistoryBinding;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HikeAdapter hikeAdapter;
    private HikeRepository hikeRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        hikeRepository = new HikeRepository(requireActivity().getApplication());
        hikeAdapter = new HikeAdapter();

        binding.recyclerHikes.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerHikes.setAdapter(hikeAdapter);

        loadHikes();
    }

    private void loadHikes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "local";

        hikeRepository.getHikesForUser(userId).observe(getViewLifecycleOwner(), hikes -> {
            if (hikes == null || hikes.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                binding.recyclerHikes.setVisibility(View.GONE);
            } else {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.recyclerHikes.setVisibility(View.VISIBLE);
                hikeAdapter.setHikes(hikes);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
