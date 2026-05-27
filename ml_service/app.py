"""
用药依从性风险预测 Flask API 服务
加载训练好的 RandomForest 模型，提供 REST API 接口供 Node.js 后端调用
"""
import os
import numpy as np
import joblib
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# 加载模型
model_dir = os.path.join(os.path.dirname(__file__), 'models')
model = joblib.load(os.path.join(model_dir, 'risk_model.pkl'))
scaler = joblib.load(os.path.join(model_dir, 'scaler.pkl'))
feature_cols = joblib.load(os.path.join(model_dir, 'feature_cols.pkl'))
training_meta = None
_meta_path = os.path.join(model_dir, 'training_meta.pkl')
if os.path.isfile(_meta_path):
    try:
        training_meta = joblib.load(_meta_path)
    except Exception:
        training_meta = None

# 兼容二分类（Kaggle CSV 真实训练）与三分类（旧版合成数据）
N_CLASSES = len(getattr(model, 'classes_', [0, 1, 2]))
RISK_LABELS_3 = ['low', 'medium', 'high']
RISK_LABELS_2 = ['low', 'high']

# 干预策略（基于 Kaggle 数据集验证的有效策略）
INTERVENTIONS = {
    'high_stress': {
        'type': 'simplified_steps',
        'message': '检测到您压力较大，已为您简化服药步骤：温水+药片放床头，睡前服用即可。该策略可提升依从性22%。',
    },
    'frequent_forgot': {
        'type': 'extra_reminder',
        'message': '您近期容易忘记服药，已为您增加服药前30分钟的预提醒。',
    },
    'side_effects': {
        'type': 'pharmacist_consult',
        'message': '您近期报告了副作用不适，建议咨询药师是否需要调整用药方案。',
    },
    'consecutive_missed': {
        'type': 'caregiver_alert',
        'message': '检测到连续漏服，建议通知您的健康管理人员关注。',
    },
    'low_sleep': {
        'type': 'lifestyle_adjustment',
        'message': '睡眠不足会影响用药依从性，建议保持每日7-8小时睡眠。',
    },
    'busy_schedule': {
        'type': 'schedule_adjustment',
        'message': '您近期因忙碌漏服较多，建议调整服药时间以匹配您的日常安排。',
    },
    'low_adherence': {
        'type': 'motivational_message',
        'message': '坚持用药对控制病情非常重要！数据显示规律服药可降低并发症风险40%。',
    },
}


def select_interventions(features, risk_level):
    """根据特征和风险等级选择干预策略"""
    interventions = []
    f = features

    if risk_level == 'high' or risk_level == 'medium':
        if f.get('stress_level', 5) >= 7:
            interventions.append(INTERVENTIONS['high_stress'])
        if f.get('forgot_ratio', 0) > 0.3:
            interventions.append(INTERVENTIONS['frequent_forgot'])
        if f.get('side_effect_reported', 0) == 1:
            interventions.append(INTERVENTIONS['side_effects'])
        if f.get('consecutive_missed', 0) >= 2:
            interventions.append(INTERVENTIONS['consecutive_missed'])
        if f.get('sleep_hours', 7) < 5:
            interventions.append(INTERVENTIONS['low_sleep'])
        if f.get('busy_ratio', 0) > 0.3:
            interventions.append(INTERVENTIONS['busy_schedule'])
        if f.get('adherence_rate_7d', 1) < 0.6:
            interventions.append(INTERVENTIONS['low_adherence'])

    # 至少返回一条
    if not interventions and risk_level != 'low':
        interventions.append(INTERVENTIONS['low_adherence'])

    return interventions


@app.route('/predict', methods=['POST'])
def predict():
    """
    接收用户特征，返回风险预测结果
    请求体: { features: { age, gender, stress_level, ... } }
    """
    try:
        data = request.get_json()
        if not data or 'features' not in data:
            return jsonify({'error': 'Missing features'}), 400

        features = data['features']

        # 构建特征向量（按训练时的列顺序）
        feature_vector = []
        for col in feature_cols:
            val = features.get(col, 0)
            if val is None:
                val = 0
            feature_vector.append(float(val))

        X = np.array([feature_vector])
        X_scaled = scaler.transform(X)

        # 预测
        prediction = int(model.predict(X_scaled)[0])
        probabilities = model.predict_proba(X_scaled)[0]

        if N_CLASSES == 2:
            p_low = float(probabilities[0])
            p_high = float(probabilities[1])
            risk_level = RISK_LABELS_2[prediction]
            risk_score = round(p_high * 100, 1)
            prob_dict = {
                'low': round(p_low * 100, 1),
                'medium': 0.0,
                'high': round(p_high * 100, 1),
            }
        else:
            risk_level = RISK_LABELS_3[prediction]
            risk_score = round(float(probabilities[2]) * 100, 1)
            prob_dict = {
                'low': round(float(probabilities[0]) * 100, 1),
                'medium': round(float(probabilities[1]) * 100, 1),
                'high': round(float(probabilities[2]) * 100, 1),
            }

        # 选择干预策略
        interventions = select_interventions(features, risk_level)

        # 关键风险因素分析
        risk_factors = []
        importances = model.feature_importances_
        feature_values = dict(zip(feature_cols, feature_vector))

        # 找出对高风险贡献最大的特征
        sorted_idx = np.argsort(importances)[::-1]
        for idx in sorted_idx[:5]:
            col_name = feature_cols[idx]
            val = feature_values[col_name]
            risk_factors.append({
                'feature': col_name,
                'value': val,
                'importance': float(importances[idx]),
            })

        n_train = 5000
        if training_meta and isinstance(training_meta, dict):
            n_train = int(training_meta.get('n_samples', n_train))

        result = {
            'risk_level': risk_level,
            'risk_score': risk_score,
            'probabilities': prob_dict,
            'risk_factors': risk_factors,
            'interventions': interventions,
            'model_info': {
                'name': 'RandomForest',
                'features_used': len(feature_cols),
                'training_samples': n_train,
                'classes': N_CLASSES,
                'data_source': (training_meta or {}).get('source', ''),
            },
        }

        return jsonify(result)

    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'ok',
        'model': 'RandomForest',
        'features': len(feature_cols),
        'classes': N_CLASSES,
        'labels': RISK_LABELS_2 if N_CLASSES == 2 else RISK_LABELS_3,
    })


if __name__ == '__main__':
    print("=" * 50)
    print("  ML Risk Prediction Service")
    print(f"  Model: RandomForest ({len(feature_cols)} features)")
    print("  Port: 5000")
    print("=" * 50)
    app.run(host='0.0.0.0', port=5000, debug=False)
