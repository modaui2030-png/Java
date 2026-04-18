# DeePay

Piattaforma di pagamenti aziendali per l'Italia e l'Europa.

## Stack

- **Backend**: Laravel 11 (in `core/`) — API, auth, gestione utenti
- **Frontend desktop**: React + Tailwind v4 (in `src/`) — landing page stile fintech
- **Frontend mobile**: React PWA (stesso bundle) — app bancaria con 4 tab + QR
- **Template Blade**: `core/resources/views/templates/basic/` — layout e viste Laravel

## Sviluppo locale

```bash
# Installa dipendenze PHP (Laravel)
cd core && composer install

# Installa dipendenze Node (React frontend)
npm install          # nella root del progetto

# Build produzione del frontend React
npm run build        # genera dist/assets/app.js e dist/assets/index.css

# Sviluppo con hot-reload
npm run dev          # Vite dev server su http://localhost:5173
```

## Deploy su Nginx

Vedi [`NGINX.md`](./NGINX.md) per la configurazione completa di Nginx, inclusa la soluzione al problema "startup shell bloccata" (React non si monta).

**Problema comune**: Se il sito mostra solo il messaggio di startup e React non si monta, quasi sempre il motivo è che Nginx sta servendo i file `.js`/`.css` come `text/html` (li passa a Laravel invece di servirli staticamente). Vedi NGINX.md §Troubleshooting.

## Deploy automatico (CI/CD)

Ogni push al branch `main` viene automaticamente distribuito al server di produzione tramite il workflow GitHub Actions in `.github/workflows/deploy.yml`.

Il workflow:
1. Builda il frontend React con Vite
2. Verifica gli artefatti di build (`npm run check-build`)
3. Carica `dist/`, `manifest.json` e `sw.js` sul server via `rsync`
4. Si collega via SSH, esegue `git pull`, `composer install`, `artisan migrate` e i comandi di cache
5. Esegue smoke test automatici (homepage 200, `app.js` con MIME corretto, `manifest.json` 200)

**Configurare i segreti GitHub** (repo → Settings → Secrets → Actions):

| Segreto | Esempio | Descrizione |
|---|---|---|
| `DEPLOY_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----…` | Chiave privata Ed25519 |
| `DEPLOY_HOST` | `deepay.srl` | Hostname o IP del server |
| `DEPLOY_USER` | `deploy` | Utente SSH |
| `DEPLOY_PATH` | `/www/wwwroot/www.deepay.srl` | Percorso assoluto sul server |

Per generare una deploy key dedicata:

```bash
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/deepay_deploy
# Aggiungi la chiave pubblica al server:
ssh-copy-id -i ~/.ssh/deepay_deploy.pub deploy@deepay.srl
# Copia la chiave privata nel segreto GitHub DEPLOY_SSH_KEY:
cat ~/.ssh/deepay_deploy
```

## Primo deploy manuale sul server

```bash
cd /var/www
git clone https://github.com/deepay999/deepayv1.001.git deepayv1.001
cd deepayv1.001

# PHP
cd core && composer install --no-dev --optimize-autoloader && cd ..
cp core/.env.example core/.env
# Modifica core/.env (APP_URL, DB_*, ecc.)
php index.php artisan key:generate
php index.php artisan migrate --force

# Node / Frontend
npm install
npm run build
npm run check-build  # verifica che dist/assets/app.js, index.css, manifest.json esistano

# Nginx
sudo nginx -t && sudo systemctl reload nginx
```

## Struttura frontend (`src/`)

```
src/
├── main.tsx                    # Entry point — rileva dispositivo (mobile/desktop)
├── styles/
│   ├── index.css               # Entry CSS (Tailwind v4 + fonts + tema)
│   ├── tailwind.css            # @import tailwindcss
│   ├── fonts.css               # Inter + Outfit da Google Fonts
│   └── theme.css               # CSS custom properties (colori, raggi, ecc.)
└── app/
    ├── App.tsx                 # Banking app mobile (4 tab + QR)
    ├── pages/
    │   └── DeepayLandingPage.tsx  # Landing page desktop (stile deblock.com)
    └── components/
        ├── HomePage.tsx        # Tab Home: saldo, IBAN, Add Money, Transfer
        ├── CardsPage.tsx       # Tab Carte: card mockup + azioni + Google Wallet
        ├── VaultsPage.tsx      # Tab Portafoglio: crypto + fiat con bilanci
        ├── QRCodePage.tsx      # QR code incasso per commerciante
        ├── TransferPage.tsx    # Trasferimento fondi
        └── SplashScreen.tsx    # Schermata di avvio animata
```

## PWA

Il file `manifest.json` e `sw.js` sono configurati per:
- **Standalone mode** (senza barra del browser) su Android e iOS
- **Shortcuts**: Ricarica, Scansiona QR, Trasferisci (visibili dal launcher Android)
- **Service Worker v3**: cache-first per asset statici, **mai** cache per HTML (evita startup shell bloccata)

## Architettura dispositivo

```
Utente apre deepay.srl/
  │
  ├── Dispositivo mobile → App bancaria (React PWA)
  │     Navigation: Home · Cripto · [QR] · Carte · Portafoglio
  │
  └── Desktop → Landing page (stile fintech)
        Hero · KPI band · Feature cards · CTA · Footer
```

