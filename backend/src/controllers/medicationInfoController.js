const MedicationInfo = require('../models/MedicationInfo');

// 初始化药品知识库数据（首次启动时自动填充）
const SEED_DATA = [
  {
    name: '阿司匹林',
    genericName: 'Aspirin',
    category: '解热镇痛药/抗血小板药',
    indication: '用于普通感冒或流行性感冒引起的发热，也用于缓解轻至中度疼痛如头痛、关节痛、偏头痛、牙痛、肌肉痛、神经痛、痛经。低剂量阿司匹林用于预防心脑血管事件。',
    dosageAndAdministration: '解热镇痛：成人一次0.3-0.6g，一日3次。抗血小板：一次75-100mg，一日1次。饭后服用，用温水送服。',
    adverseReactions: '常见胃肠道反应（恶心、呕吐、上腹不适或疼痛），较少见胃肠道出血、皮疹、支气管痉挛、肝肾功能损害。',
    contraindications: '活动性消化性溃疡、血友病或血小板减少症、对本品过敏者禁用。妊娠晚期禁用。',
    precautions: '有消化道溃疡史者慎用；手术前一周应停用；与抗凝药合用需监测凝血功能；哮喘患者慎用。',
    interactions: '与华法林合用增加出血风险；与布洛芬合用增加胃肠道出血风险；与甲氨蝶呤合用增加其毒性。',
    storageCondition: '密封，在干燥处保存',
    manufacturer: '拜耳医药',
  },
  {
    name: '二甲双胍',
    genericName: 'Metformin',
    category: '降糖药',
    indication: '用于2型糖尿病，特别是肥胖的2型糖尿病患者。可单独使用或与其他降糖药联合使用。',
    dosageAndAdministration: '起始剂量0.5g，每日2-3次，随餐服用。根据血糖监测结果逐渐增加剂量，最大日剂量2g。',
    adverseReactions: '常见胃肠道反应（腹泻、恶心、呕吐、腹胀、食欲减退），偶见乳酸酸中毒（罕见但严重）。长期使用可能导致维生素B12吸收减少。',
    contraindications: '肾功能不全（eGFR<30）、急性或慢性代谢性酸中毒、严重感染、缺氧状态、酗酒者禁用。',
    precautions: '定期监测肾功能；使用碘造影剂前后需停药；手术前后暂停使用；老年患者需评估肾功能后使用。',
    interactions: '与酒精合用增加乳酸酸中毒风险；与碘造影剂合用需暂停；与胰岛素合用需注意低血糖。',
    storageCondition: '密封保存',
    manufacturer: '中美施贵宝',
  },
  {
    name: '氨氯地平',
    genericName: 'Amlodipine',
    category: '钙通道阻滞剂/降压药',
    indication: '用于高血压和慢性稳定性心绞痛及变异型心绞痛的治疗。可单独使用或与其他降压药联合使用。',
    dosageAndAdministration: '成人起始剂量5mg，每日1次。根据血压控制情况可增至10mg，每日1次。老年患者起始剂量2.5mg。',
    adverseReactions: '常见头痛、水肿、疲劳、嗜睡、恶心、腹痛、面部潮红。偶见心悸、头晕。',
    contraindications: '对本品过敏者禁用。严重低血压患者禁用。',
    precautions: '肝功能不全者需减量；心力衰竭患者慎用；与其他降压药合用时注意低血压。',
    interactions: '与辛伐他汀合用可能增加肌病风险（辛伐他汀不超过20mg）；与环孢素合用可能增加环孢素血药浓度。',
    storageCondition: '遮光，密封保存',
    manufacturer: '辉瑞制药',
  },
  {
    name: '美托洛尔',
    genericName: 'Metoprolol',
    category: 'β受体阻滞剂/降压药',
    indication: '用于高血压、心绞痛、心肌梗死后的长期治疗、心力衰竭、心律失常。',
    dosageAndAdministration: '高血压：起始25-50mg，每日2次，可逐渐增至100-200mg/日。心衰：起始6.25mg，每日2次，缓慢递增。',
    adverseReactions: '常见疲劳、头晕、心动过缓、低血压、四肢发冷。偶见支气管痉挛、抑郁、失眠。',
    contraindications: '严重心动过缓、二度及以上房室传导阻滞、心源性休克、失代偿性心力衰竭禁用。',
    precautions: '不可突然停药（需逐渐减量）；糖尿病患者慎用（可能掩盖低血糖症状）；哮喘/COPD患者慎用。',
    interactions: '与维拉帕米/地尔硫卓合用可能导致严重心动过缓；与胰岛素合用可能掩盖低血糖症状。',
    storageCondition: '遮光，密封保存',
    manufacturer: '阿斯利康',
  },
  {
    name: '辛伐他汀',
    genericName: 'Simvastatin',
    category: 'HMG-CoA还原酶抑制剂/调脂药',
    indication: '用于高胆固醇血症、冠心病的预防和治疗。降低总胆固醇、LDL-C、甘油三酯，升高HDL-C。',
    dosageAndAdministration: '起始剂量20mg，每日1次，晚间服用。根据血脂水平调整，最大剂量40mg/日。',
    adverseReactions: '常见头痛、腹痛、便秘、恶心。偶见肌痛、肌无力。罕见横纹肌溶解症（严重）。',
    contraindications: '活动性肝病或不明原因转氨酶持续升高者禁用。妊娠及哺乳期禁用。',
    precautions: '定期监测肝功能和肌酸激酶；出现肌痛/肌无力应立即就医；避免大量饮用葡萄柚汁。',
    interactions: '与红霉素/克拉霉素合用增加横纹肌溶解风险；与氨氯地平合用时剂量不超过20mg；与环孢素合用禁忌。',
    storageCondition: '密封，阴凉处保存',
    manufacturer: '默沙东',
  },
  {
    name: '布洛芬',
    genericName: 'Ibuprofen',
    category: '非甾体抗炎药',
    indication: '用于缓解轻至中度疼痛如头痛、关节痛、偏头痛、牙痛、肌肉痛、神经痛、痛经。也用于普通感冒或流行性感冒引起的发热。',
    dosageAndAdministration: '成人一次0.2-0.4g，每4-6小时一次，一日最大剂量2.4g。饭后服用。',
    adverseReactions: '常见胃肠道反应（恶心、呕吐、胃痛、腹泻）。偶见头晕、皮疹。长期使用可能导致胃溃疡。',
    contraindications: '对本品或其他NSAIDs过敏者禁用。活动性消化性溃疡禁用。严重心力衰竭禁用。',
    precautions: '有消化道溃疡史者慎用；肾功能不全者慎用；老年患者慎用；避免与其他NSAIDs同时使用。',
    interactions: '与阿司匹林合用增加胃肠道出血风险；与华法林合用增加出血风险；与锂盐合用增加锂盐浓度。',
    storageCondition: '密封保存',
    manufacturer: '中美史克',
  },
  {
    name: '奥美拉唑',
    genericName: 'Omeprazole',
    category: '质子泵抑制剂',
    indication: '用于胃溃疡、十二指肠溃疡、反流性食管炎、卓-艾综合征。也用于与抗生素联合根除幽门螺杆菌。',
    dosageAndAdministration: '胃溃疡：20mg，每日1次，疗程4-8周。十二指肠溃疡：20mg，每日1次，疗程2-4周。晨起空腹服用。',
    adverseReactions: '常见头痛、腹泻、恶心、腹痛、便秘、胀气。长期使用可能增加骨折风险和低镁血症。',
    contraindications: '对本品过敏者禁用。',
    precautions: '长期使用需定期评估；排除胃恶性肿瘤后使用；可能影响某些药物吸收。',
    interactions: '可能降低氯吡格雷的抗血小板效果；可能影响酮康唑、伊曲康唑的吸收。',
    storageCondition: '遮光，密封保存',
    manufacturer: '阿斯利康',
  },
  {
    name: '氯吡格雷',
    genericName: 'Clopidogrel',
    category: '抗血小板药',
    indication: '用于预防动脉粥样硬化血栓形成事件。适用于近期心肌梗死、近期缺血性卒中或确诊外周动脉疾病的患者。',
    dosageAndAdministration: '成人75mg，每日1次。急性冠脉综合征可给予负荷剂量300mg，之后75mg/日。',
    adverseReactions: '常见出血（鼻出血、胃肠道出血、皮下出血）、腹泻、腹痛、消化不良。',
    contraindications: '严重肝功能损害、活动性病理性出血（如消化性溃疡或颅内出血）禁用。',
    precautions: '手术前5-7天停药；与阿司匹林合用增加出血风险；出血倾向患者慎用。',
    interactions: '与阿司匹林合用增加出血风险；奥美拉唑可能降低其疗效；与华法林合用需密切监测。',
    storageCondition: '密封保存',
    manufacturer: '赛诺菲',
  },
];

// 初始化种子数据
const seedMedicationInfo = async () => {
  try {
    const count = await MedicationInfo.countDocuments();
    if (count === 0) {
      await MedicationInfo.insertMany(SEED_DATA);
      console.log(`药品知识库初始化完成，已导入 ${SEED_DATA.length} 条药品信息`);
    }
  } catch (error) {
    console.error('药品知识库初始化失败:', error.message);
  }
};

// 搜索药品知识
exports.search = async (req, res, next) => {
  try {
    const { keyword } = req.query;
    let filter = {};
    if (keyword) {
      filter = {
        $or: [
          { name: { $regex: keyword, $options: 'i' } },
          { genericName: { $regex: keyword, $options: 'i' } },
          { category: { $regex: keyword, $options: 'i' } },
        ],
      };
    }

    const infos = await MedicationInfo.find(filter)
        .select('name genericName category indication')
        .sort({ name: 1 })
        .limit(50);

    res.json({
      success: true,
      data: { infos, total: infos.length },
    });
  } catch (error) {
    next(error);
  }
};

// 获取药品详情
exports.getById = async (req, res, next) => {
  try {
    const info = await MedicationInfo.findById(req.params.id);
    if (!info) {
      return res.status(404).json({ success: false, message: '药品信息不存在' });
    }
    res.json({ success: true, data: { info } });
  } catch (error) {
    next(error);
  }
};

exports.seedMedicationInfo = seedMedicationInfo;
