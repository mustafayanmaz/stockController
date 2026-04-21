# DOCKER İLE STOK PROJESİ KURULUM VE ÇALIŞMA REHBERİ

Bu dokuman adım adım proje Docker ortamında nasıl çalışacağını gösterir.

---

## BÖLÜM 1: Docker Desktop Kurulumu

### Windows'ta:

1. Docker Desktop web sitesine git: https://www.docker.com/products/docker-desktop
2. "Download for Windows" tıkla
3. Yükleyiciyi çalıştır
4. Tüm checkbox'ları seçili bırak
5. Yüklemeyi tamamla
6. Bilgisayarı restart et

### Kurulumu Kontrol Et:

PowerShell aç ve şunu yazı:
```powershell
docker --version
```

Çıktı: `Docker version 25.x.x, build xxxxx`

Eğer çalışmazsa:
- Docker Desktop uygulamasını aç (Start menüsünde ara)
- Tray'de Docker ikonuna sağ tıkla → Aç
- Terminal'de tekrar dene

---

## BÖLÜM 2: Projeyi Docker Ortamına Hazırlık

### Dosya Kontrolü:

Proje klasörü (C:\Users\yanmu\Desktop\üsid\projects\stok) içinde şu dosyaları kontrol et:

```
✓ Dockerfile (yeni oluşturuldu)
✓ docker-compose.yml (yeni oluşturuldu)
✓ .dockerignore (yeni oluşturuldu)
✓ pom.xml (zaten var)
✓ src/ (zaten var)
✓ application.properties (zaten var)
✓ schema.sql (zaten var)
```

Hepsi varsa, sıradaki adıma git.

---

## BÖLÜM 3: Build Öncesi - application.properties Kontrol

`src/main/resources/application.properties` dosyasını aç ve şu ayarları kontrol et:

### Şu satırları bulmalısın:

```properties
# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# Actuator endpoints (health check için önemli)
management.endpoints.web.exposure.include=health,info,metrics
```

### Docker'da kullanılacak environment variable'lar:

docker-compose.yml'de zaten set edildi:
- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- Vb.

application.properties'ye ek bir şey yazman gerekmez, docker-compose.yml entegre olur.

---

## BÖLÜM 4: İlk Defa - Docker Build et

Proje klasörüne gitmek için PowerShell'de:

```powershell
cd C:\Users\yanmu\Desktop\üsid\projects\stok
```

Sonra docker image'ını build et:

```powershell
docker build -t stok-app:1.0 .
```

Ne yapıyor?
- Dockerfile'ı okuyor
- Maven ile projeyi compile ediyor (target/stok-0.0.1-SNAPSHOT.jar)
- Java runtime environment (JRE) ile birlikte bir image oluşturuyor
- İsim: stok-app:1.0

İlk dura 5-10 dakika sürebilir (maven dependencies indiriliyor).

Başarı mesajı:
```
Successfully built xxxxxxx
Successfully tagged stok-app:1.0
```

---

## BÖLÜM 5: Docker Compose ile Başlat

Şu komutu çalıştır:

```powershell
docker-compose up
```

Ne yapıyor?
1. PostgreSQL container'ı başlat
2. PostgreSQL'in hazır olmasını bekle
3. Stok App container'ı başlat
4. Spring Boot uygulamasını boot et
5. Log'ları göster

İlk başlangıç 30-60 saniye sürebilir.

### Başarı Belirtileri:

Son satırlarda şöyle bir mesaj görmek gerekir:

```
stok_app | 2026-04-13 10:30:45.123  INFO 1 --- [main] c.m.s.StokApplication : 
stok_app | Started StokApplication in X seconds
```

Ve:

```
stok_postgres | 2026-04-13 10:30:40.123 LOG:  database system is ready to accept connections
```

---

## BÖLÜM 6: Uygulama Kontrol et

### Terminal'i Açıp Test Et:

Yeni bir PowerShell penceresi aç (bu pencere açık bırakacaksın).

```powershell
# Health check
curl http://localhost:8080/actuator/health

# Swagger UI
curl http://localhost:8080/swagger-ui.html
```

Veya tarayıcıda direkt:
```
http://localhost:8080/swagger-ui.html
```

Swagger arayüzü açılırsa, her şey çalışıyordur.

---

## BÖLÜM 7: API Test Et

### Login İsteği:

PowerShell:
```powershell
$body = @{
    username = "admin"
    password = "admin"
} | ConvertTo-Json

curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d $body
```

Çıktı (bir JWT token dönecek):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MXxxxxx"
}
```

### Ürün Oluşturma İsteği:

Token'ı kopyala, sonra:

```powershell
$token = "eyJhbGciOiJIUzI1NiJ9..." # Yukarıdan aldığın token

$body = @{
    productCode = "LAPTOP-001"
    productName = "Dell XPS 13"
    category = "Electronics"
    description = "High-performance laptop"
    unitCost = 1200.50
    active = $true
    stock = @{
        quantity = 50
        unit = "pcs"
        minimumStockLevel = 10
    }
} | ConvertTo-Json

curl -X POST http://localhost:8080/api/products `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d $body
```

Başarı: 201 status code + ResponseDto

---

## BÖLÜM 8: Durdurma ve Temizleme

### Çalışan Container'ları Durdur:

İlk PowerShell penceresinde (log'u açık olan) Ctrl+C ye bası veya:

```powershell
docker-compose down
```

Ne yapıyor?
- PostgreSQL container durur
- App container durur
- Fakat postgres_data volume kalır (veritabanı verisi kaybolmaz)

---

## BÖLÜM 9: Tekrar Başlat

Hepsi aynı:

```powershell
docker-compose up
```

PostgreSQL verisi hala var, ürünler hala veritabanında.

---

## BÖLÜM 10: Veritabanı Temizliyor Mü?

### Senaryo 1: Sadece docker-compose down
- Veritabanı: Güvenli (volume kalır)
- Sonraki up: Eski veriler hala var

### Senaryo 2: Veritabanını da Sil
```powershell
docker-compose down -v
```
- -v: Volume'leri de sil
- Sonraki up: Temiz veritaban

---

## BÖLÜM 11: Log'ları Görüntüleme

### Real-time Logs:
```powershell
docker-compose logs -f
```
- -f: Follow (yeni log'ları takip et)
- Ctrl+C ile çıkış

### Sadece App Log'ları:
```powershell
docker-compose logs app
```

### Sadece PostgreSQL Log'ları:
```powershell
docker-compose logs postgres
```

---

## BÖLÜM 12: Container'ı Detached Modda Başlat

Arka planda çalış (log output'u görme):

```powershell
docker-compose up -d
```

Sonra kontrol et:
```powershell
docker-compose ps
```

Durdurmak:
```powershell
docker-compose down
```

---

## BÖLÜM 13: Sorun Giderme

### Problem: "Port 8080 already in use"

Çözüm:
```powershell
docker-compose down
# veya
docker ps
docker kill <container_id>
```

### Problem: "Port 5432 already in use"

Çözüm:
```powershell
# Portları değiştir docker-compose.yml'de:
ports:
  - "5433:5432"  # 5433'ten 5432'ye map et
```

### Problem: "Cannot connect to Docker daemon"

Çözüm:
- Docker Desktop uygulamasını aç
- Windows restart et
- Tekrar dene

### Problem: "Connection refused dockerfile"

Çözüm:
- `docker-compose logs postgres` ile PostgreSQL başlamış mı kontrol et
- Eğer hata varsa schema.sql file path kontrol et

---

## BÖLÜM 14: Image'ı Güncellemek

Kodu değiştirdiysen:

```powershell
# Build
docker build -t stok-app:1.0 .

# Ya da sadece docker-compose otomatik build etsin:
docker-compose up --build
```

---

## BÖLÜM 15: Docker Hub'a Push (İleri - İsteğe Bağlı)

Başkasıyla paylaş:

```powershell
docker tag stok-app:1.0 username/stok-app:1.0
docker push username/stok-app:1.0
```

---

## BÖLÜM 16: Production Deployment Tavsiyeler

Şu andaki setup: Development

Production için:
1. Environment variable'ları .env dosyasına taşı
2. Health check'ler ekle
3. Resource limitleri set et
4. Logging level'ini INFO yap
5. Secrets yönetim sistemi ekle (Kubernetes va.)

---

## BÖLÜM 17: Hızlı Komut Rehberi

```powershell
# Build
docker build -t stok-app:1.0 .

# Başlat (background)
docker-compose up -d

# Durdurzaş
docker-compose down

# Kontrol et
docker-compose ps

# Log göster
docker-compose logs -f

# Temizle
docker-compose down -v

# Rebuild ve başlat
docker-compose up --build

# Container içinde bash
docker-compose exec app /bin/bash

# Container'ı kaldır
docker rm <container_id>

# Image'ı kaldır
docker rmi stok-app:1.0
```

---

## BÖLÜM 18: Bilgisayardan Başka Bilgisayara Taşıma

Başladığında:

1. Docker Desktop kur
2. Proje klasörünü kopyala
3. Aynı komutları çalıştır:
   ```powershell
   docker-compose up
   ```

Her şey aynı şekilde çalışacak!

---

## BÖLÜM 19: Özet - Bir Dakikada

```
1. Docker Desktop kur
2. Proje klasörüne git
3. docker build -t stok-app:1.0 . (bir sefer)
4. docker-compose up (her çalıştırmada)
5. Eğlenç!
```

Hepsi bu.

---

Bu rehberi takip edersen, sorun yaşamazsan. Eğer sorun yaşarsan, error message'ı oku veya soru sor.

---
