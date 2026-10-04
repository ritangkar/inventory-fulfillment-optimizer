package com.ritangkar.fulfillment.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ritangkar.fulfillment.model.CityLocation;
import com.ritangkar.fulfillment.model.InventoryRecord;
import com.ritangkar.fulfillment.model.Product;
import com.ritangkar.fulfillment.model.Warehouse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class NetworkDataRepository {

    private final List<Warehouse> warehouses;
    private final List<Product> products;
    private final Map<String, Product> productsById;
    private final Map<String, CityLocation> citiesByName;
    private final Map<String, Integer> stockByWarehouseAndProduct;
    private final List<CityLocation> allCities;

    public NetworkDataRepository() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.warehouses = readList(mapper, "sample-data/warehouses.json", Warehouse[].class);
            this.products = readList(mapper, "sample-data/products.json", Product[].class);
            List<InventoryRecord> inventory = readList(mapper, "sample-data/inventory.json", InventoryRecord[].class);
            List<CityLocation> cities = readList(mapper, "sample-data/cities.json", CityLocation[].class);

            Map<String, Product> pById = new HashMap<>();
            for (Product p : products) pById.put(p.id(), p);
            this.productsById = pById;

            Map<String, CityLocation> cByName = new HashMap<>();
            for (CityLocation c : cities) cByName.put(c.city(), c);
            this.citiesByName = cByName;
            this.allCities = cities;

            Map<String, Integer> stock = new HashMap<>();
            for (InventoryRecord r : inventory) stock.put(stockKey(r.warehouseId(), r.productId()), r.unitsAvailable());
            this.stockByWarehouseAndProduct = stock;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled network data", e);
        }
    }

    private <T> List<T> readList(ObjectMapper mapper, String classpathLocation, Class<T[]> arrayType) throws IOException {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return List.of(mapper.readValue(in, arrayType));
        }
    }

    public List<Warehouse> findAllWarehouses() { return warehouses; }
    public List<Product> findAllProducts() { return products; }

    public Optional<Product> findProductById(String id) { return Optional.ofNullable(productsById.get(id)); }
    public Optional<CityLocation> findCityByName(String city) { return Optional.ofNullable(citiesByName.get(city)); }
    public List<CityLocation> findAllCities() { return allCities; }

    public int stockOf(String warehouseId, String productId) {
        return stockByWarehouseAndProduct.getOrDefault(stockKey(warehouseId, productId), 0);
    }

    private String stockKey(String warehouseId, String productId) {
        return warehouseId + "::" + productId;
    }
}
