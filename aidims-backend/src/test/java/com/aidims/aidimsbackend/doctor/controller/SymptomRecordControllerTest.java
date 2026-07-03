package com.aidims.aidimsbackend.doctor.controller;

import com.aidims.aidimsbackend.service.PatientService;
import com.aidims.aidimsbackend.service.SymptomRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aidims.aidimsbackend.controller.SymptomRecordController;
@WebMvcTest(SymptomRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SymptomRecordController - Black Box & API Unit Tests (Receptionist Role)")
class SymptomRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SymptomRecordService symptomRecordService;

    @MockBean
    private PatientService patientService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("TC_BB_SYM_API_01: createSymptomRecord - Pain Scale exceeds range [0-10] should return 400 Bad Request")
    void createSymptomRecord_withInvalidPainScale_shouldReturnBadRequest() throws Exception {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("patient_id", "1");
        requestData.put("chief_complaint", "Đau đầu");
        requestData.put("severity_level", "Nặng");
        requestData.put("priority_level", "Khẩn cấp");
        requestData.put("pain_scale", 99);

        mockMvc.perform(post("/api/symptom-record/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestData)))
                .andExpect(status().isBadRequest());
    }
}