const errorHandler = (err, req, res, _next) => {
  console.error('Error:', err.message, err.stack);

  if (err.name === 'ValidationError') {
    const messages = Object.values(err.errors).map(e => e.message);
    return res.status(400).json({
      success: false,
      message: '数据验证失败',
      errors: messages,
    });
  }

  if (err.code === 11000) {
    const field = Object.keys(err.keyValue)[0];
    return res.status(400).json({
      success: false,
      message: `${field} 已存在`,
    });
  }

  if (err.name === 'CastError') {
    return res.status(400).json({
      success: false,
      message: '无效的ID格式',
    });
  }

  res.status(err.statusCode || 500).json({
    success: false,
    message: err.message || '服务器内部错误',
  });
};

module.exports = errorHandler;
