require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');

const connectDB = require('./config/database');
const { initFirebase } = require('./config/firebase');
const { startAllJobs } = require('./services/cronScheduler');
const errorHandler = require('./middleware/errorHandler');
const rateLimit = require('express-rate-limit');

const authRoutes = require('./routes/auth');
const medicationRoutes = require('./routes/medication');
const medicationLogRoutes = require('./routes/medicationLog');
const riskRoutes = require('./routes/risk');
const medicationInfoRoutes = require('./routes/medicationInfo');
const { seedMedicationInfo } = require('./controllers/medicationInfoController');

const app = express();

// 中间件
app.use(helmet());
app.use(cors());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 全局速率限制：每个 IP 每15分钟最多200次请求
app.use('/api', rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200,
  standardHeaders: true,
  legacyHeaders: false,
}));

// API 路由
app.use('/api/auth', authRoutes);
app.use('/api/medications', medicationRoutes);
app.use('/api/logs', medicationLogRoutes);
app.use('/api/risk', riskRoutes);
app.use('/api/medication-info', medicationInfoRoutes);

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({
    success: true,
    message: '智能终端用药指导系统服务运行正常',
    timestamp: new Date().toISOString(),
  });
});

// 错误处理
app.use(errorHandler);

const PORT = process.env.PORT || 3000;

const start = async () => {
  await connectDB();
  initFirebase();
  startAllJobs();
  await seedMedicationInfo();

  app.listen(PORT, () => {
    console.log(`服务器启动成功，端口: ${PORT}`);
    console.log(`健康检查: http://localhost:${PORT}/api/health`);
  });
};

start().catch(err => {
  console.error('服务器启动失败:', err);
  process.exit(1);
});

module.exports = app;
