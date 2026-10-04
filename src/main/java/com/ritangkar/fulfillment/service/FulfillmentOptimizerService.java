package com.ritangkar.fulfillment.service;

import com.ritangkar.fulfillment.model.*;
import com.ritangkar.fulfillment.repository.NetworkDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Determines the lowest-cost fulfillment plan for an order:
 * <ol>
 *   <li>Try every warehouse that can fill the <b>entire</b> order alone;
 *       pick the lowest-scoring one (cost + an SLA-miss penalty).</li>
 *   <li>If none can, split the order: assign each item independently to
 *       the nearest warehouse that has enough stock of just that item,
 *       consolidate items going to the same warehouse into one shipment.</li>
 *   <li>If some item has no warehouse with enough stock at all, the order
 *       is INFEASIBLE — reported explicitly, never silently dropped.</li>
 * </ol>
 * Real geography (Haversine distance), a deterministic, documented cost
 * and delivery-time model — see docs/design-decisions.md for every constant.
 */
@Service
public class FulfillmentOptimizerService {

    private static final double BASE_SHIPPING_COST = 50.0;
    private static final double PER_KM_COST = 0.05;
    private static final double PER_KG_COST = 8.0;
    private static final double AVG_KM_PER_DAY = 500.0;
    private static final int PROCESSING_DAYS = 1;
    private static final double SLA_PENALTY_PER_DAY = 100.0;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final NetworkDataRepository repository;

    public FulfillmentOptimizerService(NetworkDataRepository repository) {
        this.repository = repository;
    }

    public FulfillmentPlan plan(FulfillmentRequest request) {
        CityLocation customer = repository.findCityByName(request.customerCity())
                .orElseThrow(() -> new NoSuchElementException("Unknown city: " + request.customerCity()));

        List<WarehouseCandidate> singleWarehouseCandidates = evaluateSingleWarehouseCandidates(request, customer);
        if (!singleWarehouseCandidates.isEmpty()) {
            WarehouseCandidate best = singleWarehouseCandidates.get(0);
            List<String> rationale = List.of(
                    best.warehouseName() + " has every item in stock and offers the lowest cost+SLA score ("
                            + round2(best.score()) + ") among " + singleWarehouseCandidates.size()
                            + " warehouse(s) that could fulfill the whole order alone.",
                    String.format("Distance %.1f km \u2192 %d day(s) delivery, estimated cost \u20B9%.2f.",
                            best.distanceKm(), best.deliveryDays(), best.cost())
            );
            return new FulfillmentPlan("SINGLE_WAREHOUSE", round2(best.cost()), best.deliveryDays(),
                    best.slaMet() ? "MET" : "MISSED", best, singleWarehouseCandidates, null, null, rationale);
        }

        return planSplitFulfillment(request, customer);
    }

    private List<WarehouseCandidate> evaluateSingleWarehouseCandidates(FulfillmentRequest request, CityLocation customer) {
        List<WarehouseCandidate> candidates = new ArrayList<>();
        for (Warehouse w : repository.findAllWarehouses()) {
            boolean feasible = request.items().stream()
                    .allMatch(item -> repository.stockOf(w.id(), item.productId()) >= item.quantity());
            if (!feasible) continue;

            double distance = haversineKm(customer.lat(), customer.lon(), w.lat(), w.lon());
            double weight = totalWeight(request.items());
            double cost = shippingCost(distance, weight);
            int days = deliveryDays(distance);
            boolean slaMet = days <= request.requestedSlaDays();
            double penalty = slaMet ? 0 : SLA_PENALTY_PER_DAY * (days - request.requestedSlaDays());
            double score = cost + penalty;

            candidates.add(new WarehouseCandidate(w.id(), w.name(), round1(distance), round2(cost), days, slaMet, round2(score)));
        }
        candidates.sort(Comparator.comparingDouble(WarehouseCandidate::score));
        return candidates;
    }

    private FulfillmentPlan planSplitFulfillment(FulfillmentRequest request, CityLocation customer) {
        Map<String, List<OrderItem>> assignmentsByWarehouse = new LinkedHashMap<>();
        List<String> infeasibleItems = new ArrayList<>();

        for (OrderItem item : request.items()) {
            Warehouse best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Warehouse w : repository.findAllWarehouses()) {
                if (repository.stockOf(w.id(), item.productId()) < item.quantity()) continue;
                double distance = haversineKm(customer.lat(), customer.lon(), w.lat(), w.lon());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = w;
                }
            }
            if (best == null) {
                infeasibleItems.add(item.productId());
            } else {
                assignmentsByWarehouse.computeIfAbsent(best.id(), k -> new ArrayList<>()).add(item);
            }
        }

        if (!infeasibleItems.isEmpty()) {
            List<String> rationale = List.of(
                    "No single warehouse could supply the full order, and the following item(s) have "
                            + "insufficient stock at every warehouse to fulfill even individually: " + String.join(", ", infeasibleItems) + ".");
            return new FulfillmentPlan("INFEASIBLE", null, null, null, null, null, null, infeasibleItems, rationale);
        }

        List<Shipment> shipments = new ArrayList<>();
        for (var entry : assignmentsByWarehouse.entrySet()) {
            Warehouse w = repository.findAllWarehouses().stream()
                    .filter(x -> x.id().equals(entry.getKey())).findFirst().orElseThrow();
            double distance = haversineKm(customer.lat(), customer.lon(), w.lat(), w.lon());
            double weight = totalWeight(entry.getValue());
            double cost = shippingCost(distance, weight);
            int days = deliveryDays(distance);
            shipments.add(new Shipment(w.id(), w.name(), entry.getValue(), round1(distance), round2(cost), days));
        }

        double totalCost = shipments.stream().mapToDouble(Shipment::cost).sum();
        int overallDays = shipments.stream().mapToInt(Shipment::deliveryDays).max().orElse(0);
        String slaStatus = overallDays <= request.requestedSlaDays() ? "MET" : "MISSED";

        List<String> rationale = new ArrayList<>();
        rationale.add("No single warehouse had every item in stock, so the order was split across "
                + shipments.size() + " warehouse(s), each assigned the nearest warehouse that could supply that item.");
        rationale.add("The customer receives the full order only once every shipment arrives, so the overall "
                + "delivery estimate is the slowest of the " + shipments.size() + " shipment(s): " + overallDays + " day(s).");

        return new FulfillmentPlan("SPLIT", round2(totalCost), overallDays, slaStatus, null, null, shipments, null, rationale);
    }

    private double totalWeight(List<OrderItem> items) {
        return items.stream()
                .mapToDouble(item -> repository.findProductById(item.productId())
                        .map(p -> p.weightKg() * item.quantity())
                        .orElse(0.0))
                .sum();
    }

    private double shippingCost(double distanceKm, double weightKg) {
        return BASE_SHIPPING_COST + PER_KM_COST * distanceKm + PER_KG_COST * weightKg;
    }

    private int deliveryDays(double distanceKm) {
        return PROCESSING_DAYS + (int) Math.ceil(distanceKm / AVG_KM_PER_DAY);
    }

    /** Great-circle distance between two lat/lon points, in kilometers. */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
