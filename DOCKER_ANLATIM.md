# DOCKER - SIFIRDAN BAŞLAYAN İÇİN TAM REHBER

Docker, yazılımı her ortamda aynı şekilde çalıştırmak için bir kutuya koyup göndermeye benzer.

---

## 1) Docker Nedir - Çok Basit Anlatım

### Problem:
Senin notebook'unda mükemmel çalışan bir uygulama, başkasının bilgisayarında çalışmayabiliyor. Neden?
- Farklı Java versiyonu
- Farklı PostgreSQL versiyonu
- Farklı işletim sistemi
- Eksik kütüphaneler

### Docker Çözümü:
Docker, uygulamanı **tam olarak kurulan ortamıyla birlikte** paketler:
- İşletim sistemi (küçük bir Linux sistemi)
- Java
- Bağımlılıklar
- Uygulama kodu

Bu pakete **Container** denir.

### Analoji:
- Normal: Pizza siparişi veriyorsun, malzemeler ayrı gelerek senin evinde hazırlanıyor
- Docker: Pizzacıdan tamamen hazır, kutuyla mühürlü pizza geliyorur → açıp yiyin

---

## 2) Docker Bileşenleri

### 1. Dockerfile
- Reçete gibidir
- "Hangi işletim sistemiyle başla, Java yükle, bağımlılıkları al, uygulama kodunu koy" gibi adımlar
- Metin dosyası

### 2. Image
- Dockerfile'ın derlenmesiyle oluşan hazır şablondur
- Reçete → Image = Yaşlı Dışarı konserve tablosu (tekrar tekrar kullanılabilir)

### 3. Container
- Image'ın çalışan versiyonu
- Hazır konserve tablosundan bir tabak doldurüp servis etmek gibi
- Aynı image'tan birden fazla container çalıştırılabilir

### 4. Docker Compose
- Birden fazla container'ı bir arada yönetir
- Senin projede: Uygulama container + PostgreSQL container
- İkisi aynı anda başlar, birbiriyle konuşur

---

## 3) Senin Proje İçin Docker Neden Gerekli?

Senin projede 2 şey var:
1. Java uygulaması (Spring Boot)
2. PostgreSQL veritabanı

Normal durumda:
- PostgreSQL'i manuel yükle
- Java 17'yi yükle
- Maven build et
- Uygulamayı çalıştır

Docker ile:
- `docker-compose up` → Her şey otomatik başlar
- Başka bilgisayara taşı → Aynı komut çalışır
- İş arkadaşları aynı ortamda çalışır

---

## 4) Docker Kurulumu

Docker Desktop indir: https://www.docker.com/products/docker-desktop

Windows'ta:
- Docker Desktop installer'ı indir
- Çalıştır
- PowerShell yeniden başlat

Görmek için terminal'de:
```
docker --version
docker run hello-world
```

---

## 5) Dockerfile Nedir - Adım Adım

Dockerfile şöyle yazılır:

```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Satır satır ne olur:

**FROM openjdk:17-slim**
- Docker Hub'dan Java 17'nin hazır image'ını al
- Bu, temel işletim sistemidir
- -slim = Gereksiz kütüphaneler çıkarılmış, daha küçük

**WORKDIR /app**
- Container içindeki çalışma klasörü
- Bilgisayarındaki cd komutu gibi

**COPY target/app.jar app.jar**
- Bilgisayarındaki target/app.jar dosyasını
- Container'ın /app/app.jar'a kopyala

**EXPOSE 8080**
- Konteyner 8080 portunun dışarıya açık olduğunu söyler
- Dışarıdan bu porta erişilebilir anlamına gelir

**ENTRYPOINT ["java", "-jar", "app.jar"]**
- Container çalıştığında bu komutu çalıştır
- "Uygulamayı başlat" demektir

---

## 6) Docker Compose Nedir - Bileşik Sistem

Docker Compose, birden fazla container'ı YAML dosyasıyla tanımlar.

Örnek (docker-compose.yml):

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_USER: stok_user
      POSTGRES_PASSWORD: stok_password
      POSTGRES_DB: stok_db
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  app:
    build: .
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/stok_db
      SPRING_DATASOURCE_USERNAME: stok_user
      SPRING_DATASOURCE_PASSWORD: stok_password
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

Ne olur:
- postgres service'i: PostgreSQL database
- app service'i: Senin Spring Boot uygulaması
- depends_on: App başlamadan postgres başlasın
- volumes: Veritabanı verisi kalıcı olsun (container silinse de veri durur)
- ports: Dış dünyayla iletişim

---

## 7) Docker Komutları - Kullandığın Komutlar

### Build (Image oluştur)
```
docker build -t stok-app:1.0 .
```
- Dockerfile'dan image oluştur
- Adı: stok-app:1.0
- . = bu klasördeki Dockerfile kullan

### Run (Container başlat)
```
docker run -p 8080:8080 stok-app:1.0
```
- stok-app:1.0 image'ından container çalıştır
- -p: Port 8080'i access et

### Docker Compose (Tüm sistem)
```
docker-compose up
```
- docker-compose.yml'deki tüm service'ları başlat
- PostgreSQL + App aynı anda

```
docker-compose down
```
- Tüm container'ları durdur

---

## 8) Bu Proje İçin Adımlar

### Adım 1: Maven Build
```
mvn clean package
```
- Proje compile edilir
- Spring Boot JAR dosyası oluşur
- Konum: target/stok-0.0.1-SNAPSHOT.jar

### Adım 2: Dockerfile Oluştur
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/stok-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Adım 3: docker-compose.yml Oluştur
PostgreSQL ve App'ı bir arada olacak şekilde

### Adım 4: Başlat
```
docker-compose up
```

### Adım 5: Test Et
```
curl http://localhost:8080/swagger-ui.html
```
Swagger açılırsa başarılı

---

## 9) Volume Nedir?

Volume, container kapatılsa da veriyi kalıcı tutmak için kullanılır.

Örnek:
- Container içindeki /var/lib/postgresql/data
- Host'un postgres_data volume'üne bağlı
- Container silinse de postgres_data durur
- Yeni container başsa aynı veri kullanılır

---

## 10) Network Nedir?

Docker Compose'da tüm container'lar aynı network'te.

Örnek:
- App'ın PostgreSQL'e bağlanması:
  - Localhost kullanmaz
  - "postgres" (service adı) kullanır
  - Docker DNS otomatik çözer

---

## 11) Environment Variable Nedir?

Uygulamanın ayarlarını container'ı değiştirmeden konfigüre etmek için.

Örnek:
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/stok_db
  SPRING_PROFILES_ACTIVE: prod
```

Bu, uygulama başlarken bu değişkenleri okuyacak anlamına gelir.

---

## 12) Port Mapping Nedir?

```
ports:
  - "8080:8080"
```

- Sol 8080: Bilgisayarının portu
- Sağ 8080: Container'ın portu
- İlki yoksa, örneğin:
```
"9090:8080"
```
→ Bilgisayar 9090 → Container 8080

---

## 13) Dockerfile Best Practice'ler (Gelişmiş)

1. Multi-stage build (daha küçük image)
2. .dockerignore dosyası (target gibi gereksiz dosyaları koyma)
3. Non-root user (güvenlik)

Ama başında şu basit şekilde yeterli:

```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/stok-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 14) Komun Hatalar ve Çözümleri

**Error: "Cannot find target/xxx.jar"**
- Çözüm: Önce `mvn clean package` yap

**Error: "Port 5432 already in use"**
- Çözüm: `docker-compose down` yap veya docker-compose.yml'de port değiştir

**Error: "Connection refused to postgres"**
- Çözüm: docker-compose.yml'de depends_on kontrol et, postgres başladığını bekle

**Error: "Image not found"**
- Çözüm: `docker build` yap önce

---

## 15) Production vs Development

Development:
- Hızlı build
- Debugging açık

Production:
- Optimization
- Logging minimal
- CPU/Memory limitli
- Health checks
- Restart policy

Bu proje development şekli ile basılacak.

---

## 16) Docker Monitoring (İleri)

```
docker ps
```
- Çalışan container'ları gör

```
docker logs container_id
```
- Container log'unu gör

```
docker-compose logs app
```
- Compose ile başlattığın app'ın log'u

---

## 17) Özet - Fiziksel Benzetme

**Normal Yöntem:**
- Bilgisayarında: Java + PostgreSQL + Kod
- Başka bilgisayara taşı: Hepsi tekrar yükle, config ayarla
- Başarısız olabilir

**Docker Yöntemi:**
- Image yap: Tam ortamı paketlenmiş kutu
- Başka bilgisayara taşı: Kutuyu aç, bittikodu
- Her zaman çalışır

---

## 18) Sonuç

Docker öğrenmek 3 adım:

1. Dockerfile yaz (reçete)
2. Build et (image oluştur)
3. Run et (container başlat)

Bu proje için:
- docker-compose.yml (SQL + App)
- `docker-compose up`
- Bitti

Başka bilgisayarda:
- `docker-compose up`
- Bitti

Sıkça sorular:
- "Docker neden yavaş?" → Değil, container hafif
- "VM gibi mi?" → Değil, hafif ve hızlı
- "Güvenli mi?" → Evet, isolated ortam
- "Üretimde kullanılır mı?" → Evet, standart

---

Bu döküman jargonsuz Docker anlatımıdır. Artık Docker'ı anladığına göre, projeyi containerize edelim.
