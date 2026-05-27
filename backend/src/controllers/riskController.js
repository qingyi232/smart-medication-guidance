const axios = require('axios');
const RiskAssessment = require('../models/RiskAssessment');
const MedicationLog = require('../models/MedicationLog');
const Medication = require('../models/Medication');
const User = require('../models/User');
const { sendNotification } = require('../config/firebase');

const ML_SERVICE_URL = process.env.ML_SERVICE_URL || 'http://localhost:5000';

// ============================================================
// 特征提取 — 与 ML 模型训练时的特征列完全对齐
// ============================================================
const extractFeatures = async (userId) => {
  const user = await User.findById(userId);
  const now = new Date();
  const days7ago = new Date(now); days7ago.setDate(days7ago.getDate() - 7);
  const days30ago = new Date(now); days30ago.setDate(days30ago.getDate() - 30);

  const [logs7d, logs30d, medications] = await Promise.all([
    MedicationLog.find({ user: userId, scheduledTime: { $gte: days7ago } }),
    MedicationLog.find({ user: userId, scheduledTime: { $gte: days30ago } }),
    Medication.find({ user: userId, isActive: true }),
  ]);

  // 依从率计算
  const calcAdherence = (logs) => {
    const actionable = logs.filter(l => l.status !== 'pending');
    if (actionable.length === 0) return 1;
    return actionable.filter(l => ['taken', 'late'].includes(l.status)).length / actionable.length;
  };

  const adherence7d = calcAdherence(logs7d);
  const adherence30d = calcAdherence(logs30d);

  // 漏服/迟服统计
  const missed7d = logs7d.filter(l => l.status === 'missed').length;
  const late7d = logs7d.filter(l => l.status === 'late').length;

  // 连续漏服
  let consecutiveMissed = 0;
  const sorted = [...logs7d].sort((a, b) => b.scheduledTime - a.scheduledTime);
  for (const log of sorted) {
    if (log.status === 'missed') consecutiveMissed++;
    else break;
  }

  // 平均响应时间
  const responseTimes = logs7d
    .filter(l => l.reminderResponseTime != null)
    .map(l => l.reminderResponseTime);
  const avgResponseTime = responseTimes.length > 0
    ? responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length : 15;

  // 漏服原因统计
  const missedLogs = logs30d.filter(l => l.status === 'missed');
  const totalMissed = Math.max(missedLogs.length, 1);
  const forgotCount = missedLogs.filter(l => l.missedReason === 'forgot').length;
  const busyCount = missedLogs.filter(l => l.missedReason === 'busy').length;
  const sideEffectCount = missedLogs.filter(l => l.missedReason === 'side_effects').length;

  // 副作用报告
  const sideEffectReported = logs7d.some(l => l.sideEffects === true) ? 1 : 0;

  // 性别编码
  const genderMap = { 'male': 1, 'female': 0, 'other': 0 };
  // 教育水平编码
  const eduMap = { 'primary': 0, 'secondary': 1, 'college': 2, 'graduate': 3 };
  // 肝肾功能编码
  const organMap = { 'normal': 0, 'mild_impairment': 1, 'moderate_impairment': 2, 'severe_impairment': 2 };
  // 活动水平编码
  const activityMap = { 'sedentary': 0, 'light': 1, 'moderate': 2, 'active': 3, 'very_active': 4 };

  // 构建与训练模型完全对齐的特征字典
  return {
    age: user.age || 30,
    gender: genderMap[user.gender] || 0,
    education_level: eduMap[user.educationLevel] || 2,
    chronic_disease_count: (user.chronicDiseases || []).length,
    comorbidities: user.comorbidities || 0,
    medication_count: medications.length || 1,
    liver_function: organMap[user.liverFunction] || 0,
    kidney_function: organMap[user.kidneyFunction] || 0,
    adherence_rate_7d: adherence7d,
    adherence_rate_30d: adherence30d,
    missed_doses_7d: missed7d,
    consecutive_missed: consecutiveMissed,
    late_doses_7d: late7d,
    avg_response_time_min: avgResponseTime,
    stress_level: user.stressLevel || 5,
    sleep_hours: user.sleepHours || 7,
    activity_level: activityMap[user.activityLevel] || 2,
    social_support: user.socialSupport || 2,
    bmi: user.bmi || 24,
    forgot_ratio: forgotCount / totalMissed,
    busy_ratio: busyCount / totalMissed,
    side_effect_ratio: sideEffectCount / totalMissed,
    side_effect_reported: sideEffectReported,
  };
};

// ============================================================
// 规则兜底评分（ML 服务不可用时使用）
// ============================================================
const ruleBasedFallback = (features) => {
  let score = 20;
  if (features.adherence_rate_7d < 0.5) score += 25;
  else if (features.adherence_rate_7d < 0.8) score += 15;
  if (features.consecutive_missed >= 3) score += 20;
  else if (features.consecutive_missed >= 1) score += 10;
  if (features.stress_level >= 8) score += 10;
  else if (features.stress_level >= 6) score += 5;
  if (features.avg_response_time_min > 30) score += 15;
  if (features.sleep_hours < 5) score += 5;
  if (features.social_support <= 3) score += 5;
  if (features.side_effect_reported) score += 10;
  if (features.age > 60) score += 5;
  return Math.min(score, 100);
};

// ============================================================
// 执行风险评估（核心接口）
// ============================================================
exports.assessRisk = async (req, res, next) => {
  try {
    const userId = req.user._id;
    const features = await extractFeatures(userId);

    let riskLevel, riskScore, interventions, riskFactors, mlUsed;
    let probabilities = {};

    // 尝试调用 Python ML 服务
    try {
      const mlResponse = await axios.post(
        `${ML_SERVICE_URL}/predict`,
        { features },
        { timeout: 5000 }
      );
      const ml = mlResponse.data;

      riskLevel = ml.risk_level;       // 'low' / 'medium' / 'high'
      riskScore = ml.risk_score;       // 0-100
      probabilities = ml.probabilities;
      riskFactors = ml.risk_factors;
      interventions = ml.interventions;
      mlUsed = true;

      console.log(`[ML] 用户 ${userId} 风险评估: ${riskLevel} (${riskScore}分)`);
    } catch (mlErr) {
      // ML 服务不可用，使用规则兜底
      console.warn(`[ML] 服务不可用，使用规则兜底: ${mlErr.message}`);
      riskScore = ruleBasedFallback(features);
      riskLevel = riskScore >= 55 ? 'high' : riskScore >= 30 ? 'medium' : 'low';
      probabilities = { low: 0, medium: 0, high: 0 };
      probabilities[riskLevel] = riskScore;
      riskFactors = [];
      interventions = [];
      mlUsed = false;

      // 规则兜底的干预策略
      if (features.stress_level >= 7) {
        interventions.push({ type: 'simplified_steps', message: '检测到您压力较大，已为您简化服药步骤：温水+药片放床头。' });
      }
      if (features.consecutive_missed >= 2) {
        interventions.push({ type: 'caregiver_alert', message: '检测到连续漏服，建议通知您的健康管理人员关注。' });
      }
      if (riskLevel !== 'low' && interventions.length === 0) {
        interventions.push({ type: 'motivational_message', message: '坚持用药对控制病情非常重要！' });
      }
    }

    // 保存评估结果到数据库
    const assessment = await RiskAssessment.create({
      user: userId,
      assessmentDate: new Date(),
      riskScore,
      riskLevel,
      features,
      missedProbability: (probabilities.high || riskScore) / 100,
      interventions,
      riskFactors: riskFactors || [],
      mlModelUsed: mlUsed,
    });

    // 高风险用户推送通知
    if (riskLevel === 'high') {
      const user = await User.findById(userId);
      if (user.fcmToken && interventions.length > 0) {
        try {
          await sendNotification(user.fcmToken, {
            title: '用药风险提醒',
            body: interventions[0].message,
          });
        } catch (e) { /* FCM 推送失败不影响主流程 */ }
      }
    }

    res.json({
      success: true,
      data: {
        assessment: {
          _id: assessment._id,
          riskScore,
          riskLevel,
          probabilities,
          riskFactors: riskFactors || [],
          interventions,
          features,
          mlModelUsed: mlUsed,
          assessmentDate: assessment.assessmentDate,
        },
      },
    });
  } catch (error) {
    next(error);
  }
};

// ============================================================
// 获取最新风险评估
// ============================================================
exports.getLatestRisk = async (req, res, next) => {
  try {
    const assessment = await RiskAssessment.findOne({ user: req.user._id })
      .sort({ assessmentDate: -1 });

    res.json({
      success: true,
      data: { assessment },
    });
  } catch (error) {
    next(error);
  }
};

// ============================================================
// 获取风险评估历史
// ============================================================
exports.getRiskHistory = async (req, res, next) => {
  try {
    const { page = 1, limit = 20 } = req.query;
    const assessments = await RiskAssessment.find({ user: req.user._id })
      .sort({ assessmentDate: -1 })
      .skip((page - 1) * limit)
      .limit(parseInt(limit));

    const total = await RiskAssessment.countDocuments({ user: req.user._id });

    res.json({
      success: true,
      data: { assessments, total, page: parseInt(page), limit: parseInt(limit) },
    });
  } catch (error) {
    next(error);
  }
};

// ============================================================
// 每日自动风险评估（CronJob 调用）
// ============================================================
exports.dailyAssessment = async () => {
  try {
    const users = await User.find({ isActive: true });
    let assessed = 0;

    for (const user of users) {
      try {
        const features = await extractFeatures(user._id);
        let riskLevel, riskScore, interventions;

        try {
          const mlResponse = await axios.post(
            `${ML_SERVICE_URL}/predict`,
            { features },
            { timeout: 5000 }
          );
          riskLevel = mlResponse.data.risk_level;
          riskScore = mlResponse.data.risk_score;
          interventions = mlResponse.data.interventions || [];
        } catch {
          riskScore = ruleBasedFallback(features);
          riskLevel = riskScore >= 55 ? 'high' : riskScore >= 30 ? 'medium' : 'low';
          interventions = [];
        }

        await RiskAssessment.create({
          user: user._id,
          assessmentDate: new Date(),
          riskScore,
          riskLevel,
          features,
          missedProbability: riskScore / 100,
          interventions,
        });

        assessed++;
      } catch (e) {
        console.error(`每日评估用户 ${user._id} 失败:`, e.message);
      }
    }

    console.log(`每日风险评估完成: ${assessed}/${users.length} 用户`);
  } catch (error) {
    console.error('每日风险评估失败:', error);
  }
};

// ============================================================
// 更新干预效果（闭环追踪）
// ============================================================
exports.updateOutcome = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { outcome, feedback } = req.body;

    const assessment = await RiskAssessment.findOne({
      _id: id,
      user: req.user._id,
    });

    if (!assessment) {
      return res.status(404).json({ success: false, message: '评估记录不存在' });
    }

    assessment.outcome = outcome;
    if (feedback) assessment.feedback = feedback;
    assessment.outcomeDate = new Date();
    await assessment.save();

    res.json({
      success: true,
      message: '干预效果已更新',
      data: { assessment },
    });
  } catch (error) {
    next(error);
  }
};
