

package com.janvoice.ai.controller;

import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.entity.MaterialUnit;
import com.janvoice.ai.entity.PriceRecord;
import com.janvoice.ai.repository.LotRepository;
import com.janvoice.ai.repository.MaterialMasterRepository;
import com.janvoice.ai.repository.PriceRecordRepository;
import com.janvoice.ai.repository.RecyclerRepository;
import com.janvoice.ai.repository.TransactionRepository;
import com.janvoice.ai.repository.UserRepository;
import com.janvoice.ai.entity.User;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PriceRecordControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private MaterialMasterRepository materialRepository;
    @Autowired private PriceRecordRepository priceRepository;
    @Autowired private LotRepository lotRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private RecyclerRepository recyclerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SessionTokenService sessionTokenService;
    private MaterialMaster material;
    private Long researcherId;
    private Long adminId;
    private String researcherToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // FK-safe deletion order: transactions reference lots, lots reference
        // price records/recyclers/users, price records reference recyclers/users,
        // recyclers reference users (created_by).
        transactionRepository.deleteAll();
        lotRepository.deleteAll();
        priceRepository.deleteAll();
        recyclerRepository.deleteAll();
        userRepository.deleteAll();
        researcherId = userRepository.save(new User(null, "researcher", "test", "FIELD_RESEARCHER", "Indore")).getId();
        adminId = userRepository.save(new User(null, "admin", "test", "ADMIN", "Indore")).getId();
        researcherToken = sessionTokenService.issue(userRepository.findById(researcherId).orElseThrow());
        adminToken = sessionTokenService.issue(userRepository.findById(adminId).orElseThrow());
        material = materialRepository.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(0);
    }

    @Test
    void createsUnverifiedRecordForFieldResearcher() throws Exception {
        String body = requestBody(material.getId(), "Indore");
        mockMvc.perform(post("/api/prices").header("Authorization", "Bearer " + researcherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.material_id").value(material.getId().intValue()))
                .andExpect(jsonPath("$.verification_status").value("UNVERIFIED"));
    }

    @Test
    void rejectsUnitThatDoesNotMatchMaterial() throws Exception {
        String body = requestBody(material.getId(), "Indore").replace("\"unit\":\"" + material.getTypicalUnit() + "\"", "\"unit\":\"" + (material.getTypicalUnit() == MaterialUnit.KG ? "PIECE" : "KG") + "\"");
        mockMvc.perform(post("/api/prices").header("Authorization", "Bearer " + researcherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void excludesExpiredByDefaultAndIncludesItForHistory() throws Exception {
        save(LocalDateTime.now().minusDays(31), "Old Area");
        mockMvc.perform(get("/api/prices").param("material_id", material.getId().toString()).param("location", "Old Area"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/prices").param("material_id", material.getId().toString()).param("location", "Old Area").param("includeExpired", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].verification_status").value("EXPIRED"));
    }

    @Test
    void verifiesRecordOnlyForAdmin() throws Exception {
        PriceRecord record = save(LocalDateTime.now().minusDays(1), "Verify Area");
        mockMvc.perform(patch("/api/prices/{id}/verify", record.getId()).header("Authorization", "Bearer " + researcherToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/prices/{id}/verify", record.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verification_status").value("VERIFIED"))
            .andExpect(jsonPath("$.verified_by").value(adminId.intValue()));
    }

    @Test
    void rejectsSpoofedIdentityHeadersWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/prices").header("X-User-Id", researcherId.toString()).header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody(material.getId(), "Indore")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void historyIsChronologicalNewestFirst() throws Exception {
        save(LocalDateTime.now().minusDays(2), "History Area");
        save(LocalDateTime.now().minusDays(1), "History Area");
        mockMvc.perform(get("/api/prices/history").param("material_id", material.getId().toString()).param("location", "History Area"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].quoted_at").isNotEmpty());
    }

    private PriceRecord save(LocalDateTime quotedAt, String location) {
        PriceRecord record = new PriceRecord();
        record.setMaterial(material); record.setLocation(location); record.setBuyerType(PriceRecord.BuyerType.OTHER);
        record.setRate(new BigDecimal("10.00")); record.setUnit(material.getTypicalUnit()); record.setSourceType(PriceRecord.SourceType.OTHER_VERIFIED_SOURCE);
        record.setSourceReference("Test source"); record.setQuotedAt(quotedAt); record.setVerificationStatus(PriceRecord.VerificationStatus.UNVERIFIED);
        return priceRepository.save(record);
    }

    private String requestBody(Long materialId, String location) {
        return "{\"materialId\":" + materialId + ",\"location\":\"" + location + "\",\"buyerType\":\"OTHER\",\"rate\":10,\"unit\":\"" + material.getTypicalUnit() + "\",\"sourceType\":\"OTHER_VERIFIED_SOURCE\",\"sourceReference\":\"Local quote\",\"quotedAt\":\"" + LocalDateTime.now().minusHours(1) + "\"}";
    }
}
