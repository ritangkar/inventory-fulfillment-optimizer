package com.ritangkar.fulfillment.service;

import com.ritangkar.fulfillment.model.FulfillmentPlan;
import com.ritangkar.fulfillment.model.FulfillmentRequest;
import com.ritangkar.fulfillment.model.OrderItem;
import com.ritangkar.fulfillment.repository.NetworkDataRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Every expected value below was independently computed with a Python
 * reference implementation of the same algorithm, against this exact
 * bundled dataset, before this test was written — see docs/design-decisions.md.
 */
class FulfillmentOptimizerServiceTest {

    private final NetworkDataRepository repository = new NetworkDataRepository();
    private final FulfillmentOptimizerService service = new FulfillmentOptimizerService(repository);

    @Test
    void chooseSingleWarehouseWhenOneCanFulfillTheWholeOrder() {
        FulfillmentRequest request = new FulfillmentRequest("Pune",
                List.of(new OrderItem("PR01", 2), new OrderItem("PR14", 1)), 3);

        FulfillmentPlan plan = service.plan(request);

        assertThat(plan.strategy()).isEqualTo("SINGLE_WAREHOUSE");
        assertThat(plan.chosenWarehouse().warehouseId()).isEqualTo("W04"); // Chennai
        assertThat(plan.chosenWarehouse().distanceKm()).isCloseTo(914.4, within(0.5));
        assertThat(plan.totalCost()).isCloseTo(109.24, within(0.5));
        assertThat(plan.deliveryDays()).isEqualTo(3);
        assertThat(plan.slaStatus()).isEqualTo("MET");
    }

    @Test
    void splitsAcrossWarehousesWhenNoSingleWarehouseHasEverything() {
        FulfillmentRequest request = new FulfillmentRequest("Chandigarh",
                List.of(new OrderItem("PR01", 15), new OrderItem("PR02", 15),
                        new OrderItem("PR03", 15), new OrderItem("PR07", 15)), 5);

        FulfillmentPlan plan = service.plan(request);

        assertThat(plan.strategy()).isEqualTo("SPLIT");
        assertThat(plan.shipments()).hasSize(3);
        assertThat(plan.totalCost()).isCloseTo(680.32, within(2.0));
        assertThat(plan.deliveryDays()).isEqualTo(4);
        assertThat(plan.slaStatus()).isEqualTo("MET");

        // PR03 and PR07 both go to the same nearest capable warehouse (Mumbai) and
        // must be consolidated into ONE shipment, not two.
        assertThat(plan.shipments()).anyMatch(s -> s.items().size() == 2);
    }

    @Test
    void reportsInfeasibleWhenNoWarehouseHasEnoughStockOfAnItem() {
        FulfillmentRequest request = new FulfillmentRequest("Delhi",
                List.of(new OrderItem("PR11", 1000)), 5);

        FulfillmentPlan plan = service.plan(request);

        assertThat(plan.strategy()).isEqualTo("INFEASIBLE");
        assertThat(plan.infeasibleItems()).contains("PR11");
        assertThat(plan.totalCost()).isNull();
    }

    @Test
    void singleWarehouseCandidatesAreSortedByScoreAscending() {
        FulfillmentRequest request = new FulfillmentRequest("Pune",
                List.of(new OrderItem("PR01", 2), new OrderItem("PR14", 1)), 3);

        FulfillmentPlan plan = service.plan(request);

        List<Double> scores = plan.allCandidates().stream().map(c -> c.score()).toList();
        for (int i = 0; i < scores.size() - 1; i++) {
            assertThat(scores.get(i)).isLessThanOrEqualTo(scores.get(i + 1));
        }
    }

    @Test
    void unknownCityThrowsRatherThanSilentlyReturningAnEmptyPlan() {
        FulfillmentRequest request = new FulfillmentRequest("Atlantis",
                List.of(new OrderItem("PR01", 1)), 5);

        org.junit.jupiter.api.Assertions.assertThrows(
                java.util.NoSuchElementException.class, () -> service.plan(request));
    }
}
