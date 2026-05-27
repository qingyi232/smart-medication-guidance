"""
依从性预测模型训练
使用 Kaggle Medication Adherence Dataset 真实数据，
训练能识别高风险漏服用户的预测模型。

目标变量: future_non_adherence (0=依从, 1=不依从)
"""

import os
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.ensemble import RandomForestClassifier, GradientBoostingClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import (
    classification_report, confusion_matrix,
    roc_auc_score, accuracy_score
)
import joblib

BASE_DIR = os.path.dirname(__file__)
RAW_PATH = os.path.join(BASE_DIR, 'data', 'medication_adherence.csv')
PROCESSED_PATH = os.path.join(BASE_DIR, 'data', 'processed_adherence.csv')
MODEL_DIR = os.path.join(BASE_DIR, 'models')

# 直接从原始数据集提取的模型特征
FEATURE_COLUMNS = [
    'age', 'gender_encoded', 'dosage', 'comorbidities', 'BMI',
    'heart_rate', 'systolic_bp', 'diastolic_bp', 'blood_glucose',
    'activity_level', 'sleep_hours', 'stress_level',
    'reminder_sent', 'reminder_response_time', 'social_support',
    'hour', 'day_of_week', 'is_weekend', 'is_night',
    'historical_adherence_rate', 'avg_response_time', 'avg_stress', 'avg_sleep',
]

# 动态添加 one-hot 编码列（在数据加载后确定）
CHRONIC_PREFIX = 'chronic_'
MED_PREFIX = 'med_'
LOC_PREFIX = 'loc_'

TARGET_COLUMN = 'future_non_adherence'


def load_data():
    if not os.path.exists(PROCESSED_PATH):
        print("预处理数据不存在，正在处理...")
        from generate_dataset import preprocess
        preprocess()

    df = pd.read_csv(PROCESSED_PATH)
    print(f"加载数据: {len(df)} 条记录")
    return df


EXCLUDE_RAW_COLS = {
    'chronic_condition', 'medication_type', 'location',
}

def get_all_feature_columns(df):
    """获取所有特征列（包括动态 one-hot 列，排除原始分类列）"""
    all_cols = list(FEATURE_COLUMNS)
    for col in df.columns:
        if col in EXCLUDE_RAW_COLS or col in all_cols:
            continue
        if (col.startswith(CHRONIC_PREFIX) or
            col.startswith(MED_PREFIX) or
            col.startswith(LOC_PREFIX)):
            all_cols.append(col)
    return all_cols


def train():
    df = load_data()

    feature_cols = get_all_feature_columns(df)
    available_cols = [c for c in feature_cols if c in df.columns]
    print(f"使用 {len(available_cols)} 个特征: {available_cols}")

    X = df[available_cols].fillna(0)
    y = df[TARGET_COLUMN]

    print(f"\n目标变量分布:")
    print(f"  依从 (0): {(y == 0).sum()} ({(y == 0).mean():.1%})")
    print(f"  不依从 (1): {(y == 1).sum()} ({(y == 1).mean():.1%})")

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # --- 随机森林 ---
    print("\n" + "=" * 50)
    print("训练随机森林模型")
    print("=" * 50)
    rf_model = RandomForestClassifier(
        n_estimators=200,
        max_depth=12,
        min_samples_split=10,
        min_samples_leaf=5,
        random_state=42,
        n_jobs=-1,
    )
    rf_model.fit(X_train_scaled, y_train)
    rf_pred = rf_model.predict(X_test_scaled)
    rf_proba = rf_model.predict_proba(X_test_scaled)[:, 1]

    rf_acc = accuracy_score(y_test, rf_pred)
    rf_auc = roc_auc_score(y_test, rf_proba)
    print(f"准确率: {rf_acc:.4f}")
    print(f"AUC-ROC: {rf_auc:.4f}")
    print(f"\n{classification_report(y_test, rf_pred, target_names=['依从', '不依从'])}")

    rf_cv = cross_val_score(rf_model, X_train_scaled, y_train, cv=5, scoring='roc_auc')
    print(f"5折交叉验证 AUC: {rf_cv.mean():.4f} (±{rf_cv.std():.4f})")

    # --- 梯度提升 ---
    print("\n" + "=" * 50)
    print("训练梯度提升模型")
    print("=" * 50)
    gb_model = GradientBoostingClassifier(
        n_estimators=200,
        max_depth=6,
        learning_rate=0.1,
        min_samples_split=10,
        min_samples_leaf=5,
        random_state=42,
    )
    gb_model.fit(X_train_scaled, y_train)
    gb_pred = gb_model.predict(X_test_scaled)
    gb_proba = gb_model.predict_proba(X_test_scaled)[:, 1]

    gb_acc = accuracy_score(y_test, gb_pred)
    gb_auc = roc_auc_score(y_test, gb_proba)
    print(f"准确率: {gb_acc:.4f}")
    print(f"AUC-ROC: {gb_auc:.4f}")
    print(f"\n{classification_report(y_test, gb_pred, target_names=['依从', '不依从'])}")

    gb_cv = cross_val_score(gb_model, X_train_scaled, y_train, cv=5, scoring='roc_auc')
    print(f"5折交叉验证 AUC: {gb_cv.mean():.4f} (±{gb_cv.std():.4f})")

    # --- 选择最佳模型 ---
    if gb_cv.mean() > rf_cv.mean():
        best_model = gb_model
        best_name = 'GradientBoosting'
    else:
        best_model = rf_model
        best_name = 'RandomForest'
    print(f"\n{'=' * 50}")
    print(f"最佳模型: {best_name}")
    print(f"{'=' * 50}")

    # 特征重要性
    importance = pd.DataFrame({
        'feature': available_cols,
        'importance': best_model.feature_importances_,
    }).sort_values('importance', ascending=False)
    print(f"\nTop 15 特征重要性:")
    print(importance.head(15).to_string(index=False))

    # 保存
    os.makedirs(MODEL_DIR, exist_ok=True)
    joblib.dump(best_model, os.path.join(MODEL_DIR, 'adherence_model.pkl'))
    joblib.dump(scaler, os.path.join(MODEL_DIR, 'scaler.pkl'))
    joblib.dump(available_cols, os.path.join(MODEL_DIR, 'feature_columns.pkl'))

    print(f"\n模型已保存至 {MODEL_DIR}/")
    return best_model, scaler


if __name__ == '__main__':
    train()
