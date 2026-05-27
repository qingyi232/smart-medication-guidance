package com.medication.guide.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.R;
import com.medication.guide.adapters.ScheduleAdapter;
import com.medication.guide.databinding.ActivityMainBinding;
import com.medication.guide.network.RetrofitClient;
import com.medication.guide.utils.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private TokenManager tokenManager;
    private ScheduleAdapter scheduleAdapter;
    private com.medication.guide.utils.CacheManager cacheManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setTitle("智能用药指导");

        tokenManager = new TokenManager(this);
        cacheManager = new com.medication.guide.utils.CacheManager(this);

        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodaySchedule();
        loadAdherenceStats();
        loadLatestRisk();
    }

    private void setupRecyclerView() {
        scheduleAdapter = new ScheduleAdapter(new ArrayList<>(), this::onScheduleItemAction);
        binding.rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSchedule.setAdapter(scheduleAdapter);
    }

    private void setupListeners() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadTodaySchedule();
            loadAdherenceStats();
            loadLatestRisk();
        });

        binding.fabAddMedication.setOnClickListener(v -> {
            startActivity(new Intent(this, AddMedicationActivity.class));
        });

        binding.cardRisk.setOnClickListener(v -> {
            startActivity(new Intent(this, RiskDetailActivity.class));
        });

        binding.btnAssessRisk.setOnClickListener(v -> assessRisk());
    }

    private void loadTodaySchedule() {
        String today = new SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE).format(new Date());
        binding.tvDate.setText(today);

        RetrofitClient.getInstance(this).getApi().getTodaySchedule()
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            // 缓存今日计划数据
                            cacheManager.cacheTodaySchedule(response.body().toString());
                            JsonArray scheduleArray = data.getAsJsonArray("schedule");
                            List<JsonObject> items = new ArrayList<>();
                            for (JsonElement el : scheduleArray) {
                                items.add(el.getAsJsonObject());
                            }
                            scheduleAdapter.updateData(items);

                            if (items.isEmpty()) {
                                binding.tvEmptySchedule.setVisibility(View.VISIBLE);
                                binding.rvSchedule.setVisibility(View.GONE);
                            } else {
                                binding.tvEmptySchedule.setVisibility(View.GONE);
                                binding.rvSchedule.setVisibility(View.VISIBLE);
                            }

                            // 统计今日进度
                            int total = items.size();
                            int done = 0;
                            for (JsonObject item : items) {
                                String status = item.get("status").getAsString();
                                if ("taken".equals(status) || "late".equals(status)) done++;
                            }
                            binding.tvTodayProgress.setText(done + "/" + total);
                            binding.progressToday.setMax(Math.max(total, 1));
                            binding.progressToday.setProgress(done);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.swipeRefresh.setRefreshing(false);
                        // 离线模式：从缓存加载
                        String cached = cacheManager.getTodayScheduleOffline();
                        if (cached != null) {
                            try {
                                JsonObject cachedBody = com.google.gson.JsonParser.parseString(cached).getAsJsonObject();
                                JsonObject data = cachedBody.getAsJsonObject("data");
                                JsonArray scheduleArray = data.getAsJsonArray("schedule");
                                List<JsonObject> items = new ArrayList<>();
                                for (JsonElement el : scheduleArray) items.add(el.getAsJsonObject());
                                scheduleAdapter.updateData(items);
                                binding.tvEmptySchedule.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                                binding.rvSchedule.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                                Toast.makeText(MainActivity.this, "离线模式 - 显示缓存数据", Toast.LENGTH_SHORT).show();
                                return;
                            } catch (Exception ignored) {}
                        }
                        Toast.makeText(MainActivity.this, "加载失败，请检查网络", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAdherenceStats() {
        RetrofitClient.getInstance(this).getApi().getAdherenceStats(7)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonObject summary = data.getAsJsonObject("summary");
                            double rate = summary.get("adherenceRate").getAsDouble();
                            binding.tvAdherenceRate.setText(String.format(Locale.CHINESE, "%.1f%%", rate));

                            if (rate >= 80) {
                                binding.tvAdherenceRate.setTextColor(Color.parseColor("#43A047"));
                            } else if (rate >= 60) {
                                binding.tvAdherenceRate.setTextColor(Color.parseColor("#FB8C00"));
                            } else {
                                binding.tvAdherenceRate.setTextColor(Color.parseColor("#E53935"));
                            }

                            int taken = summary.get("taken").getAsInt();
                            int missed = summary.get("missed").getAsInt();
                            int late = summary.get("late").getAsInt();
                            binding.tvStatsDetail.setText(
                                    String.format("按时%d · 迟服%d · 漏服%d", taken, late, missed)
                            );
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void loadLatestRisk() {
        RetrofitClient.getInstance(this).getApi().getLatestRisk()
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonElement assessmentEl = data.get("assessment");
                            if (assessmentEl != null && !assessmentEl.isJsonNull()) {
                                JsonObject assessment = assessmentEl.getAsJsonObject();
                                int score = assessment.get("riskScore").getAsInt();
                                String level = assessment.get("riskLevel").getAsString();

                                binding.tvRiskScore.setText(String.valueOf(score));
                                binding.progressRisk.setMax(100);
                                binding.progressRisk.setProgress(score);

                                String levelText;
                                int color;
                                switch (level) {
                                    case "critical":
                                        levelText = "极高风险";
                                        color = Color.parseColor("#C62828");
                                        break;
                                    case "high":
                                        levelText = "高风险";
                                        color = Color.parseColor("#E53935");
                                        break;
                                    case "medium":
                                        levelText = "中等风险";
                                        color = Color.parseColor("#FB8C00");
                                        break;
                                    default:
                                        levelText = "低风险";
                                        color = Color.parseColor("#43A047");
                                        break;
                                }
                                binding.tvRiskLevel.setText(levelText);
                                binding.tvRiskLevel.setTextColor(color);
                                binding.tvRiskScore.setTextColor(color);
                            } else {
                                binding.tvRiskScore.setText("--");
                                binding.tvRiskLevel.setText("暂无评估");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void assessRisk() {
        binding.btnAssessRisk.setEnabled(false);
        RetrofitClient.getInstance(this).getApi().assessRisk()
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.btnAssessRisk.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(MainActivity.this, "风险评估完成", Toast.LENGTH_SHORT).show();
                            loadLatestRisk();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.btnAssessRisk.setEnabled(true);
                        Toast.makeText(MainActivity.this, "评估失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onScheduleItemAction(JsonObject item, String action) {
        if ("confirm".equals(action)) {
            showTakeMedicationDialog(item);
        }
    }

    private void showTakeMedicationDialog(JsonObject item) {
        JsonObject medication = item.getAsJsonObject("medication");
        String medId = medication.get("_id").getAsString();
        String medName = medication.get("name").getAsString();

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_record_medication, null);

        android.widget.RadioGroup rgStatus = dialogView.findViewById(R.id.rgStatus);
        android.widget.Spinner spinnerMissedReason = dialogView.findViewById(R.id.spinnerMissedReason);
        android.widget.Spinner spinnerStress = dialogView.findViewById(R.id.spinnerStress);
        android.widget.Spinner spinnerWellbeing = dialogView.findViewById(R.id.spinnerWellbeing);
        android.widget.Spinner spinnerActivity = dialogView.findViewById(R.id.spinnerActivity);
        android.widget.Spinner spinnerLocation = dialogView.findViewById(R.id.spinnerLocation);
        android.widget.EditText etHeartRate = dialogView.findViewById(R.id.etHeartRate);
        android.widget.EditText etBloodPressure = dialogView.findViewById(R.id.etBloodPressure);
        android.widget.EditText etBloodGlucose = dialogView.findViewById(R.id.etBloodGlucose);
        android.widget.EditText etSleepHours = dialogView.findViewById(R.id.etSleepHours);
        android.widget.CheckBox cbSideEffects = dialogView.findViewById(R.id.cbSideEffects);
        View layoutMissedReason = dialogView.findViewById(R.id.layoutMissedReason);

        // 详细数据折叠/展开
        View layoutDetailFields = dialogView.findViewById(R.id.layoutDetailFields);
        View layoutToggleDetail = dialogView.findViewById(R.id.layoutToggleDetail);
        android.widget.TextView tvToggleArrow = dialogView.findViewById(R.id.tvToggleArrow);
        layoutToggleDetail.setOnClickListener(v -> {
            if (layoutDetailFields.getVisibility() == View.GONE) {
                layoutDetailFields.setVisibility(View.VISIBLE);
                tvToggleArrow.setText("▲ 收起");
            } else {
                layoutDetailFields.setVisibility(View.GONE);
                tvToggleArrow.setText("▼ 展开");
            }
        });

        // 压力等级 1-10
        String[] stressLevels = new String[10];
        for (int i = 0; i < 10; i++) stressLevels[i] = String.valueOf(i + 1);
        spinnerStress.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, stressLevels));
        spinnerStress.setSelection(4);

        // 身体感觉 1-5
        String[] wellbeingLevels = {"1-很差", "2-较差", "3-一般", "4-较好", "5-很好"};
        spinnerWellbeing.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, wellbeingLevels));
        spinnerWellbeing.setSelection(2);

        // 活动状态
        String[] activities = {"休息中", "轻度活动", "中度活动", "剧烈活动", "睡眠中"};
        spinnerActivity.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, activities));

        // 位置
        String[] locations = {"家中", "工作", "医院", "户外", "其他"};
        spinnerLocation.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, locations));

        // 漏服原因
        String[] missedReasons = {"忘记了", "太忙", "副作用", "感觉好转", "药物用完", "故意不服", "其他"};
        spinnerMissedReason.setAdapter(new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, missedReasons));

        rgStatus.setOnCheckedChangeListener((group, checkedId) -> {
            boolean showReason = (checkedId == R.id.rbMissed || checkedId == R.id.rbSkipped);
            layoutMissedReason.setVisibility(showReason ? View.VISIBLE : View.GONE);
        });

        new AlertDialog.Builder(this)
                .setTitle(medName + " - 记录服药")
                .setView(dialogView)
                .setPositiveButton("确认", (dialog, which) -> {
                    String status;
                    int checkedId = rgStatus.getCheckedRadioButtonId();
                    if (checkedId == R.id.rbLate) status = "late";
                    else if (checkedId == R.id.rbMissed) status = "missed";
                    else if (checkedId == R.id.rbSkipped) status = "skipped";
                    else status = "taken";

                    String[] activityValues = {"resting", "light_activity", "moderate_activity", "heavy_activity", "sleeping"};
                    String[] locationValues = {"home", "work", "hospital", "outdoor", "other"};
                    String[] missedReasonValues = {"forgot", "too_busy", "side_effects", "felt_better", "ran_out", "intentional", "other"};

                    JsonObject body = new JsonObject();
                    body.addProperty("medicationId", medId);
                    body.addProperty("status", status);
                    body.addProperty("scheduledTime", item.get("scheduledTime").getAsString());
                    body.addProperty("stressLevel", spinnerStress.getSelectedItemPosition() + 1);
                    body.addProperty("wellBeingScore", spinnerWellbeing.getSelectedItemPosition() + 1);
                    body.addProperty("activityStatus", activityValues[spinnerActivity.getSelectedItemPosition()]);
                    body.addProperty("location", locationValues[spinnerLocation.getSelectedItemPosition()]);
                    body.addProperty("experiencedSideEffects", cbSideEffects.isChecked());

                    if ("missed".equals(status) || "skipped".equals(status)) {
                        body.addProperty("missedReason", missedReasonValues[spinnerMissedReason.getSelectedItemPosition()]);
                    }

                    String hr = etHeartRate.getText().toString().trim();
                    if (!hr.isEmpty()) body.addProperty("heartRate", Integer.parseInt(hr));
                    String bp = etBloodPressure.getText().toString().trim();
                    if (!bp.isEmpty()) body.addProperty("bloodPressure", bp);
                    String bg = etBloodGlucose.getText().toString().trim();
                    if (!bg.isEmpty()) body.addProperty("bloodGlucose", Double.parseDouble(bg));
                    String sl = etSleepHours.getText().toString().trim();
                    if (!sl.isEmpty()) body.addProperty("sleepHours", Double.parseDouble(sl));

                    recordMedicationLog(body);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void recordMedicationLog(JsonObject body) {
        RetrofitClient.getInstance(this).getApi().recordLog(body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "记录成功", Toast.LENGTH_SHORT).show();
                            loadTodaySchedule();
                            loadAdherenceStats();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "记录失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_medications) {
            startActivity(new Intent(this, MedicationDetailActivity.class));
            return true;
        } else if (id == R.id.action_statistics) {
            startActivity(new Intent(this, StatisticsActivity.class));
            return true;
        } else if (id == R.id.action_log_history) {
            startActivity(new Intent(this, MedicationLogHistoryActivity.class));
            return true;
        } else if (id == R.id.action_medication_info) {
            startActivity(new Intent(this, MedicationInfoActivity.class));
            return true;
        } else if (id == R.id.action_change_password) {
            startActivity(new Intent(this, ChangePasswordActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            tokenManager.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
