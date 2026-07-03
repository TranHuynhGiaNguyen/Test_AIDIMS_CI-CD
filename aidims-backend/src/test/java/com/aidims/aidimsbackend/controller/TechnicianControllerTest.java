package com.aidims.aidimsbackend.controller;

import com.aidims.aidimsbackend.entity.DicomImport;
import com.aidims.aidimsbackend.service.DicomFileService;
import com.aidims.aidimsbackend.service.DicomImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechnicianController - Unit Tests (Sẽ sinh bug nếu fail)")
class TechnicianControllerTest {

    @Mock
    private DicomImportService dicomImportService;

    @Mock
    private DicomFileService dicomFileService;

    @InjectMocks
    private DicomImportController dicomImportController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dicomImportController).build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Import DICOM - File Validation")
    class FileValidationTests {

        @Test
        @DisplayName("File rong bi tu choi")
        void testImportDicom_emptyFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "empty.dcm",
                    "application/octet-stream",
                    new byte[0]
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("File .txt bi tu choi")
        void testImportDicom_invalidExtension() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.txt",
                    "text/plain",
                    "content".getBytes()
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Thieu patient_code bi tu choi")
        void testImportDicom_missingPatientCode() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    "fake dicom".getBytes()
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Thieu study_type bi tu choi")
        void testImportDicom_missingStudyType() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    "fake dicom".getBytes()
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("File null bi tu choi")
        void testImportDicom_nullFile() throws Exception {
            mockMvc.perform(multipart("/api/dicom-import/import")
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Import DICOM - Boundary Value Analysis")
    class BVATests {

        @Test
        @DisplayName("File >100MB bi tu choi")
        void testImportDicom_fileTooLarge() throws Exception {
            byte[] largeContent = new byte[101 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "large.dcm",
                    "application/octet-stream",
                    largeContent
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }
    }
}