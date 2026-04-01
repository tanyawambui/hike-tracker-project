package com.hiketracker.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hiketracker.R;
import com.hiketracker.model.Hike;

import java.util.ArrayList;
import java.util.List;

public class HikeAdapter extends RecyclerView.Adapter<HikeAdapter.HikeViewHolder> {

    private List<Hike> hikes = new ArrayList<>();

    public void setHikes(List<Hike> hikes) {
        this.hikes = hikes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hike, parent, false);
        return new HikeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
        Hike hike = hikes.get(position);
        holder.tvDate.setText(hike.getDate());
        holder.tvDistance.setText(hike.getFormattedDistance());
        holder.tvDuration.setText(hike.getFormattedDuration());
        holder.tvHikeNumber.setText("Hike #" + (position + 1));
    }

    @Override
    public int getItemCount() {
        return hikes.size();
    }

    static class HikeViewHolder extends RecyclerView.ViewHolder {
        TextView tvHikeNumber, tvDate, tvDistance, tvDuration;

        HikeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHikeNumber = itemView.findViewById(R.id.tv_hike_number);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvDuration = itemView.findViewById(R.id.tv_duration);
        }
    }
}
