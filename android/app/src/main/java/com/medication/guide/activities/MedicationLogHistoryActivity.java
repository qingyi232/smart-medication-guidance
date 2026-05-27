package com.medication.guide.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.adapters.LogHistoryAdapter;
import com.medication.guide.databinding.ActivityMedicationLogHistoryBinding;
import com.medication.guide.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MedicationLogHistoryActivity extends AppCompatActivity {
    private ActivityMedicationLogHistoryBinding binding;
    private LogHistoryAdapter adapter;
    private int currentPage = 1;
    private int totalPages = 1;
    private String filterStatus = null;
    private String filterStartDate = null;
    private String filterEndDate = null;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMedicationLogHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("服药记录历史");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupSpinner();
        setupRecyclerView();
        setupListeners();
        loadLogs(false);
    }

    private void setupSpinner() {
        String[] statuses = {"全部状态", "已服药", "迟服", "漏服", "跳过", "待服药"};
        binding.spinnerStatus.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, statuses));
    }

    private void setupRecyclerView() {
        adapter = new LogHistoryAdapter(new ArrayList<>());
        binding.rvLogs.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLogs.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.btnStartDate.setOnClickListener(v -> showDatePicker(true));
        binding.btnEndDate.setOnClickListener(v -> showDatePicker(false));

        binding.btnFilter.setOnClickListener(v -> {
            applyFilters();
            currentPage = 1;
            loadLogs(false);
        });

        binding.btnReset.setOnClickListener(v -> {
            filterStatus = null;
            filterStartDate = null;
            filterEndDate = null;
            binding.btnStartDate.setText("开始日期");
            binding.btnEndDate.setText("结束日期");
            binding.spinnerStatus.setSelection(0);
            currentPage = 1;
            loadLogs(false);
        });

        binding.btnLoadMore.setOnClickListener(v -> {
            currentPage++;
            loadLogs(true);
        });
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            String dateStr = sdf.format(selected.getTime());
            if (isStart) {
                filterStartDate = dateStr;
                binding.btnStartDate.setText(dateStr);
            } else {
                filterEndDate = dateStr;
                binding.btnEndDate.setText(dateStr);
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void applyFilters() {
        int pos = binding.spinnerStatus.getSelectedItemPosition();
        String[] statusValues = {null, "taken", "late", "missed", "skipped", "pending"};
        filterStatus = statusValues[pos];
    }

    private void loadLogs(boolean append) {
        RetrofitClient.getInstance(this).getApi()
                .getLogsFiltered(currentPage, 20, filterStatus, filterStartDate, filterEndDate)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonArray arr = data.getAsJsonArray("logs");
                            int total = data.get("total").getAsInt();
                            totalPages = data.get("totalPages").getAsInt();

                            List<JsonObject> list = new ArrayList<>();
                            for (JsonElement el : arr) list.add(el.getAsJsonObject());

                            if (append) {
                                adapter.appendData(list);
                            } else {
                                adapter.updateData(list);
                            }

                            binding.tvResultCount.setText("共 " + total + " 条记录");
                            binding.tvEmpty.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
                            binding.rvLogs.setVisibility(total == 0 ? View.GONE : View.VISIBLE);
                            binding.btnLoadMore.setVisibility(currentPage < totalPages ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(MedicationLogHistoryActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
