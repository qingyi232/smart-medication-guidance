package com.medication.guide.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.medication.guide.databinding.ActivityRegisterBinding;
import com.medication.guide.network.RetrofitClient;
import com.medication.guide.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSpinners();
        binding.btnRegister.setOnClickListener(v -> register());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void setupSpinners() {
        String[] genders = {"请选择性别", "male", "female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, genders);
        binding.spinnerGender.setAdapter(genderAdapter);
    }

    private void register() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String realName = binding.etRealName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String ageStr = binding.etAge.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.length() < 3) {
            Toast.makeText(this, "用户名至少3个字符", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "密码至少6个字符", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
            Toast.makeText(this, "手机号格式不正确", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ageStr.isEmpty()) {
            try {
                int age = Integer.parseInt(ageStr);
                if (age < 0 || age > 150) {
                    Toast.makeText(this, "年龄范围 0-150", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "年龄格式不正确", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        binding.btnRegister.setEnabled(false);

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);
        if (!realName.isEmpty()) body.addProperty("realName", realName);
        if (!phone.isEmpty()) body.addProperty("phone", phone);
        if (!ageStr.isEmpty()) body.addProperty("age", Integer.parseInt(ageStr));

        int genderPos = binding.spinnerGender.getSelectedItemPosition();
        if (genderPos > 0) {
            body.addProperty("gender", genderPos == 1 ? "male" : "female");
        }

        // 慢性病选择
        JsonArray diseases = new JsonArray();
        if (binding.cbHypertension.isChecked()) diseases.add("hypertension");
        if (binding.cbDiabetes.isChecked()) diseases.add("diabetes");
        if (binding.cbHeartDisease.isChecked()) diseases.add("heart_disease");
        if (binding.cbAsthma.isChecked()) diseases.add("asthma");
        if (diseases.size() > 0) body.add("chronicDiseases", diseases);

        RetrofitClient.getInstance(this).getApi().register(body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.btnRegister.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            boolean success = response.body().get("success").getAsBoolean();
                            if (success) {
                                Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                String msg = response.body().has("message")
                                        ? response.body().get("message").getAsString()
                                        : "注册失败";
                                Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this, "注册失败，用户名可能已存在", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
