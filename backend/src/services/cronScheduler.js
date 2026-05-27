const cron = require('node-cron');
const axios = require('axios');
const Medication = require('../models/Medication');
const MedicationLog = require('../models/MedicationLog');
const User = require('../models/User');
const RiskAssessment = require('../models/RiskAssessment');
const { sendNotification } = require('../config/firebase');
const { extractFeatures, ruleBasedRiskScore } = require('../controllers/riskController');

/**
 * CronJob 定时调度服务
 * 基于服务器端集中式调度，而非本地闹钟，确保高可靠性。
 */

// 每分钟检查是否有需要推送的服药提醒
const startMedicationReminder = () => {
  cron.schedule('* * * * *', async () => {
    try {
      const now = new Date();
      const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;

      // 查找所有启用中的药品
      const activeMeds = await Medication.find({
        isActive: true,
        reminderEnabled: { $ne: false },
        startDate: { $lte: now },
        $or: [{ endDate: null }, { endDate: { $gte: now } }],
      });

      for (const med of activeMeds) {
        const advanceMinutes = med.reminderAdvanceMinutes || 0;

        // 检查是否有匹配的服药时间（考虑提前提醒）
        let matchedTime = null;
        for (const schedTime of med.scheduleTimes) {
          const [sh, sm] = schedTime.split(':').map(Number);
          const schedDate = new Date(now);
          schedDate.setHours(sh, sm, 0, 0);
          const reminderDate = new Date(schedDate.getTime() - advanceMinutes * 60000);
          const reminderTime = `${String(reminderDate.getHours()).padStart(2, '0')}:${String(reminderDate.getMinutes()).padStart(2, '0')}`;
          if (reminderTime === currentTime) {
            matchedTime = schedTime;
            break;
          }
        }

        if (!matchedTime) continue;

        const user = await User.findById(med.user);
        if (!user || !user.isActive) continue;

        const todayStart = new Date(now);
        todayStart.setHours(0, 0, 0, 0);
        const scheduledTime = new Date(todayStart);
        const [h, m] = matchedTime.split(':');
        scheduledTime.setHours(parseInt(h), parseInt(m), 0, 0);

        const existingLog = await MedicationLog.findOne({
          user: user._id,
          medication: med._id,
          scheduledTime,
        });

        if (!existingLog) {
          await MedicationLog.create({
            user: user._id,
            medication: med._id,
            scheduledTime,
            status: 'pending',
            reminderReceived: true,
            notificationSentAt: now,
          });
        }

        if (user.fcmToken) {
          const timingText = {
            before_meal: '饭前', after_meal: '饭后', with_meal: '随餐',
            bedtime: '睡前', empty_stomach: '空腹', any_time: '',
          };
          const timing = timingText[med.timing] || '';

          await sendNotification(
            user.fcmToken,
            '服药提醒',
            `${timing}请服用 ${med.name} ${med.dosage}${med.dosageUnit}`,
            {
              type: 'medication_reminder',
              medicationId: med._id.toString(),
              scheduledTime: scheduledTime.toISOString(),
            }
          );
        }
      }
    } catch (error) {
      console.error('服药提醒 CronJob 执行失败:', error.message);
    }
  });

  console.log('服药提醒 CronJob 已启动（每分钟执行）');
};

// 每天标记过期的 pending 记录为 missed
const startMissedDetection = () => {
  cron.schedule('0 * * * *', async () => {
    try {
      const twoHoursAgo = new Date();
      twoHoursAgo.setHours(twoHoursAgo.getHours() - 2);

      const result = await MedicationLog.updateMany(
        {
          status: 'pending',
          scheduledTime: { $lt: twoHoursAgo },
        },
        {
          $set: { status: 'missed' },
        }
      );

      if (result.modifiedCount > 0) {
        console.log(`已将 ${result.modifiedCount} 条过期记录标记为漏服`);
      }
    } catch (error) {
      console.error('漏服检测 CronJob 执行失败:', error.message);
    }
  });

  console.log('漏服检测 CronJob 已启动（每小时执行）');
};

// 每天凌晨 2 点执行风险评估
const startDailyRiskAssessment = () => {
  cron.schedule('0 2 * * *', async () => {
    try {
      const activeUsers = await User.find({ isActive: true });
      console.log(`开始每日风险评估，共 ${activeUsers.length} 位活跃用户`);

      for (const user of activeUsers) {
        const hasLogs = await MedicationLog.exists({ user: user._id });
        if (!hasLogs) continue;

        try {
          const features = await extractFeatures(user._id);

          let riskScore;
          try {
            const mlResponse = await axios.post(
              `${process.env.ML_SERVICE_URL}/predict`,
              features,
              { timeout: 5000 }
            );
            riskScore = mlResponse.data.risk_score;
          } catch {
            riskScore = ruleBasedRiskScore(features);
          }

          let riskLevel;
          if (riskScore >= 75) riskLevel = 'critical';
          else if (riskScore >= 50) riskLevel = 'high';
          else if (riskScore >= 25) riskLevel = 'medium';
          else riskLevel = 'low';

          await RiskAssessment.create({
            user: user._id,
            assessmentDate: new Date(),
            riskScore,
            riskLevel,
            features,
            missedProbability: riskScore / 100,
          });

          if ((riskLevel === 'high' || riskLevel === 'critical') && user.fcmToken) {
            await sendNotification(
              user.fcmToken,
              '健康关注提醒',
              '系统检测到您近期用药依从性偏低，请注意按时服药。',
              { type: 'daily_risk_alert', riskLevel }
            );
          }
        } catch (err) {
          console.error(`用户 ${user._id} 风险评估失败:`, err.message);
        }
      }

      console.log('每日风险评估完成');
    } catch (error) {
      console.error('每日风险评估 CronJob 执行失败:', error.message);
    }
  });

  console.log('每日风险评估 CronJob 已启动（每天凌晨 2 点执行）');
};

const startAllJobs = () => {
  startMedicationReminder();
  startMissedDetection();
  startDailyRiskAssessment();
  console.log('所有 CronJob 调度任务已启动');
};

module.exports = { startAllJobs };
