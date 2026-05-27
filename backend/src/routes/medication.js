const express = require('express');
const { body } = require('express-validator');
const medicationController = require('../controllers/medicationController');
const auth = require('../middleware/auth');

const router = express.Router();

router.use(auth);

router.post('/interaction-check', medicationController.checkInteraction);

router.post('/',
  body('name').trim().notEmpty().withMessage('药品名称不能为空'),
  body('dosage').trim().notEmpty().withMessage('剂量不能为空'),
  body('frequency').isIn([
    'once_daily', 'twice_daily', 'three_times_daily',
    'four_times_daily', 'weekly', 'as_needed',
  ]).withMessage('用药频率无效'),
  body('scheduleTimes').isArray({ min: 1 }).withMessage('至少设置一个服药时间'),
  body('scheduleTimes.*').matches(/^([01]\d|2[0-3]):([0-5]\d)$/).withMessage('时间格式无效，需HH:MM'),
  medicationController.create,
);

router.get('/', medicationController.getAll);
router.get('/:id', medicationController.getById);
router.put('/:id', medicationController.update);
router.delete('/:id', medicationController.remove);

module.exports = router;
