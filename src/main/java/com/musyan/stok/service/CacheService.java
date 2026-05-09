package com.musyan.stok.service;

import com.musyan.stok.dto.ProductDto;
import com.musyan.stok.exception.LockNotAcquiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

/**
 * Redis tabanlı ürün cache'i ve dağıtık kilit (distributed lock) servisi.
 *
 * <p>Kilit alınamazsa {@code retryCount} kez {@code retryDelayMs} ms bekleyerek
 * yeniden dener. Tüm denemeler başarısız olursa {@link LockNotAcquiredException} fırlatır.
 *
 * <p>Redis erişilemiyor olsa bile uygulamanın çalışmaya devam etmesi için
 * tüm Redis çağrıları try-catch ile sarılmıştır (cache-aside + fail-open stratejisi).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String PRODUCT_KEY_PREFIX = "product:code:";
    private static final String LOCK_KEY_PREFIX    = "lock:product:";
    private static final Duration CACHE_TTL        = Duration.ofHours(1);
    private static final Duration LOCK_TTL         = Duration.ofSeconds(10);

    @Value("${app.cache.lock.retry-count:3}")
    private int retryCount;

    @Value("${app.cache.lock.retry-delay-ms:100}")
    private long retryDelayMs;

    // ------------------------------------------------------------------ lock

    /**
     * Ürün için distributed lock almayı {@code retryCount} kez dener.
     * Başarısız olursa {@link LockNotAcquiredException} fırlatır.
     * Aktif bir transaction varsa kilit, transaction bitişinde otomatik serbest bırakılır.
     */
    public void acquireLock(String productCode) {
        String key = LOCK_KEY_PREFIX + productCode;

        for (int attempt = 1; attempt <= retryCount; attempt++) {
            try {
                boolean acquired = Boolean.TRUE.equals(
                        stringRedisTemplate.opsForValue().setIfAbsent(key, "LOCKED", LOCK_TTL));

                if (acquired) {
                    log.info("Lock alındı: {} (deneme {})", key, attempt);
                    registerLockRelease(key);
                    return;
                }

                log.warn("Lock alınamadı: {} (deneme {}/{}). {} ms bekleniyor...",
                        key, attempt, retryCount, retryDelayMs);

                if (attempt < retryCount) {
                    Thread.sleep(retryDelayMs);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new LockNotAcquiredException("Lock bekleme sırasında kesinti: " + productCode);
            } catch (Exception e) {
                log.error("Redis lock hatası ({}): {}", key, e.getMessage());
                // Redis erişilemiyor → lock olmadan devam et (fail-open)
                return;
            }
        }

        throw new LockNotAcquiredException(
                "Ürün şu anda başka bir işlem tarafından kullanılıyor, lütfen tekrar deneyin: " + productCode);
    }

    /** Transaction sonunda kilidi serbest bırak. */
    private void registerLockRelease(String key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    try {
                        stringRedisTemplate.delete(key);
                        log.info("Lock serbest bırakıldı (transaction sonrası): {}", key);
                    } catch (Exception e) {
                        log.error("Lock serbest bırakma hatası ({}): {}", key, e.getMessage());
                    }
                }
            });
        } else {
            // Transaction yoksa hemen serbest bırak
            try {
                stringRedisTemplate.delete(key);
                log.info("Lock serbest bırakıldı: {}", key);
            } catch (Exception e) {
                log.error("Lock serbest bırakma hatası ({}): {}", key, e.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------ cache

    /** Cache'ten ürünü döner. Redis erişilemiyor olursa null döner. */
    public ProductDto getProduct(String productCode) {
        String key = PRODUCT_KEY_PREFIX + productCode;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof ProductDto dto) {
                log.debug("Cache HIT: {}", key);
                return dto;
            }
        } catch (Exception e) {
            log.error("Redis okuma hatası ({}): {}", key, e.getMessage());
        }
        return null;
    }

    /** Ürünü cache'e yazar. Redis erişilemiyor olursa sessizce atlanır. */
    public void setProduct(ProductDto dto) {
        if (dto == null || dto.getProductCode() == null) return;
        String key = PRODUCT_KEY_PREFIX + dto.getProductCode();
        try {
            redisTemplate.opsForValue().set(key, dto, CACHE_TTL);
            log.debug("Cache SET: {}", key);
        } catch (Exception e) {
            log.error("Redis yazma hatası ({}): {}", key, e.getMessage());
        }
    }

    /** Ürünü cache'ten siler. Redis erişilemiyor olursa sessizce atlanır. */
    public void evictProduct(String productCode) {
        if (productCode == null) return;
        String key = PRODUCT_KEY_PREFIX + productCode;
        try {
            redisTemplate.delete(key);
            log.debug("Cache EVICT: {}", key);
        } catch (Exception e) {
            log.error("Redis silme hatası ({}): {}", key, e.getMessage());
        }
    }
}
