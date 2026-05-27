"""
数据集预处理工具
对 Kaggle Medication Adherence Dataset 进行清洗与特征工程，
生成模型训练所需的标准化特征矩阵。

数据集字段说明:
  patient_id, age, gender, chronic_condition, medication_type, dosage,
  comorbidities, BMI, event_id, timestamp, taken, missed_reason, device_id,
  heart_rate, blood_pressure, blood_glucose, activity_level, sleep_hours,
  stress_level, reminder_sent, reminder_response_time, social_support,
  location, future_non_adherence
"""

import os
import pandas as pd
import numpy as np

BASE_DIR = os.path.dirname(__file__)
RAW_PATH = os.path.join(BASE_DIR, 'data', 'medication_adherence.csv')
PROCESSED_PATH = os.path.join(BASE_DIR, 'data', 'processed_adherence.csv')


def preprocess():
    df = pd.read_csv(RAW_PATH)
    print(f"原始数据: {len(df)} 条记录, {len(df.columns)} 列")
    print(f"字段: {list(df.columns)}")
    print(f"\n目标变量 future_non_adherence 分布:\n{df['future_non_adherence'].value_counts()}")

    # === 特征工程 ===

    # 性别编码: M=1, F=0
    df['gender_encoded'] = (df['gender'] == 'M').astype(int)

    # 慢性病 one-hot 编码
    chronic_dummies = pd.get_dummies(df['chronic_condition'], prefix='chronic')
    df = pd.concat([df, chronic_dummies], axis=1)

    # 药品类型 one-hot 编码
    med_dummies = pd.get_dummies(df['medication_type'], prefix='med')
    df = pd.concat([df, med_dummies], axis=1)

    # 位置 one-hot 编码
    loc_dummies = pd.get_dummies(df['location'], prefix='loc')
    df = pd.concat([df, loc_dummies], axis=1)

    # 血压拆分为收缩压和舒张压
    bp_split = df['blood_pressure'].str.split('/', expand=True)
    df['systolic_bp'] = pd.to_numeric(bp_split[0], errors='coerce')
    df['diastolic_bp'] = pd.to_numeric(bp_split[1], errors='coerce')

    # 时间特征
    df['timestamp'] = pd.to_datetime(df['timestamp'])
    df['hour'] = df['timestamp'].dt.hour
    df['day_of_week'] = df['timestamp'].dt.dayofweek
    df['is_weekend'] = (df['day_of_week'] >= 5).astype(int)
    df['is_night'] = ((df['hour'] >= 22) | (df['hour'] <= 6)).astype(int)

    # 提醒响应时间: 填充缺失值（未发送提醒时填 -1 表示无提醒）
    df['reminder_response_time'] = df['reminder_response_time'].fillna(-1)

    # 填充其他缺失值
    df['BMI'] = df['BMI'].fillna(df['BMI'].median())
    df['heart_rate'] = df['heart_rate'].fillna(df['heart_rate'].median())
    df['blood_glucose'] = df['blood_glucose'].fillna(df['blood_glucose'].median())
    df['sleep_hours'] = df['sleep_hours'].fillna(df['sleep_hours'].median())

    # 按患者聚合历史特征
    patient_history = df.groupby('patient_id').agg(
        total_events=('taken', 'count'),
        total_taken=('taken', 'sum'),
        avg_response_time=('reminder_response_time', lambda x: x[x >= 0].mean() if (x >= 0).any() else 0),
        avg_stress=('stress_level', 'mean'),
        avg_sleep=('sleep_hours', 'mean'),
    ).reset_index()
    patient_history['historical_adherence_rate'] = (
        patient_history['total_taken'] / patient_history['total_events']
    )
    df = df.merge(patient_history[['patient_id', 'historical_adherence_rate', 'avg_response_time',
                                    'avg_stress', 'avg_sleep']], on='patient_id', how='left')

    df.to_csv(PROCESSED_PATH, index=False)
    print(f"\n处理后数据已保存: {PROCESSED_PATH}")
    print(f"处理后共 {len(df)} 条记录, {len(df.columns)} 列")

    return df


if __name__ == '__main__':
    preprocess()
