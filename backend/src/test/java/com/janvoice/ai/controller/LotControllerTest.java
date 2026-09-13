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
import java.time.LocalDateTime;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LotControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private MaterialMasterRepository materials;
    @Autowired private PriceRecordRepository prices;
    @Autowired private LotRepository lots;
    @Autowired private TransactionRepository transactions;
    @Autowired private RecyclerRepository recyclers;
    @Autowired private SessionTokenService tokens;
    private MaterialMaster material;
    private String collectorToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        // FK-safe deletion order: transactions reference lots, lots reference
        // price records/recyclers/users, price records reference recyclers/users,
        // recyclers reference users (created_by).
        transactions.deleteAll(); lots.deleteAll(); prices.deleteAll(); recyclers.deleteAll(); users.deleteAll();
        User collector = users.save(new User(null, "lot-collector", "test", "CITIZEN", "Indore"));
        User other = users.save(new User(null, "other-collector", "test", "CITIZEN", "Indore"));
        collectorToken = tokens.issue(collector); otherToken = tokens.issue(other);
        material = materials.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(0);
    }

    @Test
    void createsLotWithServerReferenceAndTokenCollector() throws Exception {
        mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":2,\"collectorId\":999999}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lot_reference", startsWith("EWS-")))
                .andExpect(jsonPath("$.collector_id").isNumber()).andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void rejectsMissingOrNonPositiveWeight() throws Exception {
        String body = "{\"materialId\":" + material.getId() + ",\"approximateWeight\":0}";
        mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void calculatesValueOnlyFromSelectedLivePrice() throws Exception {
        PriceRecord price = new PriceRecord(); price.setMaterial(material); price.setLocation("Indore"); price.setBuyerType(PriceRecord.BuyerType.OTHER); price.setRate(new BigDecimal("12.50")); price.setUnit(material.getTypicalUnit()); price.setSourceType(PriceRecord.SourceType.OTHER_VERIFIED_SOURCE); price.setQuotedAt(LocalDateTime.now().minusHours(1)); price.setVerificationStatus(PriceRecord.VerificationStatus.VERIFIED); price = prices.save(price);
        mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":2,\"selectedPriceRecordId\":" + price.getId() + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estimated_value").value(25.0));
    }

    @Test
    void excludesOtherCollectorsLots() throws Exception {
        mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":1}"));
        mockMvc.perform(get("/api/lots").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    void deniesOtherCollectorDetailAndRejectsIllegalTransition() throws Exception {
        String result = mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":1}"))
                .andReturn().getResponse().getContentAsString();
        String id = result.replaceAll(".*\"id\":([0-9]+).*", "$1");
        mockMvc.perform(get("/api/lots/" + id).header("Authorization", "Bearer " + otherToken)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(status().isBadRequest());
    }
}
