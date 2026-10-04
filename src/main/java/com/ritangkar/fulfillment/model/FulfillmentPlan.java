package com.ritangkar.fulfillment.model;

import java.util.List;

/**
 * The result of planning one order. {@code strategy} is one of
 * SINGLE_WAREHOUSE, SPLIT, or INFEASIBLE. Depending on the strategy, either
 * {@code chosenWarehouse} + {@code allCandidates} (SINGLE_WAREHOUSE),
 * {@code shipments} (SPLIT), or {@code infeasibleItems} (INFEASIBLE) is
 * populated; the others are null/empty. {@code rationale} always explains
 * the decision in plain language regardless of strategy.
 */
public record FulfillmentPlan(
        String strategy,
        Double totalCost,
        Integer deliveryDays,
        String slaStatus, // MET, MISSED, or null for INFEASIBLE
        WarehouseCandidate chosenWarehouse,
        List<WarehouseCandidate> allCandidates,
        List<Shipment> shipments,
        List<String> infeasibleItems,
        List<String> rationale
) {
}
