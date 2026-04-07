# Stok Kontrol Mikroservisi (Stock Control Microservice) - Güncelleme V2

Bu belgede, stok kontrol sistemine eklenen yeni iş kurallarının, finansal maliyet optimizasyonlarının ve güvenlik önlemlerinin neden ve nasıl yapıldığına dair mimari kararlar açıklanmıştır.

---

## 🛠️ Yapılan Geliştirmeler ve Mimari Kararlar

### 1. Sözleşme Odaklı API Tasarımı (Contract-First Interface Design)
* **Ne Yaptık?** `ProductControllerImpl` ve `StockControllerImpl` içerisindeki tüm HTTP yönlendirme anotasyonlarını (`@PostMapping`, `@GetMapping` vb.) sınıflardan söküp doğrudan `IProductController` ve `IStockController` arayüzlerine (Interface) taşıdık.
* **Neden Yaptık?** Temiz Kod (Clean Code) prensipleri gereği nesnelerin (Implementation) iş mantığı ile adreslerin taslakları (Contract) ayrılmalıdır. Diğer mikroservislerin veya yazılımcıların kodu okurken doğrudan sadece arayüze (Interface) bakarak sistemin bütün adreslerini, yeteneklerini ve Swagger dokümantasyonunu anlayabilmesi, ileride Feign Client yapılarının bu arayüzü doğrudan miras alabilmesi için yaptık.
* **Nasıl ve Nerede Yaptık?** İlgili `Impl` sınıflarından API yapılandırma anotasyonları temizlendi ve `controller/IProductController.java` ile `controller/IStockController.java` dosyalarının içerisindeki metodların üzerine yerleştirildi.

### 2. İş Kuralları ve Model Validasyonu (Business Rules & Unique Constraints)
* **Ne Yaptık?** `Product` tablosunda bulunan ürün isimlerinin (`productName`) tekrar etmesini engelleyen tekillik kuralı (unique constraint) koyduk. Gelen isteklerde isim ve kod uyuşmazlığını süzen servis katmanı denetimleri ekledik.
* **Neden Yaptık?** İstenmeyen çakışmaları ve "Aynı isme sahip farklı kodlu ürünler", "Aynı koda sahip farklı isimli ürünler" yaratılmasını engellemek zorundayız. Hatalı ürün ve stok verilerinin (yanlış isme stok girmek vs.) önüne geçebilmek adına ürün adının da en az ürün kodu kadar benzersiz ve güvenilir olması gerekmektedir.
* **Nasıl ve Nerede Yaptık?** `entity/Product.java` tablosundaki `productName` kolonuna `unique = true` eklendi. `service/ProductService.java` içerisindeki ürün ekleme (`createProduct`) ve güncelleme (`updateProduct`) metodlarına isim tekilliğini ve eşleşmesini denetleyen (çakışma varsa hataya düşen) Business Rule satırları eklendi.

### 3. Dinamik Karlılık/Maliyet Hesaplaması ve Stok Hareketleri
* **Ne Yaptık?** Sadece güncel stok adedini ve limitini tutan yapıyı genişlettik. Artık gerçekleşen her "stok girişi" işlemini, sisteme ne zaman, ne kadara girdiğine dair detaylı olarak kaydeden yepyeni bir "Stok Hareketleri" entegrasyonu kurduk. Yeni stok eklendiğinde elde var olan ürünün "Ağırlıklı Ortalama Maliyeti" anlık olarak hesaplanıyor.
* **Neden Yaptık?** Şirketin kârlılık oranını görebilmesi ve detaylı raporlama yapabilmesi için her bir ürün partisinin stoka hangi maliyet ve ücretle (`unitPrice`/`unitCost`) hangi tarihte girdiği kayıt altına alınmak zorundadır. Eski ürünleri uygun maliyetten, yeni gelenleri daha pahalıdan aldıysak, stokta o ürüne ait toplam maliyetin "Ortalama Birim Maliyeti" bilinmelidir ki şirket reel kâr marjını şeffafça görebilsin.
* **Nasıl ve Nerede Yaptık?** `entity/StockTransaction.java` tablosu/entitisi ve ona ait `StockTransactionDto` (gelişmiş veri doğrulamalı) oluşturuldu. `StockService` içerisine `addStockTransaction` isimli yeni bir Transactional fonksiyon eklendi. Bu yapı `[(Eski Miktar * Eski Maliyet) + (Yeni Miktar * Yeni Maliyet)] / Toplam Adet` formülü ile çalışarak `Product` sınıfındaki `unitCost` değerini anında güncelliyor.

### 4. JWT Tabanlı Mikroservis Güvenliği (Spring Security & Authorization)
* **Ne Yaptık?** Mikroservise şifresiz şekilde herkesin (Postman vb.) istek gönderebilmesini engelledik. Bearer Token tabanlı, süre sınırlı (1 saat ömrü olan) bir "Kurumsal API Güvenlik Duvarı" inşa ettik.
* **Neden Yaptık?** Verilerin güvenliği için endüstri standardı olan Token/Oauth2 sistemi vazgeçilmezdir. Projeyi yetkisiz erişimlere kapatıp ileride "Keycloak" gibi gelişmiş kurumsal kimlik ve erişim yönetimi (IAM) sunucularına da sorunsuz bir şekilde entegre olabilmesi için çok önemli bir temel zemin kurduk.
* **Nasıl ve Nerede Yaptık?** `pom.xml` içerisine `spring-boot-starter-security` ve `jjwt` kütüphanelerini dâhil ettik. Sisteme entegre `security/JwtUtil.java` (token üreten yetenek) ve `security/JwtFilter.java` (gelen talepleri süzüp token geçerliliğini denetleyen katman) sınıflarını meydana getirdik. `SecurityConfig.java` ile oturum doğrulama haricindeki tüm Endpoint'lerin kapılarını kilitledik. Şimdilik manuel admin/admin bilgisiyle test amaçlı token üretebilmeniz için `controller/AuthController.java` içerisinde bir giriş istasyonu açtık.
