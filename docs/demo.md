# Portfolio Demo

**Demo level:** 2 — Portfolio simulation (bundled sample data, client-side).

`lab/demos/inventory-fulfillment-optimizer.js` ports the full decision flow
— Haversine distance, cost/SLA scoring, single-warehouse selection, and
split-fulfillment consolidation — to client-side JavaScript over the exact
same bundled network (6 warehouses, 20 products, 20 cities). A recruiter
picks a delivery city, adds items, sets an SLA, and sees the identical
plan this README documents, live, including the three verified scenarios
(single-warehouse, split, infeasible). No backend call.

The real Spring Boot API (`mvn spring-boot:run`) exposes the full
`/api/fulfillment/plan` surface for arbitrary orders beyond the bundled demo.
