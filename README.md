# 📦 Stok Kontrol Yönetim Sistemi (Stock Control Microservice)

![Java](https://img.shields.io/badge/Java-17-orange.svg) 
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg) 
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)

## 📌 Proje Hakkında
Bu proje, kurumsal mimari standartlarına uygun olarak geliştirilmiş profesyonel bir **Stok ve Ürün Yönetimi Mikroservis** (REST API) uygulamasıdır. Proje, e-ticaret veya depo yönetim sistemlerinde olduğu gibi, bir ürünün statik olan temel bilgileri ile sürekli ve hızlı bir şekilde değişen stok miktarlarını ilişkisel veritabanı (PostgreSQL) üzerinde performanslı bir şekilde yönetmeyi amaçlamaktadır.

---

## 🚀 Teknolojiler (Tech Stack)
- **Java 17**
- **Spring Boot 3.3.5** (Web, Validation, Data JPA)
- **PostgreSQL**
- **Hibernate / JPA** (Criteria API & Specification Pattern)
- **Lombok**
- **SpringDoc OpenAPI (Swagger UI)**

---

## 🌟 Öne Çıkan Özellikler ve Mimari Yaklaşımlar

✅ **Clean Architecture & Interface Pattern**  
Controller ve Service katmanları temiz kod (Clean Code) prensiplerine göre tasarlanmış, Swagger gibi dışa bağımlı dokümantasyon yükü sınıflardan alınıp doğrudan Interface'lere taşınmıştır (`IProductController`, `IStockController` vb.). Böylece Business/Routing mantığı tertemiz kalmıştır.

✅ **Dinamik İstek Filtreleme (Criteria Builder & Specification Pattern)**  
Geleneksel Spring Data metotları yerine `Specification Pattern` kullanılarak; ürün kategorisi, ismi, fiyat aralığı (min/max) ve özellikle tablo birleştirme (Join) gerektiren "stokta var mı?" gibi birden çok filtrenin, performanslı ve tek bir anlık SQL sorgusuna dönüştüğü süper esnek bir arama altyapısı kurulmuştur.

✅ **Güvenli Veritabanı Yönetimi (`schema.sql`)**  
Kurumsal güvenlik gereği Hibernate'in kontrolsüz otomatik tablo oluşturma yeteneği (`ddl-auto=update`) kapatılmış, veritabanı ve tablo kurulumu kaynak koddaki `schema.sql` üzerinden güvenilir ve manuel hale getirilmiştir.

✅ **Veritabanı Normalizasyonu**  
Ürün detayları (`products` tablosu) ile sürekli değişen stok miktarları (`stocks` tablosu) tek bir tablo yerine `One-to-One` ilişkisiyle ayrı tablolara bölünerek (Normalization) olası veritabanı kilitlenme (table lock) problemleri ve tıkanıklıklar engellenmiştir.

✅ **Custom Validation (Özel Veri Doğrulama)**  
Kullanıcının gönderdiği ürün kodu gibi kritik veriler için `@ValidProductCode` adında tamamen projeye özel, Regex (Sadece büyük harf ve tire olsun formatı) ve Blacklist (TEST, NULL gibi yasaklı kelimeler girilemesin mantığı) kontrolleri yapan Constraint Validator geliştirilmiştir. Sistem hatalı veriyi daha Service'e inmeden (Fail-fast) reddetmektedir.

---

## 🛠️ Kurulum (Installation & Setup)

1. **Gereksinimler:**
   - Java 17 veya üzeri
   - PostgreSQL 16+
   - Maven

2. **Veritabanı Hazırlığı:**
   - PostgreSQL ortamınızda (pgAdmin veya CLI üzerinden) `stokdb` isimli boş bir veritabanı oluşturun.
   - Projenin içindeki `src/main/resources/application.properties` üzerinden kendi veritabanı kullanıcı adı ve şifrenizi güncelleyin:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/stokdb
   spring.datasource.username=SİZİN_PG_KULLANICI_ADINIZ
   spring.datasource.password=SİZİN_PG_ŞİFRENİZ
   ```

3. **Projeyi Çalıştırma:**
   Komut satırından projenin kök dizinine giderek aşağıdaki komutu çalıştırın:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 📚 API Dokümantasyonu (Swagger)
Proje başarıyla çalıştıktan sonra, herhangi bir harici araca (Postman vb.) ihtiyaç duymadan tüm endpointleri test edebileceğiniz interaktif Swagger UI arayüzüne şu adresten ulaşabilirsiniz:

👉 **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)**

### 💎 Temel Endpoint'ler
* **`GET /api/products`** - Tüm ürünleri ve bunlara ait stok bilgilerini listeler.
* **`POST /api/products`** - Sisteme yeni bir ürün ve ona bağlı alt stok nesnesini kaydeder.
* **`POST /api/products/filter`** - Farklı kombinasyonlardaki (Fiyat aralığı, stok durumu vb.) dinamik kriterlere göre ürün listesini getirir.
* **`GET /api/stocks/{productCode}`** - Sadece spesifik bir ürünün o anki güncel stok miktarını getirir.
* **`PUT /api/stocks/{productCode}`** - Stok giriş ve çıkışlarında ürün bilgilerine dokunmadan saf stok birimini günceller.

---
_Geliştirici: Mustafa Yanmaz_
