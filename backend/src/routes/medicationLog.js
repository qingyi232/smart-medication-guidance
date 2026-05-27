const express = require('express');
const { body } = require('express-validator');
const logController = require('../controllers/medicationLogController');
const auth = require('../middleware/auth');

const router = express.Router();

router.use(auth);

router.post('/',
  body('medicationId').notEmpty().withMessage('药品ID不能为空'),
  body('status').isIn(['taken', 'missed', 'skipped', 'late']).withMessage('状态无效'),
  body('stressLevel').optional().isInt({ min: 1, max: 10 }).withMessage('压力值需1-10'),
  body('wellBeingScore').optional().isInt({ min: 1, max: 5 }).withMessage('身体感觉需1-5'),
  logController.recordLog,
);

router.get('/', logController.getLogs);
router.get('/today', logController.getTodaySchedule);
router.get('/adherence', logController.getAdherenceStats);
router.get('/export', logController.exportLogs);

module.exports = router;
