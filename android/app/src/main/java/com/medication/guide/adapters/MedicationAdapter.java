package com.medication.guide.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.R;

import java.util.List;

public class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.ViewHolder> {
    private List<JsonObject> items;
    private final OnMedicationAction listener;

    public interface OnMedicationAction {
        void onAction(JsonObject medication, String action);
    }

    public MedicationAdapter(List<JsonObject> items, OnMedicationAction listener) {
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
                .inflate(R.layout.item_medication, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject med = items.get(position);

        holder.tvName.setText(med.get("name").getAsString());
        holder.tvDosage.setText(med.get("dosage").getAsString() + " " +
                med.get("dosageUnit").getAsString());

        String freq = med.get("frequency").getAsString();
        String freqText;
        switch (freq) {
            case "once_daily": freqText = "每日一次"; break;
            case "twice_daily": freqText = "每日两次"; break;
            case "three_times_daily": freqText = "每日三次"; break;
            case "four_times_daily": freqText = "每日四次"; break;
            case "weekly": freqText = "每周一次"; break;
            default: freqText = "按需"; break;
        }
        holder.tvFrequency.setText(freqText);

        if (med.has("scheduleTimes")) {
            JsonArray times = med.getAsJsonArray("scheduleTimes");
            StringBuilder sb = new StringBuilder();
            for (JsonElement el : times) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(el.getAsString());
            }
            holder.tvTimes.setText("时间: " + sb.toString());
        }

        if (med.has("indication") && !med.get("indication").isJsonNull()) {
            holder.tvIndication.setText("适应症: " + med.get("indication").getAsString());
            holder.tvIndication.setVisibility(View.VISIBLE);
        } else {
            holder.tvIndication.setVisibility(View.GONE);
        }

        // 药品状态标识
        boolean isActive = !med.has("isActive") || med.get("isActive").getAsBoolean();
        if (!isActive) {
            holder.tvName.setAlpha(0.5f);
            holder.tvName.setText(med.get("name").getAsString() + " [已停用]");
        } else {
            holder.tvName.setAlpha(1.0f);
        }

        holder.btnDelete.setOnClickListener(v -> listener.onAction(med, "delete"));
        holder.btnEdit.setOnClickListener(v -> listener.onAction(med, "edit"));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDosage, tvFrequency, tvTimes, tvIndication;
        ImageButton btnDelete, btnEdit;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvMedName);
            tvDosage = view.findViewById(R.id.tvDosage);
            tvFrequency = view.findViewById(R.id.tvFrequency);
            tvTimes = view.findViewById(R.id.tvTimes);
            tvIndication = view.findViewById(R.id.tvIndication);
            btnDelete = view.findViewById(R.id.btnDelete);
            btnEdit = view.findViewById(R.id.btnEdit);
        }
    }
}
