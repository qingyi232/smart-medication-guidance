package com.medication.guide.network;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ===== 认证 =====
    @POST("auth/register")
    Call<JsonObject> register(@Body JsonObject body);

    @POST("auth/login")
    Call<JsonObject> login(@Body JsonObject body);

    @GET("auth/profile")
    Call<JsonObject> getProfile();

    @PUT("auth/profile")
    Call<JsonObject> updateProfile(@Body JsonObject body);

    @PUT("auth/fcm-token")
    Call<JsonObject> updateFcmToken(@Body JsonObject body);

    // ===== 药品管理 =====
    @POST("medications")
    Call<JsonObject> addMedication(@Body JsonObject body);

    @GET("medications")
    Call<JsonObject> getMedications(@Query("active") String active);

    @GET("medications/{id}")
    Call<JsonObject> getMedication(@Path("id") String id);

    @PUT("medications/{id}")
    Call<JsonObject> updateMedication(@Path("id") String id, @Body JsonObject body);

    @DELETE("medications/{id}")
    Call<JsonObject> deleteMedication(@Path("id") String id);

    // ===== 用药记录 =====
    @POST("logs")
    Call<JsonObject> recordLog(@Body JsonObject body);

    @GET("logs")
    Call<JsonObject> getLogs(@Query("page") int page, @Query("limit") int limit);

    @GET("logs")
    Call<JsonObject> getLogsFiltered(@Query("page") int page, @Query("limit") int limit,
                                     @Query("status") String status,
                                     @Query("startDate") String startDate,
                                     @Query("endDate") String endDate);

    @GET("logs/today")
    Call<JsonObject> getTodaySchedule();

    @GET("logs/adherence")
    Call<JsonObject> getAdherenceStats(@Query("days") int days);

    // ===== 风险评估 =====
    @POST("risk/assess")
    Call<JsonObject> assessRisk();

    @GET("risk/history")
    Call<JsonObject> getRiskHistory(@Query("limit") int limit);

    @GET("risk/latest")
    Call<JsonObject> getLatestRisk();

    // ===== 修改密码 =====
    @PUT("auth/change-password")
    Call<JsonObject> changePassword(@Body JsonObject body);

    // ===== 药品交互检查 =====
    @POST("medications/interaction-check")
    Call<JsonObject> checkInteraction(@Body JsonObject body);

    // ===== 用药知识库 =====
    @GET("medication-info")
    Call<JsonObject> searchMedicationInfo(@Query("keyword") String keyword);

    @GET("medication-info/{id}")
    Call<JsonObject> getMedicationInfo(@Path("id") String id);

    // ===== 数据导出 =====
    @GET("logs/export")
    Call<JsonObject> exportLogs(@Query("days") int days);

    // ===== 干预效果追踪 =====
    @PUT("risk/{id}/outcome")
    Call<JsonObject> updateRiskOutcome(@Path("id") String id, @Body JsonObject body);

    // ===== 健康检查 =====
    @GET("health")
    Call<JsonObject> healthCheck();
}
