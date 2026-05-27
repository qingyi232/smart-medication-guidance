package com.medication.guide.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.medication.guide.databinding.ActivityChangePasswordBinding;
import com.medication.guide.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {
    private ActivityChangePasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("修改密码");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPwd = binding.etOldPassword.getText().toString().trim();
        String newPwd = binding.etNewPassword.getText().toString().trim();
        String confirmPwd = binding.etConfirmPassword.getText().toString().trim();

        if (oldPwd.isEmpty()) {
            Toast.makeText(this, "请输入旧密码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPwd.isEmpty() || newPwd.length() < 6) {
            Toast.makeText(this, "新密码至少6个字符", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnChangePassword.setEnabled(false);

        JsonObject body = new JsonObject();
        body.addProperty("oldPassword", oldPwd);
        body.addProperty("newPassword", newPwd);

        RetrofitClient.getInstance(this).getApi().changePassword(body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.btnChangePassword.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            boolean success = response.body().get("success").getAsBoolean();
                            if (success) {
                                Toast.makeText(ChangePasswordActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                String msg = response.body().has("message")
                                        ? response.body().get("message").getAsString()
                                        : "修改失败";
                                Toast.makeText(ChangePasswordActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, "旧密码错误或修改失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.btnChangePassword.setEnabled(true);
                        Toast.makeText(ChangePasswordActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
