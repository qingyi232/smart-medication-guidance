const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

let firebaseInitialized = false;

const initFirebase = () => {
  if (firebaseInitialized) return;

  const serviceAccountPath = path.join(__dirname, '../../firebase-service-account.json');

  if (fs.existsSync(serviceAccountPath)) {
    const serviceAccount = require(serviceAccountPath);
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    firebaseInitialized = true;
    console.log('Firebase Admin SDK 已初始化');
  } else {
    console.warn('Firebase 服务账号文件不存在，FCM 推送功能将不可用');
    console.warn(`请将 firebase-service-account.json 放置于: ${serviceAccountPath}`);
  }
};

const sendNotification = async (fcmToken, title, body, data = {}) => {
  if (!firebaseInitialized) {
    console.warn('Firebase 未初始化，无法发送推送');
    return null;
  }

  const message = {
    notification: { title, body },
    data: Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, String(v)])
    ),
    token: fcmToken,
  };

  try {
    const response = await admin.messaging().send(message);
    console.log(`FCM 推送成功: ${response}`);
    return response;
  } catch (error) {
    console.error(`FCM 推送失败: ${error.message}`);
    return null;
  }
};

const sendMulticastNotification = async (fcmTokens, title, body, data = {}) => {
  if (!firebaseInitialized || !fcmTokens.length) return null;

  const message = {
    notification: { title, body },
    data: Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, String(v)])
    ),
    tokens: fcmTokens,
  };

  try {
    const response = await admin.messaging().sendEachForMulticast(message);
    console.log(`FCM 批量推送: 成功${response.successCount}, 失败${response.failureCount}`);
    return response;
  } catch (error) {
    console.error(`FCM 批量推送失败: ${error.message}`);
    return null;
  }
};

module.exports = { initFirebase, sendNotification, sendMulticastNotification };
