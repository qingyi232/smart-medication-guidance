const mongoose = require('mongoose');

/**
 * 风险评估记录 — 智能应用层
 * 存储 ML 模型（RandomForest）对用户依从性的预测结果与干预策略。
 */
const riskAssessmentSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  assessmentDate: {
    type: Date,
    required: true,
  },
  // 风险评分 (0-100)
  riskScore: {
    type: Number,
    required: true,
    min: 0,
    max: 100,
  },
  // 风险等级
  riskLevel: {
    type: String,
    enum: ['low', 'medium', 'high', 'critical'],
    required: true,
  },
  // ML 模型输入特征快照（与训练特征对齐的完整字典）
  features: {
    type: mongoose.Schema.Types.Mixed,
    default: {},
  },
  // 模型预测的漏服概率
  missedProbability: {
    type: Number,
    min: 0,
    max: 1,
  },
  // ML 模型输出的风险因素
  riskFactors: [{
    feature: String,
    value: Number,
    importance: Number,
  }],
  // 是否使用了 ML 模型（false 表示规则兜底）
  mlModelUsed: {
    type: Boolean,
    default: false,
  },
  // 触发的干预策略
  interventions: [{
    type: {
      type: String,
    },
    message: String,
    delivered: { type: Boolean, default: false },
    deliveredAt: Date,
  }],
  // 干预后结果追踪（闭环）
  outcome: {
    type: mongoose.Schema.Types.Mixed,
    default: null,
  },
  feedback: String,
  outcomeDate: Date,
}, {
  timestamps: true,
});

riskAssessmentSchema.index({ user: 1, assessmentDate: -1 });
riskAssessmentSchema.index({ riskLevel: 1 });

module.exports = mongoose.model('RiskAssessment', riskAssessmentSchema);
