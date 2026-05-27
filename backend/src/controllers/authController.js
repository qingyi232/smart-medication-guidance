const jwt = require('jsonwebtoken');
const { validationResult } = require('express-validator');
const User = require('../models/User');

const generateToken = (userId) => {
  return jwt.sign({ id: userId }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN,
  });
};

// 用户注册
exports.register = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { username, password, realName, phone, age, gender, chronicDiseases } = req.body;

    const existingUser = await User.findOne({ username });
    if (existingUser) {
      return res.status(400).json({ success: false, message: '用户名已存在' });
    }

    const user = await User.create({
      username,
      password,
      realName,
      phone,
      age,
      gender,
      chronicDiseases,
    });

    const token = generateToken(user._id);

    res.status(201).json({
      success: true,
      message: '注册成功',
      data: { token, user },
    });
  } catch (error) {
    next(error);
  }
};

// 用户登录
exports.login = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { username, password } = req.body;

    const user = await User.findOne({ username }).select('+password');
    if (!user) {
      return res.status(401).json({ success: false, message: '用户名或密码错误' });
    }

    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: '用户名或密码错误' });
    }

    const token = generateToken(user._id);

    res.json({
      success: true,
      message: '登录成功',
      data: { token, user },
    });
  } catch (error) {
    next(error);
  }
};

// 获取当前用户信息
exports.getProfile = async (req, res) => {
  res.json({
    success: true,
    data: { user: req.user },
  });
};

// 更新用户健康档案
exports.updateProfile = async (req, res, next) => {
  try {
    const allowedFields = [
      'realName', 'phone', 'age', 'gender', 'chronicDiseases',
      'chronicDiseaseOther', 'liverFunction', 'kidneyFunction',
      'stressLevel', 'activityLevel', 'avatar',
      'bmi', 'comorbidities', 'socialSupport', 'educationLevel', 'sleepHours',
      'emergencyContact',
    ];

    const updates = {};
    allowedFields.forEach(field => {
      if (req.body[field] !== undefined) {
        updates[field] = req.body[field];
      }
    });

    const user = await User.findByIdAndUpdate(req.user._id, updates, {
      new: true,
      runValidators: true,
    });

    res.json({
      success: true,
      message: '个人信息更新成功',
      data: { user },
    });
  } catch (error) {
    next(error);
  }
};

// 更新 FCM Token
exports.updateFcmToken = async (req, res, next) => {
  try {
    const { fcmToken } = req.body;
    await User.findByIdAndUpdate(req.user._id, { fcmToken });
    res.json({ success: true, message: 'FCM Token 已更新' });
  } catch (error) {
    next(error);
  }
};

// 修改密码
exports.changePassword = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { oldPassword, newPassword } = req.body;

    const user = await User.findById(req.user._id).select('+password');
    if (!user) {
      return res.status(404).json({ success: false, message: '用户不存在' });
    }

    const isMatch = await user.comparePassword(oldPassword);
    if (!isMatch) {
      return res.status(400).json({ success: false, message: '旧密码错误' });
    }

    user.password = newPassword;
    await user.save();

    res.json({ success: true, message: '密码修改成功' });
  } catch (error) {
    next(error);
  }
};
