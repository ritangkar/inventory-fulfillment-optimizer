package com.ritangkar.fulfillment.model;

public record WarehouseCandidate(
        String warehouseId,
        String warehouseName,
        double distanceKm,
        double cost,
        int deliveryDays,
        boolean slaMet,
        double score
) {
}
