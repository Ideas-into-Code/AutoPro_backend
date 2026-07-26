const express = require('express');
const { authRouter } = require('./auth');

const app = express();
app.use(express.json());

app.get('/health', (_req, res) => {
  res.status(200).json({ status: 'ok' });
});

app.use('/auth', authRouter);

module.exports = app;
