package com.musyan.stok.job;

import com.musyan.stok.entity.Product;
import com.musyan.stok.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LowStockAlertJob {

    private final ProductRepository productRepository;
    private final JavaMailSender mailSender;

    @Value("${app.alert.mail.to}")
    private String alertMailTo;

    @Scheduled(cron = "${app.alert.mail.cron}")
    public void checkLowStockLevels() {
        log.info("Low stock check started at {}", LocalDateTime.now());

        List<Product> lowStockProducts = productRepository.findProductsBelowMinStockLevel();

        if (lowStockProducts.isEmpty()) {
            log.info("Low stock check completed: all products above minimum levels.");
            return;
        }

        log.warn("Low stock alert: {} product(s) below minimum level.", lowStockProducts.size());
        sendAlertMail(lowStockProducts);
    }

    private void sendAlertMail(List<Product> products) {
        String subject = "[STOK UYARISI] " + products.size() + " ürün minimum stok seviyesinin altında";

        StringBuilder body = new StringBuilder();
        body.append("Tarih: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))).append("\n\n");
        body.append("Aşağıdaki ürünler minimum stok seviyesinin altına düşmüştür:\n\n");
        body.append(String.format("%-15s %-30s %10s %10s\n", "Ürün Kodu", "Ürün Adı", "Mevcut", "Minimum"));
        body.append("-".repeat(70)).append("\n");

        for (Product p : products) {
            body.append(String.format("%-15s %-30s %10d %10d\n",
                    p.getProductCode(),
                    p.getProductName(),
                    p.getQuantity(),
                    p.getMinStockLevel()));
        }

        body.append("\nLütfen stok ikmali yapınız.");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(alertMailTo);
        message.setSubject(subject);
        message.setText(body.toString());

        try {
            mailSender.send(message);
            log.info("Low stock alert mail sent to {}", alertMailTo);
        } catch (Exception e) {
            log.error("Failed to send low stock alert mail: {}", e.getMessage());
        }
    }
}
