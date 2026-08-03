# Runbook — Deploy LunchOrder (BE + FE) lên Jump UAT

Tài liệu này ghi lại **cách thật đã chạy được** trên máy `mvno-aut-app01` (`172.23.0.70`),
không phải kế hoạch lý tưởng. Máy này **không có sudo, không có Ops-managed nginx dùng
được, và tường lửa chặn mặc định mọi cổng trừ khi được mở tường minh**. Runbook gốc giả
định một mô hình khác (Ops sửa nginx hệ thống, DB/Redis mở ra ngoài `127.0.0.1`) — mô hình
đó **không đúng với máy này**, nên toàn bộ Phần B đã viết lại theo đường thật đã đi.

Mỗi khối lệnh có nhãn cho biết chạy **ở đâu**:

- 🪟 **[LOCAL]** — máy Windows dev (PowerShell). Nơi build và đóng gói.
- 🐧 **[UAT]** — máy Jump UAT (Linux bash), vào bằng Remote Desktop → SSH (MobaXterm).

Đừng chạy lệnh của bên này ở bên kia. Đó là lỗi hay gặp nhất và tốn thời gian nhất.

---

## Sự thật về hạ tầng UAT — đọc trước khi làm gì

Ba phát hiện này quyết định toàn bộ cách deploy khác với kế hoạch ban đầu:

1. **Không có sudo, và cũng không đọc được cấu hình cần sudo.**
   `sudo -l` báo `may not run sudo`. `firewall-cmd --list-ports` (không sudo) báo
   `Authorization failed`. Không có cách nào tự xem hay tự sửa tường lửa, cấu hình
   nginx hệ thống (`/etc/nginx/`), hay thư mục ngoài `$HOME`.

2. **PostgreSQL và Redis đã cài sẵn trên máy này, nhưng chỉ lắng nghe `127.0.0.1`.**
   Không sudo nên không mở rộng ra được. Hệ quả: container **không dùng được mạng
   bridge mặc định của Docker** để chạm tới hai dịch vụ này — bridge network là một
   network namespace riêng, không thấy được `127.0.0.1` của host. Cách duy nhất khả
   thi không cần Ops là chạy container với `--network host`, để `127.0.0.1` bên trong
   container chính là `127.0.0.1` của host.

3. **Tường lửa mặc định chặn hết, kể cả cổng 80 "chính thức".**
   Máy có sẵn nginx hệ thống nghe cổng 80 (trang test mặc định của Rocky Linux), nhưng
   gọi từ bên ngoài vào cổng đó vẫn **timeout** — chứng tỏ tường lửa không tự động cho
   qua chỉ vì đó là cổng web "chuẩn". Việc duy nhất phải nhờ Ops trong toàn bộ quy
   trình là: **mở đúng 1 cổng TCP cụ thể trên tường lửa**. Không cần Ops đụng vào
   nginx hệ thống, không cần Ops tạo thư mục hay cấp quyền ghi nào khác.

Vì (1) và (2), BE và FE đều chạy dưới dạng **container tự quản lý bằng `--network host`**,
không dùng nginx hệ thống làm reverse proxy. Vì (3), việc duy nhất cần Ops là mở tường lửa
cho đúng cổng đang dùng.

### Sơ đồ triển khai thật

```
🪟 Windows local                          🐧 Jump UAT (mvno-aut-app01, 172.23.0.70)
─────────────────                        ──────────────────────────────────────────
mvn package  → jar 90 MB
docker build → image lunchorder:uat-vN
docker save  → .tar ~152 MB
                    │
                    └── copy qua RDP + SFTP ──▶ docker load
                                               │
                          container lunchorder-uat (--network host)
                          nghe 127.0.0.1:18080 ──────┐
                                                      │
npm run build -- --configuration production          │
tar.gz FE (browser/)                                 │
                    │                                │
                    └── copy qua RDP + SFTP ──▶ giải nén vào ~/lunchorder/fe
                                               │
                          container lunchorder-fe (--network host, nginx-unprivileged)
                          nghe 0.0.0.0:8081 ──┬─ /            → file FE tĩnh
                                              └─ /api/v1/     → proxy_pass 127.0.0.1:18080
                                                      │
                          Firewall (Ops mở cổng 8081/tcp) ── Internet nội bộ / VPN
                                                      │
                                         ┌────────────┴────────────┐
                                    PostgreSQL 17               Redis
                                    127.0.0.1:5432 (schema tay)  127.0.0.1:6379 (BẮT BUỘC)
```

Điểm cần nhớ:

- **Redis là phụ thuộc bắt buộc, không phải tuỳ chọn.** Filter rate limit gọi Redis
  không có `try/catch` và chạy **trước** cả xác thực. Thiếu Redis thì *mọi* request
  trả 500, kể cả `/auth/login`.
- **Không có Flyway, không migration tự động.** Schema dựng bằng tay. Profile `prod`
  đặt `ddl-auto=validate`, nên lần khởi động đầu chính là bài kiểm thử đối chiếu
  entity với CSDL thật.
- **FE bản production dùng `apiUrl` tương đối `/api/v1`.** Bắt buộc container FE vừa
  serve file tĩnh vừa proxy API **trên cùng một cổng** — đây là lý do FE tự chạy nginx
  riêng thay vì tách rời khỏi BE.
- **BE khoá loopback (`SERVER_ADDRESS=127.0.0.1`), FE mở ra ngoài (`0.0.0.0`).** BE
  không bao giờ nhận request trực tiếp từ mạng ngoài — chỉ FE (qua `proxy_pass`) mới
  chạm được BE. Đây là lớp phòng thủ thêm, không phải bắt buộc về mặt chức năng.
- **Chưa có HTTPS.** Toàn bộ đang chạy HTTP. Xem mục "Việc còn dang dở" ở cuối file.

---

## PHẦN A — Trên máy Windows local

### A0. Kiểm tra công cụ (làm một lần)

🪟 **[LOCAL]**

```powershell
foreach ($c in 'docker','mvn','java','git','npm') {
  $g = Get-Command $c -ErrorAction SilentlyContinue
  if ($g) { "OK      $c" } else { "THIEU   $c" }
}
docker version --format 'docker server = {{.Server.Os}}/{{.Server.Arch}}'
```

Cần thấy đủ 5 công cụ và `docker server = linux/amd64`. Kiến trúc phải khớp UAT
(kiểm tra bằng `uname -m` bên UAT, `x86_64` = amd64) — lệch kiến trúc ra lỗi
`exec format error` sau khi đã copy xong hàng trăm MB.

`openssl` và `psql` **không có** trên máy này. Không cần cài: tài liệu dùng lệnh thay thế
(PowerShell `RandomNumberGenerator` cho key, `psql` chạy trực tiếp trên UAT).

### A1. Trước khi build lần đầu

**A1.1 — Xác nhận `sql/seed-initial.sql` đã được commit vào git.**

```powershell
git -C d:\Workspace\LunchOrder ls-files sql/
git -C d:\Workspace\LunchOrder status --short sql/
```

Nếu hiện `??` (untracked), commit lại — clone repo ở máy khác sẽ thiếu toàn bộ
permission, role, tài khoản admin, bảng giá nếu file này không nằm trong git.

**A1.2 — `.gitignore` phải chặn `uat.env`.**

```powershell
Add-Content d:\Workspace\LunchOrder\.gitignore "`nuat.env`n*.env`n!.env.example"
git -C d:\Workspace\LunchOrder check-ignore -v uat.env
```

Lệnh thứ hai phải in ra dòng khớp luật. `uat.env` chứa toàn bộ bí mật của môi trường,
và được tạo **trực tiếp trên UAT**, không đi qua git.

### A2. Build jar

🪟 **[LOCAL]**

```powershell
cd d:\Workspace\LunchOrder
mvn clean package -DskipTests
Get-ChildItem lunchorder-bootstrap\target\*.jar |
  Select-Object Name, @{n='MB';e={[math]::Round($_.Length/1MB,1)}}, LastWriteTime
```

Kỳ vọng: `lunchorder-bootstrap-1.0-SNAPSHOT.jar`, khoảng **90 MB**, timestamp là bây giờ.

### A3. Build image BE

`Dockerfile` ở gốc repo là **single-stage** — chỉ `COPY` jar đã build sẵn. **A2 phải
chạy trước A3.** Dockerfile đã ghim `-Dspring.profiles.active=prod`.

🪟 **[LOCAL]**

```powershell
docker build --provenance=false --sbom=false -t lunchorder:uat-v1 d:\Workspace\LunchOrder
docker image inspect lunchorder:uat-v1 --format 'os/arch={{.Os}}/{{.Architecture}}'
```

`--provenance=false --sbom=false` là **cần thiết**: không có hai cờ này, `docker load`
trên daemon cũ có thể từ chối định dạng manifest list kèm attestation — chỉ phát hiện
sau khi đã copy xong hàng trăm MB.

Đặt tag theo phiên bản (`uat-v1`, `uat-v2`, ...), **không dùng `latest`** — image cũ
giữ lại trên UAT để rollback nhanh.

### A4. Xuất image BE ra file

🪟 **[LOCAL]**

```powershell
$out = "$env:USERPROFILE\Desktop\lunchorder-uat-v1.tar"
docker save lunchorder:uat-v1 -o $out
"{0} MB" -f [math]::Round((Get-Item $out).Length/1MB,1)
(Get-FileHash $out -Algorithm SHA256).Hash
```

**Đừng nén file này** — `docker save` đã xuất layer ở dạng nén sẵn, gzip chỉ tốn thời
gian mà gần như không giảm dung lượng. Ghi lại chuỗi SHA256 để đối chiếu ở A5.

### A5. Build + đóng gói FE

🪟 **[LOCAL]**

```powershell
cd d:\Workspace\LunchOrder-Web
npm ci
npm run build -- --configuration production
Get-ChildItem dist\LunchOrder-Web\browser | Select-Object Name, Length

tar -czf "$env:USERPROFILE\Desktop\lunchorder-fe.tar.gz" -C dist\LunchOrder-Web\browser .
(Get-FileHash "$env:USERPROFILE\Desktop\lunchorder-fe.tar.gz" -Algorithm SHA256).Hash
```

Khác với image BE, **FE thì nén được và nên nén** — output là text (JS/CSS/HTML),
gzip co lại đáng kể. Dấu `.` cuối lệnh `tar` là cố ý: gói **nội dung bên trong**
`browser/`, không gói cả thư mục, để giải nén ra là `index.html` nằm ngay tại thư mục
đích.

Angular dùng `fileReplacements` trong `angular.json` để build `--configuration production`
tự động chọn `environment.ts` (`apiUrl: '/api/v1'`, tương đối) thay vì
`environment.development.ts` (`apiUrl: 'http://localhost:8080/api/v1'`, tuyệt đối).
Không cần sửa gì thêm, chỉ cần build đúng `--configuration production`.

### A6. Chuyển file qua Remote Desktop

Trong cửa sổ Remote Desktop: `Show Options → Local Resources → More... → Drives` để
chia sẻ ổ đĩa Windows. Từ phía UAT, kéo-thả file qua panel SFTP của MobaXterm vào
`$HOME` của tài khoản UAT.

🐧 **[UAT]** — sau khi copy xong, **luôn** đối chiếu checksum trước khi dùng file:

```bash
sha256sum lunchorder-uat-v1.tar
sha256sum lunchorder-fe.tar.gz
```

So với chuỗi lấy ở A4/A5. Khác nhau nghĩa là file copy bị hỏng — copy lại, đừng dùng.

---

## PHẦN B — Trên Jump UAT

### Mô hình quyền trên UAT

```
[namanh_dev@mvno-aut-app01 ~]$ sudo -l
Sorry, user namanh_dev may not run sudo on mvno-aut-app01.
```

| Nhóm | Việc cụ thể | Ai làm |
|---|---|---|
| Tự làm được | mọi lệnh `docker ...` (tài khoản đã ở nhóm `docker`), `psql`/`redis-cli` qua TCP, tạo file/thư mục trong `$HOME`, tạo self-signed TLS cert bằng `openssl` | bạn |
| Phải nhờ Ops | **mở đúng 1 cổng TCP trên tường lửa** — đây là việc duy nhất thực sự bắt buộc phải nhờ trong toàn bộ quy trình này | Ops |

Không cần nhờ Ops sửa nginx hệ thống, không cần cấp thư mục `/usr/share/nginx/html`,
không cần thêm vào group `docker` (đã sẵn có) trong lần deploy này. Nếu môi trường
khác chưa có các điều kiện trên, xem lại Phần E.

### B0. Kiểm tra tiền đề

🐧 **[UAT]**

```bash
echo "=== docker ==="
docker ps 2>&1 | head -5
docker --version; uname -m

echo "=== postgres ==="
ss -lnt | grep 5432
psql -h 127.0.0.1 -U <db-user> -d lunch_booking -c "select version();"

echo "=== redis ==="
ss -lnt | grep 6379
redis-cli ping

echo "=== SELinux ==="
getenforce
```

Kỳ vọng thực tế trên máy này:

| Lệnh | Kết quả thật | Ý nghĩa |
|---|---|---|
| `ss -lnt \| grep 5432` | `127.0.0.1:5432` | Postgres chỉ nghe loopback — không dùng `-p` bridge thường được, phải `--network host` |
| `ss -lnt \| grep 6379` | `127.0.0.1:6379` | Redis cũng vậy |
| `redis-cli ping` | `PONG` | Redis không cần mật khẩu — `REDIS_PASSWORD` để trống trong `uat.env` |
| `getenforce` | `Enforcing` | SELinux đang bật — mọi bind mount vào container phải thêm hậu tố `:z` để container đọc được file trên `$HOME` |

Nếu máy khác cho kết quả `0.0.0.0:5432`/`0.0.0.0:6379` (nghe mọi interface), có thể
dùng `-p` bridge bình thường theo mô hình gốc — Phần B4 dưới đây chỉ áp dụng khi
Postgres/Redis loopback-only như máy này.

### B1. Dựng CSDL

Thứ tự bắt buộc: `schema.sql` rồi mới `seed-initial.sql`.

🐧 **[UAT]**

```bash
psql -h 127.0.0.1 -U <db-user> -d lunch_booking -f sql/schema.sql
psql -h 127.0.0.1 -U <db-user> -d lunch_booking -f sql/seed-initial.sql
```

`seed-initial.sql` chạy lại nhiều lần an toàn (`ON CONFLICT DO NOTHING`).

**Xác nhận:**

```bash
psql -h 127.0.0.1 -U <db-user> -d lunch_booking -c "select count(*) from permission;"
psql -h 127.0.0.1 -U <db-user> -d lunch_booking -c "select code from role;"
psql -h 127.0.0.1 -U <db-user> -d lunch_booking -c 'select username from "user";'
```

| Lệnh | Kỳ vọng |
|---|---|
| `count(*) from permission` | **29** |
| `code from role` | `ADMIN`, `USER` |
| `username from "user"` | `admin` |

Bảng `user` phải để trong dấu ngoặc kép (`"user"`) — không có ngoặc kép, PostgreSQL
hiểu `user` là hàm đặc biệt trả về tên tài khoản DB đang kết nối, không phải bảng.

### B2. Nạp 2 image

🐧 **[UAT]**

```bash
docker load -i lunchorder-uat-v1.tar
docker images lunchorder
```

FE dùng image công khai `nginxinc/nginx-unprivileged:1.27-alpine`, tự động tải khi
`docker run` lần đầu, không cần build/save riêng.

### B3. Tạo `uat.env`

🐧 **[UAT]**

```bash
cat > uat.env <<'EOF'
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/lunch_booking
SPRING_DATASOURCE_USERNAME=<db-user>
SPRING_DATASOURCE_PASSWORD=<db-pass>
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=
SERVER_PORT=18080
SERVER_ADDRESS=127.0.0.1
JWT_SIGNER_KEY=<dan key sinh o buoc duoi>
SECURITY_USER_NAME=<ten bat ky>
SECURITY_USER_PASSWORD=<mat khau bat ky>
MAIL_USERNAME=<smtp-user>
MAIL_PASSWORD=<smtp-pass>
APP_CORS_ALLOWED_ORIGINS=http://172.23.0.70:8081
TRUSTED_PROXY_IPS_REGEX=10\.\d{1,3}\.\d{1,3}\.\d{1,3}|192\.168\.\d{1,3}\.\d{1,3}|169\.254\.\d{1,3}\.\d{1,3}|127\.\d{1,3}\.\d{1,3}\.\d{1,3}|172\.1[6-9]\.\d{1,3}\.\d{1,3}|172\.2[0-9]\.\d{1,3}\.\d{1,3}|172\.3[01]\.\d{1,3}\.\d{1,3}|::1
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
SPRINGDOC_APIDOCS_ENABLED=true
SPRINGDOC_SWAGGERUI_ENABLED=true
EOF

chmod 600 uat.env
grep -n '=[[:space:]]' uat.env || echo "OK - khong co dong nao thua dau cach"
```

Dùng heredoc `<<'EOF'` với **nháy đơn quanh EOF** — bắt buộc, để `\d{1,3}` trong regex
không bị bash nội suy mất ký tự.

Lệnh `grep '=[[:space:]]'` ở cuối kiểm tra **khoảng trắng thừa sau dấu `=`** — lỗi từng
gặp thật (`REDIS_HOST= host.docker.internal`), `--env-file` của Docker không tự cắt
khoảng trắng nên giá trị bị sai lặng lẽ, khó phát hiện bằng mắt.

**Hai biến khác với runbook gốc, đặc thù máy dùng chung:**

| Biến | Vì sao khác |
|---|---|
| `SERVER_PORT=18080` | Máy có 3 người dùng chung (`namanh_dev`, và 2 tài khoản khác). Đổi khỏi 8080 mặc định để giảm khả năng giẫm chân người khác |
| `SERVER_ADDRESS=127.0.0.1` | Khoá BE chỉ nhận request từ chính máy — chỉ container FE (cũng `--network host`, tức cũng coi như "chính máy") mới chạm được BE qua `proxy_pass` |

Sinh `JWT_SIGNER_KEY` — 🪟 **[LOCAL]** (máy này không có `openssl`):

```powershell
$b = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

Ràng buộc quan trọng nhất: `JWT_SIGNER_KEY` phải **≥ 64 byte** (ký HS512), thiếu thì
lỗi xuất hiện **lúc đăng nhập**, không phải lúc khởi động — dễ tưởng nhầm là lỗi mật
khẩu. `TRUSTED_PROXY_IPS_REGEX` không có giá trị mặc định, thiếu là app không khởi
động được.

### B4. Chạy container BE — `--network host`, không dùng `-p`

Vì Postgres/Redis chỉ bind `127.0.0.1` (xem B0) và không có sudo để mở rộng, mạng
bridge mặc định của Docker (`-p 18080:18080`) **không chạm được** hai dịch vụ này —
bridge network là namespace mạng riêng, không thấy `127.0.0.1` của host. Escape hatch
duy nhất không cần Ops: `--network host`, để container dùng chung network namespace
với host, `127.0.0.1` trong container chính là `127.0.0.1` của host.

Đánh đổi: container mất cách ly mạng, thấy được mọi thứ trên `localhost` của host, và
chiếm thẳng cổng trên host (không qua `docker-proxy`). Bù lại bằng cách siết quyền
container tối đa có thể:

🐧 **[UAT]**

```bash
mkdir -p "$HOME/lunchorder/logs"

docker run -d --name lunchorder-uat --env-file uat.env \
  --network host \
  --user "$(id -u):$(id -g)" \
  --cap-drop ALL \
  --security-opt no-new-privileges \
  --restart unless-stopped \
  -v "$HOME/lunchorder/logs:/app/logs" \
  lunchorder:uat-v1
```

| Cờ | Vì sao |
|---|---|
| `--network host` | Cách duy nhất chạm được Postgres/Redis loopback-only mà không cần Ops |
| `--user "$(id -u):$(id -g)"` | Chạy bằng UID/GID của bạn thay vì root — image không có `USER`, mặc định là root |
| `--cap-drop ALL` | App Java không cần capability nào, cổng 18080 (>1024) không cần `NET_BIND_SERVICE` |
| `--security-opt no-new-privileges` | Chặn leo quyền qua binary setuid |
| `--restart unless-stopped` | Sống lại sau khi máy chủ khởi động lại |

**Trong log lần đầu, soi đúng hai thứ:**

| Thấy gì | Nghĩa là | Làm gì |
|---|---|---|
| `Connection to 127.0.0.1:5432 refused` (hoặc `host.docker.internal`) | Chưa dùng `--network host`, hoặc `SPRING_DATASOURCE_URL` sai host | Đối chiếu lại B3, B4 |
| `SchemaManagementException` / `missing column` | Entity lệch schema — bài kiểm thử `ddl-auto=validate` | **DỪNG.** Viết migration vào `sql/migration/`, cập nhật `sql/schema.sql` |
| `Started ... in X seconds` | Thành công | Đi tiếp B5 |

### B5. Smoke test BE

🐧 **[UAT]**

```bash
curl -i -X POST http://127.0.0.1:18080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"LunchUat@2026","rememberMe":false}'
```

Kỳ vọng: `200`, `"authenticated":true`, header `Set-Cookie: token=...`. `LunchUat@2026`
là mật khẩu khởi tạo từ `seed-initial.sql` — đổi ngay ở B7, đừng để lâu vì nó nằm sẵn
trong repo.

### B6. Chạy container FE — tự phục vụ, không qua nginx hệ thống

FE build production dùng `apiUrl: '/api/v1'` tương đối, nên FE và API phải cùng một
"cổng nhìn từ trình duyệt". Không có quyền sửa nginx hệ thống (`/etc/nginx/`), nên FE
tự chạy nginx riêng trong container của chính nó, vừa serve file tĩnh vừa
`proxy_pass` sang BE.

🐧 **[UAT]**

```bash
rm -rf ~/lunchorder/fe
mkdir -p ~/lunchorder/fe ~/lunchorder/nginx
tar -xzf ~/lunchorder-fe.tar.gz -C ~/lunchorder/fe
ls ~/lunchorder/fe/index.html

cat > ~/lunchorder/nginx/default.conf <<'EOF'
server {
    listen 8081;
    server_name _;
    root  /usr/share/nginx/html;
    index index.html;
    client_max_body_size 10M;

    location /api/v1/ {
        proxy_pass http://127.0.0.1:18080/api/v1/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

docker run -d --name lunchorder-fe \
  --network host \
  --user "$(id -u):$(id -g)" \
  --cap-drop ALL \
  --security-opt no-new-privileges \
  -v ~/lunchorder/fe:/usr/share/nginx/html:ro,z \
  -v ~/lunchorder/nginx/default.conf:/etc/nginx/conf.d/default.conf:ro,z \
  nginxinc/nginx-unprivileged:1.27-alpine
```

Ba điểm khác biệt so với chạy nginx thường:

- Image `nginxinc/nginx-unprivileged` — thiết kế sẵn để chạy non-root, không cần
  `CAP_NET_BIND_SERVICE` vì nghe cổng `8081` (>1024).
- Hậu tố `:z` trên cả hai volume — **bắt buộc vì SELinux đang `Enforcing`** (xem B0).
  Thiếu `:z`, nginx trong container bị chặn đọc file dù quyền Unix (`chmod`) đúng —
  biểu hiện là `403 Forbidden` khó hiểu.
- **Không đặt `--restart unless-stopped`** cho container này lúc đầu — coi đây là bản
  tạm để test. Sau khi xác nhận ổn định và quyết định giữ lâu dài, bật thêm:
  `docker update --restart unless-stopped lunchorder-fe`.

**Kiểm tra ngay trong máy trước khi nhờ Ops:**

```bash
ss -lnt | grep 8081        # phải thấy 0.0.0.0:8081, không phải 127.0.0.1:8081
curl -s -o /dev/null -w 'FE  -> %{http_code}\n' http://127.0.0.1:8081/
curl -s -o /dev/null -w 'API -> %{http_code}\n' -X POST http://127.0.0.1:8081/api/v1/auth/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"LunchUat@2026","rememberMe":false}'
```

Cả 2 dòng `curl` phải ra `200`. `0.0.0.0:8081` xác nhận phần mềm đã mở đúng cho mạng
ngoài — nếu sau đó vẫn không vào được từ trình duyệt, chắc chắn là do tường lửa, không
phải do cấu hình container.

### B7. Nhờ Ops mở tường lửa — việc bắt buộc duy nhất

Tường lửa mặc định chặn hết, kể cả cổng 80 hệ thống (đã tự kiểm chứng: `curl` local
được nhưng trình duyệt bên ngoài `timeout`, không phải `refused`). Nhắn Ops:

> Nhờ anh/chị mở giúp cổng **8081/TCP** trên tường lửa của máy `172.23.0.70`
> (mvno-aut-app01), cho phép truy cập từ mạng nội bộ vào. Ứng dụng đã chạy sẵn trên
> cổng đó rồi, chỉ cần mở đường vào là dùng được ngay.
>
> Lệnh cần chạy: `sudo firewall-cmd --permanent --add-port=8081/tcp && sudo firewall-cmd --reload`

Bằng chứng nên đưa kèm nếu Ops hỏi "chắc chắn không phải lỗi app": SSH (cổng 22) vào
máy vẫn chạy tốt trong khi HTTP (cổng 80, 8081) timeout — cùng đường mạng, chỉ khác
loại cổng, đặc trưng của việc bị chặn theo cổng chứ không phải mạng đứt hay app lỗi.

Sau khi Ops báo đã mở, kiểm tra lại bằng trình duyệt thật: `http://172.23.0.70:8081/`.
Vào được là xong. Vẫn timeout thì có thể còn 1 lớp chặn khác (thiết bị mạng giữa
đường, hoặc dải IP VPN chưa được gộp vào luật vừa mở) — báo lại Ops.

### B8. Đổi mật khẩu admin — làm ngay khi FE vào được

`LunchUat@2026` nằm trong `sql/seed-initial.sql`, đã commit vào git — ai đọc được repo
đều biết. Đổi qua giao diện FE (chức năng đổi mật khẩu), **đừng** `UPDATE` trực tiếp
vào DB (mật khẩu lưu dạng hash, sửa tay tạo ra bản ghi không đăng nhập được).

---

## PHẦN C — Redeploy phiên bản sau (v2, v3, ...)

### BE

🪟 **[LOCAL]**

```powershell
cd d:\Workspace\LunchOrder
mvn clean package -DskipTests
docker build --provenance=false --sbom=false -t lunchorder:uat-v2 .
docker save lunchorder:uat-v2 -o "$env:USERPROFILE\Desktop\lunchorder-uat-v2.tar"
(Get-FileHash "$env:USERPROFILE\Desktop\lunchorder-uat-v2.tar" -Algorithm SHA256).Hash
```

🐧 **[UAT]** — nếu bản mới đổi entity hoặc quyền, làm **trước** khi thay container:

```bash
# Đổi entity: chạy migration, RỒI cập nhật sql/schema.sql trong repo cho khớp
psql -h 127.0.0.1 -U <db-user> -d lunch_booking -f sql/migration/<file-moi>.sql

# Đổi @PreAuthorize / thêm quyền mới: viết script insert permission + role_permission
# (xem mẫu ở sql/seed-initial.sql, hoặc mục "Vấn đề đã biết" bên dưới)
```

🐧 **[UAT]** — thay container:

```bash
sha256sum lunchorder-uat-v2.tar
# đối chiếu với giá trị LOCAL, khác nhau thì dừng, copy lại

# Nếu code mới cần thêm biến môi trường: thêm vào ~/uat.env TRƯỚC bước dưới,
# --env-file chỉ được đọc lúc container KHỞI TẠO, không phải lúc restart.

docker load -i lunchorder-uat-v2.tar
docker rm -f lunchorder-uat

docker run -d --name lunchorder-uat --env-file uat.env \
  --network host \
  --user "$(id -u):$(id -g)" \
  --cap-drop ALL \
  --security-opt no-new-privileges \
  --restart unless-stopped \
  -v "$HOME/lunchorder/logs:/app/logs" \
  lunchorder:uat-v2

docker logs -f lunchorder-uat
# Ctrl+C khi thấy "Started LunchOrderApplication"

curl -i -X POST http://127.0.0.1:18080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<mật khẩu hiện tại>","rememberMe":false}'
```

### FE

🪟 **[LOCAL]**

```powershell
cd d:\Workspace\LunchOrder-Web
npm ci
npm run build -- --configuration production
tar -czf "$env:USERPROFILE\Desktop\lunchorder-fe.tar.gz" -C dist\LunchOrder-Web\browser .
(Get-FileHash "$env:USERPROFILE\Desktop\lunchorder-fe.tar.gz" -Algorithm SHA256).Hash
```

🐧 **[UAT]** — thay file theo kiểu **giải nén trước, đổi tên sau**, không xoá bản cũ
cho tới khi chắc chắn bản mới giải nén thành công (tránh sập cả hai bản nếu file
transfer lỗi giữa chừng):

```bash
sha256sum ~/lunchorder-fe.tar.gz
# đối chiếu với LOCAL

rm -rf ~/lunchorder/fe_new
mkdir -p ~/lunchorder/fe_new
tar -xzf ~/lunchorder-fe.tar.gz -C ~/lunchorder/fe_new
ls ~/lunchorder/fe_new/index.html   # không thấy file thì DỪNG, đừng đi tiếp

rm -rf ~/lunchorder/fe_old
mv ~/lunchorder/fe ~/lunchorder/fe_old
mv ~/lunchorder/fe_new ~/lunchorder/fe
```

Không cần khởi động lại container `lunchorder-fe` — nginx đọc file trực tiếp từ thư
mục mount mỗi lần có request. Rollback nếu bản mới lỗi:

```bash
rm -rf ~/lunchorder/fe
mv ~/lunchorder/fe_old ~/lunchorder/fe
```

Kiểm tra:

```bash
curl -s -o /dev/null -w 'FE  -> %{http_code}\n' http://127.0.0.1:8081/
```

### Quy tắc chung mỗi lần redeploy

- Giữ lại image BE cũ (`lunchorder:uat-v1`) và thư mục `fe_old` cho tới khi bản mới
  chạy ổn vài ngày — rollback là đổi tên/chạy lại, không phải build lại từ đầu.
- `uat.env` không cần tạo lại, **trừ khi có biến môi trường mới** — kiểm tra trước khi
  load image mới, đừng để app chết vì thiếu biến rồi mới phát hiện.
- Đổi `@PreAuthorize` luôn kèm migration cho `permission` + `role_permission`. Quyền
  chỉ sống trong DB, không có enum trong code.

---

## PHẦN D — Troubleshooting

| Triệu chứng | Nguyên nhân gần như luôn đúng | Cách xử lý |
|---|---|---|
| `Connection to 127.0.0.1:5432 refused` hoặc `host.docker.internal:5432 refused` | Container không dùng `--network host`, hoặc Postgres/Redis loopback-only mà container ở bridge network | Chạy lại theo B4, dùng `--network host` |
| Mọi request trả **500**, kể cả `/auth/login` | Redis không thông (filter rate limit chạy trước xác thực, không `try/catch`) | `redis-cli ping` từ trong máy, đối chiếu `REDIS_HOST=127.0.0.1` trong `uat.env` |
| `SchemaManagementException`, `missing column` | Entity lệch schema thật | Viết migration vào `sql/migration/`, cập nhật `sql/schema.sql`. **Đừng** hạ `ddl-auto` xuống `update` |
| App chết ngay lúc khởi động, log gần trống | Thiếu `TRUSTED_PROXY_IPS_REGEX` (không có mặc định), hoặc thiếu biến datasource | Đối chiếu B3 |
| Khởi động OK nhưng đăng nhập lỗi 500 | `JWT_SIGNER_KEY` ngắn hơn 64 byte | Sinh lại key, tạo container mới |
| Trình duyệt vào site bị **treo, không phản hồi** (không phải báo lỗi ngay) | Tường lửa chặn — timeout khác refused, xem mục Sự thật hạ tầng | Nhờ Ops mở đúng cổng đang dùng (B7) |
| `firewall-cmd --state`/`--list-ports` báo `Authorization failed` | Bình thường, tài khoản không đủ quyền xem luật tường lửa | Không tự xem được, phải hỏi Ops trực tiếp |
| Container FE báo `403 Forbidden` khi đọc file, dù `chmod` đúng | SELinux `Enforcing`, thiếu hậu tố `:z` trên volume mount | Thêm `:z` vào cuối mỗi `-v host:container:ro,z`, tạo lại container |
| `ss -lnt` cho cổng FE ra `127.0.0.1:PORT` thay vì `0.0.0.0:PORT` | nginx trong container chỉ nghe loopback | Kiểm tra `listen PORT;` trong `default.conf` không có tiền tố IP |
| Đăng nhập admin thành công nhưng thiếu tab "Vai trò/Quyền/Cấu hình hệ thống/Nhật ký hoạt động" | **Lỗi code FE đã biết** — xem mục "Vấn đề đã biết" bên dưới | Chạy SQL gán vai trò `SUPER_ADMIN` |
| `docker exec <container> env` bị người khác trong nhóm `docker` đọc được | Bình thường về mặt kỹ thuật — Docker daemon bỏ qua quyền Unix của file `uat.env` | Không có cách chặn nếu máy dùng chung nhóm `docker`; hạn chế bằng cách không lưu bí mật thật quan trọng trên máy dùng chung nếu tránh được |
| `exec format error` | Kiến trúc image khác kiến trúc host | `uname -m` trên UAT, build lại đúng `--platform` |
| `docker load` báo lỗi định dạng / `unexpected EOF` | Thiếu `--provenance=false`, hoặc file copy qua RDP bị hỏng | Đối chiếu `sha256sum` trước khi load |

Lệnh chẩn đoán hay dùng:

```bash
docker logs --tail 200 lunchorder-uat
docker logs --tail 200 lunchorder-fe
docker inspect lunchorder-uat --format '{{.State.Status}} exit={{.State.ExitCode}}'
docker exec lunchorder-uat env | sort
ss -lnt | grep -E '18080|8081'
```

Xoá cache/rate-limit của riêng app (an toàn với Redis dùng chung máy, không đụng data
của service khác nếu có):

```bash
for prefix in "users::*" "dishes::*" "prices::*" "permissions::*" "departments::*" "menus::*" "roles::*" "ratelimit:*" "login:attempts:*"; do
  redis-cli --scan --pattern "$prefix" | xargs -r redis-cli DEL
done
```

**Không dùng `FLUSHDB`/`FLUSHALL`** trên Redis dùng chung máy — hai lệnh đó xoá sạch
toàn bộ Redis, kể cả dữ liệu của service khác nếu có trên cùng instance.

---

## PHẦN E — Vấn đề đã biết (known issues)

### 1. Vai trò `SUPER_ADMIN` không tồn tại trong seed, nhưng frontend yêu cầu nó

Sidebar (`admin-sidebar.component.ts`) và route guard (`system.routes.ts`) ẩn/chặn 4
mục "Vai trò", "Quyền", "Cấu hình hệ thống", "Nhật ký hoạt động" trừ khi tài khoản có
vai trò tên `SUPER_ADMIN`. Nhưng `sql/seed-initial.sql` chỉ tạo 2 vai trò: `ADMIN`,
`USER` — không có `SUPER_ADMIN`. Backend (permission string) đã cấp đủ quyền cho
`ADMIN` từ đầu, đây thuần là 1 lớp khoá thừa ở FE, không khớp thiết kế quyền của BE.
Trang "Vai trò" (nơi lẽ ra tạo được vai trò này) lại tự nó cũng bị khoá bởi cùng điều
kiện — không ai bootstrap được qua UI.

**Vá tạm để test được trên UAT** (chạy qua `psql`):

```sql
INSERT INTO public.role (code, name, description) VALUES
    ('SUPER_ADMIN', 'Quản trị viên cấp cao', 'Toàn quyền hệ thống, bao gồm cấu hình và giám sát')
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.role_permission (role_id, permission_id)
SELECT r.id, p.id FROM public.role r CROSS JOIN public.permission p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO public.user_role (user_id, role_id)
SELECT u.id, r.id FROM public."user" u CROSS JOIN public.role r
WHERE u.username = 'admin' AND r.code = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
```

Đăng xuất, đăng nhập lại để lấy token mới. **Cần quyết định hướng sửa lâu dài**: thêm
`SUPER_ADMIN` vào `seed-initial.sql` chính thức (nếu đây là tính năng thật muốn giữ),
hoặc bỏ hẳn `superAdminOnly`/`superAdminGuard` ở FE (nếu `ADMIN` vốn đã đủ quyền theo
thiết kế BE, `SUPER_ADMIN` chỉ là code thừa).

### 2. Redis dùng chung máy, không có mật khẩu

`REDIS_PASSWORD` để trống vì Redis hệ thống không đặt mật khẩu. Có thể đặt tạm (mất
khi Redis restart, vì không ghi được `/etc/redis.conf`):

```bash
redis-cli CONFIG SET requirepass <giá-trị-mới>
```

Nếu đặt, phải cập nhật `REDIS_PASSWORD` trong `uat.env` và tạo lại container BE để
khớp.

### 3. Chưa có HTTPS

Toàn bộ đang chạy HTTP trên cổng 8081. Cookie JWT thiếu cờ `Secure`, dữ liệu (kể cả
mật khẩu lúc đăng nhập) đi cleartext trên mạng nội bộ. Chấp nhận được cho UAT ngắn
hạn, không nên giữ lâu dài.

Có thể tự làm không cần Ops (self-signed cert, cổng 443 đã xác nhận trống trên máy
này lúc viết tài liệu):

```bash
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ~/lunchorder/tls/uat.key -out ~/lunchorder/tls/uat.crt -subj "/CN=172.23.0.70"
```

Thêm `--cap-add NET_BIND_SERVICE` vào lệnh `docker run` của `lunchorder-fe` để bind
được cổng 443 mà không cần root, thêm block `listen 443 ssl;` trỏ 2 file cert/key vào
`default.conf`, và thêm `add_header Strict-Transport-Security "max-age=31536000" always;`
để chống downgrade về HTTP. Nhớ xin Ops mở thêm cổng 443/tcp tương tự B7. Trình duyệt
sẽ cảnh báo "Not secure" một lần (vì self-signed) nhưng kênh vẫn được mã hoá thật.

### 4. Container `lunchorder-fe` là bản tạm, tự quản lý

Không nằm trong hệ thống nginx chính thức của Ops, không có `--restart unless-stopped`
mặc định, không ai khác ngoài bạn theo dõi. Nếu quyết định giữ lâu dài, cân nhắc bàn
với Ops đưa vào quy trình quản lý chính thức thay vì để tồn tại dạng self-service mãi.

---

## PHẦN F — Checklist rút gọn cho môi trường tương tự

**Trước khi build**

- [ ] Schema/seed đã commit vào git
- [ ] File env bí mật đã vào `.gitignore` trước khi tạo
- [ ] Biết `ddl-auto` của profile prod là gì

**Khảo sát UAT trước khi quyết định cách chạy container**

- [ ] `sudo -l` — có sudo không, không có thì loại hẳn khỏi kế hoạch
- [ ] `firewall-cmd --state` (không sudo) — có tự xem được luật tường lửa không
- [ ] `docker ps` chạy được không cần sudo (tài khoản đã ở nhóm `docker`)
- [ ] DB/cache bind ở đâu: `ss -lnt` — loopback-only thì bắt buộc `--network host`,
      nghe mọi interface thì dùng `-p` bridge bình thường được
- [ ] `getenforce` — Enforcing thì mọi bind mount cần hậu tố `:z`
- [ ] Cổng dự định dùng đã trống chưa (`ss -lnt`)
- [ ] Thử `curl` cổng đã biết có service (như cổng 80 hệ thống) từ **bên ngoài máy** —
      xác nhận tường lửa có chặn theo cổng mặc định hay không, đừng giả định "cổng
      chính thức chắc đã mở sẵn"

**Đóng gói và chuyển**

- [ ] Build artifact trước, build image sau (Dockerfile single-stage)
- [ ] `--provenance=false --sbom=false` cho image sẽ `save`/`load`
- [ ] Tag theo phiên bản, không dùng `latest`
- [ ] Đối chiếu checksum sau khi copy, trước khi dùng

**Chạy container**

- [ ] Siết quyền: `--user`, `--cap-drop ALL`, `--security-opt no-new-privileges`
- [ ] Mount volume cho log ra ngoài container
- [ ] `--restart unless-stopped` cho service chính thức lâu dài; bỏ qua cho bản test tạm
- [ ] Đọc log lần khởi động đầu tiên như một bài kiểm thử

**Sau khi chạy**

- [ ] Smoke test 2 lần: trực tiếp `127.0.0.1`, và qua đường công khai thật (không chỉ
      đoán là sẽ thông)
- [ ] Đổi mọi mật khẩu khởi tạo có trong file seed
- [ ] Giữ image/thư mục bản trước để rollback nhanh
- [ ] Ghi lại rõ việc nào là tạm bợ tự làm, việc nào cần chuyển giao chính thức cho Ops
