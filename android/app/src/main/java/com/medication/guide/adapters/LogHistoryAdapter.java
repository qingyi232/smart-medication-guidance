package com.medication.guide.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.medication.guide.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogHistoryAdapter extends RecyclerView.Adapter<LogHistoryAdapter.ViewHolder> {
    private List<JsonObject> logs;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINESE);

    public LogHistoryAdapter(List<JsonObject> logs) {
        this.logs = logs;
    }

    public void updateData(List<JsonObject> newLogs) {
        this.logs = newLogs;
        notifyDataSetChanged();
    }

    public void appendData(List<JsonObject> moreLogs) {
        int start = logs.size();
        logs.addAll(moreLogs);
        notifyItemRangeInserted(start, moreLogs.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject log = logs.get(position);

        // 药品名称
        if (log.has("medication") && log.get("medication").isJsonObject()) {
            JsonObject med = log.getAsJsonObject("medication");
            holder.tvMedName.setText(med.has("name") ? med.get("name").getAsString() : "未知药品");
            String dosage = med.has("dosage") ? med.get("dosage").getAsString() : "";
            String unit = med.has("dosageUnit") ? med.get("dosageUnit").getAsString() : "";
            holder.tvDosage.setText(dosage + unit);
        } else {
            holder.tvMedName.setText("未知药品");
            holder.tvDosage.setText("");
        }

        // 计划时间
        if (log.has("scheduledTime") && !log.get("scheduledTime").isJsonNull()) {
            try {
                String timeStr = log.get("scheduledTime").getAsString();
                holder.tvScheduledTime.setText("计划: " + timeStr.substring(0, Math.min(16, timeStr.length())).replace("T", " "));
            } catch (Exception e) {
                holder.tvScheduledTime.setText("");
            }
        }

        // 状态
        String status = log.has("status") ? log.get("status").getAsString() : "pending";
        String statusText;
        int statusColor;
        switch (status) {
            case "taken":
                statusText = "已服药";
                statusColor = Color.parseColor("#43A047");
                break;
            case "late":
                statusText = "迟服";
                statusColor = Color.parseColor("#FB8C00");
                break;
            case "missed":
                statusText = "漏服";
                statusColor = Color.parseColor("#E53935");
                break;
            case "skipped":
                statusText = "跳过";
                statusColor = Color.parseColor("#B0BEC5");
                break;
            default:
                statusText = "待服药";
                statusColor = Color.parseColor("#1976D2");
                break;
        }
        holder.tvStatus.setText(statusText);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(statusColor);
        bg.setCornerRadius(12f);
        holder.tvStatus.setBackground(bg);

        // 漏服原因
        if (("missed".equals(status) || "skipped".equals(status)) && log.has("missedReason") && !log.get("missedReason").isJsonNull()) {
            String reason = log.get("missedReason").getAsString();
            String reasonText;
            switch (reason) {
                case "forgot": reasonText = "忘记了"; break;
                case "too_busy": reasonText = "太忙"; break;
                case "side_effects": reasonText = "副作用"; break;
                case "felt_better": reasonText = "感觉好转"; break;
                case "ran_out": reasonText = "药物用完"; break;
                case "intentional": reasonText = "故意不服"; break;
                default: reasonText = "其他"; break;
            }
            holder.tvMissedReason.setText("原因: " + reasonText);
            holder.tvMissedReason.setVisibility(View.VISIBLE);
        } else {
            holder.tvMissedReason.setVisibility(View.GONE);
        }

        // 健康数据
        StringBuilder healthInfo = new StringBuilder();
        if (log.has("stressLevel") && !log.get("stressLevel").isJsonNull())
            healthInfo.append("压力:").append(log.get("stressLevel").getAsInt()).append(" ");
        if (log.has("heartRate") && !log.get("heartRate").isJsonNull())
            healthInfo.append("心率:").append(log.get("heartRate").getAsInt()).append(" ");
        if (log.has("bloodPressure") && !log.get("bloodPressure").isJsonNull())
            healthInfo.append("血压:").append(log.get("bloodPressure").getAsString()).append(" ");
        if (log.has("bloodGlucose") && !log.get("bloodGlucose").isJsonNull())
            healthInfo.append("血糖:").append(log.get("bloodGlucose").getAsDouble()).append(" ");

        if (healthInfo.length() > 0) {
            holder.tvHealthData.setText(healthInfo.toString().trim());
            holder.tvHealthData.setVisibility(View.VISIBLE);
        } else {
            holder.tvHealthData.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedName, tvStatus, tvScheduledTime, tvDosage, tvMissedReason, tvHealthData;

        ViewHolder(View itemView) {
            super(itemView);
            tvMedName = itemView.findViewById(R.id.tvMedName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvScheduledTime = itemView.findViewById(R.id.tvScheduledTime);
            tvDosage = itemView.findViewById(R.id.tvDosage);
            tvMissedReason = itemView.findViewById(R.id.tvMissedReason);
            tvHealthData = itemView.findViewById(R.id.tvHealthData);
        }
    }
}
