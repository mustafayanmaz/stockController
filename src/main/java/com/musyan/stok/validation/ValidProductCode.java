package com.musyan.stok.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ProductCodeValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProductCode {

    String message() default "Geçersiz ürün kodu formatı. Sadece büyük harf, rakam ve tire (-) içerebilir ve kara listede (TEST, NULL vb.) olamaz.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
