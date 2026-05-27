"""
使用 Kaggle 数据集「IoT-Based Chronic Medication Adherence Dataset」中的
medication_adherence.csv 训练风险预测模型。

数据源: https://www.kaggle.com/datasets/programmer3/iot-based-chronic-medication-adherence-dataset
默认读取路径: 项目根目录下的 medication_adherence.csv（可通过环境变量 ML_DATA_CSV 覆盖）

标签: future_non_adherence（二分类）。推理服务将映射为低/高两档，并在展示上兼容原有三档 UI。
特征: 与 backend/src/controllers/riskController.js 中 extractFeatures 的 23 个字段对齐。
"""
import os
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.preprocessing import StandardScaler
import joblib

np.random.seed(42)

FEATURE_ORDER = [
    "age",
    "gender",
    "education_level",
    "chronic_disease_count",
    "comorbidities",
    "medication_count",
    "liver_function",
    "kidney_function",
    "adherence_rate_7d",
    "adherence_rate_30d",
    "missed_doses_7d",
    "consecutive_missed",
    "late_doses_7d",
    "avg_response_time_min",
    "stress_level",
    "sleep_hours",
    "activity_level",
    "social_support",
    "bmi",
    "forgot_ratio",
    "busy_ratio",
    "side_effect_ratio",
    "side_effect_reported",
]


def resolve_csv_path():
    env = os.environ.get("ML_DATA_CSV")
    if env and os.path.isfile(env):
        return env
    base = os.path.dirname(__file__)
    root = os.path.abspath(os.path.join(base, ".."))
    default = os.path.join(root, "medication_adherence.csv")
    return default


def map_activity_to_level(x):
    """CSV 中 activity_level 为约 1001–9999 的数值，映射到 0–4 与 App 一致。"""
    x = float(x)
    return int(np.clip((x - 1000.0) / 2250.0, 0, 4))


def scale_stress(s):
    """CSV 压力多为 1–5，映射到约 1–10。"""
    return float(np.clip(s * 2.0, 1, 10))


def scale_social(s):
    """CSV 社会支持 0–5，映射到 0–10。"""
    return float(np.clip(s * 2.0, 0, 10))


def build_features_from_csv(df: pd.DataFrame) -> pd.DataFrame:
    df = df.copy()
    df["timestamp"] = pd.to_datetime(df["timestamp"])
    df.sort_values(["patient_id", "timestamp"], inplace=True)

    global_med_rt = df["reminder_response_time"].median()
    if pd.isna(global_med_rt):
        global_med_rt = 60.0

    rows = []

    for pid, g in df.groupby("patient_id", sort=False):
        g = g.sort_values("timestamp").reset_index(drop=True)
        n = len(g)
        for i in range(n):
            row = g.iloc[i]
            ts = row["timestamp"]

            past = g[g["timestamp"] < ts]
            past7 = past[past["timestamp"] >= ts - pd.Timedelta(days=7)]
            past30 = past[past["timestamp"] >= ts - pd.Timedelta(days=30)]

            # 依从率（仅历史，不含当前行）
            def adherence(sub):
                if len(sub) == 0:
                    return 1.0
                t = sub["taken"].astype(int)
                return float(t.mean()) if len(t) else 1.0

            adherence_7d = adherence(past7)
            adherence_30d = adherence(past30)

            missed_7d = int((past7["taken"] == 0).sum()) if len(past7) else 0
            late_7d = 0

            # 连续漏服：从上一事件往回数
            cons = 0
            for j in range(i - 1, -1, -1):
                if int(g.iloc[j]["taken"]) == 0:
                    cons += 1
                else:
                    break

            missed30 = past30[past30["taken"] == 0]
            tm = max(len(missed30), 1)
            forgot = (missed30["missed_reason"] == "forgetfulness").sum()
            busy = (missed30["missed_reason"] == "busy").sum()
            se = (missed30["missed_reason"] == "side_effects").sum()

            rt_series = past7["reminder_response_time"].dropna()
            avg_rt = float(rt_series.mean()) if len(rt_series) else float(
                row["reminder_response_time"] if pd.notna(row["reminder_response_time"]) else global_med_rt
            )
            if pd.isna(avg_rt):
                avg_rt = global_med_rt

            hist_inc = g.iloc[: i + 1]
            chronic_disease_count = int(
                np.clip(hist_inc["chronic_condition"].nunique(dropna=True), 1, 4)
            )
            medication_count = int(max(hist_inc["medication_type"].nunique(dropna=True), 1))

            win7_inc = g[
                (g["timestamp"] >= ts - pd.Timedelta(days=7))
                & (g["timestamp"] <= ts)
            ]
            se_report = 1 if (win7_inc["missed_reason"] == "side_effects").any() else 0

            gender = 1 if str(row["gender"]).upper().startswith("M") else 0

            feat = {
                "age": float(row["age"]),
                "gender": gender,
                "education_level": 2.0,
                "chronic_disease_count": float(chronic_disease_count),
                "comorbidities": float(row["comorbidities"]),
                "medication_count": float(medication_count),
                "liver_function": 0.0,
                "kidney_function": 0.0,
                "adherence_rate_7d": adherence_7d,
                "adherence_rate_30d": adherence_30d,
                "missed_doses_7d": float(missed_7d),
                "consecutive_missed": float(cons),
                "late_doses_7d": float(late_7d),
                "avg_response_time_min": avg_rt,
                "stress_level": scale_stress(row["stress_level"]),
                "sleep_hours": float(row["sleep_hours"]),
                "activity_level": float(map_activity_to_level(row["activity_level"])),
                "social_support": scale_social(row["social_support"]),
                "bmi": float(row["BMI"]),
                "forgot_ratio": forgot / tm,
                "busy_ratio": busy / tm,
                "side_effect_ratio": se / tm,
                "side_effect_reported": float(se_report),
            }
            feat["label"] = int(row["future_non_adherence"])
            rows.append(feat)

    out = pd.DataFrame(rows)
    return out


def main():
    csv_path = resolve_csv_path()
    print("=" * 60)
    print("  IoT Medication Adherence — 真实 CSV 训练")
    print("=" * 60)
    print(f"\n数据文件: {csv_path}")

    if not os.path.isfile(csv_path):
        print("\n[ERROR] 未找到 medication_adherence.csv。")
        print("请将 Kaggle 下载的 CSV 放到项目根目录，或设置环境变量 ML_DATA_CSV=绝对路径")
        return

    print("\n[1/4] 读取并构造特征（与后端 extractFeatures 字段对齐）...")
    raw = pd.read_csv(csv_path)
    data = build_features_from_csv(raw)
    y = data["label"].values
    X = data[FEATURE_ORDER].values

    print(f"  样本数: {len(data)}")
    print(f"  future_non_adherence=1 比例: {y.mean():.4f}")

    print("\n[2/4] 划分训练 / 测试集...")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    scaler = StandardScaler()
    X_train_s = scaler.fit_transform(X_train)
    X_test_s = scaler.transform(X_test)

    print("\n[3/4] 训练 RandomForest（二分类）...")
    rf = RandomForestClassifier(
        n_estimators=300,
        max_depth=16,
        min_samples_split=4,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1,
    )
    rf.fit(X_train_s, y_train)

    acc = rf.score(X_test_s, y_test)
    print(f"  测试集准确率: {acc:.4f}")

    try:
        proba = rf.predict_proba(X_test_s)[:, 1]
        auc = roc_auc_score(y_test, proba)
        print(f"  ROC-AUC: {auc:.4f}")
    except Exception as e:
        print(f"  ROC-AUC: (skip) {e}")

    print("\n  分类报告:")
    y_pred = rf.predict(X_test_s)
    print(classification_report(y_test, y_pred, target_names=["依从(0)", "未来可能不依从(1)"]))

    cv_scores = cross_val_score(rf, scaler.transform(X), y, cv=5, scoring="roc_auc")
    print(f"  5折交叉验证 ROC-AUC: {cv_scores.mean():.4f} +/- {cv_scores.std():.4f}")

    print("\n[4/4] 保存模型...")
    model_dir = os.path.join(os.path.dirname(__file__), "models")
    os.makedirs(model_dir, exist_ok=True)
    joblib.dump(rf, os.path.join(model_dir, "risk_model.pkl"))
    joblib.dump(scaler, os.path.join(model_dir, "scaler.pkl"))
    joblib.dump(FEATURE_ORDER, os.path.join(model_dir, "feature_cols.pkl"))
    meta = {
        "source": "Kaggle programmer3/iot-based-chronic-medication-adherence-dataset",
        "csv_file": os.path.basename(csv_path),
        "n_samples": int(len(data)),
        "task": "binary_future_non_adherence",
        "feature_count": len(FEATURE_ORDER),
    }
    joblib.dump(meta, os.path.join(model_dir, "training_meta.pkl"))

    print(f"  已写入 {model_dir}/")
    print("  - risk_model.pkl")
    print("  - scaler.pkl")
    print("  - feature_cols.pkl")
    print("  - training_meta.pkl")
    print("\n[OK] 训练完成（基于真实 medication_adherence.csv）。")


if __name__ == "__main__":
    main()
