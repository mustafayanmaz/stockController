package com.musyan.stok.event;

import java.math.BigDecimal;

public record CostIncreasedEvent(String productCode, BigDecimal oldAverageCost, BigDecimal newAverageCost) {
}
