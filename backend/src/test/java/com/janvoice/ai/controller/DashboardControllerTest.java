package com.janvoice.ai.controller;

import com.janvoice.ai.entity.*;
import com.janvoice.ai.repository.*;
import com.janvoice.ai.service.SessionTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionRepository transactions;
    @Autowired private LotRepository lots;
    @Autowired private PriceRecordRepository priceRecords;
    @Autowired private RecyclerRepository recyclers;
    @Autowired private MaterialMasterRepository materials;
    @Autowired private UserRepository users;
    @Autowired private SessionTokenService tokens;

    private MaterialMaster material;
    private User collector;
    private User other;
    private Recycler recycler;
    private String collectorToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        // FK-safe deletion: transactions -> lots -> price records -> recyclers -> users.
        transactions.deleteAll(); lots.deleteAll(); priceRecords.deleteAll(); recyclers.deleteAll(); users.deleteAll();
        collector = users.save(new User(null, "dash-collector", "test", "CITIZEN", "Indore"));
        other = users.save(new User(null, "dash-other", "test", "CITIZEN", "Indore"));
        User recyclerUser = users.save(new User(null, "dash-recycler", "test", "RECYCLER", "Indore"));
        collectorToken = tokens.issue(collector); otherToken = tokens.issue(other);
        material = materials.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(0);
        recycler = new Recycler(); recycler.setBusinessName("Authorized Recycler"); recycler.setPhone("9876543210"); recycler.setCity("Indore"); recycler.setCreatedBy(recyclerUser.getId()); recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.VERIFIED); recycler = recyclers.save(recycler);
    }

    @Test
    void newCollectorSeesAllZerosAndEmptyLists() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + collectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_earnings").value(0))
                .andExpect(jsonPath("$.pending_amount").value(0))
                .andExpect(jsonPath("$.total_kg_collected").value(0))
                .andExpect(jsonPath("$.paid_transactions_count").value(0))
                .andExpect(jsonPath("$.total_lots_count").value(0))
                .andExpect(jsonPath("$.active_lots_count").value(0))
                .andExpect(jsonPath("$.completed_handovers_count").value(0));
        mockMvc.perform(get("/api/dashboard/recent-lots").header("Authorization", "Bearer " + collectorToken)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/dashboard/recent-transactions").header("Authorization", "Bearer " + collectorToken)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void mixedStatusesAggregateCorrectly() throws Exception {
        // lot A: completed handover, PAID transaction of 100, final weight 2.5 (approx 2.0).
        Lot completed = lot(Lot.Status.RECYCLER_CONFIRMED, "2.0", "2.5");
        tx(completed, "100.00", Transaction.PaymentStatus.PAID);
        // lot B: in progress, PENDING transaction of 200.
        Lot inProgress = lot(Lot.Status.READY_FOR_HANDOVER, "3.0", null);
        tx(inProgress, "200.00", Transaction.PaymentStatus.PENDING);
        // lot C: in progress, no transaction.
        lot(Lot.Status.ACCEPTED, "1.0", null);
        // lot D: cancelled, excluded from active/completed.
        lot(Lot.Status.CANCELLED, "0.5", null);

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + collectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_earnings").value(100.00))
                .andExpect(jsonPath("$.pending_amount").value(200.00))
                .andExpect(jsonPath("$.total_kg_collected").value(2.5))
                .andExpect(jsonPath("$.paid_transactions_count").value(1))
                .andExpect(jsonPath("$.total_lots_count").value(4))
                .andExpect(jsonPath("$.active_lots_count").value(2))
                .andExpect(jsonPath("$.completed_handovers_count").value(1));
    }

    @Test
    void dashboardNumbersMatchDetailEndpoints_NoDrift() throws Exception {
        Lot paidLot = lot(Lot.Status.RECYCLER_CONFIRMED, "2.0", "2.5");
        tx(paidLot, "120.00", Transaction.PaymentStatus.PAID);
        Lot pendingLot = lot(Lot.Status.HANDED_OVER, "3.0", null);
        tx(pendingLot, "80.00", Transaction.PaymentStatus.PENDING);
        lot(Lot.Status.CREATED, "1.0", null);

        // Dashboard summary must agree with the detail endpoints (/lots, /transactions).
        int lotsCount = new org.json.JSONArray(body(get("/api/lots"))).length();
        int paidCount = new org.json.JSONArray(body(get("/api/transactions").param("payment_status", "PAID"))).length();
        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + collectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_lots_count").value(lotsCount))
                .andExpect(jsonPath("$.paid_transactions_count").value(paidCount))
                .andExpect(jsonPath("$.total_earnings").value(120.00))
                .andExpect(jsonPath("$.pending_amount").value(80.00));

        // Recent lists reference real records that also appear in the detail views.
        mockMvc.perform(get("/api/dashboard/recent-lots").param("limit", "3").header("Authorization", "Bearer " + collectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].lot_reference", hasItem(paidLot.getLotReference())))
                .andExpect(jsonPath("$[*].lot_reference", hasItem(pendingLot.getLotReference())))
                .andExpect(jsonPath("$[*].status", hasItem("RECYCLER_CONFIRMED")));
        mockMvc.perform(get("/api/dashboard/recent-transactions").param("limit", "2").header("Authorization", "Bearer " + collectorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].payment_status", hasItem("PAID")))
                .andExpect(jsonPath("$[*].payment_status", hasItem("PENDING")));
    }

    @Test
    void collectorCannotSeeAnotherCollectorsDashboard() throws Exception {
        Lot completed = lot(Lot.Status.RECYCLER_CONFIRMED, "2.0", "2.5");
        tx(completed, "100.00", Transaction.PaymentStatus.PAID);
        lot(Lot.Status.ACCEPTED, "1.0", null);

        mockMvc.perform(get("/api/dashboard/summary").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_earnings").value(0))
                .andExpect(jsonPath("$.total_kg_collected").value(0))
                .andExpect(jsonPath("$.total_lots_count").value(0));
        mockMvc.perform(get("/api/dashboard/recent-lots").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/dashboard/recent-transactions").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unauthenticatedDashboardIsRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/recent-lots")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/recent-transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void limitIsClampedBetweenOneAndFifty() throws Exception {
        for (int i = 0; i < 7; i++) lot(Lot.Status.CREATED, "1.0", null);

        mockMvc.perform(get("/api/dashboard/recent-lots").param("limit", "3").header("Authorization", "Bearer " + collectorToken)).andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(get("/api/dashboard/recent-lots").param("limit", "999").header("Authorization", "Bearer " + collectorToken)).andExpect(jsonPath("$", hasSize(7)));
        mockMvc.perform(get("/api/dashboard/recent-lots").param("limit", "0").header("Authorization", "Bearer " + collectorToken)).andExpect(jsonPath("$", hasSize(5)));
        mockMvc.perform(get("/api/dashboard/recent-lots").param("limit", "-4").header("Authorization", "Bearer " + collectorToken)).andExpect(jsonPath("$", hasSize(5)));
        mockMvc.perform(get("/api/dashboard/recent-lots").header("Authorization", "Bearer " + collectorToken)).andExpect(jsonPath("$", hasSize(5)));
    }

    // ------------------------------------------------------------------ helpers

    private MvcResult result(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) throws Exception {
        return mockMvc.perform(builder.header("Authorization", "Bearer " + collectorToken)).andReturn();
    }

    private String body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) throws Exception {
        return result(builder).getResponse().getContentAsString();
    }

    private Lot lot(Lot.Status status, String approxWeight, String finalWeight) {
        Lot lot = new Lot();
        lot.setLotReference("DASH-" + System.nanoTime());
        lot.setCollector(collector);
        lot.setMaterial(material);
        lot.setApproximateWeight(new BigDecimal(approxWeight));
        if (finalWeight != null) lot.setFinalWeight(new BigDecimal(finalWeight));
        lot.setStatus(status);
        lot.setRecycler(recycler);
        return lots.save(lot);
    }

    private Transaction tx(Lot lot, String amount, Transaction.PaymentStatus status) {
        Transaction transaction = new Transaction();
        transaction.setLot(lot);
        transaction.setCollector(collector);
        transaction.setRecycler(recycler);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setPaymentMode(Transaction.PaymentMode.CASH);
        transaction.setPaymentStatus(status);
        transaction.setTransactionTime(LocalDateTime.now());
        return transactions.save(transaction);
    }
}