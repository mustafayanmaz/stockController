# Updates3 - Karsilastirmali Gelistirme Raporu

Bu dokuman, son turda yapilan tum degisiklikleri "oncesi ve sonrasi" mantigi ile detayli aciklar.

Amac:
- Ne degisti?
- Neden degisti?
- Hangi dosyada degisti?
- Calisma mantigi onceden nasildi, simdi nasil?

---

## 1) Genel Ozet

Bu turda iki ana baslikta gelistirme yapildi:

1. Docker ortamina tasima ve calistirma altyapisi
2. Stok/FIFO/Order/maliyet akisinin is kurali seviyesinde gelistirilmesi

Ek olarak:
- Test/deneme icin Postman koleksiyonu eklendi
- Async maliyet hesaplama event tabanli hale getirildi
- Stok operasyonu 3 net metoda ayrildi: add, remove, update

---

## 2) Onceki Durum vs Yeni Durum (Ust Seviye)

### Onceki Durum
- Satis (order) yapisi yoktu.
- FIFO lot bazli tuketim yoktu.
- Stok maliyeti add isleminde senkron ortalama hesapla guncelleniyordu.
- Stok cikisi (remove) lot bazli historical cost korumuyordu.
- Stok islemi tek stock kaydi ustunden ilerliyordu.
- Docker dosyalari yoktu.

### Yeni Durum
- `/api/orders` ile satis yapisi eklendi.
- FIFO ile lot bazli tuketim eklendi.
- Hangi lot/transactiondan tuketildigi `sale_order_allocations` ile izleniyor.
- Historical cost korunuyor.
- Ortalama maliyet sadece kalan aktif lotlar uzerinden, asenkron hesaplanip urune yaziliyor.
- Maliyet artisinda genisletilebilir hook eklendi.
- Dockerfile + docker-compose + .dockerignore + kullanim rehberleri eklendi.

---

## 3) Docker Tarafi Degisiklikleri

## 3.1 Dockerfile eklendi
Dosya: `Dockerfile`

### Once
- Proje docker ile paketli degildi.

### Sonra
- Multi-stage build yapisi eklendi:
  - Build stage: `maven:3.9-eclipse-temurin-17`
  - Runtime stage: `eclipse-temurin:17-alpine`
- Non-root user eklendi (`appuser`).
- `app.jar` runtime imaja kopyalanip calistiriliyor.

### Neden
- Daha guvenli, daha tasinabilir, daha temiz deployment.
- Build bagimliliklari runtime imajina tasinmiyor.

## 3.2 docker-compose eklendi
Dosya: `docker-compose.yml`

### Once
- Uygulama + PostgreSQL tek komutta ayaga kalkmiyordu.

### Sonra
- `postgres` ve `app` servisleri tanimlandi.
- PostgreSQL init script ile `schema.sql` calistiriliyor.
- `depends_on` + healthcheck ile dogru baslama sirasi saglandi.
- Environment ile Spring datasource ayarlari disardan verildi.
- `SPRING_SQL_INIT_MODE=never` ile schema iki kez calistirma riski engellendi.

### Neden
- Lokal ve ekip ortami tutarli olsun.
- Tek komutla sistem ayaga kalksin.

## 3.3 .dockerignore eklendi
Dosya: `.dockerignore`

### Once
- Build context gereksiz dosyalari da tasiyabilirdi.

### Sonra
- `target`, `.git`, `.idea`, log vb gereksiz dosyalar dislandi.

### Neden
- Build hizi ve image temizligi.

---

## 4) Stok API ve Is Kurali Degisiklikleri

## 4.1 Add / Remove / Update ayrimi netlestirildi
Dosyalar:
- `src/main/java/com/musyan/stok/controller/IStockController.java`
- `src/main/java/com/musyan/stok/controller/impl/StockControllerImpl.java`
- `src/main/java/com/musyan/stok/service/StockService.java`
- `src/main/java/com/musyan/stok/dto/StockRemoveDto.java`

### Once
- Add vardi.
- Update vardi.
- Remove yoktu.

### Sonra
- `POST /api/stocks/{productCode}/add` -> ustune ekler
- `POST /api/stocks/{productCode}/remove` -> stoktan dusurur
- `PUT /api/stocks/{productCode}` -> hedef degeri set eder

### Neden
- Is mantigini netlestirmek.
- Kullanici beklentisi ile API davranisini birebir eslestirmek.

---

## 5) FIFO Lot Mimarisi

## 5.1 StockTransaction modeli genisletildi
Dosya: `src/main/java/com/musyan/stok/entity/StockTransaction.java`
Yeni enum: `src/main/java/com/musyan/stok/entity/StockTransactionType.java`

### Once
- Stock transaction sadece quantity/unitPrice/unitCost tutuyordu.
- Lot bazinda ne kadarin kaldigi izlenemiyordu.

### Sonra
- `remainingQuantity` eklendi.
- `transactionType` eklendi (`IN`, `OUT`).

### Neden
- FIFO yapabilmek icin her giris lotunun kalan miktari bilinmeli.
- Cikis hareketi ile giris hareketi tipi ayrisabilmeli.

## 5.2 FIFO sorgu metodu eklendi
Dosya: `src/main/java/com/musyan/stok/repository/StockTransactionRepository.java`

### Eklenen metod
- `findByStockStockIdAndTransactionTypeAndRemainingQuantityGreaterThanOrderByTransactionDateAscTransactionIdAsc(...)`

### Neden
- FIFO icin "en eski aktif lotlari" sirali almak gerekir.

---

## 6) Remove Islemi Artık FIFO

Dosya: `src/main/java/com/musyan/stok/service/StockService.java`

### Once
- Remove islemi yoktu.

### Sonra
- Remove eklendi ve FIFO calisiyor:
  1. Stok kaydi bulunur.
  2. Genel quantity kontrol edilir.
  3. FIFO lotlar cekilir.
  4. En eski lottan baslanarak parca parca dusulur.
  5. Lotlarin `remainingQuantity` alanlari guncellenir.
  6. OUT transaction kaydi olusur.

### Neden
- Gercek hayatta stok cikisi lot bazli izlenmeli.
- Maliyet takibi historical olarak dogru kalmali.

---

## 7) Add ve Update Davranislarinin Yeni Kurallara Uyumlanmasi

Dosya: `src/main/java/com/musyan/stok/service/StockService.java`

## 7.1 Add
### Once
- Add aninda ortalama maliyet senkron hesaplanip Product.unitCost set ediliyordu.

### Sonra
- Add, IN lot transaction olusturuyor (`remainingQuantity = quantity`).
- Ana stok quantity artiyor.
- `StockChangedEvent` publish edilerek maliyet hesap asenkron tetikleniyor.

## 7.2 Update
### Once
- Mapper ile quantity/unit/minimum direkt eziliyordu.

### Sonra
- Hedef quantity ile mevcut quantity farki hesaplanir:
  - fark > 0: IN adjust transaction
  - fark < 0: remove akisi ile FIFO dusum
  - fark = 0: sadece alan guncelleme
- Sonunda `StockChangedEvent` publish edilir.

### Neden
- Update islemi lot muhasebesini bozmadan miktar ayari yapabilsin.

---

## 8) Order (Satis) Altyapisi

## 8.1 Yeni API
Dosyalar:
- `src/main/java/com/musyan/stok/controller/IOrderController.java`
- `src/main/java/com/musyan/stok/controller/impl/OrderControllerImpl.java`
- `src/main/java/com/musyan/stok/dto/OrderRequestDto.java`
- `src/main/java/com/musyan/stok/dto/OrderResponseDto.java`

Endpoint:
- `POST /api/orders`

### Neden
- Satis islemini stoktan ayri, is kurali net bir servis olarak modellemek.

## 8.2 Yeni Entity ve Repository
Dosyalar:
- `src/main/java/com/musyan/stok/entity/SaleOrder.java`
- `src/main/java/com/musyan/stok/entity/SaleOrderAllocation.java`
- `src/main/java/com/musyan/stok/repository/SaleOrderRepository.java`
- `src/main/java/com/musyan/stok/repository/SaleOrderAllocationRepository.java`

### Mantik
- `SaleOrder`: satisin ust kaydi
- `SaleOrderAllocation`: bu satisin hangi lotlardan, ne kadar tukettigi

### Neden
- "Hangi stok kaydindan dustu" gereksinimi dogrudan karsilansin.
- Sonradan muhasebe/raporlamada izlenebilirlik olsun.

## 8.3 OrderService FIFO akis
Dosya: `src/main/java/com/musyan/stok/service/OrderService.java`

### Akis
1. Urun stok kaydi bulunur.
2. Yeterli stok kontrol edilir.
3. FIFO lotlar cekilir.
4. Satis miktari lotlara dagitilir.
5. Her dagitim icin allocation kaydi olusturulur.
6. OUT transaction olusturulur.
7. Stok quantity dusurulur.
8. `StockChangedEvent` publish edilir.

### Cikti
- `orderCode`, `totalAmount`, `totalCost`, `grossProfit` doner.

---

## 9) Ortalama Maliyet Hesabi Artik Asenkron

Dosyalar:
- `src/main/java/com/musyan/stok/StokApplication.java`
- `src/main/java/com/musyan/stok/event/StockChangedEvent.java`
- `src/main/java/com/musyan/stok/service/StockCostAsyncService.java`

## 9.1 Teknik degisiklik
- `@EnableAsync` eklendi.
- Stock degisimi oldugunda event publish ediliyor.
- Listener `@Async` olarak calisiyor.

## 9.2 Hesap kurali
- Sadece aktif lotlar (`IN` ve `remainingQuantity > 0`) hesaba katiliyor.
- Formul:

Ortalama Maliyet = (kalan lotlarin toplam maliyeti) / (toplam kalan miktar)

### Once
- Add aninda senkron, toplam eski+ yeni ortalama.

### Sonra
- Add/remove/order/update fark etmeksizin stock degisimi event uretir.
- Hesap tek merkezde asenkron yapilir.

### Neden
- Is mantigini merkezilestrmek.
- Servis akislarini hafifletmek.
- Event-driven genisleme imkani.

---

## 10) Maliyet Artisi Hook (Extensible tasarim)

Dosyalar:
- `src/main/java/com/musyan/stok/event/CostIncreasedEvent.java`
- `src/main/java/com/musyan/stok/service/CostIncreaseHandler.java`
- `src/main/java/com/musyan/stok/service/NoOpCostIncreaseHandler.java`

### Once
- Ortalama maliyet artinca bagli aksiyon mekanizmasi yoktu.

### Sonra
- `CostIncreaseHandler` arayuzu eklendi.
- Simdilik `NoOp` implementasyon var.
- Maliyet artarsa event publish ediliyor.

### Neden
- Simdi zorunlu degil ama ileride fiyat guncelleme/workflow kolay eklensin.

---

## 11) SQL Schema Degisiklikleri

Dosya: `src/main/resources/schema.sql`

### stock_transactions tablosu
- `remaining_quantity` eklendi.
- `transaction_type` eklendi.

### Yeni tablolar
- `sale_orders`
- `sale_order_allocations`

### Neden
- FIFO lot takibi ve order allocation izleme veritabanina kalici yazilsin.

Onemli not:
- Eski Docker volume kullaniliyorsa yeni kolonlar gorunmeyebilir.
- Temiz schema icin `docker-compose down -v` sonra `docker-compose up --build` gerekir.

---

## 11.1) Tablo Yapisi ve Sutun Aciklamalari

### products

| Sutun | Tip | Aciklama |
|---|---|---|
| `product_id` | BIGSERIAL PK | Her urunun benzersiz sayisal kimlik numarasi. |
| `product_code` | VARCHAR(50) UNIQUE | Dis dunyaya acilan urun kodu (orn. "PRD-001"). API'de birincil arama anahtari olarak kullanilir. |
| `product_name` | VARCHAR(150) | Urunu tarif eden insan okunur isim. |
| `category` | VARCHAR(100) | Urunu gruplayan kategori bilgisi. Filtreleme ve raporlama icin kullanilir. |
| `description` | VARCHAR(500) | Opsiyonel ek aciklama. |
| `unit_cost` | NUMERIC(12,2) | Urununun guncel ortalama lot maliyeti. Her stok hareketi sonrasi `StockCostAsyncService` tarafindan asenkron guncellenir. |
| `active` | BOOLEAN | Urununun aktif olup olmadigi. Pasif urunlere yeni stok girisi veya satis yapilmaz. |
| `created_at/by` | TIMESTAMP/VARCHAR | Kaydin kim tarafindan ne zaman olusturuldugu (audit). |
| `updated_at/by` | TIMESTAMP/VARCHAR | Kaydin son kim tarafindan ne zaman guncellendigi (audit). |

---

### stocks

| Sutun | Tip | Aciklama |
|---|---|---|
| `stock_id` | BIGSERIAL PK | Stok satirinin benzersiz kimlik numarasi. |
| `product_id` | BIGINT UNIQUE FK | Hangi urune ait oldugu. UNIQUE kisiti urun-stok iliskisini bire-bir tutar. |
| `quantity` | INTEGER | Anliktaki toplam stok miktari. Her IN/OUT hareketinde servis tarafindan guncellenir. |
| `unit` | VARCHAR(30) | Stok birimi (orn. "adet", "kg", "litre"). Miktarin ne ile olculdugunu belirtir. |
| `minimum_stock_level` | INTEGER | Minimum stok esigi. Bu degerin altina dustugunda uyari tetiklenebilir. |
| `created_at/by` | TIMESTAMP/VARCHAR | Audit. |
| `updated_at/by` | TIMESTAMP/VARCHAR | Audit. |

---

### stock_transactions

Her stok IN veya OUT hareketi bu tabloya bir satir olarak yazilir. FIFO hesabinin ana verisidir.

| Sutun | Tip | Aciklama |
|---|---|---|
| `transaction_id` | BIGSERIAL PK | Hareketin benzersiz kimlik numarasi. FIFO siralama icin `transaction_date` ile birlikte kullanilir. |
| `stock_id` | BIGINT FK | Hangi stok satirina ait oldugu. |
| `quantity` | INTEGER | Hareket miktari. IN icin pozitif, OUT icin negatif deger yazilir. |
| `unit_price` | NUMERIC(12,2) | Hareket anindaki satis fiyati. OUT hareketinde satildigi fiyat; IN hareketinde 0 olabilir. |
| `unit_cost` | NUMERIC(12,2) | Hareket anindaki birim maliyet. IN lotunun gercek alis maliyeti; OUT icin FIFO tuketiminden hesaplanan ortalama maliyet. |
| `remaining_quantity` | INTEGER | Bu IN lotundan hala kac adet tuketilmemis oldugu. FIFO ile dusum yapildikca azalir; 0 olunca lot tamamen tuketilmis demektir. |
| `transaction_type` | VARCHAR(10) | `IN` (stok girisi) veya `OUT` (stok cikisi/satis). FIFO sorgusu sadece `IN` tipindeki aktif lotlari ceker. |
| `transaction_date` | TIMESTAMP | Hareketin gerceklesme zamani. FIFO siralamasinda birincil kriter ("en eski lot once"). |
| `created_at/by` | TIMESTAMP/VARCHAR | Audit. |
| `updated_at/by` | TIMESTAMP/VARCHAR | Audit. |

---

### sale_orders

Bir satis isleminin ust kaydidir. Kac adet, kac paradan satildi, maliyeti ne, kar ne kadar sorularinin cevabidir.

| Sutun | Tip | Aciklama |
|---|---|---|
| `order_id` | BIGSERIAL PK | Satisin benzersiz kimlik numarasi. |
| `order_code` | VARCHAR(40) UNIQUE | Insan okunur satis kodu ("ORD-XXXXXXXX"). UUID'den uretilir. |
| `product_id` | BIGINT FK | Hangi urun satildi. |
| `quantity` | INTEGER | Satilan toplam miktar. |
| `unit_price` | NUMERIC(12,2) | Satis sirasinda belirlenen birim satis fiyati. |
| `total_amount` | NUMERIC(12,2) | Toplam satis tutari (`quantity * unit_price`). |
| `total_cost` | NUMERIC(12,2) | FIFO ile hesaplanan satilan miktarin toplam maliyeti. Brut kar hesabinda kullanilir. |
| `gross_profit` | NUMERIC(12,2) | Ham kar (`total_amount - total_cost`). Vergisiz, genel gider haric. |
| `order_date` | TIMESTAMP | Satisin gerceklestigi zaman. |
| `created_at/by` | TIMESTAMP/VARCHAR | Audit. |
| `updated_at/by` | TIMESTAMP/VARCHAR | Audit. |

---

### sale_order_allocations

Bir satisin hangi stok lotlarindan kac adet tukettigini kayit altina alir. "Bu satiste hangi lot kullanildi?" sorusunun cevabidir.

| Sutun | Tip | Aciklama |
|---|---|---|
| `allocation_id` | BIGSERIAL PK | Dagitim satirinin benzersiz kimlik numarasi. |
| `order_id` | BIGINT FK | Hangi satisa ait oldugu. |
| `source_transaction_id` | BIGINT FK | Tuketilen stok lotunun `stock_transactions` kaydina referans. Hangi lottan tuketime gidildigi buradan izlenir. |
| `quantity` | INTEGER | Bu lottan kac adet tuketildi. |
| `unit_cost` | NUMERIC(12,2) | Tuketilen lotun birim maliyeti (lotun `unit_cost` degerinden kopyalanir). |
| `line_cost` | NUMERIC(12,2) | Bu lottan tuketimin toplam maliyeti (`quantity * unit_cost`). `sale_orders.total_cost` bu satirlarin toplamidir. |
| `created_at/by` | TIMESTAMP/VARCHAR | Audit. |
| `updated_at/by` | TIMESTAMP/VARCHAR | Audit. |

---

## 12) Race Condition Giderildi + Urun Varligi Kontrolu Netlesti

### 12.1 Sorunlar
- `addStockTransaction`, `removeStockTransaction`, `updateStock` ve `createOrder` metodlari
  ayni stok satirini read-modify-write akisiyla guncelliyordu.
- Esit zamanda gelen iki istek ayni `quantity`'i okuyabilir, biri digerinin yazisini ezerdi (lost update).
- `addStockTransaction` basarili olurken arka planda urun yoksa "Stock not found" hatasi donuyordu;
  asil sorun "Product not found" oldugu halde mesaj yanliticiydi.

### 12.2 Onceki Durum
- Tum yazma metodlari `findByProductProductCode(...)` kullaniyordu (lock yok).
- `StockRepository`'de pessimistic lock destegi yoktu.
- `addStockTransaction`'da urun varligi kontrol edilmiyordu.
- `@Transactional` + varsayilan `READ_COMMITTED` izolasyonu lost update'i engellemiyordu.

### 12.3 Yapilan Degisiklikler

#### `StockRepository`
Dosya: `src/main/java/com/musyan/stok/repository/StockRepository.java`

- `findByProductProductCodeWithLock` metodu eklendi:
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM Stock s WHERE s.product.productCode = :productCode")
  Optional<Stock> findByProductProductCodeWithLock(@Param("productCode") String productCode);
  ```
- Okuma amaclı `fetchStockByProductCode` için eski lock'suz metod korundu,
  gereksiz kilit almamak ve performansi korumak icin.

#### `StockService`
Dosya: `src/main/java/com/musyan/stok/service/StockService.java`

- `ProductRepository` bagımliligi eklendi.
- `addStockTransaction`:
  - Stoku kilitlemeden once `productRepository.existsByProductCode(productCode)` kontrolu yapiliyor.
  - Urun yoksa `ResourceNotFoundException("Product", ...)` firlatiliyor (onceden "Stock not found" geliyordu).
  - Stok sorgusu `findByProductProductCodeWithLock` ile yapiliyor.
- `removeStockTransaction`: Stok sorgusu `findByProductProductCodeWithLock` ile yapiliyor.
- `updateStock`: Stok sorgusu `findByProductProductCodeWithLock` ile yapiliyor.

#### `OrderService`
Dosya: `src/main/java/com/musyan/stok/service/OrderService.java`

- `createOrder`: Stok sorgusu `findByProductProductCodeWithLock` ile yapiliyor.
- Esit zamanda gelen iki satis istegi artik ayni stok satirini esit zamanda degistiremez.

### 12.4 Sonuc
- Tum yazma islemleri (`add`, `remove`, `update`, `createOrder`) veritabani seviyesinde `SELECT ... FOR UPDATE` kullanir.
- Lost update riski ortadan kaldirildi.
- Urun bulunamadigi durumda mesaj artik dogruca "Product not found" olarak donuyor.
- Okuma islemi (`fetchStockByProductCode`) lock almadan devam ediyor.

---

## 12) Postman Test Paketi

Dosya: `postman_stok_fifo_collection.json`

### Icerik
- Login
- Product create
- Iki farkli lot add
- FIFO remove
- Order create
- Son stok ve urun maliyet kontrolu

### Neden
- Is kurallarini hizli dogrulamak.
- Manuel test adimlarini standardize etmek.

---

## 13) Kucuk Temizlikler

- `ProductService` icinde kullanilmayan import kaldirildi.
- Bazı gecici/not yorumlari kaldirildi veya etkisiz hale geldi.

---

## 14) Gereksinim - Karsilama Matrisi

1. Order islemi eklensin
- Karsilandi: `OrderService`, `IOrderController`, `OrderControllerImpl`, DTO/Entity/Repository

2. Satis stoktan parca parca dussun
- Karsilandi: FIFO lot tuketim akisi

3. Hangi stok kaydindan dustugu gorulsun
- Karsilandi: `SaleOrderAllocation.sourceTransaction`

4. Ayni urunde cok lot varsa dagitilsin
- Karsilandi: allocation listesi

5. FIFO olsun
- Karsilandi: transaction date + id sirali lot tuketim

6. Stok bazli unitCost historical korunsun
- Karsilandi: lot bazli `unitCost` ve allocation line cost

7. Ortalama maliyet sadece kalan stoktan hesaplansin
- Karsilandi: `remainingQuantity > 0` lotlar uzerinden hesap

8. Maliyet artisinda extensible yapi olsun
- Karsilandi: `CostIncreaseHandler` hook + event

9. Ortalama maliyet asenkron olsun
- Karsilandi: `@EnableAsync`, `StockChangedEvent`, `StockCostAsyncService`

10. Mevcut yapıyı bozmadan ilgili katmanlar eklensin
- Karsilandi: controller/service/repository/entity seviyesinde lokal genisleme, tum sistemi yeniden yazma yok

---

## 15) StockRepository Tasarim Notu Cozuldu

Dosya: `src/main/java/com/musyan/stok/repository/StockRepository.java`

### Gecmisteki Not
Repository dosyasinin en altinda su yorum bulunuyordu:
```
// error exception firlatmali burda cunku ilerde liste olabilir
```

### Neyi Kastediyordu
- Urun-stok iliskisi ileride bire-cok (bir urune birden fazla stok yeri) olursa
  `Optional<Stock>` donus tipi yetersiz kalir; `List<Stock>` gerekmesi durumunda
  metot imzasinin degismesi lazim olacakti.
- Yorum, bu riski ve exception duzeni eksikligini hatirlatmak icin birakilmisti.

### Neden Meselenin Cozumu Repository'e Exception Eklemek Degil

Spring Data JPA'da repository katmaninin gorevi sadece veriyi getirmektir.
Is kurali (exception firlatma, hata mesaji uretme) servis katmanina aittir.
Repository'e `orElseThrow` eklemek sorumluluk karisikligina yol acar.

### Dogru Pattern Nedir
Serviste `orElseThrow` kullanilir:
```java
// StockService - addStockTransaction
Stock existingStock = stockRepository.findByProductProductCodeWithLock(productCode)
        .orElseThrow(() -> new ResourceNotFoundException("Stock", "productCode", productCode));
```
Bu pattern:
- Repository'yi saf veri erisim katmani olarak tutar.
- Hata mesajini ve tipini servis belirler (is kurali).
- Test edilebilirlik ve katman ayrimi korunur.

### Urun Kontrol Eklenmesi
`addStockTransaction` metoduna `productRepository.existsByProductCode()` kontrolu eklenerek
asil bir kaynagin bulunamadigi da netlestirildi:
- Urun yoksa  → `ResourceNotFoundException("Product", ...)`
- Stok kaydi yoksa → `ResourceNotFoundException("Stock", ...)`

Onceden her iki durumda da "Stock not found" donuyordu; artik hata mesaji dogrudan anlamli.

### Sonuc
- Yorum amacina ulasti ve kaldirildi.
- Exception akisi servis katmaninda, dogru mesajlarla calisir halde.
- Repository temiz ve tek sorumluluklu kaldi.

---

## 16) Sonuc

Bu tur sonunda proje:
- Docker ile tasinabilir ve tek komutla calisabilir hale geldi.
- Stok hareketi is kurali olarak olgunlasti.
- FIFO + order + lot allocation + historical cost + async average cost yapisi eklendi.
- Gelecekte fiyat guncelleme gibi senaryolara acik bir extension noktasi olustu.
- Race condition ve kayip guncelleme riski pessimistic lock ile giderildi.
- Urun/stok bulunamadi hatalari katman ayirimi gozetilerek dogru mesajlarla donecek hale getirildi.

Kisaca:
- Once: basit stok artir/azalt
- Simdi: lot bazli izlenebilir, maliyet muhasebesi daha dogru, satis odakli, guvenli ve genisletilebilir stok sistemi
