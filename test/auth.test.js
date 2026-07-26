const test = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const app = require('../src/app');
const { resetStore } = require('../src/authStore');

test.beforeEach(() => {
  resetStore();
});

test('signup and login return jwt token', async () => {
  const signup = await request(app)
    .post('/auth/signup')
    .send({ email: 'customer@autopro.dev', password: 'Pass1234!' })
    .expect(201);

  assert.equal(signup.body.user.role, 'Customer');
  assert.ok(signup.body.token);

  const login = await request(app)
    .post('/auth/login')
    .send({ email: 'customer@autopro.dev', password: 'Pass1234!' })
    .expect(200);

  assert.ok(login.body.token);
});

test('signup rejects invalid email and weak password', async () => {
  await request(app)
    .post('/auth/signup')
    .send({ email: 'not-an-email', password: 'Pass1234!' })
    .expect(400);

  await request(app)
    .post('/auth/signup')
    .send({ email: 'valid@autopro.dev', password: 'weak' })
    .expect(400);
});

test('jwt middleware protects /auth/me', async () => {
  const signup = await request(app)
    .post('/auth/signup')
    .send({ email: 'mechanic@autopro.dev', password: 'Pass1234!', role: 'Mechanic' })
    .expect(201);

  await request(app).get('/auth/me').expect(401);

  const me = await request(app)
    .get('/auth/me')
    .set('Authorization', 'Bearer ' + signup.body.token)
    .expect(200);

  assert.equal(me.body.user.role, 'Mechanic');
});

test('role middleware allows only admin endpoint access', async () => {
  const customer = await request(app)
    .post('/auth/signup')
    .send({ email: 'c@autopro.dev', password: 'Pass1234!', role: 'Customer' })
    .expect(201);

  const admin = await request(app)
    .post('/auth/signup')
    .send({ email: 'a@autopro.dev', password: 'Pass1234!', role: 'Admin' })
    .expect(201);

  await request(app)
    .get('/auth/admin')
    .set('Authorization', 'Bearer ' + customer.body.token)
    .expect(403);

  await request(app)
    .get('/auth/admin')
    .set('Authorization', 'Bearer ' + admin.body.token)
    .expect(200);
});

test('password reset updates password and invalidates old one', async () => {
  await request(app)
    .post('/auth/signup')
    .send({ email: 'reset@autopro.dev', password: 'OldPass123!' })
    .expect(201);

  const resetRequest = await request(app)
    .post('/auth/password-reset/request')
    .send({ email: 'reset@autopro.dev' })
    .expect(200);

  assert.ok(resetRequest.body.resetToken);

  await request(app)
    .post('/auth/password-reset/confirm')
    .send({
      email: 'reset@autopro.dev',
      resetToken: resetRequest.body.resetToken,
      newPassword: 'NewPass123!',
    })
    .expect(200);

  await request(app)
    .post('/auth/login')
    .send({ email: 'reset@autopro.dev', password: 'OldPass123!' })
    .expect(401);

  await request(app)
    .post('/auth/login')
    .send({ email: 'reset@autopro.dev', password: 'NewPass123!' })
    .expect(200);
});
