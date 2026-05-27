const express = require('express');
const riskController = require('../controllers/riskController');
const auth = require('../middleware/auth');

const router = express.Router();

router.use(auth);

router.post('/assess', riskController.assessRisk);
router.get('/history', riskController.getRiskHistory);
router.get('/latest', riskController.getLatestRisk);
router.put('/:id/outcome', riskController.updateOutcome);

module.exports = router;
