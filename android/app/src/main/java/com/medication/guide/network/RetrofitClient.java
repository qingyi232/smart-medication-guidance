package com.medication.guide.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.medication.guide.BuildConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static RetrofitClient instance;
    private final ApiService apiService;

    private RetrofitClient(Context context) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .addInterceptor(new TokenExpiredInterceptor(context))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
        return instance;
    }

    public ApiService getApi() {
        return apiService;
    }

    private static class AuthInterceptor implements Interceptor {
        private final Context context;

        AuthInterceptor(Context context) {
            this.context = context;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
            String token = prefs.getString("token", null);

            Request original = chain.request();
            if (token != null) {
                Request request = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(request);
            }
            return chain.proceed(original);
        }
    }

    // Token 过期拦截器 — 401 时自动清除 Token 并跳转登录页
    static class TokenExpiredInterceptor implements Interceptor {
        private final Context context;

        TokenExpiredInterceptor(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            if (response.code() == 401) {
                // 清除 Token
                SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
                prefs.edit().remove("token").apply();

                // 跳转登录页
                android.content.Intent intent = new android.content.Intent(context,
                        com.medication.guide.activities.LoginActivity.class);
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            }
            return response;
        }
    }
}
