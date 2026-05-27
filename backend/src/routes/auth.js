const express = require('express');
const { body } = require('express-validator');
const authController = require('../controllers/authController');
const auth = require('../middleware/auth');

const router = express.Router();

router.post('/register',
  body('username').trim().isLength({ min: 3, max: 20 }).withMessage('用户名需3-20个字符'),
  body('password').isLength({ min: 6 }).withMessage('密码至少6个字符'),
  body('age').optional().isInt({ min: 0, max: 150 }).withMessage('年龄无效'),
  body('gender').optional().isIn(['male', 'female', 'other']).withMessage('性别无效'),
  authController.register,
);

router.post('/login',
  body('username').trim().notEmpty().withMessage('用户名不能为空'),
  body('password').notEmpty().withMessage('密码不能为空'),
  authController.login,
);

router.get('/profile', auth, authController.getProfile);
router.put('/profile', auth, authController.updateProfile);
router.put('/fcm-token', auth, authController.updateFcmToken);
router.put('/change-password', auth,
  body('oldPassword').notEmpty().withMessage('旧密码不能为空'),
  body('newPassword').isLength({ min: 6 }).withMessage('新密码至少6个字符'),
  authController.changePassword,
);

module.exports = router;
