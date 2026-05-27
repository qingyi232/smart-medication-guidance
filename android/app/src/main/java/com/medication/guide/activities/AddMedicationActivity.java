package com.medication.guide.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.medication.guide.databinding.ActivityAddMedicationBinding;
import com.medication.guide.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMedicationActivity extends AppCompatActivity {
    private ActivityAddMedicationBinding binding;
    private final List<String> scheduleTimes = new ArrayList<>();
    private String editMedicationId = null;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMedicationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSpinners();

        editMedicationId = getIntent().getStringExtra("medication_id");
        isEditMode = editMedicationId != null;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "编辑药品" : "添加药品");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (isEditMode) {
            binding.btnSave.setText("更新药品");
            loadMedicationData();
        }

        binding.btnAddTime.setOnClickListener(v -> showTimePicker());
        binding.btnSave.setOnClickListener(v -> saveMedication());
    }

    private void loadMedicationData() {
        String dataStr = getIntent().getStringExtra("medication_data");
        if (dataStr == null) return;

        try {
            JsonObject med = com.google.gson.JsonParser.parseString(dataStr).getAsJsonObject();

            binding.etMedName.setText(med.get("name").getAsString());
            binding.etDosage.setText(med.get("dosage").getAsString());

            if (med.has("indication") && !med.get("indication").isJsonNull())
                binding.etIndication.setText(med.get("indication").getAsString());
            if (med.has("precautions") && !med.get("precautions").isJsonNull())
                binding.etPrecautions.setText(med.get("precautions").getAsString());

            // 提醒设置回显
            if (med.has("reminderEnabled"))
                binding.switchReminder.setChecked(med.get("reminderEnabled").getAsBoolean());
            if (med.has("reminderAdvanceMinutes") && !med.get("reminderAdvanceMinutes").isJsonNull()) {
                int adv = med.get("reminderAdvanceMinutes").getAsInt();
                int[] advValues = {0, 5, 10, 15, 30};
                for (int i = 0; i < advValues.length; i++) {
                    if (advValues[i] == adv) { binding.spinnerAdvanceMinutes.setSelection(i); break; }
                }
            }
            if (med.has("reminderType") && !med.get("reminderType").isJsonNull()) {
                String rt = med.get("reminderType").getAsString();
                String[] rtValues = {"notification", "alarm", "silent"};
                for (int i = 0; i < rtValues.length; i++) {
                    if (rtValues[i].equals(rt)) { binding.spinnerReminderType.setSelection(i); break; }
                }
            }
            if (med.has("reminderRepeatCount") && !med.get("reminderRepeatCount").isJsonNull())
                binding.spinnerRepeatCount.setSelection(Math.min(med.get("reminderRepeatCount").getAsInt(), 3));

            if (med.has("scheduleTimes")) {
                JsonArray times = med.getAsJsonArray("scheduleTimes");
                for (int i = 0; i < times.size(); i++) {
                    scheduleTimes.add(times.get(i).getAsString());
                }
                updateTimesDisplay();
            }

            String[] frequencyValues = {"once_daily", "twice_daily", "three_times_daily",
                    "four_times_daily", "weekly", "as_needed"};
            String freq = med.get("frequency").getAsString();
            for (int i = 0; i < frequencyValues.length; i++) {
                if (frequencyValues[i].equals(freq)) {
                    binding.spinnerFrequency.setSelection(i);
                    break;
                }
            }

            String[] dosageFormValues = {"tablet", "capsule", "liquid", "injection",
                    "patch", "inhaler", "drops", "powder", "ointment"};
            if (med.has("dosageForm")) {
                String form = med.get("dosageForm").getAsString();
                for (int i = 0; i < dosageFormValues.length; i++) {
                    if (dosageFormValues[i].equals(form)) {
                        binding.spinnerDosageForm.setSelection(i);
                        break;
                    }
                }
            }

            String[] timingValues = {"before_meal", "after_meal", "with_meal",
                    "bedtime", "empty_stomach", "any_time"};
            if (med.has("timing")) {
                String timing = med.get("timing").getAsString();
                for (int i = 0; i < timingValues.length; i++) {
                    if (timingValues[i].equals(timing)) {
                        binding.spinnerTiming.setSelection(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "加载药品数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSpinners() {
        String[] frequencies = {"每日一次", "每日两次", "每日三次", "每日四次", "每周一次", "按需服用"};
        String[] frequencyValues = {"once_daily", "twice_daily", "three_times_daily",
                "four_times_daily", "weekly", "as_needed"};
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, frequencies);
        binding.spinnerFrequency.setAdapter(freqAdapter);

        String[] dosageForms = {"片剂", "胶囊", "液体", "注射", "贴剂", "吸入剂", "滴剂", "粉末", "软膏"};
        ArrayAdapter<String> formAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, dosageForms);
        binding.spinnerDosageForm.setAdapter(formAdapter);

        String[] timings = {"饭前", "饭后", "随餐", "睡前", "空腹", "任何时间"};
        ArrayAdapter<String> timingAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, timings);
        binding.spinnerTiming.setAdapter(timingAdapter);

        // 提醒设置 Spinners
        String[] advanceMinutes = {"准时提醒", "提前5分钟", "提前10分钟", "提前15分钟", "提前30分钟"};
        binding.spinnerAdvanceMinutes.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, advanceMinutes));

        String[] reminderTypes = {"通知推送", "闹钟提醒", "静默（仅记录）"};
        binding.spinnerReminderType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, reminderTypes));

        String[] repeatCounts = {"不重复", "重复1次", "重复2次", "重复3次"};
        binding.spinnerRepeatCount.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, repeatCounts));
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
            scheduleTimes.add(time);
            updateTimesDisplay();
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
    }

    private void updateTimesDisplay() {
        binding.tvScheduleTimes.setText("已设置: " + String.join(", ", scheduleTimes));
    }

    private void saveMedication() {
        String name = binding.etMedName.getText().toString().trim();
        String dosage = binding.etDosage.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入药品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (dosage.isEmpty()) {
            Toast.makeText(this, "请输入剂量", Toast.LENGTH_SHORT).show();
            return;
        }
        if (scheduleTimes.isEmpty()) {
            Toast.makeText(this, "请至少添加一个服药时间", Toast.LENGTH_SHORT).show();
            return;
        }

        // 先检查药品交互作用
        binding.btnSave.setEnabled(false);
        checkInteractionThenSave(name);
    }

    private void checkInteractionThenSave(String newMedName) {
        // 获取用户现有药品列表
        RetrofitClient.getInstance(this).getApi().getMedications("true")
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            com.google.gson.JsonArray meds = data.getAsJsonArray("medications");
                            com.google.gson.JsonArray names = new com.google.gson.JsonArray();
                            names.add(newMedName);
                            for (com.google.gson.JsonElement el : meds) {
                                JsonObject med = el.getAsJsonObject();
                                String existingName = med.get("name").getAsString();
                                // 编辑模式下排除自身
                                if (isEditMode && med.get("_id").getAsString().equals(editMedicationId)) continue;
                                names.add(existingName);
                            }

                            if (names.size() < 2) {
                                doSaveMedication();
                                return;
                            }

                            JsonObject body = new JsonObject();
                            body.add("medicationNames", names);
                            RetrofitClient.getInstance(AddMedicationActivity.this).getApi()
                                    .checkInteraction(body)
                                    .enqueue(new Callback<JsonObject>() {
                                        @Override
                                        public void onResponse(Call<JsonObject> call2, Response<JsonObject> resp2) {
                                            if (resp2.isSuccessful() && resp2.body() != null) {
                                                JsonObject interData = resp2.body().getAsJsonObject("data");
                                                boolean hasInteraction = interData.get("hasInteraction").getAsBoolean();
                                                if (hasInteraction) {
                                                    showInteractionWarning(interData);
                                                } else {
                                                    doSaveMedication();
                                                }
                                            } else {
                                                doSaveMedication();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<JsonObject> call2, Throwable t2) {
                                            doSaveMedication();
                                        }
                                    });
                        } else {
                            doSaveMedication();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        doSaveMedication();
                    }
                });
    }

    private void showInteractionWarning(JsonObject interData) {
        com.google.gson.JsonArray interactions = interData.getAsJsonArray("interactions");
        StringBuilder msg = new StringBuilder("检测到以下药品交互作用：\n\n");
        for (com.google.gson.JsonElement el : interactions) {
            JsonObject inter = el.getAsJsonObject();
            String severity = inter.get("severity").getAsString();
            String severityText = "high".equals(severity) ? "⚠️ 高风险" : "⚡ 中风险";
            msg.append(severityText).append("\n");
            msg.append(inter.get("drugA").getAsString()).append(" ↔ ").append(inter.get("drugB").getAsString()).append("\n");
            msg.append(inter.get("description").getAsString()).append("\n\n");
        }
        msg.append("是否仍要添加此药品？");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("药品交互作用警告")
                .setMessage(msg.toString())
                .setPositiveButton("仍然添加", (d, w) -> doSaveMedication())
                .setNegativeButton("取消", (d, w) -> binding.btnSave.setEnabled(true))
                .setCancelable(false)
                .show();
    }

    private void doSaveMedication() {
        String name = binding.etMedName.getText().toString().trim();
        String dosage = binding.etDosage.getText().toString().trim();

        String[] frequencyValues = {"once_daily", "twice_daily", "three_times_daily",
                "four_times_daily", "weekly", "as_needed"};
        String[] dosageFormValues = {"tablet", "capsule", "liquid", "injection",
                "patch", "inhaler", "drops", "powder", "ointment"};
        String[] timingValues = {"before_meal", "after_meal", "with_meal",
                "bedtime", "empty_stomach", "any_time"};

        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("dosage", dosage);
        body.addProperty("dosageUnit", "mg");
        body.addProperty("frequency", frequencyValues[binding.spinnerFrequency.getSelectedItemPosition()]);
        body.addProperty("dosageForm", dosageFormValues[binding.spinnerDosageForm.getSelectedItemPosition()]);
        body.addProperty("timing", timingValues[binding.spinnerTiming.getSelectedItemPosition()]);

        JsonArray times = new JsonArray();
        for (String t : scheduleTimes) times.add(t);
        body.add("scheduleTimes", times);

        String indication = binding.etIndication.getText().toString().trim();
        String precautions = binding.etPrecautions.getText().toString().trim();
        if (!indication.isEmpty()) body.addProperty("indication", indication);
        if (!precautions.isEmpty()) body.addProperty("precautions", precautions);

        // 提醒设置
        body.addProperty("reminderEnabled", binding.switchReminder.isChecked());
        int[] advanceValues = {0, 5, 10, 15, 30};
        body.addProperty("reminderAdvanceMinutes", advanceValues[binding.spinnerAdvanceMinutes.getSelectedItemPosition()]);
        String[] reminderTypeValues = {"notification", "alarm", "silent"};
        body.addProperty("reminderType", reminderTypeValues[binding.spinnerReminderType.getSelectedItemPosition()]);
        body.addProperty("reminderRepeatCount", binding.spinnerRepeatCount.getSelectedItemPosition());

        binding.btnSave.setEnabled(false);

        Call<JsonObject> call;
        if (isEditMode) {
            call = RetrofitClient.getInstance(this).getApi().updateMedication(editMedicationId, body);
        } else {
            call = RetrofitClient.getInstance(this).getApi().addMedication(body);
        }

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                binding.btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AddMedicationActivity.this,
                            isEditMode ? "药品更新成功" : "药品添加成功",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddMedicationActivity.this,
                            isEditMode ? "更新失败" : "添加失败",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                binding.btnSave.setEnabled(true);
                Toast.makeText(AddMedicationActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
