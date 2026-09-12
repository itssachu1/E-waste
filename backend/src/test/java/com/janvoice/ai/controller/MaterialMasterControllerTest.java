package com.janvoice.ai.controller;

import com.janvoice.ai.repository.MaterialMasterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaterialMasterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaterialMasterRepository repository;

    @Test
    void listsMigratedMaterials() throws Exception {
        mockMvc.perform(get("/api/materials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(30)))
                .andExpect(jsonPath("$[*].commonName", hasItem("Laptop")));
    }

    @Test
    void filtersByCategory() throws Exception {
        mockMvc.perform(get("/api/materials").param("category", "IT & Telecom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].category", hasItem("IT & Telecom")));
    }

    @Test
    void searchesByMaterialName() throws Exception {
        mockMvc.perform(get("/api/materials").param("search", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].commonName").value("Laptop"));
    }

    @Test
    void returnsMaterialById() throws Exception {
        Long id = repository.findByActiveTrueOrderByCategoryAscCommonNameAsc().stream()
                .filter(material -> "Laptop".equals(material.getCommonName()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(get("/api/materials/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commonName").value("Laptop"))
                .andExpect(jsonPath("$.recoverableMaterials").value("PCB; battery; display; copper; aluminium; plastic housing; storage; RAM"));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/materials/999999"))
                .andExpect(status().isNotFound());
    }
}
