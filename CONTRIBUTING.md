# Contributing to DeePay

Thank you for helping improve DeePay! Please read these guidelines before submitting a PR or issue.

---

## Getting started

```bash
# 1. Fork & clone
git clone https://github.com/<your-fork>/deepayv1.001.git
cd deepayv1.001

# 2. Install PHP dependencies
cd core && composer install && cd ..

# 3. Install Node dependencies and build
npm install
npm run build

# 4. Copy and configure environment
cp core/.env.example core/.env
# Edit core/.env with your local DB / APP_KEY settings
php index.php artisan key:generate
php index.php artisan migrate
php index.php artisan storage:link
```

---

## Branch strategy

| Branch | Purpose |
|---|---|
| `main` | Stable production code |
| `develop` | Integration branch for upcoming release |
| `feature/*` | New features (branch from `develop`) |
| `fix/*` | Bug fixes (branch from `main` for hotfixes, `develop` for regular fixes) |
| `docs/*` | Documentation-only changes |

---

## Pull request checklist

- [ ] Branch is up-to-date with `develop` (or `main` for hotfixes)
- [ ] No `*.env`, `*.sql`, `*.zip`, `*.key`, `*.pem` files committed
- [ ] Migrations have both `up()` and `down()` methods
- [ ] New routes are documented in `docs/ROUTING.md`
- [ ] New security-relevant code follows `docs/SECURITY.md`
- [ ] No secrets, credentials, or API keys hardcoded
- [ ] Tested locally (manual smoke test at minimum)

---

## Commit message format

```
type(scope): short description

[optional longer description]
```

Types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`

Examples:
```
feat(auth): add admin 2FA with Google Authenticator
fix(ipn): add trace-id to Alipay IPN error logs
docs(routing): document all public and restricted routes
```

---

## Code style

- PHP: PSR-12, Laravel conventions
- Blade: follow existing template structure
- No inline styles unless extending a base component

---

## Security issues

**Do not open a public issue for security vulnerabilities.**
Email the maintainers directly. We aim to respond within 48 hours.

---

## Questions?

Open a [GitHub Discussion](https://github.com/deepay999/deepayv1.001/discussions) or a regular issue using one of the issue templates.
