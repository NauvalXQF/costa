# Costa

Personal expense tracker for students: a Laravel API + native Android app (Kotlin & Jetpack Compose).

- **Backend**: Laravel 13, SQLite, Sanctum tokens, multi-user (per-account data), category management, summaries (daily / weekly / monthly), budget tracking with a rolling daily food allowance (debt/bonus carries to the next day), CSV & PDF export.
- **Android**: Kotlin + Compose, login/registration, quick add, history with category donut chart, statistics, category management (reorder/hide/custom), notifications (daily reminder, budget warnings, weekly recap, monthly reset), pull-to-refresh, CSV/PDF export to Downloads, and an in-app APK updater.

## Structure

```
backend/   Laravel API (deploy via Docker, see backend/DEPLOY.md)
android/   Android Studio project (Kotlin, minSdk 35)
```

## Running locally

### Backend

```powershell
cd backend
composer install
cp .env.example .env
php artisan key:generate
php artisan migrate --seed
php artisan serve --port=8000
```

Set `COSTA_INVITE_CODE` in `.env` to allow self-registration from the app.

### Android

Open `android/` in Android Studio (JBR 21), run on a device with minSdk 35.
Default API base URL: `https://costa.opankcs.com/` (can be changed from the login screen or Settings).

## Deployment

1. Build the backend image and start the container (`docker compose up -d --build`).
2. Expose it through Cloudflare Tunnel: `costa.opankcs.com` → `http://host.docker.internal:8084`.
3. Upload the APK to `backend/storage/app/public/costa.apk` — it is served at `/download/costa.apk`.

## Accounts

- Registration is protected by `COSTA_INVITE_CODE`.
- Each account has isolated expenses, settings, and custom categories.

## License

MIT — see [LICENSE](LICENSE).
