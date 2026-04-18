# DeePay — Nginx Configuration Guide

## Required Nginx Server Block

The following configuration is required for the DeePay homepage (React SPA + Laravel hybrid) to work correctly.

**Key points:**
1. Static assets (JS, CSS, images) must be served by Nginx directly — **never** forwarded to PHP/Laravel
2. Navigation/HTML requests must go to Laravel (`/index.php`)
3. The `dist/` folder (pre-built React bundle) must be accessible as static files
4. The `assets/` folder (Laravel template CSS/JS/images) must also be accessible as static files
5. `manifest.json` and `sw.js` live in the **repository root** (same directory as `index.php`)

```nginx
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name deepay.srl www.deepay.srl;

    # Root is the repository root (where index.php and manifest.json live)
    root /www/wwwroot/www.deepay.srl;
    index index.php;

    ssl_certificate     /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;

    # ── 1. Pre-built React bundle ────────────────────────────────
    # These files MUST be served as static files with correct MIME types.
    # If Nginx forwards them to PHP, the browser gets HTML instead of JS/CSS
    # → React fails to mount → startup shell is stuck.
    location /dist/ {
        try_files $uri =404;
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable";
        add_header X-Content-Type-Options "nosniff";
    }

    # ── 2. Laravel template & global assets (CSS/JS/images/fonts) ─
    # assets/ contains Bootstrap, FontAwesome, Slick, template CSS/JS, etc.
    # These must be served statically or the Laravel-powered pages will
    # show 404s for every stylesheet and script.
    location /assets/ {
        try_files $uri =404;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000";
        add_header X-Content-Type-Options "nosniff";
    }

    # ── 3. General static file extension catch-all ────────────────
    location ~* \.(css|js|mjs|map|png|jpg|jpeg|gif|svg|ico|webp|ttf|otf|woff|woff2)$ {
        try_files $uri =404;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000, immutable";
        add_header X-Content-Type-Options "nosniff";
    }

    # ── 4. PWA files — no caching so updates are instant ─────────
    location = /sw.js {
        try_files $uri =404;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        add_header Service-Worker-Allowed "/";
    }
    location = /manifest.json {
        try_files $uri =404;
        add_header Cache-Control "no-cache";
    }

    # ── 5. SPA / Laravel — everything else goes through PHP ──────
    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }

    # ── 6. Charset — prevents garbled text (乱码) ────────────────
    charset utf-8;
    source_charset utf-8;

    # ── 7. PHP-FPM ───────────────────────────────────────────────
    location ~ \.php$ {
        include fastcgi_params;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        fastcgi_pass unix:/run/php/php8.2-fpm.sock;  # adjust PHP version
        fastcgi_hide_header X-Powered-By;
        charset utf-8;
    }

    # ── 8. Security ───────────────────────────────────────────────
    location ~ /\.(?!well-known).* { deny all; }
    location ~ /core/storage/       { deny all; }
    location ~ /core/bootstrap/     { deny all; }
}

# HTTP → HTTPS redirect
server {
    listen 80;
    listen [::]:80;
    server_name deepay.srl www.deepay.srl;
    return 301 https://$host$request_uri;
}
```

---

## First-time Server Setup

Run these commands once on the server to clone the repo and prepare the environment.

```bash
# 1. Clone the repository
cd /var/www
git clone https://github.com/deepay999/deepayv1.001.git deepayv1.001
cd deepayv1.001

# 2. Install PHP dependencies
cd core && composer install --no-dev --optimize-autoloader && cd ..

# 3. Configure Laravel environment
cp core/.env.example core/.env
# Edit core/.env — set APP_URL, DB_*, MAIL_*, etc.
php index.php artisan key:generate
php index.php artisan migrate --force

# 4. Install Node dependencies and build React frontend
npm install
npm run build

# 5. Verify build artifacts were produced
npm run check-build

# 6. Copy (or symlink) Nginx config and reload
# sudo cp /path/to/your/nginx.conf /etc/nginx/sites-available/deepay
# sudo ln -s /etc/nginx/sites-available/deepay /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

---

## Automatic Deployment (GitHub Actions)

After the first-time setup above, every push to `main` is deployed automatically by the workflow in `.github/workflows/deploy.yml`.

**Required GitHub Secrets** (set in repo → Settings → Secrets → Actions):

| Secret | Example value | Description |
|---|---|---|
| `DEPLOY_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----…` | Ed25519 private key; the matching public key must be in `~/.ssh/authorized_keys` on the server |
| `DEPLOY_HOST` | `deepay.srl` | Server hostname or IP |
| `DEPLOY_USER` | `deploy` | SSH login user |
| `DEPLOY_PATH` | `/www/wwwroot/www.deepay.srl` | Absolute path to the repository on the server |

**To generate a dedicated deploy key:**

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deepay_deploy
# Add the public key to the server:
ssh-copy-id -i ~/.ssh/deepay_deploy.pub deploy@deepay.srl
# Copy the private key into the GitHub secret DEPLOY_SSH_KEY:
cat ~/.ssh/deepay_deploy
```

The workflow will:
1. Build the React/Vite frontend on the GitHub runner
2. Rsync `dist/`, `manifest.json`, and `sw.js` to the server
3. SSH in, pull latest code, run `composer install`, `artisan migrate`, cache commands
4. Run smoke tests (homepage 200, `app.js` correct MIME, `manifest.json` 200)

---

## Troubleshooting: "Startup shell stuck, React doesn't mount"

### Step 1 — Check that JS assets load correctly

```
curl -I https://deepay.srl/dist/assets/app.js
```

You should see:
- **HTTP/2 200**
- `content-type: application/javascript` (NOT `text/html`)

If you see `text/html` for a `.js` file, Nginx is forwarding the request to Laravel. Fix: add the `/dist/` location block above.

### Step 2 — Check Laravel template CSS/JS loads correctly

```bash
curl -I https://deepay.srl/assets/templates/basic/css/main.css
curl -I https://deepay.srl/assets/templates/basic/js/slick.min.js
curl -I https://deepay.srl/assets/global/css/all.min.css
```

All must return HTTP 200 with `content-type: text/css` or `application/javascript`.
If any returns 404 the `assets/` directory was not deployed or Nginx root is wrong.

### Step 3 — Clear stale Service Worker cache

The service worker may have cached an old startup shell. In Chrome:
1. Open DevTools → **Application** → **Service Workers**
2. Click **Unregister** (for `deepay.srl`)
3. Then: Application → **Storage** → **Clear site data**
4. Hard-reload: `Ctrl+Shift+R`

The `sw.js` (v3) never caches HTML responses, so this issue won't recur after deployment.

### Step 4 — Verify dist/ files are deployed

```bash
ls -la /www/wwwroot/www.deepay.srl/dist/assets/
# Expected:
#   app.js       (~420 KB) — React bundle
#   index.css    (~27 KB)  — Tailwind styles
#   favicon.png
#   manifest.json
```

If `dist/` is missing, run on the server:
```bash
cd /www/wwwroot/www.deepay.srl
npm install
npm run build
npm run check-build   # verifies all expected artifacts exist
```

### Step 5 — Verify manifest.json is reachable

```bash
curl -I https://deepay.srl/manifest.json
# Expected: HTTP/2 200, content-type: application/json
```

`manifest.json` lives at the **repository root** (`/www/wwwroot/www.deepay.srl/manifest.json`).
Both Blade templates (`home.blade.php` and `partials/seo.blade.php`) reference it via
`asset('manifest.json')` which resolves to `/manifest.json` — served directly by Nginx
via the `location = /manifest.json` block above.

### Step 6 — Check PHP/Laravel is returning the homepage correctly

```bash
curl -s https://deepay.srl/ | grep -c 'deepay-startup-shell'
# Should return: 1
```

---

## Common Failure Modes

| Symptom | Root cause | Fix |
|---|---|---|
| `$(...).slick is not a function` | `slick.min.js` not loaded (404) | Check `assets/templates/basic/js/slick.min.js` exists and Nginx serves `/assets/` correctly |
| Font Awesome icons missing | `fa-solid-900.woff2` / `.ttf` 404 | Check `assets/global/css/all.min.css` loads; fonts referenced inside that CSS must also be under `/assets/` |
| `manifest.json` 404 | File missing or Nginx root wrong | Ensure `manifest.json` is in repo root and Nginx `root` points there |
| CSS returning `text/html` | Nginx forwards `.css` to PHP | Add `/assets/` location block and `~* \.css$` static rule |
| Startup shell stuck | `dist/assets/app.js` not deployed or served as HTML | Run `npm run build` and check Nginx `/dist/` location |
| `color.php` 404 | PHP-FPM not running or wrong socket path | Check `fastcgi_pass` socket, verify `php8.2-fpm` is active |
| **Garbled text (乱码)** | Nginx/PHP not sending `charset=utf-8` header | Add `charset utf-8; source_charset utf-8;` to the `server {}` block (see config above) |
| **Uploaded images missing / broken** | `storage` symlink not created | Run `php index.php artisan storage:link` on the server; the `deploy.sh` now does this automatically |
| **All images broken (wrong domain)** | `APP_URL` wrong in `.env` | Set `APP_URL=https://deepay.srl` (or `https://www.deepay.srl`) in `core/.env` |

---

## PWA / "Add to Home Screen"

- **Android / Chrome**: Users see the browser install prompt automatically
- **iOS / Safari**: Users see an in-app banner (shown by `home.blade.php`) instructing them to use Safari's Share → "Add to Home Screen"
- The manifest is at `/manifest.json` with `display: "standalone"` and shortcuts for Ricarica, Scansiona, Trasferisci

---

## Architecture

```
Request: GET /
   │
   ├── Nginx: static file? (/dist/, /assets/, *.js, *.css, manifest.json, sw.js)
   │     └── Serve directly (correct MIME, long cache)
   │
   └── Nginx: everything else
         └── Laravel (index.php → home.blade.php)
               └── Returns HTML shell with:
                     - Startup shell (CSS only, no JS needed)
                     - <div id="root">
                     - <script src="/dist/assets/app.js">
                           │
                           ├── Mobile device detected?
                           │     └── Render App.tsx (banking PWA)
                           │           Tabs: Home · Cripto · Carte · Portafoglio · QR
                           │
                           └── Desktop?
                                 └── Render DeepayLandingPage.tsx
                                       Hero + KPIs + Features + CTA
```

