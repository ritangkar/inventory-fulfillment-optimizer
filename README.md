# Inventory & Fulfillment Optimizer

Determines the lowest-cost, SLA-aware fulfillment plan for an order across
a synthetic multi-warehouse network — a single warehouse when one can
serve the whole order, a cost-minimizing multi-warehouse split when none
can, and an honest "infeasible" when even that isn't possible.

Part of the [Commerce Engineering Lab](https://ritangkar.github.io/#commerce-lab) —
17 small, production-minded engineering capabilities by Ritangkar Dey.

---

## 1. Project Overview

6 warehouses across major Indian cities, 20 products with real weights, a
20-city delivery network, real Haversine great-circle distances. Given an
order, the service returns exactly how it would be fulfilled and why.

## 2. Problem

"Which warehouse should fulfill this order?" sounds simple until inventory
is scattered unevenly across a network — then it becomes a real
optimization question involving cost, distance, delivery time, and
sometimes no single warehouse having everything in stock.

## 3. Solution

Try every warehouse that could fulfill the whole order alone, pick the
cheapest (cost + SLA-miss penalty); if none can, split the order
item-by-item to the nearest capable warehouse and consolidate same-warehouse
items into one shipment; if some item can't be fulfilled anywhere, say so
explicitly.

## 4. Key Features

- Real Haversine great-circle distance over real Indian city coordinates
- Deterministic, documented cost model (base + per-km + per-kg) and
  delivery-time estimate
- SLA scored as a penalty (not a hard filter) so a late-but-real plan
  still beats no plan
- Split fulfillment with correct shipment consolidation — two items
  routed to the same warehouse become one shipment, not two
- Explicit `INFEASIBLE` result when no warehouse network combination can
  supply an item, rather than a silently wrong answer

## 5. Architecture

```
FulfillmentRequest
   │
   ├─ any warehouse can fill the WHOLE order? ──yes──► pick lowest cost+SLA score
   │                                                     └─► SINGLE_WAREHOUSE
   └─ no ──► assign each item to its nearest capable warehouse
              ├─ every item assignable ──► consolidate ──► SPLIT
              └─ some item unassignable ──► INFEASIBLE
```

See [`docs/architecture.md`](docs/architecture.md) for the full diagram.

## 6. Technical Approach

- No optimization library — a greedy, per-item nearest-warehouse
  heuristic, chosen deliberately for a 6-warehouse network (see design
  decisions for exactly when a real solver would earn its complexity)
- Haversine distance verified against known real city-pair distances
  before being trusted for the rest of the algorithm

## 7. Design Decisions

See [`docs/design-decisions.md`](docs/design-decisions.md) — including the
precise, stated trade-off of the greedy heuristic (capacity isn't yet a
hard constraint across items) rather than glossing over it.

## 8. Sample Input

```json
POST /api/fulfillment/plan
{
  "customerCity": "Chandigarh",
  "items": [
    { "productId": "PR01", "quantity": 15 },
    { "productId": "PR02", "quantity": 15 },
    { "productId": "PR03", "quantity": 15 },
    { "productId": "PR07", "quantity": 15 }
  ],
  "requestedSlaDays": 5
}
```

## 9. Sample Output

Real output for the input above (a genuine SPLIT scenario, independently verified):

```json
{
  "strategy": "SPLIT",
  "totalCost": 680.32,
  "deliveryDays": 4,
  "slaStatus": "MET",
  "shipments": [
    { "warehouseId": "W05", "warehouseName": "Kolkata Distribution Center", "items": [{"productId":"PR01","quantity":15}], "distanceKm": 1464.2, "cost": 219.21, "deliveryDays": 4 },
    { "warehouseId": "W02", "warehouseName": "Delhi Distribution Center", "items": [{"productId":"PR02","quantity":15}], "distanceKm": 227.8, "cost": 145.39, "deliveryDays": 2 },
    { "warehouseId": "W01", "warehouseName": "Mumbai Distribution Center", "items": [{"productId":"PR03","quantity":15},{"productId":"PR07","quantity":15}], "distanceKm": 1354.3, "cost": 315.72, "deliveryDays": 4 }
  ]
}
```

*PR03 and PR07 both route to Mumbai and correctly consolidate into one shipment.*

## 10. How to Run

```bash
git clone <your-repo-url>
cd inventory-fulfillment-optimizer
mvn spring-boot:run
```

API available at `http://localhost:8089`.

## 11. How to Test

```bash
mvn test
```

Covers all three strategies (single-warehouse, split with consolidation,
infeasible) against independently pre-computed reference values, plus
candidate-ordering and unknown-city error handling.

## 12. API Documentation

| Method | Path | Description |
|---|---|---|
| POST | `/api/fulfillment/plan` | The core planning endpoint |
| GET | `/api/warehouses` | The warehouse network |
| GET | `/api/products` | The product catalog (with weights) |
| GET | `/api/cities` | Valid delivery cities |

## 13. Portfolio Demo

The Commerce Engineering Lab demo (`lab/demos/inventory-fulfillment-optimizer.js`)
ports the full decision flow to client-side JavaScript over the same
bundled network. See [`docs/demo.md`](docs/demo.md).

## 14. Limitations

- Greedy split-fulfillment doesn't yet enforce warehouse daily capacity as
  a hard constraint across items in the same order (documented, not hidden)
- Straight-line (Haversine) distance, not real road/route distance
- Cost and SLA-penalty constants are illustrative business-rule
  assumptions, not fitted to real carrier data

## 15. Future Enhancements

- Enforce `capacityUnitsPerDay` as a real constraint in the split-assignment loop
- Swap in a proper routing-distance API/library for real road distances
- A true joint optimizer (e.g. a small integer program) for networks large
  enough that greedy assignment's gap from optimal becomes material

## 16. License

MIT — see [`LICENSE`](LICENSE).
