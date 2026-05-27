const { validationResult } = require('express-validator');
const Medication = require('../models/Medication');

// 药品交互作用数据库（基于公开药物相互作用数据）
const DRUG_INTERACTIONS = {
  // 格式: { drugA: { drugB: { severity, description } } }
  '阿司匹林': {
    '华法林': { severity: 'high', description: '两者合用会显著增加出血风险，需密切监测凝血功能' },
    '布洛芬': { severity: 'medium', description: '同时使用可能增加胃肠道出血风险' },
    '氯吡格雷': { severity: 'medium', description: '合用增加出血风险，但某些情况下医生会联合使用' },
    '肝素': { severity: 'high', description: '合用显著增加出血风险' },
  },
  '华法林': {
    '阿司匹林': { severity: 'high', description: '两者合用会显著增加出血风险，需密切监测凝血功能' },
    '对乙酰氨基酚': { severity: 'medium', description: '大剂量对乙酰氨基酚可能增强华法林的抗凝效果' },
    '维生素K': { severity: 'high', description: '维生素K会降低华法林的抗凝效果' },
  },
  '二甲双胍': {
    '酒精': { severity: 'high', description: '合用可能导致乳酸酸中毒' },
    '造影剂': { severity: 'high', description: '使用碘造影剂前后需停用二甲双胍' },
    '胰岛素': { severity: 'medium', description: '合用可能增加低血糖风险，需调整剂量' },
  },
  '辛伐他汀': {
    '红霉素': { severity: 'high', description: '合用可能增加横纹肌溶解风险' },
    '克拉霉素': { severity: 'high', description: '合用可能增加横纹肌溶解风险' },
    '葡萄柚汁': { severity: 'medium', description: '葡萄柚汁可能增加他汀类药物血药浓度' },
    '环孢素': { severity: 'high', description: '合用显著增加肌病风险' },
  },
  '氨氯地平': {
    '辛伐他汀': { severity: 'medium', description: '氨氯地平可能增加辛伐他汀血药浓度，增加肌病风险' },
    '环孢素': { severity: 'medium', description: '氨氯地平可能增加环孢素血药浓度' },
  },
  '美托洛尔': {
    '维拉帕米': { severity: 'high', description: '合用可能导致严重心动过缓或心脏传导阻滞' },
    '地尔硫卓': { severity: 'high', description: '合用可能导致严重心动过缓' },
    '胰岛素': { severity: 'medium', description: 'β受体阻滞剂可能掩盖低血糖症状' },
  },
  '氯硝西泮': {
    '酒精': { severity: 'high', description: '合用可能导致严重的中枢神经系统抑制' },
    '阿片类药物': { severity: 'high', description: '合用可能导致呼吸抑制甚至死亡' },
  },
  '布洛芬': {
    '阿司匹林': { severity: 'medium', description: '同时使用可能增加胃肠道出血风险' },
    '锂盐': { severity: 'medium', description: '布洛芬可能增加锂盐血药浓度' },
    '甲氨蝶呤': { severity: 'high', description: '布洛芬可能增加甲氨蝶呤毒性' },
    '华法林': { severity: 'medium', description: '合用增加出血风险' },
  },
  '氢氯噻嗪': {
    '锂盐': { severity: 'high', description: '噻嗪类利尿剂可能增加锂盐毒性' },
    '地高辛': { severity: 'medium', description: '利尿剂导致的低钾可能增加地高辛毒性' },
  },
  '卡托普利': {
    '螺内酯': { severity: 'high', description: '合用可能导致高钾血症' },
    '补钾制剂': { severity: 'high', description: 'ACEI类药物与补钾合用可能导致高钾血症' },
  },
};

// 检查药品交互作用
exports.checkInteraction = async (req, res, next) => {
  try {
    const { medicationNames } = req.body;

    if (!medicationNames || !Array.isArray(medicationNames) || medicationNames.length < 2) {
      return res.status(400).json({
        success: false,
        message: '请提供至少两种药品名称',
      });
    }

    const interactions = [];

    for (let i = 0; i < medicationNames.length; i++) {
      for (let j = i + 1; j < medicationNames.length; j++) {
        const drugA = medicationNames[i];
        const drugB = medicationNames[j];

        // 双向查找
        let interaction = null;
        if (DRUG_INTERACTIONS[drugA] && DRUG_INTERACTIONS[drugA][drugB]) {
          interaction = DRUG_INTERACTIONS[drugA][drugB];
        } else if (DRUG_INTERACTIONS[drugB] && DRUG_INTERACTIONS[drugB][drugA]) {
          interaction = DRUG_INTERACTIONS[drugB][drugA];
        }

        // 模糊匹配（药品名称可能包含通用名）
        if (!interaction) {
          for (const [keyA, interMap] of Object.entries(DRUG_INTERACTIONS)) {
            if (drugA.includes(keyA) || keyA.includes(drugA)) {
              for (const [keyB, inter] of Object.entries(interMap)) {
                if (drugB.includes(keyB) || keyB.includes(drugB)) {
                  interaction = inter;
                  break;
                }
              }
            }
            if (interaction) break;
          }
        }

        if (interaction) {
          interactions.push({
            drugA,
            drugB,
            severity: interaction.severity,
            description: interaction.description,
          });
        }
      }
    }

    res.json({
      success: true,
      data: {
        hasInteraction: interactions.length > 0,
        interactions,
        checkedCount: medicationNames.length,
      },
    });
  } catch (error) {
    next(error);
  }
};

// 添加药品
exports.create = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const medication = await Medication.create({
      ...req.body,
      user: req.user._id,
    });

    res.status(201).json({
      success: true,
      message: '药品添加成功',
      data: { medication },
    });
  } catch (error) {
    next(error);
  }
};

// 获取用户所有药品
exports.getAll = async (req, res, next) => {
  try {
    const { active } = req.query;
    const filter = { user: req.user._id };
    if (active !== undefined) {
      filter.isActive = active === 'true';
    }

    const medications = await Medication.find(filter).sort({ createdAt: -1 });

    res.json({
      success: true,
      data: { medications, total: medications.length },
    });
  } catch (error) {
    next(error);
  }
};

// 获取单个药品详情
exports.getById = async (req, res, next) => {
  try {
    const medication = await Medication.findOne({
      _id: req.params.id,
      user: req.user._id,
    });

    if (!medication) {
      return res.status(404).json({ success: false, message: '药品不存在' });
    }

    res.json({ success: true, data: { medication } });
  } catch (error) {
    next(error);
  }
};

// 更新药品信息
exports.update = async (req, res, next) => {
  try {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
      return res.status(400).json({ success: false, errors: errors.array() });
    }

    const medication = await Medication.findOneAndUpdate(
      { _id: req.params.id, user: req.user._id },
      req.body,
      { new: true, runValidators: true }
    );

    if (!medication) {
      return res.status(404).json({ success: false, message: '药品不存在' });
    }

    res.json({
      success: true,
      message: '药品信息更新成功',
      data: { medication },
    });
  } catch (error) {
    next(error);
  }
};

// 删除药品（软删除）
exports.remove = async (req, res, next) => {
  try {
    const medication = await Medication.findOneAndUpdate(
      { _id: req.params.id, user: req.user._id },
      { isActive: false },
      { new: true }
    );

    if (!medication) {
      return res.status(404).json({ success: false, message: '药品不存在' });
    }

    res.json({ success: true, message: '药品已删除' });
  } catch (error) {
    next(error);
  }
};
