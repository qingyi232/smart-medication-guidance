"""
ML 预测服务 API
基于 Flask 提供依从性预测 REST API，供 Node.js 后端调用。
使用 Kaggle Medication Adherence Dataset 训练的模型。
"""

import os
import numpy as np
import joblib
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

MODEL_DIR = os.path.join(os.path.dirname(__file__), 'models')

model = None
scaler = None
feature_columns = None

# Node.js 后端传入的字段到模型特征的映射
GENDER_MAP = {'female': 0, 'male': 1, 'other': 0}
ACTIVITY_MAP = {'sedentary': 1000, 'light': 3000, 'moderate': 5000, 'active': 7000, 'very_active': 9000}
CHRONIC_MAP = {
    'hypertension': 'chronic_hypertension',
    'diabetes': 'chronic_diabetes',
    'heart_disease': 'chronic_cardiovascular',
    'asthma': 'chronic_asthma',
    'copd': 'chronic_asthma',
    'arthritis': 'chronic_cardiovascular',
    'depression': 'chronic_hypertension',
}
LOCATION_MAP = {'home': 'loc_home', 'work': 'loc_work', 'hospital': 'loc_hospital'}


def load_model():
    global model, scaler, feature_columns
    model_path = os.path.join(MODEL_DIR, 'adherence_model.pkl')
    scaler_path = os.path.join(MODEL_DIR, 'scaler.pkl')
    columns_path = os.path.join(MODEL_DIR, 'feature_columns.pkl')

    if not os.path.exists(model_path):
        print("模型文件不存在，正在训练模型...")
        from train_model import train
        train()

    model = joblib.load(model_path)
    scaler = joblib.load(scaler_path)
    feature_columns = joblib.load(columns_path)
    print(f"模型加载成功，特征数: {len(feature_columns)}")


def transform_features(data):
    """
    将来自 Node.js 后端的用户特征转换为模型输入格式。
    后端传入格式:
      { age, gender, chronicDiseases[], recentMissedCount, recentLateCount,
        avgResponseTime, stressLevel, activityLevel, adherenceRate7d,
        adherenceRate30d, consecutiveMissed, timeSinceLastTaken }
    """
    chronic = data.get('chronicDiseases', [])
    now_hour = data.get('hour', 8)

    features = {}
    for col in feature_columns:
        features[col] = 0

    features['age'] = data.get('age', 50)
    features['gender_encoded'] = GENDER_MAP.get(data.get('gender', 'other'), 0)
    features['dosage'] = 100
    features['comorbidities'] = max(len(chronic) - 1, 0)
    features['BMI'] = data.get('bmi', 24.0)
    features['heart_rate'] = data.get('heartRate', 72)

    bp = data.get('bloodPressure', '120/80').split('/')
    features['systolic_bp'] = int(bp[0]) if len(bp) == 2 else 120
    features['diastolic_bp'] = int(bp[1]) if len(bp) == 2 else 80

    features['blood_glucose'] = data.get('bloodGlucose', 100)
    features['activity_level'] = ACTIVITY_MAP.get(data.get('activityLevel', 'moderate'), 5000)
    features['sleep_hours'] = data.get('sleepHours', 7)
    features['stress_level'] = data.get('stressLevel', 5)
    features['reminder_sent'] = 1
    features['reminder_response_time'] = data.get('avgResponseTime', 30)
    features['social_support'] = data.get('socialSupport', 2)

    features['hour'] = now_hour
    features['day_of_week'] = data.get('dayOfWeek', 3)
    features['is_weekend'] = 1 if data.get('dayOfWeek', 3) >= 5 else 0
    features['is_night'] = 1 if (now_hour >= 22 or now_hour <= 6) else 0

    features['historical_adherence_rate'] = data.get('adherenceRate30d', 0.8)
    features['avg_response_time'] = data.get('avgResponseTime', 30)
    features['avg_stress'] = data.get('stressLevel', 5)
    features['avg_sleep'] = data.get('sleepHours', 7)

    # One-hot: 慢性病
    for disease in chronic:
        col = CHRONIC_MAP.get(disease)
        if col and col in features:
            features[col] = 1

    # One-hot: 位置
    loc_col = LOCATION_MAP.get(data.get('location', 'home'))
    if loc_col and loc_col in features:
        features[loc_col] = 1

    return np.array([[features[col] for col in feature_columns]])


@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请求体不能为空'}), 400

        features = transform_features(data)
        features_scaled = scaler.transform(features)

        missed_probability = float(model.predict_proba(features_scaled)[0][1])
        risk_score = int(round(missed_probability * 100))

        risk_level = 'low'
        if risk_score >= 75:
            risk_level = 'critical'
        elif risk_score >= 50:
            risk_level = 'high'
        elif risk_score >= 25:
            risk_level = 'medium'

        return jsonify({
            'missed_probability': round(missed_probability, 4),
            'risk_score': risk_score,
            'risk_level': risk_level,
            'prediction': int(missed_probability > 0.5),
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/batch-predict', methods=['POST'])
def batch_predict():
    """批量预测，用于每日定时风险评估"""
    try:
        data_list = request.get_json()
        if not isinstance(data_list, list):
            return jsonify({'error': '请求体需为数组'}), 400

        results = []
        for data in data_list:
            features = transform_features(data)
            features_scaled = scaler.transform(features)
            prob = float(model.predict_proba(features_scaled)[0][1])
            risk_score = int(round(prob * 100))

            risk_level = 'low'
            if risk_score >= 75: risk_level = 'critical'
            elif risk_score >= 50: risk_level = 'high'
            elif risk_score >= 25: risk_level = 'medium'

            results.append({
                'user_id': data.get('userId'),
                'missed_probability': round(prob, 4),
                'risk_score': risk_score,
                'risk_level': risk_level,
            })

        return jsonify({'results': results})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'ok',
        'model_loaded': model is not None,
        'service': '用药依从性预测服务',
        'features_count': len(feature_columns) if feature_columns else 0,
    })


@app.route('/feature-importance', methods=['GET'])
def feature_importance():
    if model is None:
        return jsonify({'error': '模型未加载'}), 500

    importance = dict(zip(feature_columns, model.feature_importances_.tolist()))
    sorted_importance = dict(sorted(importance.items(), key=lambda x: x[1], reverse=True))

    return jsonify({'feature_importance': sorted_importance})


if __name__ == '__main__':
    load_model()
    app.run(host='0.0.0.0', port=5000, debug=True)
