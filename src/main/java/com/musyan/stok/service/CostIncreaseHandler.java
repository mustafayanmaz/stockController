package com.musyan.stok.service;

import java.math.BigDecimal;

public interface CostIncreaseHandler {

    void onCostIncreased(String productCode, BigDecimal oldAverageCost, BigDecimal newAverageCost);
}
