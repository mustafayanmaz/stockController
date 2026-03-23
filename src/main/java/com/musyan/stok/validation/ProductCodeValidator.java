package com.musyan.stok.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class ProductCodeValidator implements ConstraintValidator<ValidProductCode, String> {

    // Sadece büyük harf, rakam ve tire karakterlerine izin veren Regex
    private static final String PRODUCT_CODE_PATTERN = "^[A-Z0-9-]+$";

    // Sistemde kullanılmasını istemediğimiz yasaklı kelimeler
    private static final List<String> BLACKLIST = Arrays.asList(
            "TEST", "DUMMY", "NULL", "UNDEFINED", "EMPTY", "NONE");

    @Override
    public void initialize(ValidProductCode constraintAnnotation) {
        // Başlangıçta yapılması gereken bir ayar varsa buraya yazılır
    }

    @Override
    public boolean isValid(String productCode, ConstraintValidatorContext context) {

        if (productCode == null || productCode.trim().isEmpty()) {
            return false; // NotBlank zaten yakalayacak ama biz de false dönelim
        }

        // 1. Kural: Regex Format Kontrolü
        if (!productCode.matches(PRODUCT_CODE_PATTERN)) {
            setCustomMessage(context,
                    "Ürün kodu sadece BÜYÜK HARF, RAKAM ve TİRE (-) karakterleri içerebilir. (Örn: LAPTOP-001)");
            return false;
        }

        // 2. Kural: Yasaklı Kelime (Blacklist) Kontrolü
        boolean isBlacklisted = BLACKLIST.stream()
                .anyMatch(blacklistedWord -> productCode.contains(blacklistedWord));

        if (isBlacklisted) {
            setCustomMessage(context, "Ürün kodu yasaklı kelimeleri (TEST, DUMMY, NULL vb.) içeremez.");
            return false;
        }

        // Tüm testleri geçerse true döner
        return true;
    }

    // Dinamik hata mesajı oluşturmak için yardımcı metod
    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }
}
