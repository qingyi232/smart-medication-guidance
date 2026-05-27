package com.medication.guide.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.adapters.MedicationInfoAdapter;
import com.medication.guide.databinding.ActivityMedicationInfoBinding;
import com.medication.guide.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MedicationInfoActivity extends AppCompatActivity {
    private ActivityMedicationInfoBinding binding;
    private MedicationInfoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicationInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("用药知识库");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new MedicationInfoAdapter(new ArrayList<>(), this::showDetail);
        binding.rvInfoList.setLayoutManager(new LinearLayoutManager(this));
        binding.rvInfoList.setAdapter(adapter);

        binding.btnSearch.setOnClickListener(v -> searchMedInfo());
        binding.btnBackToList.setOnClickListener(v -> showList());

        // 初始加载全部
        searchMedInfo();
    }

    private void searchMedInfo() {
        String keyword = binding.etSearch.getText().toString().trim();
        RetrofitClient.getInstance(this).getApi().searchMedicationInfo(keyword)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonArray arr = data.getAsJsonArray("infos");
                            List<JsonObject> list = new ArrayList<>();
                            for (JsonElement el : arr) list.add(el.getAsJsonObject());
                            adapter.updateData(list);

                            binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                            binding.rvInfoList.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(MedicationInfoActivity.this, "搜索失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDetail(JsonObject info) {
        String id = info.has("_id") ? info.get("_id").getAsString() : "";
        RetrofitClient.getInstance(this).getApi().getMedicationInfo(id)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject detail = response.body().getAsJsonObject("data").getAsJsonObject("info");
                            displayDetail(detail);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(MedicationInfoActivity.this, "加载详情失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayDetail(JsonObject detail) {
        binding.rvInfoList.setVisibility(View.GONE);
        binding.tvEmpty.setVisibility(View.GONE);
        binding.scrollDetail.setVisibility(View.VISIBLE);

        binding.tvDetailName.setText(getStr(detail, "name"));
        binding.tvDetailGenericName.setText(getStr(detail, "genericName") + " | " + getStr(detail, "category"));

        StringBuilder sb = new StringBuilder();
        appendSection(sb, "【适应症】", getStr(detail, "indication"));
        appendSection(sb, "【用法用量】", getStr(detail, "dosageAndAdministration"));
        appendSection(sb, "【不良反应】", getStr(detail, "adverseReactions"));
        appendSection(sb, "【禁忌】", getStr(detail, "contraindications"));
        appendSection(sb, "【注意事项】", getStr(detail, "precautions"));
        appendSection(sb, "【药物相互作用】", getStr(detail, "interactions"));
        appendSection(sb, "【贮藏】", getStr(detail, "storageCondition"));
        appendSection(sb, "【生产厂家】", getStr(detail, "manufacturer"));

        binding.tvDetailContent.setText(sb.toString());
    }

    private void showList() {
        binding.scrollDetail.setVisibility(View.GONE);
        binding.rvInfoList.setVisibility(View.VISIBLE);
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private void appendSection(StringBuilder sb, String title, String content) {
        if (!content.isEmpty()) {
            sb.append(title).append("\n").append(content).append("\n\n");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (binding.scrollDetail.getVisibility() == View.VISIBLE) {
            showList();
            return true;
        }
        finish();
        return true;
    }
}
