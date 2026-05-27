package com.medication.guide.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.adapters.MedicationAdapter;
import com.medication.guide.databinding.ActivityMedicationDetailBinding;
import com.medication.guide.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MedicationDetailActivity extends AppCompatActivity {
    private ActivityMedicationDetailBinding binding;
    private MedicationAdapter adapter;
    private List<JsonObject> allMedications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicationDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("我的药品");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new MedicationAdapter(new ArrayList<>(), this::onMedicationAction);
        binding.rvMedications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMedications.setAdapter(adapter);

        // 筛选器
        String[] filters = {"全部", "启用中", "已停用"};
        binding.spinnerFilter.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, filters));
        binding.spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 搜索
        binding.etSearchMed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, AddMedicationActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedications();
    }

    private void loadMedications() {
        // 加载全部药品（包括停用的）
        RetrofitClient.getInstance(this).getApi().getMedications(null)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonArray arr = data.getAsJsonArray("medications");
                            allMedications.clear();
                            for (JsonElement el : arr) allMedications.add(el.getAsJsonObject());
                            applyFilter();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(MedicationDetailActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilter() {
        String keyword = binding.etSearchMed.getText().toString().trim().toLowerCase();
        int filterPos = binding.spinnerFilter.getSelectedItemPosition();

        List<JsonObject> filtered = new ArrayList<>();
        for (JsonObject med : allMedications) {
            // 状态筛选
            boolean isActive = !med.has("isActive") || med.get("isActive").getAsBoolean();
            if (filterPos == 1 && !isActive) continue;
            if (filterPos == 2 && isActive) continue;

            // 关键词搜索
            if (!keyword.isEmpty()) {
                String name = med.has("name") ? med.get("name").getAsString().toLowerCase() : "";
                String generic = med.has("genericName") && !med.get("genericName").isJsonNull()
                        ? med.get("genericName").getAsString().toLowerCase() : "";
                if (!name.contains(keyword) && !generic.contains(keyword)) continue;
            }

            filtered.add(med);
        }

        adapter.updateData(filtered);
        binding.tvMedCount.setText("共 " + filtered.size() + " 种药品");
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void onMedicationAction(JsonObject medication, String action) {
        String id = medication.get("_id").getAsString();
        String name = medication.get("name").getAsString();

        if ("delete".equals(action)) {
            new AlertDialog.Builder(this)
                    .setTitle("删除药品")
                    .setMessage("确定要删除 " + name + " 吗？")
                    .setPositiveButton("删除", (d, w) -> deleteMedication(id))
                    .setNegativeButton("取消", null)
                    .show();
        } else if ("edit".equals(action)) {
            Intent intent = new Intent(this, AddMedicationActivity.class);
            intent.putExtra("medication_id", id);
            intent.putExtra("medication_data", medication.toString());
            startActivity(intent);
        }
    }

    private void deleteMedication(String id) {
        RetrofitClient.getInstance(this).getApi().deleteMedication(id)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MedicationDetailActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                            loadMedications();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(MedicationDetailActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
