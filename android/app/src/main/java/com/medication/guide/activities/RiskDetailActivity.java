package com.medication.guide.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.R;
import com.medication.guide.adapters.RiskHistoryAdapter;
import com.medication.guide.databinding.ActivityRiskDetailBinding;
import com.medication.guide.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RiskDetailActivity extends AppCompatActivity {
    private ActivityRiskDetailBinding binding;
    private RiskHistoryAdapter historyAdapter;
    private String currentAssessmentId = null;

    // 特征名中文映射
    private static final Map<String, String> FEATURE_NAMES = new HashMap<>();
    static {
        FEATURE_NAMES.put("avg_response_time_min", "平均提醒响应时间");
        FEATURE_NAMES.put("stress_level", "压力等级");
        FEATURE_NAMES.put("consecutive_missed", "连续漏服次数");
        FEATURE_NAMES.put("age", "年龄");
        FEATURE_NAMES.put("adherence_rate_7d", "7日依从率");
        FEATURE_NAMES.put("adherence_rate_30d", "30日依从率");
        FEATURE_NAMES.put("missed_doses_7d", "7日漏服次数");
        FEATURE_NAMES.put("sleep_hours", "日均睡眠时长");
        FEATURE_NAMES.put("social_support", "社会支持度");
        FEATURE_NAMES.put("bmi", "BMI指数");
        FEATURE_NAMES.put("medication_count", "用药种类数");
        FEATURE_NAMES.put("chronic_disease_count", "慢性病数量");
        FEATURE_NAMES.put("forgot_ratio", "忘记服药比例");
        FEATURE_NAMES.put("busy_ratio", "忙碌漏服比例");
        FEATURE_NAMES.put("side_effect_reported", "副作用报告");
        FEATURE_NAMES.put("activity_level", "活动水平");
        FEATURE_NAMES.put("late_doses_7d", "7日迟服次数");
        FEATURE_NAMES.put("gender", "性别");
        FEATURE_NAMES.put("education_level", "教育水平");
        FEATURE_NAMES.put("liver_function", "肝功能");
        FEATURE_NAMES.put("kidney_function", "肾功能");
        FEATURE_NAMES.put("comorbidities", "合并症数量");
        FEATURE_NAMES.put("side_effect_ratio", "副作用漏服比例");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRiskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI 风险评估详情");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        historyAdapter = new RiskHistoryAdapter(new ArrayList<>());
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(historyAdapter);

        binding.btnRefreshRisk.setOnClickListener(v -> performAssessment());
        binding.btnFollowedYes.setOnClickListener(v -> submitOutcome(true));
        binding.btnFollowedNo.setOnClickListener(v -> submitOutcome(false));

        loadLatestRisk();
        loadRiskHistory();
    }

    private void loadLatestRisk() {
        RetrofitClient.getInstance(this).getApi().getLatestRisk()
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            if (data.has("assessment") && !data.get("assessment").isJsonNull()) {
                                displayAssessment(data.getAsJsonObject("assessment"));
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(RiskDetailActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayAssessment(JsonObject assessment) {
        if (assessment.has("_id")) {
            currentAssessmentId = assessment.get("_id").getAsString();
        }

        // 风险评分
        double score = assessment.has("riskScore") ? assessment.get("riskScore").getAsDouble() : 0;
        String level = assessment.has("riskLevel") ? assessment.get("riskLevel").getAsString() : "low";

        binding.tvScore.setText(String.valueOf(Math.round(score)));
        binding.progressScore.setProgress((int) score);

        // AI 模型标识
        boolean mlUsed = assessment.has("mlModelUsed") && assessment.get("mlModelUsed").getAsBoolean();
        binding.tvModelName.setText(mlUsed ? "RandomForest 集成学习模型" : "规则引擎（ML服务离线）");
        binding.tvModelDetail.setText(mlUsed
                ? "基于 Kaggle 数据集训练 · 23维特征 · 5000样本 · 准确率76%"
                : "ML服务不可用，使用规则兜底评估");

        // 风险等级颜色
        int color;
        String levelText;
        switch (level) {
            case "high":
                color = Color.parseColor("#F44336");
                levelText = "高风险";
                break;
            case "medium":
                color = Color.parseColor("#FF9800");
                levelText = "中风险";
                break;
            default:
                color = Color.parseColor("#4CAF50");
                levelText = "低风险";
                break;
        }
        binding.tvScore.setTextColor(color);
        binding.tvRiskLevel.setText(levelText);
        binding.tvRiskLevel.setTextColor(color);

        // 概率分布
        if (assessment.has("probabilities") && !assessment.get("probabilities").isJsonNull()) {
            JsonObject prob = assessment.getAsJsonObject("probabilities");
            double pLow = prob.has("low") ? prob.get("low").getAsDouble() : 0;
            double pMed = prob.has("medium") ? prob.get("medium").getAsDouble() : 0;
            double pHigh = prob.has("high") ? prob.get("high").getAsDouble() : 0;

            binding.tvProbLow.setText(String.format("%.1f%%", pLow));
            binding.tvProbMedium.setText(String.format("%.1f%%", pMed));
            binding.tvProbHigh.setText(String.format("%.1f%%", pHigh));
            binding.progressProbLow.setProgress((int) pLow);
            binding.progressProbMedium.setProgress((int) pMed);
            binding.progressProbHigh.setProgress((int) pHigh);
        }

        // 关键风险因素
        binding.layoutRiskFactors.removeAllViews();
        if (assessment.has("riskFactors") && assessment.get("riskFactors").isJsonArray()) {
            JsonArray factors = assessment.getAsJsonArray("riskFactors");
            for (JsonElement el : factors) {
                JsonObject factor = el.getAsJsonObject();
                String featureName = factor.has("feature") ? factor.get("feature").getAsString() : "";
                double value = factor.has("value") ? factor.get("value").getAsDouble() : 0;
                double importance = factor.has("importance") ? factor.get("importance").getAsDouble() : 0;

                addRiskFactorRow(featureName, value, importance);
            }
        }

        // 干预策略
        binding.layoutInterventions.removeAllViews();
        if (assessment.has("interventions") && assessment.get("interventions").isJsonArray()) {
            JsonArray interventions = assessment.getAsJsonArray("interventions");
            if (interventions.size() > 0) {
                binding.layoutInterventionCard.setVisibility(View.VISIBLE);
                for (JsonElement el : interventions) {
                    JsonObject intervention = el.getAsJsonObject();
                    String message = intervention.has("message") ? intervention.get("message").getAsString() : "";
                    if (!message.isEmpty()) {
                        addInterventionRow(message);
                    }
                }
            } else {
                binding.layoutInterventionCard.setVisibility(View.GONE);
            }
        }

        // 反馈按钮
        if (assessment.has("interventions") && assessment.get("interventions").isJsonArray()
                && assessment.getAsJsonArray("interventions").size() > 0) {
            binding.layoutFeedback.setVisibility(View.VISIBLE);
        }
    }

    private void addRiskFactorRow(String featureName, double value, double importance) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 8, 0, 8);

        // 特征名
        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f));
        String displayName = FEATURE_NAMES.getOrDefault(featureName, featureName);
        tvName.setText(displayName);
        tvName.setTextSize(13);
        tvName.setTextColor(getResources().getColor(R.color.text_primary));

        // 当前值
        TextView tvValue = new TextView(this);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f));
        if (featureName.contains("rate") || featureName.contains("ratio")) {
            tvValue.setText(String.format("%.0f%%", value * 100));
        } else {
            tvValue.setText(String.format("%.1f", value));
        }
        tvValue.setTextSize(13);
        tvValue.setGravity(android.view.Gravity.CENTER);

        // 重要性进度条
        ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pb.setLayoutParams(new LinearLayout.LayoutParams(0, 16, 1.0f));
        pb.setMax(100);
        pb.setProgress((int) (importance * 500)); // 放大显示

        row.addView(tvName);
        row.addView(tvValue);
        row.addView(pb);

        binding.layoutRiskFactors.addView(row);
    }

    private void addInterventionRow(String message) {
        TextView tv = new TextView(this);
        tv.setText("• " + message);
        tv.setTextSize(14);
        tv.setTextColor(getResources().getColor(R.color.text_primary));
        tv.setPadding(0, 8, 0, 8);
        tv.setLineSpacing(4, 1);
        binding.layoutInterventions.addView(tv);
    }

    private void loadRiskHistory() {
        RetrofitClient.getInstance(this).getApi().getRiskHistory(20)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonArray arr = data.getAsJsonArray("assessments");
                            List<JsonObject> items = new ArrayList<>();
                            if (arr != null) {
                                for (JsonElement el : arr) items.add(el.getAsJsonObject());
                            }
                            historyAdapter.updateData(items);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void performAssessment() {
        binding.btnRefreshRisk.setEnabled(false);
        binding.btnRefreshRisk.setText("AI 分析中...");

        RetrofitClient.getInstance(this).getApi().assessRisk()
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.btnRefreshRisk.setEnabled(true);
                        binding.btnRefreshRisk.setText("重新评估");
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            if (data.has("assessment")) {
                                displayAssessment(data.getAsJsonObject("assessment"));
                                Toast.makeText(RiskDetailActivity.this, "AI 评估完成", Toast.LENGTH_SHORT).show();
                            }
                            loadRiskHistory();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.btnRefreshRisk.setEnabled(true);
                        binding.btnRefreshRisk.setText("重新评估");
                        Toast.makeText(RiskDetailActivity.this, "评估失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void submitOutcome(boolean followed) {
        if (currentAssessmentId == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("outcome", followed ? "followed" : "not_followed");

        RetrofitClient.getInstance(this).getApi()
                .updateRiskOutcome(currentAssessmentId, body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(RiskDetailActivity.this,
                                    followed ? "感谢反馈！已记录遵循" : "已记录，我们会优化建议",
                                    Toast.LENGTH_SHORT).show();
                            binding.layoutFeedback.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
