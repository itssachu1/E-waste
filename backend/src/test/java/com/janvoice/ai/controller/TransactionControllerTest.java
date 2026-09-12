package com.janvoice.ai.controller;

import com.janvoice.ai.entity.*;
import com.janvoice.ai.repository.*;
import com.janvoice.ai.service.SessionTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private TransactionRepository transactions;
    @Autowired private LotRepository lots;
    @Autowired private RecyclerRepository recyclers;
    @Autowired private MaterialMasterRepository materials;
    @Autowired private UserRepository users;
    @Autowired private SessionTokenService tokens;
    private MaterialMaster material;
    private User collector;
    private User other;
    private User recyclerUser;
    private Recycler recycler;
    private String collectorToken;
    private String otherToken;
    private String recyclerToken;

    @BeforeEach
    void setUp() {
        transactions.deleteAll(); lots.deleteAll(); recyclers.deleteAll(); users.deleteAll();
        collector = users.save(new User(null, "tx-collector", "test", "CITIZEN", "Indore"));
        other = users.save(new User(null, "tx-other", "test", "CITIZEN", "Indore"));
        recyclerUser = users.save(new User(null, "tx-recycler", "test", "RECYCLER", "Indore"));
        collectorToken = tokens.issue(collector); otherToken = tokens.issue(other); recyclerToken = tokens.issue(recyclerUser);
        material = materials.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(0);
        recycler = new Recycler(); recycler.setBusinessName("Authorized Recycler"); recycler.setPhone("9876543210"); recycler.setCity("Indore"); recycler.setCreatedBy(recyclerUser.getId()); recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.VERIFIED); recycler = recyclers.save(recycler);
    }

    @Test
    void rejectsIneligibleLotAndDerivesParties() throws Exception {
        Lot lot = lot(Lot.Status.CREATED);
        String body = "{\"lotId\":" + lot.getId() + ",\"amount\":100,\"paymentMode\":\"CASH\",\"collectorId\":999,\"recyclerId\":999}";
        mockMvc.perform(post("/api/transactions").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        lot.setStatus(Lot.Status.READY_FOR_HANDOVER); lots.save(lot);
        mockMvc.perform(post("/api/transactions").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.collector_id").value(collector.getId().intValue())).andExpect(jsonPath("$.recycler_id").value(recycler.getId().intValue())).andExpect(jsonPath("$.payment_status").value("PENDING"));
    }

    @Test
    void paidStatusAtomicallyUpdatesLotAndPreventsDuplicate() throws Exception {
        Lot lot = lot(Lot.Status.READY_FOR_HANDOVER);
        String body = "{\"lotId\":" + lot.getId() + ",\"amount\":125.50,\"paymentMode\":\"UPI\",\"paymentReference\":\"UPI-1\"}";
        String response = mockMvc.perform(post("/api/transactions").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getContentAsString();
        String id = response.replaceAll(".*\"id\":([0-9]+).*", "$1");
        mockMvc.perform(patch("/api/transactions/" + id + "/status").header("Authorization", "Bearer " + recyclerToken).contentType(MediaType.APPLICATION_JSON).content("{\"paymentStatus\":\"PAID\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.payment_status").value("PAID"));
        mockMvc.perform(get("/api/lots/" + lot.getId()).header("Authorization", "Bearer " + collectorToken)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.final_sale_amount").value(125.50));
        mockMvc.perform(post("/api/transactions").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void earningsIsZeroForCollectorWithoutPaidTransactionsAndIsPrivate() throws Exception {
        mockMvc.perform(get("/api/earnings").header("Authorization", "Bearer " + collectorToken)).andExpect(status().isOk()).andExpect(jsonPath("$.total_earnings").value(0)).andExpect(jsonPath("$.total_kg_collected").value(0)).andExpect(jsonPath("$.completed_handovers_count").value(0));
        mockMvc.perform(get("/api/earnings").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk()).andExpect(jsonPath("$.total_earnings").value(0));
    }

    @Test
    void otherCollectorCannotViewTransactions() throws Exception {
        Lot lot = lot(Lot.Status.READY_FOR_HANDOVER);
        mockMvc.perform(post("/api/transactions").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"lotId\":" + lot.getId() + ",\"amount\":100,\"paymentMode\":\"CASH\"}"));
        mockMvc.perform(get("/api/transactions").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    private Lot lot(Lot.Status status) { Lot lot = new Lot(); lot.setLotReference("TEST-" + System.nanoTime()); lot.setCollector(collector); lot.setMaterial(material); lot.setApproximateWeight(new BigDecimal("2.0")); lot.setStatus(status); lot.setRecycler(recycler); return lots.save(lot); }
}
