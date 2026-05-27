const mongoose = require('mongoose');

/**
 * 药品知识库 — 药品说明书信息
 */
const medicationInfoSchema = new mongoose.Schema({
  name: {
    type: String,
    required: true,
    trim: true,
    index: true,
  },
  genericName: {
    type: String,
    trim: true,
  },
  category: {
    type: String,
    trim: true,
  },
  indication: {
    type: String,
    required: true,
  },
  dosageAndAdministration: {
    type: String,
    required: true,
  },
  adverseReactions: String,
  contraindications: String,
  precautions: String,
  interactions: String,
  storageCondition: String,
  manufacturer: String,
}, {
  timestamps: true,
});

medicationInfoSchema.index({ name: 'text', genericName: 'text', category: 'text' });

module.exports = mongoose.model('MedicationInfo', medicationInfoSchema);
