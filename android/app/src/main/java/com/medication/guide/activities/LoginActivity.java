package com.medication.guide.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.medication.guide.databinding.ActivityLoginBinding;
import com.medication.guide.network.RetrofitClient;
import com.medication.guide.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tokenManager = new TokenManager(this);

        if (tokenManager.isLoggedIn()) {
            navigateToMain();
            return;
        }

        binding.btnLogin.setOnClickListener(v -> login());
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void login() {
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show();
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

        binding.btnLogin.setEnabled(false);

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        RetrofitClient.getInstance(this).getApi().login(body)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.btnLogin.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject data = response.body().getAsJsonObject("data");
                            String token = data.get("token").getAsString();
                            tokenManager.saveToken(token);
                            tokenManager.saveUsername(username);

                            JsonObject user = data.getAsJsonObject("user");
                            if (user.has("_id")) {
                                tokenManager.saveUserId(user.get("_id").getAsString());
                            }

                            Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "网络连接失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
