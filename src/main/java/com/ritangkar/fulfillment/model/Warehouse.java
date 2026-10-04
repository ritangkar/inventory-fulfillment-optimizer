package com.ritangkar.fulfillment.model;

public record Warehouse(String id, String name, String city, double lat, double lon, int capacityUnitsPerDay) {
}
