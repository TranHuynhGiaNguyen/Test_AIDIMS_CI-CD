package com.aidims.aidimsbackend.controller;

import com.aidims.aidimsbackend.service.DicomFileService;
import com.aidims.aidimsbackend.service.DicomImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DicomImportController - Import DICOM Unit Tests")
class DicomImportControllerTest {

    @Mock
    private DicomImportService dicomImportService;

    @Mock
    private DicomFileService dicomFileService;

    @InjectMocks
    private DicomImportController dicomImportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dicomImportController).build();
    }

    // ===================================================================
    // BLACK-BOX TESTS: Equivalence Partitioning + Boundary Value Analysis
    // ===================================================================

    @Nested
    @DisplayName("Black-box: Equivalence Partitioning Tests")
    class EquivalencePartitioningTests {

        // ==================== LỚP HỢP LỆ (PASS) ====================

        @Test
        @DisplayName("TC1: Hop le - File .dcm, dung kich thuoc, day du metadata")
        void testImportDicom_ValidFile_Dcm() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    content
            );

            when(dicomImportService.saveDicomImport(any())).thenReturn(null);
            doNothing().when(dicomFileService).copyFileToFrontend(any(), any());

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test import")
                    .param("performed_by", "7"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Import thành công"));

            verify(dicomImportService, times(1)).saveDicomImport(any());
            verify(dicomFileService, times(1)).copyFileToFrontend(any(), any());
        }

        @Test
        @DisplayName("TC2: BVA - File size MIN (1 byte)")
        void testImportDicom_FileSize_Min() throws Exception {
            byte[] content = new byte[1];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "min.dcm",
                    "application/octet-stream",
                    content
            );

            when(dicomImportService.saveDicomImport(any())).thenReturn(null);
            doNothing().when(dicomFileService).copyFileToFrontend(any(), any());

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC3: BVA - File size MAX (100MB)")
        void testImportDicom_FileSize_Max() throws Exception {
            byte[] content = new byte[100 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "max.dcm",
                    "application/octet-stream",
                    content
            );

            when(dicomImportService.saveDicomImport(any())).thenReturn(null);
            doNothing().when(dicomFileService).copyFileToFrontend(any(), any());

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC4: Hop le - File .dicom extension")
        void testImportDicom_ValidFile_DicomExtension() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dicom",
                    "application/octet-stream",
                    content
            );

            when(dicomImportService.saveDicomImport(any())).thenReturn(null);
            doNothing().when(dicomFileService).copyFileToFrontend(any(), any());

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test import")
                    .param("performed_by", "7"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC5: Hop le - File .dc3 extension")
        void testImportDicom_ValidFile_Dc3Extension() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dc3",
                    "application/octet-stream",
                    content
            );

            when(dicomImportService.saveDicomImport(any())).thenReturn(null);
            doNothing().when(dicomFileService).copyFileToFrontend(any(), any());

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test import")
                    .param("performed_by", "7"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("TC6: Hop le - File .dic extension")
        void testImportDicom_ValidFile_DicExtension() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dic",
                    "application/octet-stream",
                    content
            );

            when(dicomImportService.saveDicomImport(any())).thenReturn(null);
            doNothing().when(dicomFileService).copyFileToFrontend(any(), any());

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test import")
                    .param("performed_by", "7"))
                    .andExpect(status().isOk());
        }

        // ==================== LỚP KHÔNG HỢP LỆ (FAIL - CÓ VALIDATION) ====================

        @Test
        @DisplayName("TC7: File null - Lop khong hop le X1")
        void testImportDicom_FileNull() throws Exception {
            mockMvc.perform(multipart("/api/dicom-import/import")
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());

            verify(dicomImportService, never()).saveDicomImport(any());
        }

        @Test
        @DisplayName("TC8: File rong - Lop khong hop le X2, X8")
        void testImportDicom_EmptyFile() throws Exception {
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
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());

            verify(dicomImportService, never()).saveDicomImport(any());
            verify(dicomFileService, never()).copyFileToFrontend(any(), any());
        }

        @Test
        @DisplayName("TC9: File qua lon >100MB - Lop khong hop le X9")
        void testImportDicom_FileTooLarge() throws Exception {
            byte[] content = new byte[101 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "large.dcm",
                    "application/octet-stream",
                    content
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC10: File .txt - Lop khong hop le X3")
        void testImportDicom_InvalidExtension_Txt() throws Exception {
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
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("DICOM")));

            verify(dicomImportService, never()).saveDicomImport(any());
        }

        @Test
        @DisplayName("TC11: File .jpg - Lop khong hop le X4")
        void testImportDicom_InvalidExtension_Jpg() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "image.jpg",
                    "image/jpeg",
                    "image".getBytes()
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("TC12: Fake .dcm.exe - Lop khong hop le X7")
        void testImportDicom_FakeDcmExeExtension() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "virus.dcm.exe",
                    "application/octet-stream",
                    "exe".getBytes()
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());
        }

        // ==================== LỚP KHÔNG HỢP LỆ (ĐÃ FIX BUG - MONG ĐỢI 400) ====================

        @Test
        @DisplayName("TC13: patient_code null - Lop khong hop le X10")
        void testImportDicom_NullPatientCode() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    content
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());

            verify(dicomImportService, never()).saveDicomImport(any());
        }

        @Test
        @DisplayName("TC14: patient_code rong - DA FIX BUG, mong doi 400")
        void testImportDicom_EmptyPatientCode() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    content
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Ma benh nhan khong duoc de trong!"));

            verify(dicomImportService, never()).saveDicomImport(any());
        }

        @Test
        @DisplayName("TC15: study_type null - Lop khong hop le X13")
        void testImportDicom_NullStudyType() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    content
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

            verify(dicomImportService, never()).saveDicomImport(any());
        }

        @Test
        @DisplayName("TC16: study_type rong - DA FIX BUG, mong doi 400")
        void testImportDicom_EmptyStudyType() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    content
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Loai chup khong duoc de trong!"));

            verify(dicomImportService, never()).saveDicomImport(any());
        }

        @Test
        @DisplayName("TC17: patient_name null - Lop khong hop le X15")
        void testImportDicom_NullPatientName() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    content
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("study_type", "CT Scanner")
                    .param("body_part", "Chest")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());

            verify(dicomImportService, never()).saveDicomImport(any());
        }

        @Test
        @DisplayName("TC18: body_part null - Lop khong hop le X17")
        void testImportDicom_NullBodyPart() throws Exception {
            byte[] content = new byte[1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "scan.dcm",
                    "application/octet-stream",
                    content
            );

            mockMvc.perform(multipart("/api/dicom-import/import")
                    .file(file)
                    .param("patient_code", "BN001")
                    .param("patient_name", "Nguyễn Văn A")
                    .param("study_type", "CT Scanner")
                    .param("technical_params", "{\"kVp\":\"120\"}")
                    .param("notes", "Test")
                    .param("performed_by", "7"))
                    .andExpect(status().isBadRequest());

            verify(dicomImportService, never()).saveDicomImport(any());
        }
    }
}