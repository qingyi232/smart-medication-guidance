const { validationResult } = require('express-validator');
const MedicationLog = require('../models/MedicationLog');
const Medication = require('../models/Medication');

// 记录服药行为
exports.recordLog = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const { medicationId, status, missedReason, missedReasonDetail,
      stressLevel, activityStatus, wellBeingScore,
      experiencedSideEffects, sideEffectDescription, notes,
      heartRate, bloodPressure, bloodGlucose, sleepHours, location } = req.body;

    const medication = await Medication.findOne({
      _id: medicationId,
      user: req.user._id,
    });
    if (!medication) {
      return res.status(404).json({ success: false, message: '药品不存在' });
    }

    const now = new Date();
    const logData = {
      user: req.user._id,
      medication: medicationId,
      scheduledTime: req.body.scheduledTime || now,
      status,
      stressLevel,
      activityStatus,
      wellBeingScore,
      experiencedSideEffects,
      sideEffectDescription,
      notes,
      heartRate,
      bloodPressure,
      bloodGlucose,
      sleepHours,
      location,
    };

    if (status === 'taken' || status === 'late') {
      logData.actualTime = now;
      if (logData.scheduledTime) {
        logData.reminderResponseTime = Math.floor(
          (now - new Date(logData.scheduledTime)) / 1000
        );
      }
    }

    if (status === 'missed' || status === 'skipped') {
      logData.missedReason = missedReason;
      logData.missedReasonDetail = missedReasonDetail;
    }

    const log = await MedicationLog.create(logData);

    res.status(201).json({
      success: true,
      message: '服药记录已保存',
      data: { log },
    });
  } catch (error) {
    next(error);
  }
};

// 获取用户服药记录
exports.getLogs = async (req, res, next) => {
  try {
    const { medicationId, status, startDate, endDate, page = 1, limit = 20 } = req.query;

    const filter = { user: req.user._id };
    if (medicationId) filter.medication = medicationId;
    if (status) filter.status = status;
    if (startDate || endDate) {
      filter.scheduledTime = {};
      if (startDate) filter.scheduledTime.$gte = new Date(startDate);
      if (endDate) filter.scheduledTime.$lte = new Date(endDate);
    }

    const skip = (parseInt(page) - 1) * parseInt(limit);
    const [logs, total] = await Promise.all([
      MedicationLog.find(filter)
        .populate('medication', 'name dosage dosageUnit frequency')
        .sort({ scheduledTime: -1 })
        .skip(skip)
        .limit(parseInt(limit)),
      MedicationLog.countDocuments(filter),
    ]);

    res.json({
      success: true,
      data: {
        logs,
        total,
        page: parseInt(page),
        totalPages: Math.ceil(total / parseInt(limit)),
      },
    });
  } catch (error) {
    next(error);
  }
};

// 获取今日服药计划与状态
exports.getTodaySchedule = async (req, res, next) => {
  try {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    const medications = await Medication.find({
      user: req.user._id,
      isActive: true,
      startDate: { $lte: tomorrow },
      $or: [
        { endDate: null },
        { endDate: { $gte: today } },
      ],
    });

    const todayLogs = await MedicationLog.find({
      user: req.user._id,
      scheduledTime: { $gte: today, $lt: tomorrow },
    });

    const logMap = new Map();
    todayLogs.forEach(log => {
      const key = `${log.medication}_${log.scheduledTime.toISOString()}`;
      logMap.set(key, log);
    });

    const schedule = [];
    medications.forEach(med => {
      (med.scheduleTimes || []).forEach(time => {
        const [h, m] = time.split(':');
        const scheduledTime = new Date(today);
        scheduledTime.setHours(parseInt(h), parseInt(m), 0, 0);

        const key = `${med._id}_${scheduledTime.toISOString()}`;
        const existingLog = logMap.get(key);

        schedule.push({
          medication: {
            _id: med._id,
            name: med.name,
            dosage: med.dosage,
            dosageUnit: med.dosageUnit,
            dosageForm: med.dosageForm,
            timing: med.timing,
          },
          scheduledTime,
          time,
          status: existingLog ? existingLog.status : 'pending',
          logId: existingLog ? existingLog._id : null,
        });
      });
    });

    schedule.sort((a, b) => a.scheduledTime - b.scheduledTime);

    res.json({
      success: true,
      data: { schedule, date: today },
    });
  } catch (error) {
    next(error);
  }
};

// 获取依从性统计
exports.getAdherenceStats = async (req, res, next) => {
  try {
    const { days = 7 } = req.query;
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - parseInt(days));
    startDate.setHours(0, 0, 0, 0);

    const logs = await MedicationLog.find({
      user: req.user._id,
      scheduledTime: { $gte: startDate },
    });

    const total = logs.length;
    const taken = logs.filter(l => l.status === 'taken').length;
    const late = logs.filter(l => l.status === 'late').length;
    const missed = logs.filter(l => l.status === 'missed').length;
    const skipped = logs.filter(l => l.status === 'skipped').length;
    const pending = logs.filter(l => l.status === 'pending').length;

    const adherenceRate = total > 0 ? ((taken + late) / (total - pending)) * 100 : 0;

    const avgResponseTime = logs
      .filter(l => l.reminderResponseTime != null)
      .reduce((acc, l, _, arr) => acc + l.reminderResponseTime / arr.length, 0);

    // 按日统计
    const dailyStats = {};
    logs.forEach(log => {
      const dateKey = log.scheduledTime.toISOString().split('T')[0];
      if (!dailyStats[dateKey]) {
        dailyStats[dateKey] = { total: 0, taken: 0, late: 0, missed: 0, skipped: 0, pending: 0 };
      }
      dailyStats[dateKey].total++;
      dailyStats[dateKey][log.status]++;
    });

    res.json({
      success: true,
      data: {
        summary: {
          total, taken, late, missed, skipped, pending,
          adherenceRate: Math.round(adherenceRate * 100) / 100,
          avgResponseTime: Math.round(avgResponseTime),
        },
        dailyStats,
        period: { start: startDate, end: new Date(), days: parseInt(days) },
      },
    });
  } catch (error) {
    next(error);
  }
};

// 导出用药报告（JSON格式，包含完整统计）
exports.exportLogs = async (req, res, next) => {
  try {
    const { days = 30 } = req.query;
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - parseInt(days));
    startDate.setHours(0, 0, 0, 0);

    const logs = await MedicationLog.find({
      user: req.user._id,
      scheduledTime: { $gte: startDate },
    }).populate('medication', 'name dosage dosageUnit frequency timing');

    const total = logs.length;
    const taken = logs.filter(l => l.status === 'taken').length;
    const late = logs.filter(l => l.status === 'late').length;
    const missed = logs.filter(l => l.status === 'missed').length;
    const skipped = logs.filter(l => l.status === 'skipped').length;
    const pending = logs.filter(l => l.status === 'pending').length;
    const actionable = total - pending;
    const adherenceRate = actionable > 0 ? ((taken + late) / actionable) * 100 : 0;

    // 按药品统计
    const medStats = {};
    logs.forEach(log => {
      const medName = log.medication ? log.medication.name : '未知药品';
      if (!medStats[medName]) {
        medStats[medName] = { total: 0, taken: 0, late: 0, missed: 0, skipped: 0 };
      }
      medStats[medName].total++;
      if (log.status !== 'pending') medStats[medName][log.status]++;
    });

    // 按日统计
    const dailyExportStats = {};
    logs.forEach(log => {
      const dateKey = log.scheduledTime.toISOString().split('T')[0];
      if (!dailyExportStats[dateKey]) {
        dailyExportStats[dateKey] = { total: 0, taken: 0, late: 0, missed: 0, skipped: 0 };
      }
      dailyExportStats[dateKey].total++;
      if (log.status !== 'pending') dailyExportStats[dateKey][log.status]++;
    });

    // 漏服原因统计
    const missedReasons = {};
    logs.filter(l => l.missedReason).forEach(l => {
      missedReasons[l.missedReason] = (missedReasons[l.missedReason] || 0) + 1;
    });

    // 详细记录
    const records = logs.map(l => ({
      drugName: l.medication ? l.medication.name : '未知',
      dosage: l.medication ? `${l.medication.dosage}${l.medication.dosageUnit}` : '',
      scheduledTime: l.scheduledTime.toISOString(),
      actualTime: l.actualTime ? l.actualTime.toISOString() : '',
      status: l.status,
      missedReason: l.missedReason || '',
      stressLevel: l.stressLevel || null,
      heartRate: l.heartRate || null,
      bloodPressure: l.bloodPressure || '',
      bloodGlucose: l.bloodGlucose || null,
    }));

    const User = require('../models/User');
    const user = await User.findById(req.user._id);

    res.json({
      success: true,
      data: {
        report: {
          title: '用药报告',
          generatedAt: new Date().toISOString(),
          period: { start: startDate.toISOString(), end: new Date().toISOString(), days: parseInt(days) },
          userName: user.realName || user.username,
          summary: {
            total, taken, late, missed, skipped, pending,
            adherenceRate: Math.round(adherenceRate * 100) / 100,
          },
          medicationStats: medStats,
          dailyStats: dailyExportStats,
          missedReasons,
          records,
        },
      },
    });
  } catch (error) {
    next(error);
  }
};
