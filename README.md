# LunchOrder — Backend

Hệ thống backend cho ứng dụng **LunchOrder** (đặt cơm trưa nội bộ VNPost). Dự án được xây dựng theo kiến trúc **multi-module Maven** trên nền **Spring Boot 3.4.3 / Java 21**, tách bạch rõ ràng giữa tầng hạ tầng dùng chung, nghiệp vụ quản trị hệ thống và nghiệp vụ đặt cơm.

## Tổng quan (Project Overview)

Backend cung cấp REST API phục vụ toàn bộ luồng nghiệp vụ: xác thực người dùng (JWT), quản lý thực đơn & món ăn, đặt cơm, cơm khách, giá & tổng hợp/báo cáo đơn hàng, thông báo và trao đổi phiếu. Ứng dụng sử dụng:

- **PostgreSQL** làm cơ sở dữ liệu chính.
- **Redis** — phụ thuộc **bắt buộc** (không phải tuỳ chọn): dùng cho caching và cho hàng loạt bộ giới hạn tần suất (rate limit) chạy ở tầng filter, trước cả xác thực — đăng nhập sai, tổng số request đọc/ghi theo IP, đặt cơm, đổi/nhận phiếu ăn. Thiếu Redis là mọi request lỗi 500, kể cả `/auth/login`.
- **JWT (jjwt)** cho xác thực & phân quyền, token đặt trong cookie `HttpOnly`.
- **MapStruct** cho ánh xạ Entity ↔ DTO, **Lombok** giảm boilerplate.
- **Spring Mail** cho gửi email — bắt buộc cấu hình (báo cáo đơn hàng hằng ngày, thông báo). **Cloudinary** cho lưu trữ hình ảnh thực đơn — tuỳ chọn. **Apache POI** cho xuất Excel.

Toàn bộ API được publish dưới context path `/api/v1` tại cổng `8080`.

## Cấu trúc dự án (Project Structure)

Dự án gồm 5 module Maven, `groupId` chung là `vn.vnpost.lunchorder`:

| Module | Vai trò |
| --- | --- |
| `lunchorder-common` | Tầng nền tảng dùng chung: các lớp base (`ApiResponse`, `BaseMapper`, `PageResponse`), entity, enum, exception & `GlobalExceptionHandler`. Là dependency gốc cho các module khác. |
| `lunchorder-tools` | Các tiện ích tích hợp bên thứ ba: gửi email (Spring Mail), xuất/nhập Excel (Apache POI), upload ảnh (Cloudinary). |
| `lunchorder-system` | Nghiệp vụ quản trị hệ thống: người dùng, phòng ban, vai trò, phân quyền, nhật ký thao tác (audit log). Cũng chứa cấu hình bảo mật chung (`WebSecurityConfig`) và bộ lọc rate limit toàn cục (`GlobalRateLimitFilter`). Phụ thuộc `lunchorder-common` và `lunchorder-tools`. |
| `lunchorder-core` | Nghiệp vụ lõi của ứng dụng: thực đơn & món ăn (kèm ảnh qua Cloudinary), đơn đặt cơm, cơm khách (guest meal), giá bán, tổng hợp & báo cáo đơn hàng (kèm gửi email tự động hằng ngày), thông báo, trao đổi phiếu, cấu hình hệ thống (`systemconfig`). Phụ thuộc `lunchorder-common` và `lunchorder-system`. |
| `lunchorder-bootstrap` | Module khởi chạy: chứa `LunchOrderApplication` (`@SpringBootApplication`) và các file cấu hình `application*.yml`. Đây là artifact được đóng gói thành file `.jar` chạy được. |

Sơ đồ phụ thuộc: `bootstrap` → `core` + `system` → `common` + `tools`.

## Yêu cầu môi trường (Prerequisites)

- **JDK 21** (bắt buộc — dự án cấu hình `maven.compiler.source/target = 21`).
- **Maven 3.9+** (hoặc dùng Maven Wrapper nếu có).
- **PostgreSQL 14+** — đã tạo sẵn database (mặc định dev: `meal_order_vnpost`).
- **Redis 6+** — chạy tại `localhost:6379` cho môi trường dev. **Bắt buộc**: bộ lọc rate limit gọi Redis trước cả bước xác thực, thiếu Redis là toàn bộ API (kể cả đăng nhập) lỗi 500.
- Tài khoản **SMTP** (vd. Gmail app password) — **bắt buộc**, dùng cho gửi báo cáo đơn hàng hằng ngày và thông báo; không có giá trị mặc định nên thiếu là ứng dụng không khởi động được.
- (Tùy chọn) Tài khoản **Cloudinary** nếu cần dùng chức năng upload ảnh thực đơn.

## Cài đặt & chạy Local (Local Setup)

### 1. Chuẩn bị cơ sở dữ liệu

Tạo database PostgreSQL và khởi tạo schema từ thư mục `sql/`:

```bash
createdb meal_order_vnpost
psql -U postgres -d meal_order_vnpost -f sql/schema.sql
```

Ở môi trường `dev`, `spring.jpa.hibernate.ddl-auto` được đặt là `update` nên Hibernate cũng sẽ tự đồng bộ schema khi khởi động.

### 2. Cấu hình biến môi trường

Các file `application-dev.yml` / `application-prod.yml` đọc cấu hình nhạy cảm từ biến môi trường. Với môi trường `dev`, tối thiểu cần khai báo:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_db_password
export JWT_SIGNER_KEY=your_base64_signer_key
export SECURITY_USER_NAME=admin
export SECURITY_USER_PASSWORD=admin_password
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_app_password
# Tùy chọn: CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
```

Trên Windows PowerShell:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your_db_password"
$env:JWT_SIGNER_KEY = "your_base64_signer_key"
```

> Ngoài ra dự án hỗ trợ file `application-local.yml` (đã được gitignore) để khai báo cấu hình cục bộ mà không cần export biến môi trường.

> `JWT_SIGNER_KEY` phải là chuỗi base64 giải mã ra **tối thiểu 64 byte** (thuật toán ký HS512) — thiếu độ dài sẽ lỗi ngay khi đăng nhập chứ không phải lúc khởi động.

Các ngưỡng rate limit (số lần đăng nhập sai, số request đọc/ghi mỗi phút, số lần đặt cơm / đổi phiếu mỗi vài giây) được cấu hình sẵn dưới khoá `security.*` trong `application-dev.yml` / `application-prod.yml`, có thể override qua biến môi trường tương ứng (xem `application-prod.yml`) mà không cần sửa code.

### 3. Build toàn bộ dự án

Chạy tại thư mục gốc (nơi chứa `pom.xml` cha) để build tất cả module theo đúng thứ tự phụ thuộc:

```bash
mvn clean install
```

Bỏ qua test khi build nhanh:

```bash
mvn clean install -DskipTests
```

### 4. Chạy ứng dụng

Chạy trực tiếp module bootstrap qua plugin Spring Boot:

```bash
mvn -pl lunchorder-bootstrap spring-boot:run
```

Hoặc chạy file `.jar` vừa được đóng gói:

```bash
java -jar lunchorder-bootstrap/target/lunchorder-bootstrap-1.0-SNAPSHOT.jar
```

Profile mặc định là `dev` (`spring.profiles.active: dev`). Sau khi khởi động, API sẵn sàng tại:

```
http://localhost:8080/api/v1
```

## Triển khai (Deployment)

### 1. Đóng gói bản production

Build và đóng gói toàn bộ dự án (bỏ qua test để tăng tốc pipeline nếu cần):

```bash
mvn clean package -DskipTests
```

Artifact chạy được nằm tại:

```
lunchorder-bootstrap/target/lunchorder-bootstrap-1.0-SNAPSHOT.jar
```

### 2. Cấu hình biến môi trường cho production

Profile `prod` đọc **toàn bộ** cấu hình từ biến môi trường (không có giá trị mặc định cho thông tin nhạy cảm). Cần thiết lập trước khi chạy:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/<db-name>
export SPRING_DATASOURCE_USERNAME=<db-user>
export SPRING_DATASOURCE_PASSWORD=<db-password>
export REDIS_HOST=<redis-host>
export REDIS_PORT=6379
export REDIS_PASSWORD=<redis-password>
export JWT_SIGNER_KEY=<jwt-signer-key>
export JWT_VALID_DURATION=<seconds>
export JWT_REFRESHABLE_DURATION=<seconds>
export SECURITY_USER_NAME=<admin-user>
export SECURITY_USER_PASSWORD=<admin-password>
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=<smtp-user>
export MAIL_PASSWORD=<smtp-password>
export APP_CORS_ALLOWED_ORIGINS=https://your-frontend-domain
export CLOUDINARY_CLOUD_NAME=<cloud-name>
export CLOUDINARY_API_KEY=<api-key>
export CLOUDINARY_API_SECRET=<api-secret>
# Nếu chạy sau reverse proxy (nginx, proxy biên...):
export TRUSTED_PROXY_IPS_REGEX=<regex-ip-cua-proxy-tin-cay>
```

> Ở profile `prod`, `ddl-auto` được đặt là `validate` — Hibernate chỉ kiểm tra schema chứ **không** tự thay đổi cấu trúc bảng. Không có Flyway/migration tự động; schema được dựng bằng tay từ `sql/` trước khi chạy lần đầu. Swagger UI (`springdoc`) cũng bị tắt hẳn ở profile này.

> `TRUSTED_PROXY_IPS_REGEX` cấu hình `server.tomcat.remoteip.internal-proxies` — chỉ IP khớp regex này mới được tin để đọc header `X-Forwarded-For`. Thiếu hoặc sai khi chạy sau proxy sẽ khiến toàn bộ người dùng bị gộp chung một địa chỉ IP trong mắt bộ lọc rate limit.

### 3. Chạy với profile production

```bash
java -jar lunchorder-bootstrap/target/lunchorder-bootstrap-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

Có thể tinh chỉnh JVM và cấu hình bổ sung khi chạy:

```bash
java -Xms512m -Xmx1024m \
  -jar lunchorder-bootstrap/target/lunchorder-bootstrap-1.0-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

### 4. Chạy bằng Docker

Dự án có sẵn `Dockerfile` (build từ jar đã đóng gói sẵn ở bước 1, profile `prod` mặc định):

```bash
mvn clean package -DskipTests
docker build -t lunchorder:latest .
docker run -d --name lunchorder --env-file prod.env -p 8080:8080 lunchorder:latest
```

Container cần toàn bộ biến môi trường ở mục 2 (qua `--env-file` hoặc `-e`), và cần truy cập được PostgreSQL + Redis. Xem [`docs/deploy-uat.md`](docs/deploy-uat.md) và [`docs/deploy-public.md`](docs/deploy-public.md) để có runbook triển khai đầy đủ (build/save/load image, chạy `--network host`, cấu hình reverse proxy, checklist bảo mật khi mở ra internet...).

---

*LunchOrder Backend — `vn.vnpost.lunchorder` · Spring Boot 3.4.3 · Java 21 · PostgreSQL · Redis*
