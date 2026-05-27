const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
  username: {
    type: String,
    required: [true, '用户名不能为空'],
    unique: true,
    trim: true,
    minlength: [3, '用户名至少3个字符'],
    maxlength: [20, '用户名最多20个字符'],
  },
  password: {
    type: String,
    required: [true, '密码不能为空'],
    minlength: [6, '密码至少6个字符'],
    select: false,
  },
  realName: {
    type: String,
    trim: true,
  },
  phone: {
    type: String,
    trim: true,
  },
  // 紧急联系人
  emergencyContact: {
    name: { type: String, trim: true },
    phone: { type: String, trim: true },
    relationship: {
      type: String,
      enum: ['spouse', 'parent', 'child', 'sibling', 'friend', 'doctor', 'other'],
    },
  },
  // 健康档案字段 — 与 Kaggle Medication Adherence Dataset 对齐
  age: {
    type: Number,
    min: 0,
    max: 150,
  },
  gender: {
    type: String,
    enum: ['male', 'female', 'other'],
  },
  chronicDiseases: [{
    type: String,
    enum: [
      'hypertension',   // 高血压
      'diabetes',       // 糖尿病
      'heart_disease',    // 心脏病
      'cardiovascular',   // 心血管疾病
      'copd',             // 慢阻肺
      'asthma',           // 哮喘
      'arthritis',      // 关节炎
      'depression',     // 抑郁症
      'other',          // 其他
    ],
  }],
  chronicDiseaseOther: String,
  // 肝肾功能状态
  liverFunction: {
    type: String,
    enum: ['normal', 'mild_impairment', 'moderate_impairment', 'severe_impairment'],
    default: 'normal',
  },
  kidneyFunction: {
    type: String,
    enum: ['normal', 'mild_impairment', 'moderate_impairment', 'severe_impairment'],
    default: 'normal',
  },
  // FCM 推送 token
  fcmToken: {
    type: String,
    default: null,
  },
  // 用户自评压力等级 (1-10)
  stressLevel: {
    type: Number,
    min: 1,
    max: 10,
    default: 5,
  },
  // 活动状态
  activityLevel: {
    type: String,
    enum: ['sedentary', 'light', 'moderate', 'active', 'very_active'],
    default: 'moderate',
  },
  // Kaggle 数据集对齐字段
  bmi: {
    type: Number,
    min: 10,
    max: 60,
  },
  comorbidities: {
    type: Number,
    default: 0,
    min: 0,
  },
  socialSupport: {
    type: Number,
    min: 0,
    max: 10,
    default: 2,
  },
  educationLevel: {
    type: String,
    enum: ['primary', 'secondary', 'college', 'graduate'],
    default: 'college',
  },
  sleepHours: {
    type: Number,
    min: 0,
    max: 24,
    default: 7,
  },
  avatar: String,
  isActive: {
    type: Boolean,
    default: true,
  },
}, {
  timestamps: true,
});

userSchema.pre('save', async function () {
  if (!this.isModified('password')) return;
  this.password = await bcrypt.hash(this.password, 12);
});

userSchema.methods.comparePassword = async function (candidatePassword) {
  return bcrypt.compare(candidatePassword, this.password);
};

userSchema.methods.toJSON = function () {
  const obj = this.toObject();
  delete obj.password;
  return obj;
};

module.exports = mongoose.model('User', userSchema);
