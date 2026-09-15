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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access test for a real (non-demo) Recycler account — the case that reported
 * "403 on dashboard / earnings / lots / transactions" while the demo profiles
 * (which never call the API) worked.
 *
 * <p>Two separate defects produced that report and both are covered here:
 * <ul>
 *   <li>CORS: every call carries a Bearer token, so the browser preflights first;
 *       those preflights failed (500 / 403) — see the CorsConfig tests.</li>
 *   <li>Authorisation: {@code /api/earnings} threw "Collector role required" for
 *       the RECYCLER role issued by AuthController.</li>
 * </ul>
 * A recycler login must be able to load every screen its UI calls, and must only
 * ever see its own figures.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecyclerRoleAccessTest {

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
    private User recyclerUser;
    private String recyclerToken;
    private Recycler recycler;

    @BeforeEach
    void setUp() {
        // FK-safe deletion: transactions -> lots -> price records -> recyclers -> users.
        transactions.deleteAll(); lots.deleteAll(); priceRecords.deleteAll(); recyclers.deleteAll(); users.deleteAll();
        collector = users.save(new User(null, "rra-collector", "test", "CITIZEN", "Indore"));
        recyclerUser = users.save(new User(null, "rra-recycler", "test", "RECYCLER", "Indore"));
        recyclerToken = tokens.issue(recyclerUser);
        material = materials.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(0);
        recycler = new Recycler();
        recycler.setBusinessName("Authorized Recycler");
        recycler.setPhone("9876543210");
        recycler.setCity("Indore");
        recycler.setCreatedBy(recyclerUser.getId());
        recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.VERIFIED);
        recycler = recyclers.save(recycler);
    }

    @Test
    void recyclerLoginCanLoadEveryEndpointItsUiCalls() throws Exception {
        seedLedger();

        for (String endpoint : new String[]{
                "/api/dashboard/summary",
                "/api/dashboard/recent-lots",
                "/api/dashboard/recent-transactions",
                "/api/dashboard/recycler-summary",
                "/api/lots",
                "/api/transactions",
                "/api/earnings"}) {
            mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + recyclerToken))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void recyclerListsAndEarningsAreScopedToTheirOwnLots() throws Exception {
        seedLedger();

        mockMvc.perform(get("/api/lots").header("Authorization", "Bearer " + recyclerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/transactions").header("Authorization", "Bearer " + recyclerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/earnings").header("Authorization", "Bearer " + recyclerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_earnings").value(4500.00))
                .andExpect(jsonPath("$.pending_amount").value(800.00))
                .andExpect(jsonPath("$.total_kg_collected").value(9.50))
                .andExpect(jsonPath("$.completed_handovers_count").value(1));
    }

    @Test
    void anotherRecyclerSeesTheirOwnEmptyFigures() throws Exception {
        seedLedger();
        User stranger = users.save(new User(null, "rra-stranger", "test", "RECYCLER", "Indore"));

        mockMvc.perform(get("/api/earnings").header("Authorization", "Bearer " + tokens.issue(stranger)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_earnings").value(0))
                .andExpect(jsonPath("$.pending_amount").value(0))
                .andExpect(jsonPath("$.completed_handovers_count").value(0));
        mockMvc.perform(get("/api/lots").header("Authorization", "Bearer " + tokens.issue(stranger)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void collectorEarningsKeepWorkingAndStayCollectorScoped() throws Exception {
        seedLedger();

        mockMvc.perform(get("/api/earnings").header("Authorization", "Bearer " + tokens.issue(collector)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_earnings").value(4500.00))
                .andExpect(jsonPath("$.pending_amount").value(800.00));
    }

    @Test
    void recyclerWithoutAProfileGetsZeroesInsteadOfForbidden() throws Exception {
        recyclers.deleteAll();

        mockMvc.perform(get("/api/earnings").header("Authorization", "Bearer " + recyclerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_earnings").value(0));
        mockMvc.perform(get("/api/dashboard/recycler-summary").header("Authorization", "Bearer " + recyclerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.has_profile").value(false));
    }

    @Test
    void earningsStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/earnings")).andExpect(status().isUnauthorized());
    }

    /** One settled (PAID) and one still-to-be-settled (PENDING) lot for the recycler. */
    private void seedLedger() {
        tx(lot(Lot.Status.PAID, "10.0", "9.5"), "4500.00", Transaction.PaymentStatus.PAID);
        tx(lot(Lot.Status.RECYCLER_CONFIRMED, "4.0", null), "800.00", Transaction.PaymentStatus.PENDING);
    }

    private Lot lot(Lot.Status status, String approximateWeight, String finalWeight) {
        Lot lot = new Lot();
        lot.setLotReference("RRA-" + System.nanoTime());
        lot.setCollector(collector);
        lot.setMaterial(material);
        lot.setApproximateWeight(new BigDecimal(approximateWeight));
        if (finalWeight != null) lot.setFinalWeight(new BigDecimal(finalWeight));
        lot.setStatus(status);
        lot.setRecycler(recycler);
        return lots.save(lot);
    }

    private void tx(Lot lot, String amount, Transaction.PaymentStatus status) {
        Transaction transaction = new Transaction();
        transaction.setLot(lot);
        transaction.setCollector(collector);
        transaction.setRecycler(recycler);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setPaymentMode(Transaction.PaymentMode.CASH);
        transaction.setPaymentStatus(status);
        transaction.setTransactionTime(LocalDateTime.now());
        transactions.save(transaction);
    }
}