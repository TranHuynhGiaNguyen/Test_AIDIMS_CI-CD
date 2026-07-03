package com.aidims.aidimsbackend.doctor.controller;

import com.aidims.aidimsbackend.controller.DicomViewerController;
import com.aidims.aidimsbackend.service.DicomViewerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DicomViewerController.class)
@AutoConfigureMockMvc(addFilters = false)
class DicomViewerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DicomViewerService dicomViewerService;

    @Test
    void getAllDicomViewer_shouldReturnOk() throws Exception {
        when(dicomViewerService.getAllDicomViewer())
                .thenReturn(List.of(
                        Map.of(
                                "id", 1,
                                "patientCode", "BN001",
                                "fileName", "image1.dcm"
                        )
                ));

        mockMvc.perform(get("/api/dicom-viewer/all"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllDicomViewer_whenException_shouldReturnInternalServerError() throws Exception {
        when(dicomViewerService.getAllDicomViewer())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/dicom-viewer/all"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDicomViewerById_shouldReturnOk() throws Exception {
        when(dicomViewerService.getDicomViewerById(1L))
                .thenReturn(Map.of(
                        "id", 1,
                        "patientCode", "BN001"
                ));

        mockMvc.perform(get("/api/dicom-viewer/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getDicomViewerById_whenNotFound_shouldReturnNotFound() throws Exception {
        when(dicomViewerService.getDicomViewerById(999L))
                .thenReturn(null);

        mockMvc.perform(get("/api/dicom-viewer/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDicomViewerById_whenException_shouldReturnInternalServerError() throws Exception {
        when(dicomViewerService.getDicomViewerById(1L))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/api/dicom-viewer/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDicomViewerByPatient_shouldReturnOk() throws Exception {
        when(dicomViewerService.getDicomViewerByPatient("BN001"))
                .thenReturn(List.of(
                        Map.of("patientCode", "BN001")
                ));

        mockMvc.perform(get("/api/dicom-viewer/patient/BN001"))
                .andExpect(status().isOk());
    }

    @Test
    void getDicomViewerByPatient_whenException_shouldReturnInternalServerError() throws Exception {
        when(dicomViewerService.getDicomViewerByPatient("BN001"))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/api/dicom-viewer/patient/BN001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void searchDicomViewer_shouldReturnOk() throws Exception {
        when(dicomViewerService.searchDicomViewer("BN001"))
                .thenReturn(List.of(
                        Map.of("patientCode", "BN001")
                ));

        mockMvc.perform(get("/api/dicom-viewer/search")
                                .param("keyword", "BN001"))
                .andExpect(status().isOk());
    }

    @Test
    void searchDicomViewer_whenException_shouldReturnInternalServerError() throws Exception {
        when(dicomViewerService.searchDicomViewer("BN001"))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/api/dicom-viewer/search")
                                .param("keyword", "BN001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getDicomViewerStats_shouldReturnOk() throws Exception {
        when(dicomViewerService.getDicomViewerStats())
                .thenReturn(Map.of(
                        "total", 10
                ));

        mockMvc.perform(get("/api/dicom-viewer/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void getDicomViewerStats_whenException_shouldReturnInternalServerError() throws Exception {
        when(dicomViewerService.getDicomViewerStats())
                .thenThrow(new RuntimeException());

        mockMvc.perform(get("/api/dicom-viewer/stats"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void serveImage_shouldReturnOk() throws Exception {
        when(dicomViewerService.getDicomViewerFilePath("test.jpg"))
                .thenReturn(null);

        mockMvc.perform(get("/api/dicom-viewer/serve/test.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void serveImageLegacyEndpoint_shouldReturnOk() throws Exception {
        when(dicomViewerService.getDicomViewerFilePath("test.jpg"))
                .thenReturn(null);

        mockMvc.perform(get("/api/dicom-viewer/image/test.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void downloadFile_shouldReturnOk() throws Exception {
        when(dicomViewerService.getDicomViewerFilePath("test.jpg"))
                .thenReturn(null);

        mockMvc.perform(get("/api/dicom-viewer/download/test.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void testConnection_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/dicom-viewer/test"))
                .andExpect(status().isOk());
    }

    @Test
    void healthCheck_shouldReturnHealthy() throws Exception {
        when(dicomViewerService.getAllDicomViewer())
                .thenReturn(List.of());

        when(dicomViewerService.getDicomViewerStats())
                .thenReturn(Map.of("total", 0));

        mockMvc.perform(get("/api/dicom-viewer/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"));
    }

    @Test
    void healthCheck_shouldReturnUnhealthy() throws Exception {
        when(dicomViewerService.getAllDicomViewer())
                .thenThrow(new RuntimeException("DB Error"));

        mockMvc.perform(get("/api/dicom-viewer/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("unhealthy"));
    }
}
