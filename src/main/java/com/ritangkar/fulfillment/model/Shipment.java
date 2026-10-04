package com.ritangkar.fulfillment.model;

import java.util.List;

public record Shipment(
        String warehouseId,
        String warehouseName,
        List<OrderItem> items,
        double distanceKm,
        double cost,
        int deliveryDays
) {
}
