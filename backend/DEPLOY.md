# Deploy Costa ke Server (`costa.opankcs.com`)

## 0. Secrets

Buat `backend/.env` di server (tidak ikut repo):

```env
APP_KEY=base64:...
COSTA_INVITE_CODE=...
```

Prasyarat: laptop server menyala, **Tailscale aktif**, Docker Desktop jalan.

## 1. Kirim folder backend ke server

Dari laptop (PowerShell), di folder `C:\Users\<user>\Documents\Projects\costa`:

```powershell
# buat arsip tanpa vendor & database lokal
tar --exclude=backend/vendor --exclude=backend/database/database.sqlite -czf costa-backend.tar.gz backend

scp costa-backend.tar.gz devserver@<tailscale-ip>:C:/Users/<user>/costa-backend.tar.gz
```

SSH ke server:

```powershell
ssh devserver@<tailscale-ip>
```

Di server:

```powershell
mkdir C:\Users\<user>\costa -Force
tar -xzf C:\Users\<user>\costa-backend.tar.gz -C C:\Users\<user>
cd C:\Users\<user>\backend
docker compose up -d --build
```

Cek container:

```powershell
docker ps --filter name=costa-app
Invoke-WebRequest http://localhost:8084/api/categories
```

Respons `401`/`405` = API hidup (butuh login untuk data).

## 2. Cloudflare Tunnel

Di Cloudflare Zero Trust -> Networks -> Tunnels -> (tunnel yang dipakai) -> **Public Hostname**:

- Subdomain: `costa`
- Domain: `opankcs.com`
- Service: `http://host.docker.internal:8084`

Simpan. Lalu tes:

```powershell
Invoke-WebRequest https://costa.opankcs.com/api/categories
```

## 3. Android

- Fresh install otomatis memakai default `https://costa.opankcs.com/`.
- Perangkat lama: layar login -> **Ubah server** -> isi `https://costa.opankcs.com/`.

## 4. Backup SQLite (harian)

Jalankan di server (Task Scheduler / cron):

```powershell
Copy-Item C:\Users\<user>\backend\database\database.sqlite "C:\Users\<user>\backup\costa-$(Get-Date -Format yyyyMMdd).sqlite" -Force
```

Simpan 14 hari terakhir, hapus file lebih lama secara berkala.
