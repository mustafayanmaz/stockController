# Stok Kontrol Mikroservisi (Stock Control Microservice)

Bu proje profesyonel yazılım mimarisi standartlarına uygun olarak refactor (yeniden yapılandırma) edilmiştir. Aşağıda yapılan tüm iyileştirmelerin teknik detayları, neden yapıldıkları ve nerede bulundukları açıklanmıştır.

---

## 🛠️ Yapılan Geliştirmeler ve Mimari Kararlar

### 1. H2 In-Memory DB'den PostgreSQL'e Geçiş
* **Ne Yaptık?** Projenin veritabanı altyapısını sadece geçici hafızada tutan H2'den, dünyanın en güçlü açık kaynaklı ilişkisel veritabanı olan PostgreSQL'e taşıdık.
* **Neden Yaptık?** H2 sadece geliştirme ve eğitim süreçleri için uygundur. Üretim (Production) ortamında kalıcı, güvenli, eşzamanlı bağlantılara uygun ve ilişkisel veri bütünlüğünü %100 destekleyen gerçek bir RDBMS (PostgreSQL) kullanmak profesyonel bir zorunluluktur.
* **Nasıl ve Nerede Yaptık?** `pom.xml` içerisindeki H2 bağımlılığını silip PostgreSQL driver'ını ekledik. `src/main/resources/application.properties` içerisinde `spring.datasource.url`, `username` ve `password` ayarlarını PostgreSQL sunucusuna bağlanacak şekilde güncelledik.

### 2. Hibernate DDL-Auto Yerine `schema.sql` Kullanımı
* **Ne Yaptık?** Hibernate'in tabloları otomatik oluşturma (`ddl-auto=update`) özelliğini kapattık ve veritabanı şemasının kontrolünü `schema.sql` dosyasına verdik.
* **Neden Yaptık?** Kurumsal projelerde veritabanı şemasının ORM aracı tarafından kontrolsüzce/gizlice değiştirilmesi bir güvenlik zafiyetidir ve istenmez. Şema değişikliklerinin izlenebilir olması ve SQL scriptleriyle yönetilmesi (ileride Flyway/Liquibase geçişine hazırlık) en iyi uygulamadır (best-practice).
* **Nasıl ve Nerede Yaptık?** `application.properties` dosyasında `ddl-auto=none` ve `spring.sql.init.mode=always` ayarlarını yaptık. `src/main/resources/schema.sql` dosyasını oluşturup veritabanı tablolarının `CREATE TABLE` komutlarını yazdık.

### 3. Entity ve Tablo Optimizasyonu (Product ve Stock Ayrımı)
* **Ne Yaptık?** Tek bir `Product` tablosunda ve sınıfında tutulan "ürün" ve "stok" bilgilerini iki ayrı tabloya ve Entity'ye böldük. İkisi arasına `product_id` üzerinden `@OneToOne` ilişkisi kurduk.
* **Neden Yaptık?** Veritabanı normalizasyon kuralları gereği, ürünün statik olan meta verileri (isim, kategori, fiyat) ile saniyede onlarca kez değişen stok miktarının aynı tabloda tutulması bir mimari hatadır (Anti-pattern). Bu ayrım veritabanındaki "Lock (Kilitlenme)" mekanizmasını rahatlatır, okuma/yazma performansını ciddi ölçüde artırır.
* **Nasıl ve Nerede Yaptık?** `entity/Product.java` ve yepyeni oluşturduğumuz `entity/Stock.java` sınıflarında ilişkiyi (cascade ve orphanRemoval destekli) tanımladık. Güvenli veri transferi için `dto/StockDto.java` oluşturup `ProductDto` içine nesne olarak bağladık.

### 4. Dinamik Filtreleme (JPA Criteria Query & Specification Pattern)
* **Ne Yaptık?** `ProductSpecification` adında bir sınıf dizayn ederek ürünler üzerinde parametrik ve son derece esnek, dinamik bir filtreleme altyapısı kurduk. Yalnızca stokta olanları (quantity > 0) bulabilmek için iki tabloyu birbirine arka planda `Join` ile bağladık.
* **Neden Yaptık?** Spring Data katmanını geleneksel (sabit) metotlarla doldurursak (`findByCategoryAndPriceGreaterThanAndActive...`) çoklu filtre özelliklerini yönetmek tam bir kâbus olurdu. Specification Pattern ile sadece kullanıcının girdiği alanlara göre anında (runtime) bir SQL sorgusu inşa eden kusursuz ve endüstri standardı bir filtreleme altyapısı sağlamış olduk.
* **Nasıl ve Nerede Yaptık?** 7 farklı kriter barındıran `dto/ProductFilterDto.java` DTO'sunu oluşturduk. `specification/ProductSpecification.java` içinde ürün koduna, fiyata, kategoriye ve mevcut stok durumuna göre (INNER JOIN ile) Dinamik `Predicate`'ler yazdık. Sistemin bu yapısal değişimi anlaması için ise `ProductRepository` arayüzüne (interface) `JpaSpecificationExecutor` donanımını ekledik.

### 5. Service Katmanı Temizliği (Clean Code & Yüklerden Arındırma)
* **Ne Yaptık?** Service katmanında yer alan o eski `IProductService` arayüzünü (interface) ve onun `impl/ProductServiceImpl` sınıfını sildik. Yerine doğrudan `ProductService` ve ekstra olarak `StockService` sınıflarını kullandık.
* **Neden Yaptık?** Projenin gelişme aşamasında, eğer bir servisin birden fazla implementasyonu (farklı çalışma şekli) olmayacaksa, Spring dünyasında Interface ve Implementasyon sınıflarını ayrı ayrı tutmak "Aşırı Mühendislik (Over-engineering)" kabul edilir. Kodu gereksiz dosyalardan temizlemek ve doğrudan amaca hizmet eden temiz sınıflar bırakmak profesyonelliğin göstergesidir.
* **Nasıl ve Nerede Yaptık?** `service/` dizinindeki klasör kalabalığını yok edip iş mantığını doğrudan `@Service` notasyonu alan `ProductService.java` ve `StockService.java` sınıflarında topladık. Ayrıca bu katmandaki veri yazma (INSERT, UPDATE) metotlarına eksik olan `@Transactional` notasyonunu ekleyerek veritabanında işlem tutarlılığını garanti altına aldık (işlem yarıda kesilirse hatasız bir şekilde geri alınabilmesi için).

### 6. Controller Katmanı Dokümantasyon Yönetimi (Interface/Impl Yapısı)
* **Ne Yaptık?** Adım 5'te yaptığımızın tam tersi bir strateji ile bu kez Controller katmanında `IProductController` adında bir arayüz (Interface) tanımlayıp, HTTP API Endpoint'lerindeki iş mantığını `ProductControllerImpl` isimli diğer bir sınıfta çalışacak şekilde böldük.
* **Neden Yaptık?** Swagger/OpenAPI gibi dışa dönük belge üreticilerinin sahip olduğu uzun `@Operation` ve `@Tag` notasyonları asıl kodların arasında inanılmaz bir kalabalık yaratmaktadır ve Controller dosyalarını okumayı imkânsız hale getirmektedir. Bu notasyonları bir Interface'e taşıyarak (Contract-first yaklaşımı) belgeleme ile iş mantığını (Routing mantığı) net çizgilerle ayırdık. Böylece Impl sınıfları tertemiz görünüme kavuştu.
* **Nasıl ve Nerede Yaptık?** `controller/IProductController.java` ve `controller/IStockController.java` arayüzlerinde gerekli olan tüm swagger dokümantasyonunu tasarladık. Yeni oluşturduğumuz `controller/impl/` klasöründe ise bunları `implements` ederek projenin yönlendirme kodlarının (`@GetMapping`, `@PostMapping` vb.) sade ve tutarlı çalışmasını sağladık.

### 7. Custom Validation (Özel Veri Doğrulama Anotasyonu)
* **Ne Yaptık?** Kendi yazdığımız orijinal `@ValidProductCode` adında yeni bir doğrulama (validation) mekanizması ve ona ait bir uyarı anotasyonu icat ettik.
* **Neden Yaptık?** Dışarıdan gelen verinin (kullanıcının gönderdiği datanın) API'mizin diplerine yani Service ve Repository seviyelerine inmeden en üst kapıda (DTO aşamasında) durdurulup kontrol edilmesi, güvenliğin, veri bütünlüğünün ve performansın (Fails Fast) en kritik ilkesidir. Hazır kütüphanelerin sahip olduğu basit `@Size` veya `@NotNull` yetenekleri bize yetmediği için; format gerektiren Regex (sadece büyük harf ve tire olsun) ve Blacklist (Kara liste: TEST, NULL, NONE vb. kelimeler kullanılamasın) kontrolleri için çok daha kapsamlı olan kendi filtremizi uyguladık.
* **Nasıl ve Nerede Yaptık?** `validation/ValidProductCode.java` dosyasıyla kullanılacak kelime/etiket ismini ve hata mesajını tanımladık. `validation/ProductCodeValidator.java` sınıfıyla mekanizmanın sahip olması gereken Regex ve Yasaklı Kelime süzgeçlerini programladık. Son hamle olarak bu yazdığımız yeteneği `dto/ProductDto.java` daki `productCode` alanının üstüne yerleştirerek tüm proje için devreye soktuk.
