const mongoose = require('mongoose');

/**
 * 用药行为记录 — 结构化数据采集层
 * 字段设计对齐 Kaggle Medication Adherence Dataset，
 * 为 ML 依从性预测模型提供高质量训练数据。
 */
const medicationLogSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  medication: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Medication',
    required: true,
  },
  // 计划服药时间
  scheduledTime: {
    type: Date,
    required: true,
  },
  // 实际服药时间（为 null 表示漏服）
  actualTime: {
    type: Date,
    default: null,
  },
  // 服药状态
  status: {
    type: String,
    enum: ['taken', 'missed', 'skipped', 'late', 'pending'],
    default: 'pending',
  },
  // 提醒响应时长（秒）— 从提醒推送到用户确认的耗时
  reminderResponseTime: {
    type: Number,
    default: null,
  },
  // 漏服原因
  missedReason: {
    type: String,
    enum: ['forgot', 'too_busy', 'side_effects', 'felt_better', 'ran_out', 'intentional', 'other', null],
    default: null,
  },
  missedReasonDetail: String,
  // 用户当次自评压力值 (1-10)
  stressLevel: {
    type: Number,
    min: 1,
    max: 10,
    default: null,
  },
  // 当次活动状态
  activityStatus: {
    type: String,
    enum: ['resting', 'light_activity', 'moderate_activity', 'heavy_activity', 'sleeping'],
    default: null,
  },
  // 用户自评身体感觉 (1-5, 1=很差, 5=很好)
  wellBeingScore: {
    type: Number,
    min: 1,
    max: 5,
    default: null,
  },
  // 是否感到副作用
  experiencedSideEffects: {
    type: Boolean,
    default: false,
  },
  sideEffectDescription: String,
  // 服药前是否收到提醒
  reminderReceived: {
    type: Boolean,
    default: false,
  },
  // 通知推送时间
  notificationSentAt: {
    type: Date,
    default: null,
  },
  // Kaggle 数据集对齐 — 健康指标采集
  heartRate: {
    type: Number,
    default: null,
  },
  bloodPressure: {
    type: String,
    default: null,
  },
  bloodGlucose: {
    type: Number,
    default: null,
  },
  sleepHours: {
    type: Number,
    default: null,
  },
  // 用药位置
  location: {
    type: String,
    enum: ['home', 'work', 'hospital', 'outdoor', 'other', null],
    default: null,
  },
  // 备注
  notes: String,
}, {
  timestamps: true,
});

medicationLogSchema.index({ user: 1, scheduledTime: -1 });
medicationLogSchema.index({ user: 1, medication: 1, scheduledTime: -1 });
medicationLogSchema.index({ status: 1, scheduledTime: -1 });

module.exports = mongoose.model('MedicationLog', medicationLogSchema);
