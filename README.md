# 基于智能终端用药指导系统的设计与实现

## 项目概述

本系统是一个融合规则驱动提醒、智能终端交互、数据闭环反馈与AI扩展能力的用药指导系统。通过构建 Android 移动终端与云服务器协同架构，实现个体化用药提醒与智能干预。

## 技术架构

| 层级 | 技术栈 |
|------|--------|
| 移动端 | Android (Java) + Retrofit + Firebase Cloud Messaging |
| 后端服务 | Node.js + Express + MongoDB |
| 定时调度 | node-cron (CronJob) |
| 消息推送 | Firebase Cloud Messaging (FCM) |
| 智能模块 | Python + scikit-learn + Flask |
| 数据集 | Kaggle Medication Adherence Dataset (2800条, 24特征) |

## 项目结构

```
├── backend/                    # 后端服务
│   ├── src/
│   │   ├── config/            # 数据库与Firebase配置
│   │   ├── models/            # 数据模型 (User, Medication, MedicationLog, RiskAssessment)
│   │   ├── controllers/       # 控制器 (认证, 药品, 记录, 风险评估)
│   │   ├── routes/            # API路由
│   │   ├── middleware/        # 中间件 (JWT认证, 错误处理)
│   │   ├── services/          # 服务 (CronJob调度)
│   │   └── app.js             # 入口文件
│   └── ml/                    # 机器学习模块
│       ├── data/              # 数据集
│       ├── models/            # 训练好的模型
│       ├── generate_dataset.py # 数据预处理
│       ├── train_model.py     # 模型训练
│       └── app.py             # ML预测API服务
├── android/                   # Android客户端
│   └── app/src/main/
│       ├── java/.../          # Java源代码
│       │   ├── activities/    # Activity页面
│       │   ├── adapters/      # RecyclerView适配器
│       │   ├── network/       # 网络请求 (Retrofit)
│       │   ├── services/      # FCM服务
│       │   └── utils/         # 工具类
│       └── res/               # 资源文件
└── medication_adherence.csv   # Kaggle原始数据集
```

## 核心功能模块

### 1. 结构化用药记录器（数据采集层）
- 用户设置服药计划，系统按时推送提醒
- 记录行为数据对齐 Kaggle 数据集字段规范
- 采集：年龄/性别/慢性病、提醒响应时长、漏服原因、压力值、活动状态

### 2. 数据验证型风险识别器（智能应用层）
- 使用 Kaggle 数据集 2800 条标注样本训练预测模型
- 支持 RandomForest 和 GradientBoosting 双模型对比
- 每日自动提取用户特征，输出风险评分
- 规则引擎降级方案（ML服务不可用时）

### 3. 智能干预策略
- 基于数据集验证的有效策略自动匹配
- 干预类型：额外提醒、简化步骤、激励信息、监护人通知、时间调整、药师咨询

## 启动方式

### 后端服务
```bash
cd backend
npm install
npm run dev        # 开发模式 (nodemon)
# 或
npm start          # 生产模式
```

### ML预测服务
```bash
cd backend/ml
pip install -r requirements.txt
python train_model.py    # 训练模型
python app.py            # 启动Flask API (端口5000)
```

### Android客户端
使用 Android Studio 打开 `android/` 目录，配置 Firebase 后编译运行。

## API接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |
| GET | /api/auth/profile | 获取个人信息 |
| PUT | /api/auth/profile | 更新健康档案 |
| POST | /api/medications | 添加药品 |
| GET | /api/medications | 获取药品列表 |
| POST | /api/logs | 记录服药行为 |
| GET | /api/logs/today | 今日服药计划 |
| GET | /api/logs/adherence | 依从性统计 |
| POST | /api/risk/assess | 执行风险评估 |
| GET | /api/risk/latest | 最新风险评估 |
| GET | /api/risk/history | 评估历史 |

## 数据集说明

使用 Kaggle Medication Adherence Dataset，包含以下关键字段：
- `patient_id`, `age`, `gender`, `chronic_condition`
- `medication_type`, `dosage`, `comorbidities`, `BMI`
- `heart_rate`, `blood_pressure`, `blood_glucose`
- `activity_level`, `sleep_hours`, `stress_level`
- `reminder_sent`, `reminder_response_time`
- `social_support`, `location`
- `future_non_adherence`（目标变量）
