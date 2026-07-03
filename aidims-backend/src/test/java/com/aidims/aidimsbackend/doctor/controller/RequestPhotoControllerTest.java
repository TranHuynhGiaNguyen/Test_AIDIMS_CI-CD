package com.aidims.aidimsbackend.doctor.controller;

import com.aidims.aidimsbackend.controller.RequestPhotoController;
import com.aidims.aidimsbackend.dto.RequestPhotoDTO;
import com.aidims.aidimsbackend.service.RequestPhotoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequestPhotoController.class)
@AutoConfigureMockMvc(addFilters = false)
class RequestPhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestPhotoService requestPhotoService;

    @Autowired
    private ObjectMapper objectMapper;

    private RequestPhotoDTO createValidDto() {
        RequestPhotoDTO dto = new RequestPhotoDTO();
        dto.setPatientId(1L);
        dto.setImagingType("X-Ray");
        dto.setBodyPart("Chest");
        dto.setClinicalIndication("Chest pain");
        dto.setRequestDate(LocalDate.now().plusDays(1));
        dto.setRequestCode("REQ001");
        return dto;
    }

    @Test
    void testConnection_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/request-photo/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value("OK"));
    }

    @Test
    void createRequest_shouldReturnSuccess() throws Exception {
        RequestPhotoDTO dto = createValidDto();

        when(requestPhotoService.createRequest(any(RequestPhotoDTO.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Tạo yêu cầu chụp thành công"));
    }

    @Test
    void createRequest_whenPatientIdNull_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setPatientId(null);

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void createRequest_whenImagingTypeNull_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setImagingType(null);

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_whenImagingTypeBlank_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setImagingType(" ");

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_whenBodyPartNull_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setBodyPart(null);

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_whenBodyPartBlank_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setBodyPart(" ");

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_whenClinicalIndicationNull_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setClinicalIndication(null);

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_whenClinicalIndicationBlank_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setClinicalIndication(" ");

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_whenRequestDateInPast_shouldReturnBadRequest() throws Exception {
        RequestPhotoDTO dto = createValidDto();
        dto.setRequestDate(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void createRequest_whenServiceThrowsException_shouldReturnInternalServerError() throws Exception {
        RequestPhotoDTO dto = createValidDto();

        when(requestPhotoService.createRequest(any(RequestPhotoDTO.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/request-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void getAllRequests_shouldReturnSuccess() throws Exception {
        when(requestPhotoService.getAllRequests())
                .thenReturn(List.of(createValidDto()));

        mockMvc.perform(get("/api/request-photo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void getAllRequests_whenException_shouldReturnInternalServerError() throws Exception {
        when(requestPhotoService.getAllRequests())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/request-photo"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void getRequestsByPatient_shouldReturnSuccess() throws Exception {
        when(requestPhotoService.getRequestsByPatientId(1L))
                .thenReturn(List.of(createValidDto()));

        mockMvc.perform(get("/api/request-photo/patient/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void getRequestsByPatient_whenException_shouldReturnInternalServerError() throws Exception {
        when(requestPhotoService.getRequestsByPatientId(1L))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/request-photo/patient/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
