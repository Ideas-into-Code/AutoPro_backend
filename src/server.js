const app = require('./app');

const port = process.env.PORT || 3000;

app.listen(port, () => {
  // eslint-disable-next-line no-console
  console.log(`AutoPro backend running on port ${port}`);
});
