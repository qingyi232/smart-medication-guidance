package com.medication.guide.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;
import com.medication.guide.R;

import java.util.List;

public class MedicationInfoAdapter extends RecyclerView.Adapter<MedicationInfoAdapter.ViewHolder> {
    private List<JsonObject> infos;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(JsonObject info);
    }

    public MedicationInfoAdapter(List<JsonObject> infos, OnItemClickListener listener) {
        this.infos = infos;
        this.listener = listener;
    }

    public void updateData(List<JsonObject> newInfos) {
        this.infos = newInfos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medication_info, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject info = infos.get(position);

        holder.tvName.setText(info.has("name") ? info.get("name").getAsString() : "");
        holder.tvCategory.setText(info.has("category") && !info.get("category").isJsonNull()
                ? info.get("category").getAsString() : "");
        holder.tvIndication.setText(info.has("indication") && !info.get("indication").isJsonNull()
                ? info.get("indication").getAsString() : "");

        holder.itemView.setOnClickListener(v -> listener.onItemClick(info));
    }

    @Override
    public int getItemCount() {
        return infos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCategory, tvIndication;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvInfoName);
            tvCategory = itemView.findViewById(R.id.tvInfoCategory);
            tvIndication = itemView.findViewById(R.id.tvInfoIndication);
        }
    }
}
