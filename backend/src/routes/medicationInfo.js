const express = require('express');
const medicationInfoController = require('../controllers/medicationInfoController');
const auth = require('../middleware/auth');

const router = express.Router();

router.use(auth);

router.get('/', medicationInfoController.search);
router.get('/:id', medicationInfoController.getById);

module.exports = router;
