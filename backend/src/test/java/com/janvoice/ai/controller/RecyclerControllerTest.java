package com.janvoice.ai.controller;

import com.janvoice.ai.dto.RecyclerRequest;
import com.janvoice.ai.entity.MaterialMaster;
import com.janvoice.ai.entity.Recycler;
import com.janvoice.ai.entity.User;
import com.janvoice.ai.repository.MaterialMasterRepository;
import com.janvoice.ai.repository.RecyclerRepository;
import com.janvoice.ai.repository.UserRepository;
import com.janvoice.ai.service.SessionTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecyclerControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private RecyclerRepository recyclerRepository;
    @Autowired private MaterialMasterRepository materialRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SessionTokenService tokens;
    private MaterialMaster first;
    private MaterialMaster second;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        recyclerRepository.deleteAll();
        userRepository.deleteAll();
        User admin = userRepository.save(new User(null, "recycler-admin", "test", "ADMIN", "Indore"));
        User user = userRepository.save(new User(null, "recycler-user", "test", "CITIZEN", "Indore"));
        adminToken = tokens.issue(admin);
        userToken = tokens.issue(user);
        first = materialRepository.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(0);
        second = materialRepository.findByActiveTrueOrderByCategoryAscCommonNameAsc().get(1);
    }

    @Test
    void createsPendingRecyclerWithMultipleAcceptedMaterials() throws Exception {
        String body = requestBody(first.getId(), second.getId());
        mockMvc.perform(post("/api/recyclers").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorization_status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.accepted_materials", hasSize(2)));
    }

    @Test
    void rejectsInvalidPhoneAndMissingMaterials() throws Exception {
        String body = requestBody(first.getId()).replace("9876543210", "bad");
        mockMvc.perform(post("/api/recyclers").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        String noMaterials = body.replace("\"acceptedMaterialIds\":[" + first.getId() + "]", "\"acceptedMaterialIds\":[]").replace("bad", "9876543210");
        mockMvc.perform(post("/api/recyclers").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(noMaterials)).andExpect(status().isBadRequest());
    }

    @Test
    void verifiesOnlyWithAdminAndSource() throws Exception {
        Recycler recycler = save(first);
        mockMvc.perform(patch("/api/recyclers/{id}/verify", recycler.getId()).header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verificationSource\":\"SPCB list\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/recyclers/{id}/verify", recycler.getId()).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"verificationSource\":\"SPCB list\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorization_status").value("VERIFIED"))
                .andExpect(jsonPath("$.verification_source").value("SPCB list"));
    }

    @Test
    void matchPrioritizesVerifiedAndExcludesRejected() throws Exception {
        Recycler pending = save(first); pending.setCity("Indore"); recyclerRepository.save(pending);
        Recycler verified = save(first); verified.setAuthorizationStatus(Recycler.AuthorizationStatus.VERIFIED); recyclerRepository.save(verified);
        Recycler rejected = save(first); rejected.setAuthorizationStatus(Recycler.AuthorizationStatus.REJECTED); recyclerRepository.save(rejected);
        mockMvc.perform(get("/api/recyclers/match").param("material_id", first.getId().toString()).param("location", "Indore"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].authorization_status").value("VERIFIED"));
    }

    @Test
    void matchIsEmptyWhenNoRecyclerAcceptsMaterial() throws Exception {
        save(first);
        mockMvc.perform(get("/api/recyclers/match").param("material_id", second.getId().toString()).param("location", "Indore"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void adminRejectsWithRequiredReason() throws Exception {
        Recycler recycler = save(first);
        mockMvc.perform(patch("/api/recyclers/{id}/reject", recycler.getId()).header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Registration could not be confirmed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorization_status").value("REJECTED"));
    }

    private Recycler save(MaterialMaster material) {
        Recycler recycler = new Recycler();
        recycler.setBusinessName("Test Recycler"); recycler.setPhone("9876543210"); recycler.setCity("Indore"); recycler.setAcceptedMaterials(new HashSet<>(Set.of(material))); recycler.setAuthorizationStatus(Recycler.AuthorizationStatus.PENDING_VERIFICATION);
        return recyclerRepository.save(recycler);
    }

    private String requestBody(Long... materialIds) {
        String ids = java.util.Arrays.stream(materialIds).map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        return "{\"businessName\":\"Registered Recycler\",\"phone\":\"9876543210\",\"city\":\"Indore\",\"acceptedMaterialIds\":[" + ids + "],\"pickupAvailable\":true}";
    }
}
