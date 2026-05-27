package com.medication.guide.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.R;
import com.medication.guide.databinding.ActivityStatisticsBinding;
import com.medication.guide.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {
    private ActivityStatisticsBinding binding;
    private int currentDays = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("用药统计");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupTimeRangeButtons();
        loadAllData();

        binding.btnExport.setOnClickListener(v -> exportReport());
    }

    private void setupTimeRangeButtons() {
        binding.btn7Days.setOnClickListener(v -> switchDays(7));
        binding.btn14Days.setOnClickListener(v -> switchDays(14));
        binding.btn30Days.setOnClickListener(v -> switchDays(30));
    }

    private void switchDays(int days) {
        currentDays = days;
        // 更新按钮样式
        binding.btn7Days.setStrokeColorResource(days == 7 ? R.color.primary : R.color.text_secondary);
        binding.btn14Days.setStrokeColorResource(days == 14 ? R.color.primary : R.color.text_secondary);
        binding.btn30Days.setStrokeColorResource(days == 30 ? R.color.primary : R.color.text_secondary);

        // 用背景色区分选中
        binding.btn7Days.setBackgroundColor(days == 7 ? getResources().getColor(R.color.primary) : Color.TRANSPARENT);
        binding.btn14Days.setBackgroundColor(days == 14 ? getResources().getColor(R.color.primary) : Color.TRANSPARENT);
        binding.btn30Days.setBackgroundColor(days == 30 ? getResources().getColor(R.color.primary) : Color.TRANSPARENT);
        binding.btn7Days.setTextColor(days == 7 ? Color.WHITE : getResources().getColor(R.color.primary));
        binding.btn14Days.setTextColor(days == 14 ? Color.WHITE : getResources().getColor(R.color.primary));
        binding.btn30Days.setTextColor(days == 30 ? Color.WHITE : getResources().getColor(R.color.primary));

        loadAllData();
    }

    private void loadAllData() {
        loadStats(currentDays);
        loadStats30d();
        loadRiskHistory();
    }

    private void loadStats(int days) {
        RetrofitClient.getInstance(this).getApi().getAdherenceStats(days)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonObject summary = data.getAsJsonObject("summary");
                            JsonObject dailyStats = data.getAsJsonObject("dailyStats");

                            setupPieChart(summary, days);
                            setupBarChart(dailyStats);

                            double rate = summary.get("adherenceRate").getAsDouble();
                            binding.tvAdherenceRate7d.setText(String.format("%.1f%%", rate));
                            binding.tvTotalDoses.setText(String.valueOf(summary.get("total").getAsInt()));
                            binding.tvTakenDoses.setText(String.valueOf(summary.get("taken").getAsInt()));
                            binding.tvMissedDoses.setText(String.valueOf(summary.get("missed").getAsInt()));
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(StatisticsActivity.this, "加载统计数据失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadStats30d() {
        RetrofitClient.getInstance(this).getApi().getAdherenceStats(30)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            JsonObject summary = data.getAsJsonObject("summary");
                            double rate = summary.get("adherenceRate").getAsDouble();
                            binding.tvAdherenceRate30d.setText(String.format("%.1f%%", rate));
                            int avgResp = summary.get("avgResponseTime").getAsInt();
                            binding.tvAvgResponse.setText(avgResp > 0 ? avgResp + "秒" : "--");
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void setupPieChart(JsonObject summary, int days) {
        PieChart chart = binding.pieChart;
        int taken = summary.get("taken").getAsInt();
        int late = summary.get("late").getAsInt();
        int missed = summary.get("missed").getAsInt();
        int skipped = summary.get("skipped").getAsInt();

        List<PieEntry> entries = new ArrayList<>();
        if (taken > 0) entries.add(new PieEntry(taken, "按时服药"));
        if (late > 0) entries.add(new PieEntry(late, "迟服"));
        if (missed > 0) entries.add(new PieEntry(missed, "漏服"));
        if (skipped > 0) entries.add(new PieEntry(skipped, "跳过"));

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "暂无数据"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(
                Color.parseColor("#43A047"),
                Color.parseColor("#FB8C00"),
                Color.parseColor("#E53935"),
                Color.parseColor("#B0BEC5")
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter(chart));

        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setEntryLabelTextSize(11f);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setCenterText(days + "日统计");
        chart.setCenterTextSize(14f);
        chart.setHoleRadius(40f);
        chart.setTransparentCircleRadius(45f);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.animateY(800);
        chart.invalidate();
    }

    private void setupBarChart(JsonObject dailyStats) {
        BarChart chart = binding.barChart;
        List<BarEntry> takenEntries = new ArrayList<>();
        List<BarEntry> missedEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        List<String> dates = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : dailyStats.entrySet()) {
            dates.add(entry.getKey());
        }
        Collections.sort(dates);

        int i = 0;
        for (String date : dates) {
            JsonObject day = dailyStats.getAsJsonObject(date);
            int t = day.get("taken").getAsInt() + day.get("late").getAsInt();
            int m = day.get("missed").getAsInt();
            takenEntries.add(new BarEntry(i, t));
            missedEntries.add(new BarEntry(i, m));
            labels.add(date.substring(5));
            i++;
        }

        BarDataSet takenSet = new BarDataSet(takenEntries, "已服药");
        takenSet.setColor(Color.parseColor("#43A047"));

        BarDataSet missedSet = new BarDataSet(missedEntries, "漏服");
        missedSet.setColor(Color.parseColor("#E53935"));

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;

        BarData barData = new BarData(takenSet, missedSet);
        barData.setBarWidth(barWidth);
        chart.setData(barData);

        if (dates.size() > 1) {
            chart.groupBars(0f, groupSpace, barSpace);
        }

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(dates.size() > 1);
        xAxis.setLabelRotationAngle(currentDays > 14 ? 45f : 0f);

        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.animateY(800);
        chart.invalidate();
    }

    private void loadRiskHistory() {
        RetrofitClient.getInstance(this).getApi().getRiskHistory(currentDays)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            setupRiskLineChart(data.getAsJsonArray("assessments"));
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void setupRiskLineChart(com.google.gson.JsonArray assessments) {
        LineChart chart = binding.lineChartRisk;
        List<Entry> entries = new ArrayList<>();

        for (int i = assessments.size() - 1; i >= 0; i--) {
            JsonObject a = assessments.get(i).getAsJsonObject();
            int score = a.get("riskScore").getAsInt();
            entries.add(new Entry(assessments.size() - 1 - i, score));
        }

        if (entries.isEmpty()) {
            chart.setNoDataText("暂无风险评估数据");
            chart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "风险评分趋势");
        dataSet.setColor(Color.parseColor("#E53935"));
        dataSet.setCircleColor(Color.parseColor("#E53935"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#FFCDD2"));
        dataSet.setFillAlpha(80);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getAxisLeft().setAxisMaximum(100);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getLegend().setEnabled(true);
        chart.animateX(800);
        chart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void exportReport() {
        binding.btnExport.setEnabled(false);
        binding.btnExport.setText("正在生成报告...");

        RetrofitClient.getInstance(this).getApi().exportLogs(currentDays)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.btnExport.setEnabled(true);
                        binding.btnExport.setText("📊 导出用药报告");

                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject report = response.body().getAsJsonObject("data").getAsJsonObject("report");
                            showExportDialog(report);
                        } else {
                            Toast.makeText(StatisticsActivity.this, "导出失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.btnExport.setEnabled(true);
                        binding.btnExport.setText("📊 导出用药报告");
                        Toast.makeText(StatisticsActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showExportDialog(JsonObject report) {
        JsonObject summary = report.getAsJsonObject("summary");
        JsonObject period = report.getAsJsonObject("period");

        StringBuilder sb = new StringBuilder();
        sb.append("【用药报告】\n");
        sb.append("用户: ").append(report.get("userName").getAsString()).append("\n");
        sb.append("时间范围: ").append(period.get("days").getAsInt()).append("天\n\n");

        sb.append("📊 总体统计\n");
        sb.append("总剂次: ").append(summary.get("total").getAsInt()).append("\n");
        sb.append("已服药: ").append(summary.get("taken").getAsInt()).append("\n");
        sb.append("迟服: ").append(summary.get("late").getAsInt()).append("\n");
        sb.append("漏服: ").append(summary.get("missed").getAsInt()).append("\n");
        sb.append("依从率: ").append(String.format("%.1f%%", summary.get("adherenceRate").getAsDouble())).append("\n\n");

        if (report.has("medicationStats")) {
            sb.append("💊 各药品统计\n");
            JsonObject medStats = report.getAsJsonObject("medicationStats");
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : medStats.entrySet()) {
                JsonObject ms = entry.getValue().getAsJsonObject();
                sb.append(entry.getKey()).append(": 共").append(ms.get("total").getAsInt())
                        .append("次, 已服").append(ms.get("taken").getAsInt())
                        .append(", 漏服").append(ms.get("missed").getAsInt()).append("\n");
            }
        }

        // 复制到剪贴板
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("用药报告")
                .setMessage(sb.toString())
                .setPositiveButton("复制到剪贴板", (d, w) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("用药报告", sb.toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "报告已复制到剪贴板", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("分享", (d, w) -> {
                    android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString());
                    startActivity(android.content.Intent.createChooser(shareIntent, "分享用药报告"));
                })
                .setNegativeButton("关闭", null)
                .show();
    }
}
