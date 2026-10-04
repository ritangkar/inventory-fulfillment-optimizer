package com.ritangkar.fulfillment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory & Fulfillment Optimizer.
 *
 * Given an order (customer location + items), determines the lowest-cost
 * fulfillment plan across a synthetic multi-warehouse network: a single
 * warehouse when one can serve the whole order, a cost-minimizing
 * multi-warehouse split when none can. Real geography (Haversine distance
 * over real Indian city coordinates), a deterministic cost/SLA model, no
 * external optimization library — see docs/design-decisions.md for why.
 */
@SpringBootApplication
public class FulfillmentOptimizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FulfillmentOptimizerApplication.class, args);
    }
}
