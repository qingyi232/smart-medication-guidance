package com.medication.guide.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.medication.guide.R;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
    private List<JsonObject> items;
    private final OnItemAction listener;

    public interface OnItemAction {
        void onAction(JsonObject item, String action);
    }

    public ScheduleAdapter(List<JsonObject> items, OnItemAction listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<JsonObject> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject item = items.get(position);
        JsonObject medication = item.getAsJsonObject("medication");

        holder.tvMedName.setText(medication.get("name").getAsString());
        holder.tvTime.setText(item.get("time").getAsString());

        String dosage = medication.get("dosage").getAsString();
        String unit = medication.get("dosageUnit").getAsString();
        holder.tvDosage.setText(dosage + " " + unit);

        String timing = medication.get("timing").getAsString();
        String timingText;
        switch (timing) {
            case "before_meal": timingText = "饭前"; break;
            case "after_meal": timingText = "饭后"; break;
            case "with_meal": timingText = "随餐"; break;
            case "bedtime": timingText = "睡前"; break;
            case "empty_stomach": timingText = "空腹"; break;
            default: timingText = ""; break;
        }
        holder.tvTiming.setText(timingText);

        String status = item.get("status").getAsString();
        switch (status) {
            case "taken":
                holder.tvStatus.setText("✓ 已服");
                holder.tvStatus.setTextColor(Color.parseColor("#43A047"));
                holder.btnConfirm.setVisibility(View.GONE);
                break;
            case "late":
                holder.tvStatus.setText("⏰ 迟服");
                holder.tvStatus.setTextColor(Color.parseColor("#FB8C00"));
                holder.btnConfirm.setVisibility(View.GONE);
                break;
            case "missed":
                holder.tvStatus.setText("✗ 漏服");
                holder.tvStatus.setTextColor(Color.parseColor("#E53935"));
                holder.btnConfirm.setVisibility(View.GONE);
                break;
            case "skipped":
                holder.tvStatus.setText("跳过");
                holder.tvStatus.setTextColor(Color.parseColor("#78909C"));
                holder.btnConfirm.setVisibility(View.GONE);
                break;
            default:
                holder.tvStatus.setText("待服药");
                holder.tvStatus.setTextColor(Color.parseColor("#00897B"));
                holder.btnConfirm.setVisibility(View.VISIBLE);
                break;
        }

        holder.btnConfirm.setOnClickListener(v -> listener.onAction(item, "confirm"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedName, tvTime, tvDosage, tvTiming, tvStatus;
        Button btnConfirm;

        ViewHolder(View view) {
            super(view);
            tvMedName = view.findViewById(R.id.tvMedName);
            tvTime = view.findViewById(R.id.tvTime);
            tvDosage = view.findViewById(R.id.tvDosage);
            tvTiming = view.findViewById(R.id.tvTiming);
            tvStatus = view.findViewById(R.id.tvStatus);
            btnConfirm = view.findViewById(R.id.btnConfirm);
        }
    }
}
