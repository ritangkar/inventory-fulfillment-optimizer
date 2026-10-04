# Design Decisions

---

**Decision:** A greedy, per-item nearest-capable-warehouse heuristic for
split fulfillment, rather than a true combinatorial optimization solver
(e.g. integer programming via OR-Tools).

**Why:** With 6 warehouses and a handful of items per order, the solution
space is small enough that a greedy nearest-warehouse assignment is very
likely to be optimal or extremely close to it, while staying fully
explainable — every assignment is "closest warehouse with enough stock,"
one sentence, no solver internals to unpack. The master brief for this
collection is explicit that an optimization library should be used only
"where appropriate," and pulling in a full MIP solver for a 6-node network
would be exactly the "unnecessary complexity" the brief warns against.

**Trade-off, stated precisely:** greedy per-item assignment does not
consider warehouse *capacity* interactions across items (an early item
could "use up" a warehouse's practical capacity in a way a joint optimizer
would route around) — this project models per-warehouse daily capacity as
informational context (`capacityUnitsPerDay` on `Warehouse`) but doesn't
yet enforce it as a hard constraint in the assignment loop. See Future
Enhancements.

**Future:** If capacity enforcement or a genuinely large warehouse network
made the greedy heuristic's gap from optimal material, that's the point
where a real solver (e.g. a Java binding to OR-Tools, or a small integer
program solved with a lightweight LP library) would be worth the added
dependency and complexity.

---

**Decision:** Real Haversine great-circle distance over real Indian city
coordinates, rather than synthetic/fictional coordinates.

**Why:** Distance is the single input every other number in this project
(cost, delivery days, SLA status) derives from. Using real coordinates
means the distances are independently checkable against any map or
geocoding tool — verified here against two well-known city pairs
(Mumbai–Delhi, Bangalore–Chennai) before trusting the formula for the rest
of the network.

**Trade-off:** None significant — Haversine is a standard, well-understood
formula; the only real limitation is that it's straight-line distance, not
road/route distance (see Future Enhancements).

---

**Decision:** SLA violations are scored as a penalty added to cost, not
enforced as a hard filter that excludes a warehouse from consideration.

**Why:** A plan that "misses SLA by one day" is still useful information —
excluding it entirely would mean an order with no warehouse able to meet
a tight SLA gets reported as unfulfillable, even though a slightly-late
delivery is almost always better than none. `slaStatus` (`MET`/`MISSED`)
on the response makes the trade-off visible to the caller either way.

**Trade-off:** The exact penalty size (₹100/day late) is an illustrative
business-rule constant, not derived from real carrier SLA-violation costs
— documented plainly rather than presented as calibrated.
