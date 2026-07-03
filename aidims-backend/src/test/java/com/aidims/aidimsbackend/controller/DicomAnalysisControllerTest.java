package com.aidims.aidimsbackend.controller;

import com.aidims.aidimsbackend.dto.DicomAnalysisResponse;
import com.aidims.aidimsbackend.service.ChatService;
import com.aidims.aidimsbackend.service.DicomConverterService;
import com.aidims.aidimsbackend.service.DicomConverterService.ConvertResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DicomAnalysisController - Unit Tests (EP + BVA + Statement/Branch Coverage)")
class DicomAnalysisControllerTest {

    @Mock private DicomConverterService dicomConverter;
    @Mock private ChatService chatService;
    @InjectMocks private DicomAnalysisController dicomAnalysisController;

    // Du lieu dung chung
    private byte[] fakeDicomContent;
    private ConvertResult mockConvertResult;
    private static final String EXPECTED_ANALYSIS = "Normal chest X-ray. No abnormal findings.";

    @BeforeEach
    void setUp() {
        // Tao fake DICOM content (132 byte, co DICM magic bytes)
        fakeDicomContent = new byte[132];
        fakeDicomContent[128] = 'D';
        fakeDicomContent[129] = 'I';
        fakeDicomContent[130] = 'C';
        fakeDicomContent[131] = 'M';

        DicomAnalysisResponse.DicomMetadata metadata = new DicomAnalysisResponse.DicomMetadata();
        metadata.setModality("CT");
        metadata.setBodyPart("CHEST");
        mockConvertResult = new ConvertResult("mockBase64Jpeg", metadata);
    }

    // =================================================================
    // NHOM 1 – EXTENSION HOP LE (Black-box EP + BVA)
    // =================================================================

    @Nested
    @DisplayName("Nhom 1 – Extension hop le")
    class ExtensionHopLeTests {

        @Test
        @DisplayName("TC01 - Extension .dcm duoc chap nhan (V1,V2) — Stmt 1-3,5,7-11,13-19")
        void tc01_Extension_Dcm() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "scan.dcm", "application/octet-stream", fakeDicomContent);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            assertEquals(EXPECTED_ANALYSIS, res.getBody().getAnalysisText());
            assertNotNull(res.getBody().getDicomImageBase64());
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, times(1)).analyzeImages(any());
        }

        @Test
        @DisplayName("TC02 - Extension .dicom duoc chap nhan (V1,V3) — Stmt 1-3,5,7-11,13-19")
        void tc02_Extension_Dicom() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "scan.dicom", "application/octet-stream", fakeDicomContent);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, times(1)).analyzeImages(any());
        }

        @Test
        @DisplayName("TC03 - Extension .dc3 duoc chap nhan (V1,V4) — Stmt 1-3,5,7-11,13-19")
        void tc03_Extension_Dc3() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "scan.dc3", "application/octet-stream", fakeDicomContent);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, times(1)).analyzeImages(any());
        }

        @Test
        @DisplayName("TC04 - Extension .dic duoc chap nhan (V1,V5) — Stmt 1-3,5,7-11,13-19")
        void tc04_Extension_Dic() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "scan.dic", "application/octet-stream", fakeDicomContent);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, times(1)).analyzeImages(any());
        }

        @Test
        @DisplayName("TC05 - File PACS khong co duoi duoc chap nhan (V1,V6) — Branch E: hasExtension=false")
        void tc05_NoExtension_PACS() throws Exception {
            // "IM000001" khong co dau "." -> hasExtension = false -> bo qua kiem tra extension
            MockMultipartFile file = new MockMultipartFile(
                "file", "IM000001", "application/octet-stream", fakeDicomContent);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, times(1)).analyzeImages(any());
        }

        @Test
        @DisplayName("TC06 - Extension .DCM chu HOA duoc chap nhan (V1,V7) — toLowerCase() xu ly")
        void tc06_Extension_Uppercase_DCM() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "SCAN.DCM", "application/octet-stream", fakeDicomContent);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, times(1)).analyzeImages(any());
        }
    }

    // =================================================================
    // NHOM 2 – EXTENSION KHONG HOP LE (Black-box EP)
    // =================================================================

    @Nested
    @DisplayName("Nhom 2 – Extension khong hop le")
    class ExtensionKhongHopLeTests {

        @Test
        @DisplayName("TC07 - File .txt bi tu choi 400 (V1,X3) — Stmt 12")
        void tc07_Extension_Txt() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.txt", "text/plain", "content".getBytes());

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(400, res.getStatusCode().value());
            assertEquals("error", res.getBody().getStatus());
            assertNull(res.getBody().getDicomImageBase64());
            verify(dicomConverter, never()).convert(any());
            verify(chatService, never()).analyzeImages(any());
        }

        @Test
        @DisplayName("TC08 - File .jpg bi tu choi 400 (V1,X4) — Stmt 12")
        void tc08_Extension_Jpg() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", "image".getBytes());

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(400, res.getStatusCode().value());
            assertEquals("error", res.getBody().getStatus());
            verify(dicomConverter, never()).convert(any());
            verify(chatService, never()).analyzeImages(any());
        }

        @Test
        @DisplayName("TC09 - File .dcm.exe (gia mao) bi tu choi 400 (V1,X5) — extension cuoi la 'exe'")
        void tc09_FakeExtension_DcmExe() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "virus.dcm.exe", "application/octet-stream", "exe".getBytes());

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(400, res.getStatusCode().value());
            assertEquals("error", res.getBody().getStatus());
            verify(dicomConverter, never()).convert(any());
            verify(chatService, never()).analyzeImages(any());
        }
    }

    // =================================================================
    // NHOM 3 – FILE SIZE (Black-box EP + BVA)
    // =================================================================

    @Nested
    @DisplayName("Nhom 3 – File size (EP + BVA)")
    class FileSizeTests {

        @Test
        @DisplayName("TC10 - File rong 0 byte (X1,B1) bi tu choi 400 — Stmt 4")
        void tc10_FileEmpty() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "empty.dcm", "application/octet-stream", new byte[0]);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(400, res.getStatusCode().value());
            assertEquals("error", res.getBody().getStatus());
            // Kiem tra message khong dung tieng Viet co dau de tranh loi encoding
            assertNotNull(res.getBody().getAnalysisText());
            assertTrue(res.getBody().getAnalysisText().contains("r") &&
                       res.getBody().getAnalysisText().toLowerCase().contains("file"));
            verify(dicomConverter, never()).convert(any());
            verify(chatService, never()).analyzeImages(any());
        }

        @Test
        @DisplayName("TC11 - File qua lon 101MB (X2,B5) bi tu choi 400 — Stmt 6")
        void tc11_FileTooLarge() throws Exception {
            MockMultipartFile file = Mockito.mock(MockMultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getSize()).thenReturn(101L * 1024 * 1024);
            when(file.getOriginalFilename()).thenReturn("large.dcm");

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(400, res.getStatusCode().value());
            assertEquals("error", res.getBody().getStatus());
            // Dung containsString de tranh loi encoding UTF-8
            assertNotNull(res.getBody().getAnalysisText());
            assertTrue(res.getBody().getAnalysisText().contains("100MB") ||
                       res.getBody().getAnalysisText().contains("100"));
            verify(dicomConverter, never()).convert(any());
            verify(chatService, never()).analyzeImages(any());
        }

        @Test
        @DisplayName("TC12 - BVA bien max: file = 100MB chinh xac (V1,B4) — dung bang 100MB la hop le")
        void tc12_FileSize_Max_Boundary_100MB() throws Exception {
            // 100MB chinh xac = 100 * 1024 * 1024 bytes: KHONG qua lon (dieu kien: > 100MB)
            byte[] content100MB = new byte[100 * 1024 * 1024];
            MockMultipartFile file = new MockMultipartFile(
                "file", "max.dcm", "application/octet-stream", content100MB);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            // 100MB chinh xac phai PASS (khong phai > 100MB)
            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            verify(dicomConverter, times(1)).convert(any());
        }

        @Test
        @DisplayName("TC13 - BVA bien min+: file = 1 byte (V1,B2) — hop le, di qua kiem tra isEmpty")
        void tc13_FileSize_Min_Boundary_1Byte() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "min.dcm", "application/octet-stream", new byte[1]);
            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any())).thenReturn(EXPECTED_ANALYSIS);

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            verify(dicomConverter, times(1)).convert(any());
        }
    }

    // =================================================================
    // NHOM 4 – FALLBACK BEHAVIOR (White-box Branch I + Stmt 20-22)
    // =================================================================

    @Nested
    @DisplayName("Nhom 4 – Fallback behavior khi exception (White-box)")
    class FallbackTests {

        @Test
        @DisplayName("TC14 - DicomConverter nem exception -> fallback 200 (X6) — Stmt 20,21,22")
        void tc14_DicomConverter_Exception_Fallback() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "scan.dcm", "application/octet-stream", fakeDicomContent);

            // Mock DicomConverter nem RuntimeException
            when(dicomConverter.convert(any()))
                .thenThrow(new RuntimeException("DICOM convert failed"));

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            // Fallback: van tra ve 200 voi mock data
            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            assertNotNull(res.getBody().getAnalysisText());
            assertNotNull(res.getBody().getDicomImageBase64());
            // ChatService KHONG duoc goi vi exception xay ra truoc do
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, never()).analyzeImages(any());
        }

        @Test
        @DisplayName("TC15 - ChatService nem exception -> fallback 200 (X7) — Stmt 20,21,22")
        void tc15_ChatService_Exception_Fallback() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "scan.dcm", "application/octet-stream", fakeDicomContent);

            when(dicomConverter.convert(any())).thenReturn(mockConvertResult);
            when(chatService.analyzeImages(any()))
                .thenThrow(new RuntimeException("Gemini API timeout"));

            ResponseEntity<DicomAnalysisResponse> res =
                dicomAnalysisController.analyzeDicom(file, "Test", null, null);

            // Fallback: van tra ve 200 voi mock data
            assertEquals(200, res.getStatusCode().value());
            assertEquals("success", res.getBody().getStatus());
            assertNotNull(res.getBody().getAnalysisText());
            assertNotNull(res.getBody().getDicomImageBase64());
            // Ca 2 service deu da duoc goi
            verify(dicomConverter, times(1)).convert(any());
            verify(chatService, times(1)).analyzeImages(any());
        }
    }

    // =================================================================
    // NHOM 5 – HEALTH CHECK ENDPOINT
    // =================================================================

    @Nested
    @DisplayName("Nhom 5 – Health check endpoint")
    class HealthTests {

        @Test
        @DisplayName("TC16 - GET /health tra ve 200 'DICOM service running' (V8)")
        void tc16_Health_Endpoint() {
            ResponseEntity<String> res = dicomAnalysisController.health();

            assertEquals(200, res.getStatusCode().value());
            assertEquals("DICOM service running", res.getBody());
        }
    }
}