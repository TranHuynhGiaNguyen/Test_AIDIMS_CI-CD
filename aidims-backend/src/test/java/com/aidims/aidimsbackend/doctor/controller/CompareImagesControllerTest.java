package com.aidims.aidimsbackend.doctor.controller;

import com.aidims.aidimsbackend.controller.CompareImagesController;
import com.aidims.aidimsbackend.service.CompareImagesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompareImagesController.class)
@AutoConfigureMockMvc(addFilters = false)
class CompareImagesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompareImagesService compareImagesService;

    @Test
    void testEndpoint_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/compare-images/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("CompareImages API is active."));
    }

    @Test
    void searchPatientImages_shouldReturnImages() throws Exception {
        Map<String, Object> image = new HashMap<>();
        image.put("imageUrl", "uploads/test image.dcm");
        image.put("patientCode", "BN001");

        when(compareImagesService.searchByPatientCode("BN001"))
                .thenReturn(List.of(image));

        mockMvc.perform(get("/api/compare-images/search")
                .param("keyword", "BN001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientCode").value("BN001"))
                .andExpect(jsonPath("$[0].imageUrl").value("uploads/test%20image.dcm"));
    }

    @Test
    void searchPatientImages_withEmptyResult_shouldReturnOk() throws Exception {
        when(compareImagesService.searchByPatientCode("NOT_FOUND"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/compare-images/search")
                .param("keyword", "NOT_FOUND"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void searchPatientImages_whenServiceThrowsException_shouldReturnInternalServerError() throws Exception {
        when(compareImagesService.searchByPatientCode(anyString()))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/compare-images/search")
                .param("keyword", "BN001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void searchPatientImages_withoutKeyword_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/compare-images/search"))
                .andExpect(status().isBadRequest());
    }
}
