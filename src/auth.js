const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const { rateLimit } = require('express-rate-limit');
const { createUser, findUserByEmail } = require('./authStore');

const router = express.Router();

const ROLES = Object.freeze({
  CUSTOMER: 'Customer',
  MECHANIC: 'Mechanic',
  ADMIN: 'Admin',
});

const VALID_ROLES = new Set(Object.values(ROLES));
const JWT_SECRET = process.env.JWT_SECRET;
const RESET_TOKEN_TTL_MS = 15 * 60 * 1000;

if (!JWT_SECRET) {
  throw new Error('JWT_SECRET doit être défini');
}

function issueToken(user) {
  return jwt.sign(
    { sub: String(user.id), email: user.email, role: user.role },
    JWT_SECRET,
    { expiresIn: '1h' }
  );
}

const authRateLimiter = rateLimit({
  windowMs: 60_000,
  limit: 100,
  standardHeaders: 'draft-8',
  legacyHeaders: false,
  message: { message: 'Trop de requêtes, réessayez plus tard' },
});

function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isStrongPassword(password) {
  return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/.test(password);
}

function sanitizeUser(user) {
  return { id: user.id, email: user.email, role: user.role };
}

function authenticateJWT(req, res, next) {
  const authorization = req.header('Authorization');
  if (!authorization || !authorization.startsWith('Bearer ')) {
    return res.status(401).json({ message: 'Token manquant' });
  }

  const token = authorization.slice(7);
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    return next();
  } catch {
    return res.status(401).json({ message: 'Token invalide' });
  }
}

function authorizeRoles(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user || !allowedRoles.includes(req.user.role)) {
      return res.status(403).json({ message: 'Accès refusé' });
    }
    return next();
  };
}

router.post('/signup', async (req, res) => {
  const { email, password, role = ROLES.CUSTOMER } = req.body || {};

  if (!email || !password) {
    return res.status(400).json({ message: 'Email et mot de passe requis' });
  }

  if (!isValidEmail(email)) {
    return res.status(400).json({ message: 'Email invalide' });
  }

  if (!isStrongPassword(password)) {
    return res
      .status(400)
      .json({ message: 'Mot de passe trop faible (8+ avec majuscule, minuscule, chiffre, symbole)' });
  }

  if (!VALID_ROLES.has(role)) {
    return res.status(400).json({ message: 'Rôle invalide' });
  }

  if (findUserByEmail(email)) {
    return res.status(409).json({ message: 'Utilisateur déjà existant' });
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = createUser({ email, passwordHash, role });
  const token = issueToken(user);

  return res.status(201).json({ user: sanitizeUser(user), token });
});

router.post('/login', async (req, res) => {
  const { email, password } = req.body || {};
  if (!email || !password) {
    return res.status(400).json({ message: 'Email et mot de passe requis' });
  }

  if (!isValidEmail(email)) {
    return res.status(400).json({ message: 'Email invalide' });
  }

  const user = findUserByEmail(email);
  if (!user) {
    return res.status(401).json({ message: 'Identifiants invalides' });
  }

  const isPasswordValid = await bcrypt.compare(password, user.passwordHash);
  if (!isPasswordValid) {
    return res.status(401).json({ message: 'Identifiants invalides' });
  }

  const token = issueToken(user);
  return res.status(200).json({ user: sanitizeUser(user), token });
});

router.post('/password-reset/request', (req, res) => {
  const { email } = req.body || {};
  if (!email) {
    return res.status(400).json({ message: 'Email requis' });
  }

  if (!isValidEmail(email)) {
    return res.status(400).json({ message: 'Email invalide' });
  }

  const user = findUserByEmail(email);
  let resetToken;

  if (user) {
    resetToken = crypto.randomBytes(32).toString('hex');
    user.resetTokenHash = crypto.createHash('sha256').update(resetToken).digest('hex');
    user.resetTokenExpiresAt = Date.now() + RESET_TOKEN_TTL_MS;
  }

  return res.status(200).json({
    message: 'Si le compte existe, un lien de réinitialisation a été généré',
    ...(process.env.NODE_ENV === 'test' && resetToken ? { resetToken } : {}),
  });
});

router.post('/password-reset/confirm', async (req, res) => {
  const { email, resetToken, newPassword } = req.body || {};
  if (!email || !resetToken || !newPassword) {
    return res
      .status(400)
      .json({ message: 'Email, resetToken et newPassword requis' });
  }

  if (!isValidEmail(email)) {
    return res.status(400).json({ message: 'Email invalide' });
  }

  if (!isStrongPassword(newPassword)) {
    return res
      .status(400)
      .json({ message: 'Mot de passe trop faible (8+ avec majuscule, minuscule, chiffre, symbole)' });
  }

  const user = findUserByEmail(email);
  if (!user || !user.resetTokenHash || !user.resetTokenExpiresAt) {
    return res.status(400).json({ message: 'Token de réinitialisation invalide' });
  }

  const providedTokenHash = crypto
    .createHash('sha256')
    .update(resetToken)
    .digest('hex');

  const isTokenValid =
    user.resetTokenHash === providedTokenHash && user.resetTokenExpiresAt > Date.now();

  if (!isTokenValid) {
    return res.status(400).json({ message: 'Token de réinitialisation invalide' });
  }

  user.passwordHash = await bcrypt.hash(newPassword, 10);
  user.resetTokenHash = null;
  user.resetTokenExpiresAt = null;

  return res.status(200).json({ message: 'Mot de passe mis à jour' });
});

router.get('/me', authRateLimiter, authenticateJWT, (req, res) => {
  return res.status(200).json({ user: req.user });
});

router.get('/admin', authRateLimiter, authenticateJWT, authorizeRoles(ROLES.ADMIN), (_req, res) => {
  return res.status(200).json({ message: 'Bienvenue admin' });
});

module.exports = {
  authRouter: router,
  ROLES,
  authenticateJWT,
  authorizeRoles,
};
