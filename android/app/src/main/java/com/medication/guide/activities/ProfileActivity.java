package com.medication.guide.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.medication.guide.R;
import com.medication.guide.databinding.ActivityProfileBinding;
import com.medication.guide.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("个人健康档案");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupSpinners();
        setupSeekBar();
        loadProfile();

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void setupSpinners() {
        String[] stressLevels = new String[10];
        for (int i = 0; i < 10; i++) stressLevels[i] = String.valueOf(i + 1);
        binding.spinnerStress.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, stressLevels));

        String[] activities = {"久坐", "轻度活动", "适度活动", "活跃", "非常活跃"};
        binding.spinnerActivity.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, activities));

        String[] relationships = {"配偶", "父母", "子女", "兄弟姐妹", "朋友", "医生", "其他"};
        binding.spinnerRelationship.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, relationships));
    }

    private void setupSeekBar() {
        binding.seekBarSocialSupport.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                binding.tvSocialSupportValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadProfile() {
        RetrofitClient.getInstance(this).getApi().getProfile()
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject user = response.body().getAsJsonObject("data")
                                    .getAsJsonObject("user");

                            if (user.has("realName") && !user.get("realName").isJsonNull())
                                binding.etRealName.setText(user.get("realName").getAsString());
                            if (user.has("phone") && !user.get("phone").isJsonNull())
                                binding.etPhone.setText(user.get("phone").getAsString());
                            if (user.has("age") && !user.get("age").isJsonNull())
                                binding.etAge.setText(String.valueOf(user.get("age").getAsInt()));
                            if (user.has("stressLevel") && !user.get("stressLevel").isJsonNull())
                                binding.spinnerStress.setSelection(user.get("stressLevel").getAsInt() - 1);

                            if (user.has("activityLevel") && !user.get("activityLevel").isJsonNull()) {
                                String[] actValues = {"sedentary", "light", "moderate", "active", "very_active"};
                                String act = user.get("activityLevel").getAsString();
                                for (int i = 0; i < actValues.length; i++) {
                                    if (actValues[i].equals(act)) {
                                        binding.spinnerActivity.setSelection(i);
                                        break;
                                    }
                                }
                            }

                            // 性别
                            if (user.has("gender") && !user.get("gender").isJsonNull()) {
                                String gender = user.get("gender").getAsString();
                                switch (gender) {
                                    case "male": binding.rbMale.setChecked(true); break;
                                    case "female": binding.rbFemale.setChecked(true); break;
                                    case "other": binding.rbOther.setChecked(true); break;
                                }
                            }

                            // 慢性病
                            if (user.has("chronicDiseases") && user.get("chronicDiseases").isJsonArray()) {
                                JsonArray diseases = user.getAsJsonArray("chronicDiseases");
                                for (JsonElement el : diseases) {
                                    String d = el.getAsString();
                                    switch (d) {
                                        case "hypertension": binding.cbHypertension.setChecked(true); break;
                                        case "diabetes": binding.cbDiabetes.setChecked(true); break;
                                        case "heart_disease": binding.cbHeartDisease.setChecked(true); break;
                                        case "asthma": binding.cbAsthma.setChecked(true); break;
                                        case "arthritis": binding.cbArthritis.setChecked(true); break;
                                        case "depression": binding.cbDepression.setChecked(true); break;
                                    }
                                }
                            }

                            // BMI
                            if (user.has("bmi") && !user.get("bmi").isJsonNull())
                                binding.etBmi.setText(String.valueOf(user.get("bmi").getAsDouble()));

                            // 睡眠时长
                            if (user.has("sleepHours") && !user.get("sleepHours").isJsonNull())
                                binding.etSleepHours.setText(String.valueOf(user.get("sleepHours").getAsDouble()));

                            // 社会支持度
                            if (user.has("socialSupport") && !user.get("socialSupport").isJsonNull()) {
                                int ss = user.get("socialSupport").getAsInt();
                                binding.seekBarSocialSupport.setProgress(ss);
                                binding.tvSocialSupportValue.setText(String.valueOf(ss));
                            }

                            // 紧急联系人
                            if (user.has("emergencyContact") && !user.get("emergencyContact").isJsonNull()) {
                                JsonObject ec = user.getAsJsonObject("emergencyContact");
                                if (ec.has("name") && !ec.get("name").isJsonNull())
                                    binding.etEmergencyName.setText(ec.get("name").getAsString());
                                if (ec.has("phone") && !ec.get("phone").isJsonNull())
                                    binding.etEmergencyPhone.setText(ec.get("phone").getAsString());
                                if (ec.has("relationship") && !ec.get("relationship").isJsonNull()) {
                                    String[] relValues = {"spouse", "parent", "child", "sibling", "friend", "doctor", "other"};
                                    String rel = ec.get("relationship").getAsString();
                                    for (int i = 0; i < relValues.length; i++) {
                                        if (relValues[i].equals(rel)) {
                                            binding.spinnerRelationship.setSelection(i);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(ProfileActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveProfile() {
        JsonObject body = new JsonObject();

        String realName = binding.etRealName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String ageStr = binding.etAge.getText().toString().trim();

        if (!realName.isEmpty()) body.addProperty("realName", realName);
        if (!phone.isEmpty()) body.addProperty("phone", phone);
        if (!ageStr.isEmpty()) body.addProperty("age", Integer.parseInt(ageStr));

        body.addProperty("stressLevel", binding.spinnerStress.getSelectedItemPosition() + 1);

        String[] actValues = {"sedentary", "light", "moderate", "active", "very_active"};
        body.addProperty("activityLevel", actValues[binding.spinnerActivity.getSelectedItemPosition()]);

        // 性别
        int genderId = binding.rgGender.getCheckedRadioButtonId();
        if (genderId == R.id.rbMale) body.addProperty("gender", "male");
        else if (genderId == R.id.rbFemale) body.addProperty("gender", "female");
        else if (genderId == R.id.rbOther) body.addProperty("gender", "other");

        // 慢性病
        JsonArray diseases = new JsonArray();
        if (binding.cbHypertension.isChecked()) diseases.add("hypertension");
        if (binding.cbDiabetes.isChecked()) diseases.add("diabetes");
        if (binding.cbHeartDisease.isChecked()) diseases.add("heart_disease");
        if (binding.cbAsthma.isChecked()) diseases.add("asthma");
        if (binding.cbArthritis.isChecked()) diseases.add("arthritis");
        if (binding.cbDepression.isChecked()) diseases.add("depression");
        body.add("chronicDiseases", diseases);

        // BMI
        String bmiStr = binding.etBmi.getText().toString().trim();
        if (!bmiStr.isEmpty()) body.addProperty("bmi", Double.parseDouble(bmiStr));

        // 睡眠时长
        String sleepStr = binding.etSleepHours.getText().toString().trim();
        if (!sleepStr.isEmpty()) body.addProperty("sleepHours", Double.parseDouble(sleepStr));

        // 社会支持度
        body.addProperty("socialSupport", binding.seekBarSocialSupport.getProgress());

        // 紧急联系人
        String ecName = binding.etEmergencyName.getText().toString().trim();
        String ecPhone = binding.etEmergencyPhone.getText().toString().trim();
        if (!ecName.isEmpty() || !ecPhone.isEmpty()) {
            JsonObject ec = new JsonObject();
            ec.addProperty("name", ecName);
            ec.addProperty("phone", ecPhone);
            String[] relValues = {"spouse", "parent", "child", "sibling", "friend", "doctor", "other"};
            ec.addProperty("relationship", relValues[binding.spinnerRelationship.getSelectedItemPosition()]);
            body.add("emergencyContact", ec);
        }

        binding.btnSave.setEnabled(false);
        RetrofitClient.getInstance(this).getApi().updateProfile(body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.btnSave.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.btnSave.setEnabled(true);
                        Toast.makeText(ProfileActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
