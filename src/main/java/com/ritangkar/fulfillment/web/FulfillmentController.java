package com.ritangkar.fulfillment.web;

import com.ritangkar.fulfillment.model.CityLocation;
import com.ritangkar.fulfillment.model.FulfillmentPlan;
import com.ritangkar.fulfillment.model.FulfillmentRequest;
import com.ritangkar.fulfillment.model.Product;
import com.ritangkar.fulfillment.model.Warehouse;
import com.ritangkar.fulfillment.repository.NetworkDataRepository;
import com.ritangkar.fulfillment.service.FulfillmentOptimizerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FulfillmentController {

    private final FulfillmentOptimizerService optimizerService;
    private final NetworkDataRepository repository;

    public FulfillmentController(FulfillmentOptimizerService optimizerService, NetworkDataRepository repository) {
        this.optimizerService = optimizerService;
        this.repository = repository;
    }

    /** POST /api/fulfillment/plan — the core endpoint. */
    @PostMapping("/api/fulfillment/plan")
    public FulfillmentPlan plan(@Valid @RequestBody FulfillmentRequest request) {
        return optimizerService.plan(request);
    }

    @GetMapping("/api/warehouses")
    public List<Warehouse> warehouses() {
        return repository.findAllWarehouses();
    }

    @GetMapping("/api/products")
    public List<Product> products() {
        return repository.findAllProducts();
    }

    @GetMapping("/api/cities")
    public List<CityLocation> cities() {
        return repository.findAllCities();
    }
}
