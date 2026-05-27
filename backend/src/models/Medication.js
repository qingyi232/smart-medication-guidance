const mongoose = require('mongoose');

const medicationSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  name: {
    type: String,
    required: [true, '药品名称不能为空'],
    trim: true,
  },
  genericName: {
    type: String,
    trim: true,
  },
  // 剂型
  dosageForm: {
    type: String,
    enum: ['tablet', 'capsule', 'liquid', 'injection', 'patch', 'inhaler', 'drops', 'powder', 'ointment', 'other'],
    default: 'tablet',
  },
  // 单次剂量
  dosage: {
    type: String,
    required: [true, '剂量不能为空'],
  },
  // 剂量单位
  dosageUnit: {
    type: String,
    enum: ['mg', 'g', 'ml', 'tablet', 'capsule', 'drop', 'puff', 'unit'],
    default: 'mg',
  },
  // 用药频率
  frequency: {
    type: String,
    enum: ['once_daily', 'twice_daily', 'three_times_daily', 'four_times_daily', 'weekly', 'as_needed'],
    required: true,
  },
  // 具体服药时间（24小时制，如 ["08:00", "20:00"]）
  scheduleTimes: [{
    type: String,
    match: /^([01]\d|2[0-3]):([0-5]\d)$/,
  }],
  // 用药时机
  timing: {
    type: String,
    enum: ['before_meal', 'after_meal', 'with_meal', 'bedtime', 'empty_stomach', 'any_time'],
    default: 'after_meal',
  },
  // 疗程开始日期
  startDate: {
    type: Date,
    required: true,
    default: Date.now,
  },
  // 疗程结束日期（可选，空表示长期服用）
  endDate: {
    type: Date,
    default: null,
  },
  // 适应症
  indication: String,
  // 注意事项
  precautions: String,
  // 副作用说明
  sideEffects: String,
  // 存储条件
  storageCondition: String,
  // 药品图片
  imageUrl: String,
  // 是否启用
  isActive: {
    type: Boolean,
    default: true,
  },
  // 提醒设置
  reminderEnabled: {
    type: Boolean,
    default: true,
  },
  // 提前提醒分钟数（0表示准时提醒）
  reminderAdvanceMinutes: {
    type: Number,
    default: 0,
    min: 0,
    max: 60,
  },
  // 提醒方式
  reminderType: {
    type: String,
    enum: ['notification', 'alarm', 'silent'],
    default: 'notification',
  },
  // 重复提醒次数（0表示不重复）
  reminderRepeatCount: {
    type: Number,
    default: 1,
    min: 0,
    max: 5,
  },
  // 重复提醒间隔（分钟）
  reminderRepeatInterval: {
    type: Number,
    default: 5,
    min: 1,
    max: 30,
  },
}, {
  timestamps: true,
});

medicationSchema.index({ user: 1, isActive: 1 });

module.exports = mongoose.model('Medication', medicationSchema);
