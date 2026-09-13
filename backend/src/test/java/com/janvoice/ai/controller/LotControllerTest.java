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
    private User recyclerUser;
    private Recycler recycler;
    private String collectorToken;
    private String otherToken;
    private String recyclerToken;

    @BeforeEach
    void setUp() {
        // FK-safe deletion order: transactions reference lots, lots reference
        // price records/recyclers/users, price records reference recyclers/users,
        // recyclers reference users (created_by).
        transactions.deleteAll(); lots.deleteAll(); prices.deleteAll(); recyclers.deleteAll(); users.deleteAll();
        User collector = users.save(new User(null, "lot-collector", "test", "CITIZEN", "Indore"));
        User other = users.save(new User(null, "other-collector", "test", "CITIZEN", "Indore"));
        recyclerUser = users.save(new User(null, "lot-recycler", "test", "RECYCLER", "Indore"));
        collectorToken = tokens.issue(collector); otherToken = tokens.issue(other); recyclerToken = tokens.issue(recyclerUser);
        material = materials.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(0);
        recycler = new Recycler(); recycler.setBusinessName("Registered Recycler Co"); recycler.setPhone("9876543210"); recycler.setCity("Indore"); recycler.setCreatedBy(recyclerUser.getId()); recycler.setAcceptedMaterials(new java.util.HashSet<>(java.util.Set.of(material))); recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.VERIFIED); recycler = recyclers.save(recycler);
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

    @Test
    void walksCompleteHandoverJourney() throws Exception {
        String result = mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":2}"))
                .andReturn().getResponse().getContentAsString();
        String id = result.replaceAll(".*\"id\":([0-9]+).*", "$1");

        mockMvc.perform(patch("/api/lots/" + id + "/recycler").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"recyclerId\":" + recycler.getId() + "}")).andExpect(status().isOk()).andExpect(jsonPath("$.recycler_id").value(recycler.getId().intValue()));

        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"QUOTE_REQUESTED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("QUOTE_REQUESTED"));
        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + recyclerToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"QUOTE_RECEIVED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("QUOTE_RECEIVED"));
        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READY_FOR_HANDOVER\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY_FOR_HANDOVER"));
        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"HANDED_OVER\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HANDED_OVER")).andExpect(jsonPath("$.handed_over_at").isNotEmpty());

        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + recyclerToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RECYCLER_CONFIRMED\",\"finalWeight\":2.35}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECYCLER_CONFIRMED"))
                .andExpect(jsonPath("$.recycler_confirmed_at").isNotEmpty()).andExpect(jsonPath("$.final_weight").value(2.35));
    }

    @Test
    void recyclerCannotConfirmReceiptBeforeHandover() throws Exception {
        String result = mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":1}"))
                .andReturn().getResponse().getContentAsString();
        String id = result.replaceAll(".*\"id\":([0-9]+).*", "$1");
        mockMvc.perform(patch("/api/lots/" + id + "/recycler").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"recyclerId\":" + recycler.getId() + "}"));
        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + recyclerToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RECYCLER_CONFIRMED\"}")).andExpect(status().isForbidden());
    }

    @Test
    void collectorCannotSelfConfirmReceipt() throws Exception {
        String result = mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":1}"))
                .andReturn().getResponse().getContentAsString();
        String id = result.replaceAll(".*\"id\":([0-9]+).*", "$1");
        mockMvc.perform(patch("/api/lots/" + id + "/recycler").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"recyclerId\":" + recycler.getId() + "}"));
        // The collector owns the handover step, not the confirmation step.
        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"READY_FOR_HANDOVER\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/lots/" + id + "/status").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RECYCLER_CONFIRMED\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void recyclerSeesMatchedLotsInOwnInbox() throws Exception {
        String result = mockMvc.perform(post("/api/lots").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":" + material.getId() + ",\"approximateWeight\":1}"))
                .andReturn().getResponse().getContentAsString();
        String id = result.replaceAll(".*\"id\":([0-9]+).*", "$1");
        mockMvc.perform(patch("/api/lots/" + id + "/recycler").header("Authorization", "Bearer " + collectorToken).contentType(MediaType.APPLICATION_JSON).content("{\"recyclerId\":" + recycler.getId() + "}"));
        // Other collectors must never see the lot.
        mockMvc.perform(get("/api/lots").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        // The matched recycler actor sees it in their handover inbox.
        mockMvc.perform(get("/api/lots").header("Authorization", "Bearer " + recyclerToken)).andExpect(status().isOk()).andExpect(jsonPath("$[*].lot_reference", org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.startsWith("EWS-"))));
    }
}
