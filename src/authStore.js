// In-memory store for local development/tests. Replace with persistent storage in production.
const usersByEmail = new Map();
let nextId = 1;

if (process.env.NODE_ENV === 'production') {
  throw new Error('Le store en mémoire ne doit pas être utilisé en production');
}

function resetStore() {
  usersByEmail.clear();
  nextId = 1;
}

function createUser({ email, passwordHash, role }) {
  const user = {
    id: nextId++,
    email,
    passwordHash,
    role,
    resetTokenHash: null,
    resetTokenExpiresAt: null,
  };
  usersByEmail.set(email, user);
  return user;
}

function findUserByEmail(email) {
  return usersByEmail.get(email);
}

module.exports = {
  resetStore,
  createUser,
  findUserByEmail,
};
