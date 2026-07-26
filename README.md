# A lire

Dépôt officiel de l'équipe de développement.

##  Objectif

Ce dépôt suit une organisation stricte basée sur Git Flow et la méthodologie Scrum, afin d'assurer un développement propre, collaboratif et efficace.

##  Organisation des branches

| Branche   | Rôle                                                  |
|-----------|--------------------------------------------------------|
| `main`    | Version stable en production. Aucun développement direct. |
| `preprod` | Version destinée aux tests avant mise en production.  |
| `develop` | Branche principale de développement.                  |

**Flux de fusion :**

### Une branche = une tâche

- `feat/nom-fonctionnalite` : nouvelle fonctionnalité
- `fix/nom-bug` : correction d'un bug
- `refactor/nom-module` : amélioration du code sans changer le comportement
- `docs/nom-doc` : documentation
- `test/nom-test` : ajout de tests
- `ci/nom-config` : intégration continue
- `chore/nom-tache` : maintenance

**Créer une branche :**
```bash
git checkout develop
git pull
git checkout -b feat/nom-fonctionnalite
```

##  Convention des commits

Format obligatoire :
```bash
git commit -m "#NumeroTicket Nom du ticket"
```

Exemples :
```bash
git commit -m "#15 Systeme de paiement"
git commit -m "#32 Correction login"
```

##  Workflow

1. Mettre à jour `develop`
2. Créer une branche
3. Développer
4. Committer régulièrement
5. Push
6. Ouvrir une Pull Request vers `develop`
7. Après validation : fusion dans `develop`
8. Déploiement vers `preprod`
9. Après validation : fusion dans `main`

##  Philosophie de travail

- Une branche = une tâche
- Un commit = une idée claire
- Code relu avant fusion
- Communication permanente
- Respect des conventions

##  Les 5 valeurs de Scrum

- **Engagement** : Respecter les objectifs du Sprint
- **Courage** : Signaler les difficultés et proposer des solutions
- **Focus** : Travailler sur les priorités
- **Ouverture** : Partager les informations
- **Respect** : Respecter les membres de l'équipe

##  Suivi & Événements

Chaque samedi, chaque membre remet un compte rendu (CR) contenant :

- Tickets terminés
- Tickets en cours
- Difficultés rencontrées
- Solutions proposées
- Objectifs de la semaine suivante

##  Checklist avant un Push

- [ ] Branche correcte
- [ ] Code testé
- [ ] Aucun fichier inutile
- [ ] Commit conforme
- [ ] Push vers la bonne branche

## API Authentification (JWT)

### Lancer le projet
```bash
npm install
export JWT_SECRET="votre-secret-jwt"
npm start
```

### Lancer les tests
```bash
npm test
```

### Endpoints
- `POST /auth/signup` : création de compte (`Customer`, `Mechanic`, `Admin`)
- `POST /auth/login` : connexion et génération du JWT
- `POST /auth/password-reset/request` : demande de réinitialisation du mot de passe
- `POST /auth/password-reset/confirm` : validation de la réinitialisation du mot de passe
- `GET /auth/me` : endpoint protégé JWT (profil connecté)
- `GET /auth/admin` : endpoint protégé avec rôle `Admin`

Documentation OpenAPI: `docs/swagger.yaml`
