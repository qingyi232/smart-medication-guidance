package com.medication.guide.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.medication.guide.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RiskHistoryAdapter extends RecyclerView.Adapter<RiskHistoryAdapter.ViewHolder> {
    private List<JsonObject> items;

    public RiskHistoryAdapter(List<JsonObject> items) {
        this.items = items;
    }

    public void updateData(List<JsonObject> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_risk_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject item = items.get(position);

        int score = item.get("riskScore").getAsInt();
        String level = item.get("riskLevel").getAsString();

        holder.tvScore.setText(String.valueOf(score));

        String levelText;
        int color;
        switch (level) {
            case "critical": levelText = "极高"; color = Color.parseColor("#C62828"); break;
            case "high": levelText = "高"; color = Color.parseColor("#E53935"); break;
            case "medium": levelText = "中等"; color = Color.parseColor("#FB8C00"); break;
            default: levelText = "低"; color = Color.parseColor("#43A047"); break;
        }
        holder.tvLevel.setText(levelText);
        holder.tvLevel.setTextColor(color);
        holder.tvScore.setTextColor(color);

        if (item.has("assessmentDate")) {
            String dateStr = item.get("assessmentDate").getAsString();
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = inputFormat.parse(dateStr);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINESE);
                holder.tvDate.setText(outputFormat.format(date));
            } catch (ParseException e) {
                holder.tvDate.setText(dateStr.substring(0, Math.min(16, dateStr.length())));
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvScore, tvLevel, tvDate;

        ViewHolder(View view) {
            super(view);
            tvScore = view.findViewById(R.id.tvScore);
            tvLevel = view.findViewById(R.id.tvLevel);
            tvDate = view.findViewById(R.id.tvDate);
        }
    }
}
