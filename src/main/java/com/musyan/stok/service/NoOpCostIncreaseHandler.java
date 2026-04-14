package com.musyan.stok.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NoOpCostIncreaseHandler implements CostIncreaseHandler {

    @Override
    public void onCostIncreased(String productCode, BigDecimal oldAverageCost, BigDecimal newAverageCost) {
        // Extension hook: integrate price-update workflow here when needed.
    }
}
