# STOK PROJESI - BASLANGICTAN UZMANA TAM ANLATIM

Bu dokuman, bu projeye hic hakim olmayan bir kisinin bile projeyi basta genel mantik, sonra teknik detay, sonra dosya/sinif bazinda tamamen anlayabilmesi icin yazildi.

Amac: "Bu proje ne yapiyor, nasil yapiyor, hangi dosya neden var, bir istek sunucuya gelince adim adim neler oluyor?" sorularinin hepsini tek yerden cevaplamak.

---

## 1) Bu Proje Ne?

Bu proje bir **stok yonetim API** uygulamasidir.

Kisa anlatim:
- Bir urun olusturursun (ornek: LAPTOP-001)
- Bu urune bagli stok bilgisi olur (ornek: 50 adet)
- Sonradan stoga yeni giris yaparsin (ornek: +20 adet)
- Sistem hem stok miktarini gunceller hem de maliyeti agirlikli ortalama ile tekrar hesaplar
- Tum bunlari JWT token ile guvenli sekilde yaparsin

Yani bu sistemin ana isi:
1. Urunleri yonetmek
2. Stoklari yonetmek
3. Stok hareketini kaydetmek
4. Guvenli API sunmak

---

## 2) Hangi Teknolojiler Kullaniliyor?

Projede temel olarak su teknolojiler var:

- Java 17
- Spring Boot
- Spring Web (REST API)
- Spring Data JPA (veritabani islemleri)
- Spring Security (kimlik dogrulama/erisim kontrolu)
- JWT (token tabanli giris)
- PostgreSQL (veritabani)
- Jakarta Validation (input dogrulama)
- Springdoc OpenAPI / Swagger (API dokumani)
- Lombok (daha az tekrar kod)
- Maven (bagimlilik ve build yonetimi)

---

## 3) Ustten Bakis - Katmanli Mimari

Proje katmanli bir yapi kullanir. Bu, kodun temiz ve bakimi kolay olmasini saglar.

### Katmanlar:

1. Controller katmani
- Dis dunyadan gelen HTTP isteklerini alir
- Uygun service metodunu cagirir
- Sonucu HTTP cevabi olarak doner

2. Service katmani
- Asil is kurallari burada calisir
- Kontrol, dogrulama, hesaplama, transaction burada olur

3. Repository katmani
- Veritabani ile konusur
- Kaydetme, silme, arama gibi operasyonlar burada

4. Entity katmani
- Veritabanindaki tablolarin Java karsiligidir

5. DTO katmani
- API istek/cevap tasima objeleridir
- Entity ile birebir ayni olmak zorunda degildir

6. Mapper katmani
- Entity <-> DTO donusumlerini yapar

7. Security katmani
- JWT uretme, JWT dogrulama, endpoint koruma

8. Exception + Validation katmani
- Hata yonetimi
- Giris verisi dogrulama

Bu ayrim neden onemli?
- Kodlar birbirine dolanmaz
- Test etmek kolaylasir
- Yeni ozellik eklemek daha guvenli olur

---

## 4) Klasor ve Dosya Yapisinin Ne Oldugu / Neden Boylesine Kuruldugu

### Kok dizin

- HELP.md
  - Proje yardim notlari (genelde IDE veya baslangic rehberi)

- README.md
  - Projeyi calistirma ve genel tanitim dokumani

- Updates.md, Updates2.md
  - Gelistirme notlari / degisiklik kayitlari

- pom.xml
  - Maven konfigurasyonu
  - Hangi kutuphaneler var, Java versiyonu ne, build nasil olur gibi bilgiler burada

- mvnw, mvnw.cmd
  - Maven wrapper
  - Bilgisayarda global Maven olmasa da projeyi calistirmayi kolaylastirir

### src/main/java/com/musyan/stok

Burasi uygulamanin asil Java kodlaridir.

#### Kok uygulama dosyasi

- StokApplication.java
  - Spring Boot baslangic noktasi
  - Uygulamayi ayaga kaldiran ana sinif
  - JPA auditing aktiflestirme bulunur

#### audit

- AuditAwareImpl.java
  - createdBy / updatedBy gibi alanlara kim yazsin sorusunu cevaplar
  - Su an sabit olarak SYSTEM doner

#### config

- OpenApiConfig.java
  - Swagger/OpenAPI metadata ayarlari
  - API adini, versiyonunu, aciklamasini dokumanda gosterir

#### constants

- StockConstants.java
  - Sabit mesajlar ve kodlar
  - Ayni literal degerleri tekrar tekrar yazmamak icin

#### controller

- AuthController.java
  - Login endpointi
  - Basariliysa JWT token uretir

- IProductController.java
  - Product endpointlerinin sozlesme arayuzu
  - Swagger aciklamalari burada tutulur

- IStockController.java
  - Stock endpointlerinin sozlesme arayuzu

#### controller/impl

- ProductControllerImpl.java
  - IProductController implementasyonu
  - Product isteklerini ProductService'e delege eder

- StockControllerImpl.java
  - IStockController implementasyonu
  - Stock islemlerini StockService'e delege eder

#### dto

- AuthRequestDto.java
  - Login istegi verisi (username/password)

- AuthResponseDto.java
  - Login cevabi (token)

- ErrorResponseDto.java
  - Standart hata cevabi

- ProductDto.java
  - Urun API tasima modeli

- ProductFilterDto.java
  - Urun filtreleme kriterleri

- ResponseDto.java
  - Basari durum cevap modeli (statusCode/statusMsg)

- StockDto.java
  - Stok API tasima modeli

- StockTransactionDto.java
  - Stok giris hareketi verisi

#### entity

- BaseEntity.java
  - Tum entity siniflarinin ortak alanlari (createdAt vb)

- Product.java
  - products tablosunun Java modeli

- Stock.java
  - stocks tablosunun Java modeli

- StockTransaction.java
  - stock_transactions tablosunun Java modeli

#### exception

- GlobalExceptionHandler.java
  - Tum uygulama hatalarini tek merkezden yonetir

- InsufficientStockException.java
  - Yetersiz stok istisnasi (genisleme icin)

- ProductAlreadyExistsException.java
  - Ayni urun kod/isim olusunca firlatilir

- ResourceNotFoundException.java
  - Aranan kayit bulunamazsa firlatilir

#### mapper

- ProductMapper.java
  - Product Entity <-> ProductDto donusumu

- StockMapper.java
  - Stock Entity <-> StockDto donusumu

#### repository

- ProductRepository.java
  - Product veritabani islemleri

- StockRepository.java
  - Stock veritabani islemleri

- StockTransactionRepository.java
  - StockTransaction veritabani islemleri

#### security

- JwtFilter.java
  - Her istekte JWT var mi/gecerli mi kontrol eder

- JwtUtil.java
  - Token uretme ve token cozumleme islemleri

- SecurityConfig.java
  - Hangi endpoint acik, hangisi korumali ayarlari

#### service

- ProductService.java
  - Urun olusturma, guncelleme, silme, getirme, filtreleme is kurallari

- StockService.java
  - Stok sorgu/guncelleme ve stok hareketi ekleme is kurallari

#### specification

- ProductSpecification.java
  - Dinamik filtreleme icin JPA criteria yapisi

#### validation

- ProductCodeValidator.java
  - Product code formatini kontrol eder

- ValidProductCode.java
  - Custom annotation

### src/main/resources

- application.properties
  - Veritabani baglanti ayarlari
  - JPA ayarlari
  - Swagger/Actuator gibi runtime ayarlar

- schema.sql
  - Veritabani tablolarini olusturan SQL tanimlari

### src/test

- StokApplicationTests.java
  - Baslangic seviyesinde test sinifi (context load)

### target

- Derleme sonucu olusan ciktilar
- Elle duzenlenmez, Maven tarafindan uretilir

---

## 5) Veritabani Modeli - Cok Basit Dille

Bu projede 3 ana tablo var:

1. products
- Urunun kimligi ve tanim bilgileri
- Ornek alanlar: product_code, product_name, category, unit_cost

2. stocks
- O urunun anlik stok bilgisi
- quantity, unit, minimum_stock_level
- product_id ile products tablosuna bagli

3. stock_transactions
- Stok hareket kaydi
- "Ne zaman ne kadar stok eklendi, maliyet neydi" bilgisi
- stock_id ile stocks tablosuna bagli

Iliski:
- 1 Product -> 1 Stock
- 1 Stock -> N StockTransaction

Bu neden guzel?
- Urun bilgisi ile hareket bilgisi ayrilir
- Gecmis hareketler izlenir
- Performans ve raporlama daha mantikli olur

---

## 6) Uygulama Calisma Mantigi - Uctan Uca Akis

Asagida bir kullanicinin sisteme girip urun olusturmasi ve stoga urun eklemesi adim adim anlatiliyor.

### Adim 1: Login

- Kullanici /api/auth/login endpointine username/password gonderir
- Dogruysa JWT token doner
- Bu token sonraki tum korumali endpointlerde Authorization header ile gonderilir

### Adim 2: Urun Olusturma

- /api/products endpointine ProductDto gonderilir
- Controller istegi alir
- Service:
  - productCode ve productName benzersiz mi bakar
  - DTO -> Entity map eder
  - Product ve ona bagli Stock kaydini kaydeder
- Basariliysa ResponseDto doner

### Adim 3: Stok Guncelleme / Hareket Ekleme

- /api/stocks/{productCode}/add endpointine StockTransactionDto gonderilir
- Service:
  - Product ve Stock kaydini bulur
  - Quantity artirir
  - Agirlikli ortalama maliyeti yeniden hesaplar
  - Yeni transaction kaydi olusturur
  - Transactional oldugu icin hepsi bir butun olarak commit olur

### Adim 4: Listeleme/Filtreleme

- /api/products ile tum urunler
- /api/products/filter ile dinamik filtre
- Filter kriterleri dolu olan alanlara gore query olusur

---

## 7) JWT Guvenlik Yapisi - Baslangic Seviyesi Anlatim

JWT, sunucunun kullaniciya verdigi imzali bir bilet gibidir.

- Login basarili olunca token olusur
- Token icinde username ve son kullanma zamani bulunur
- Her istekte token gonderilir
- JwtFilter bu tokeni kontrol eder
- Gecerliyse kullaniciyi dogrulanmis kabul eder

SecurityConfig kurallari:
- /api/auth/** acik
- Swagger endpointleri acik
- Diger API endpointleri token ister

Session stateless:
- Sunucu oturum tutmaz
- Her istek token ile kendi kimligini ispatlar

---

## 8) Dogrulama (Validation) Nasil Calisiyor?

Validation, sisteme yanlis/eksik veri girmeyi engeller.

Ornekler:
- Bos alan kontrolu
- Uzunluk limiti
- Sayisal deger limiti
- Custom product code kontrolu

Product code custom kural ozeti:
- Buyuk harf, rakam, tire disinda karakter olamaz
- Belirli kelimeleri iceremez (ornek test/dummy/null gibi)
- Uygun format degilse request 400 ile reddedilir

---

## 9) Hata Yonetimi (Exception Handling)

GlobalExceptionHandler tum hatalari tek formatta doner.

Bu neden onemli?
- Frontend her hatada ayni JSON seklini gorur
- Hata yonetimi merkezilesir
- Kod tekrarini azaltir

Tipik hata akislari:
- Ayni urun kodu var -> ProductAlreadyExistsException
- Aranan urun yok -> ResourceNotFoundException
- Validation fail -> MethodArgumentNotValidException

Donen hata modeli:
- apiPath
- errorCode
- errorMessage
- errorTime

---

## 10) Product Service Is Kurallari - Derin Anlatim

ProductService su isleri yapar:

1. createProduct
- Duplicate kontrolu (kod + ad)
- DTO -> Entity
- Product + Stock birlikte kayit

2. fetchProduct
- productCode ile urun bul
- Yoksa not found

3. fetchAllProducts
- Tum urunleri getir

4. filterProducts
- ProductSpecification ile dinamik query

5. updateProduct
- Kaydi bul
- Guncellenecek alanlari set et
- Duplicate isim gibi durumlari kontrol et
- Kaydet

6. deleteProduct
- productCode ile bul
- Sil
- Iliskili stock kaydi cascade ile silinir

---

## 11) Stock Service Is Kurallari - Derin Anlatim

StockService su isleri yapar:

1. fetchStock
- productCode'dan product bulur
- Product'tan stock'u doner

2. updateStock
- Quantity, unit, minimum stock level alanlarini gunceller

3. addStockTransaction
- En kritik is akisi
- Yeni stok girisi oldugunda:
  - Mevcut stok + yeni miktar hesaplanir
  - Agirlikli ortalama maliyet hesaplanir
  - Product unit_cost guncellenir
  - Stock quantity guncellenir
  - Transaction kaydi acilir

Agirlikli ortalama mantigi:
- Eski stok buyuklugu ve eski maliyetin etkisi korunur
- Yeni gelen partinin maliyeti de hesaba katilir
- Sonuc daha gercekci ortalama maliyet verir

---

## 12) ProductSpecification Neden Var?

Kullanici bazen cok esnek filtre ister:
- Koda gore
- Ada gore
- Kategoriye gore
- Fiyat araligina gore
- Aktif/pasif
- Stokta var/yok

Bu filtreler her kombinasyonda gelebilir.

ProductSpecification bu sorunu cozer:
- Kriter doluysa predicate ekler
- Bossa eklemez
- Sonunda dinamik where query olusur

Bu sayede tek endpoint ile cok farkli filtre senaryosu cozulur.

---

## 13) Mapper Katmani Neden Onemli?

Neden DTO direkt Entity degil?
- API ile DB modelini bagimsiz tutmak icin
- Guvenlik (gereksiz alan expose etmemek)
- Gelecekte API degisse DB'yi zorlamamak

Mapper siniflari bu ayrimi temiz tutar.

---

## 14) Audit Mantigi

BaseEntity tum tablolara ortak alanlar verir:
- createdAt
- createdBy
- updatedAt
- updatedBy

JPA auditing sayesinde tarih alanlari otomatik dolabilir.

AuditAwareImpl su an "SYSTEM" dondugu icin:
- Islem yapan gercek kullanici tutulmuyor
- Gelistirme asamasinda kabul edilebilir
- Uretimde SecurityContext'ten username cekilmesi daha dogru olur

---

## 15) API Endpoint Ozeti (Fonksiyonel Harita)

Auth:
- POST /api/auth/login

Product:
- POST /api/products
- GET /api/products/{productCode}
- GET /api/products
- POST /api/products/filter
- PUT /api/products/{productCode}
- DELETE /api/products/{productCode}
- GET /api/products/validate/{productCode}

Stock:
- GET /api/stocks/{productCode}
- PUT /api/stocks/{productCode}
- POST /api/stocks/{productCode}/add

---

## 16) application.properties Ne Is Yapiyor?

Buradaki ayarlar runtime davranisini belirler:
- Hangi veritabanina baglanacagi
- SQL log acik mi
- Hibernate/JPA ayarlari
- Swagger ve actuator endpoint davranislari

Yani "uygulamanin ortam ayarlari merkezi" burasidir.

---

## 17) schema.sql Ne Is Yapiyor?

- Uygulama baslarken tablo olusturma/yapilandirma icin SQL tanimlar verir
- Product, Stock, StockTransaction tablolarini ve baglantilarini kurar
- Constraint ve foreign key'ler veri butunlugunu garanti etmeye yardimci olur

---

## 18) Test Durumu

Test klasorunde temel bir context test var.

Bu ne anlama gelir?
- Uygulama basic startup olarak acilabiliyor mu, buna bakiyor
- Ama is kurallari, service davranisi, endpoint cevaplari icin daha fazla test gereklidir

---

## 19) Bu Projede Bilincli Tasarim Kararlari

1. Product ve Stock ayrimi
- Sik degisen veriyi (stock) ayri tabloda tutarak daha temiz model

2. DTO kullanimi
- API ve DB ayrimi

3. Global exception handler
- Tutarli hata formati

4. JWT stateless security
- Mikroservis uyumlu

5. Specification pattern
- Esnek filtreleme

6. Transactional service
- Birden fazla DB adimi tek islem gibi davranir

---

## 20) Dikkat Edilmesi Gereken Gelistirme Noktalari

Bu proje guzel bir temel kurmus, ama su alanlarda guclendirilebilir:

- Hardcoded admin/admin yerine kullanici tablosu + sifre hash
- Hardcoded JWT secret yerine ortam degiskeni
- Role bazli yetkilendirme
- Pagination/sorting
- Daha kapsamli testler (unit + integration)
- Optimistic locking (@Version)
- InsufficientStockException'in aktif kullanimi
- Gercek auditor username
- Uretim ortaminda actuator ve SQL log kisitlari

---

## 21) Sifirdan Baslayan Biri Icin Zihinsel Model

Eger projeyi ilk defa okuyorsan su sirada incele:

1. StokApplication.java
- Uygulama nereden basliyor

2. SecurityConfig + JwtFilter + JwtUtil
- Guvenlik akisi

3. Controller interface + impl siniflari
- Hangi endpoint ne yapiyor

4. ProductService ve StockService
- Asil is mantigi

5. Entity ve schema.sql
- Veri modeli

6. DTO + Mapper
- API modeli ve donusum

7. GlobalExceptionHandler
- Hatalar nasil donuyor

Bu sirayla gidersen proje icerisinde kaybolmazsin.

---

## 22) Kisa Ama Tam Sonuc

Bu proje:
- JWT ile korunan
- Urun + stok + stok hareketini yoneten
- Katmanli, temiz ve genisletilebilir
- Spring Boot tabanli bir stok yonetim API'sidir.

Tek cumle ozet:
- "Bu sistem urunleri ve stok hareketlerini guvenli sekilde yonetir, stok arttikca maliyeti agirlikli ortalama ile gunceller, tum veri akislarini katmanli mimari ile duzenli halde tutar."

---

## 23) Sinif Bazli Tam Liste (Hizli Basvuru)

- StokApplication: Uygulama giris noktasi
- AuditAwareImpl: Auditor bilgisi saglayici
- OpenApiConfig: Swagger/OpenAPI ayari
- StockConstants: Sabit mesaj/kod degerleri
- AuthController: Login ve token uretimi
- IProductController: Product endpoint sozlesmesi
- IStockController: Stock endpoint sozlesmesi
- ProductControllerImpl: Product endpoint implementasyonu
- StockControllerImpl: Stock endpoint implementasyonu
- AuthRequestDto: Login request modeli
- AuthResponseDto: Login response modeli
- ErrorResponseDto: Standart hata modeli
- ProductDto: Product tasima modeli
- ProductFilterDto: Filtre kriter modeli
- ResponseDto: Basari cevap modeli
- StockDto: Stock tasima modeli
- StockTransactionDto: Stok hareket modeli
- BaseEntity: Ortak audit alanlari
- Product: Product entity
- Stock: Stock entity
- StockTransaction: Stock hareket entity
- GlobalExceptionHandler: Merkez hata yonetimi
- InsufficientStockException: Yetersiz stok hatasi
- ProductAlreadyExistsException: Duplicate urun hatasi
- ResourceNotFoundException: Kaynak bulunamadi hatasi
- ProductMapper: Product donusumleri
- StockMapper: Stock donusumleri
- ProductRepository: Product DB erisim katmani
- StockRepository: Stock DB erisim katmani
- StockTransactionRepository: Stock transaction DB erisim katmani
- JwtFilter: JWT request filtresi
- JwtUtil: JWT token utility
- SecurityConfig: Guvenlik kurallari
- ProductService: Product is kurallari
- StockService: Stock is kurallari
- ProductSpecification: Dinamik filtre query olusturucu
- ProductCodeValidator: Product code custom validator
- ValidProductCode: Product code annotation

---

Bu dokuman proje onboarding amaciyla hazirlandi. Yeni baslayan biri bu dosyayi okuyup API'nin ne oldugunu, nasil calistigini, dosyalarin neden o sekilde organize edildigini ve hangi sinifin ne ise yaradigini adim adim anlayabilir.
