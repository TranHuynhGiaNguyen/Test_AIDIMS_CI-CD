package com.aidims.aidimsbackend.service;

import com.aidims.aidimsbackend.entity.DicomImport;
import com.aidims.aidimsbackend.repository.DicomImportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DicomImportService - Unit Tests")
class DicomImportServiceTest {

    @Mock
    private DicomImportRepository dicomImportRepository;

    @InjectMocks
    private DicomImportService dicomImportService;

    private DicomImport validImport;

    @BeforeEach
    void setUp() {
        validImport = new DicomImport();
        validImport.setPatientCode("BN001");
        validImport.setFileName("scan.dcm");
        validImport.setStatus("imported");
    }

    // =================================================================
    // NHÓM 1 – LỚP HỢP LỆ (Black-box EP)
    // =================================================================

    @Nested
    @DisplayName("Nhóm 1 – Lớp hợp lệ (EP)")
    class HopLeTests {

        @Test
        @DisplayName("✅ TC1: patientCode và fileName .dcm hợp lệ (V1, V2) — Stmt 1,3,5,6")
        void tc1_Valid_Normal() {
            when(dicomImportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DicomImport result = dicomImportService.saveDicomImport(validImport);

            assertNotNull(result);
            assertEquals("BN001", result.getPatientCode());
            assertEquals("scan.dcm", result.getFileName());
            verify(dicomImportRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("✅ TC2: fileName chữ HOA .DCM vẫn hợp lệ (V1, V2) — toLowerCase()")
        void tc2_Valid_Uppercase() {
            validImport.setFileName("SCAN.DCM");
            when(dicomImportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DicomImport result = dicomImportService.saveDicomImport(validImport);

            assertNotNull(result);
            assertEquals("SCAN.DCM", result.getFileName());
            verify(dicomImportRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("✅ TC3: patientCode có khoảng trắng thừa vẫn hợp lệ (V1, V2) — trim()")
        void tc3_Valid_Trimmed() {
            validImport.setPatientCode("  BN001  ");
            when(dicomImportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DicomImport result = dicomImportService.saveDicomImport(validImport);

            assertNotNull(result);
            verify(dicomImportRepository, times(1)).save(any());
        }
    }

    // =================================================================
    // NHÓM 2 – KHÔNG HỢP LỆ: patientCode (Black-box EP)
    // =================================================================

    @Nested
    @DisplayName("Nhóm 2 – Không hợp lệ: patientCode (EP)")
    class PatientCodeInvalidTests {

        @Test
        @DisplayName("❌ TC4: patientCode = null → IllegalArgumentException (X1) — Stmt 1,2")
        void tc4_Invalid_PatientCode_Null() {
            validImport.setPatientCode(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));

            assertEquals("Mã bệnh nhân không được để trống", ex.getMessage());
            verify(dicomImportRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ TC5: patientCode = \"\" → IllegalArgumentException (X2) — Stmt 1,2")
        void tc5_Invalid_PatientCode_Empty() {
            validImport.setPatientCode("");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));

            assertEquals("Mã bệnh nhân không được để trống", ex.getMessage());
            verify(dicomImportRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ TC6: patientCode = \"   \" → IllegalArgumentException (X3) — Stmt 1,2")
        void tc6_Invalid_PatientCode_Whitespace() {
            validImport.setPatientCode("   ");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));

            assertEquals("Mã bệnh nhân không được để trống", ex.getMessage());
            verify(dicomImportRepository, never()).save(any());
        }
    }

    // =================================================================
    // NHÓM 3 – KHÔNG HỢP LỆ: fileName (Black-box EP)
    // =================================================================

    @Nested
    @DisplayName("Nhóm 3 – Không hợp lệ: fileName (EP)")
    class FileNameInvalidTests {

        @Test
        @DisplayName("❌ TC7: fileName = null → IllegalArgumentException (X4) — Stmt 1,3,4")
        void tc7_Invalid_FileName_Null() {
            validImport.setFileName(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));

            assertEquals("Chỉ chấp nhận file định dạng .dcm", ex.getMessage());
            verify(dicomImportRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ TC8: fileName = \"scan.txt\" → IllegalArgumentException (X5) — Stmt 1,3,4")
        void tc8_Invalid_FileName_Txt() {
            validImport.setFileName("scan.txt");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));

            assertEquals("Chỉ chấp nhận file định dạng .dcm", ex.getMessage());
            verify(dicomImportRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ TC9: fileName = \"virus.exe\" → IllegalArgumentException (X5) — Stmt 1,3,4")
        void tc9_Invalid_FileName_Exe() {
            validImport.setFileName("virus.exe");

            assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));
            verify(dicomImportRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ TC10: fileName = \"scan.dicom\" → IllegalArgumentException (X5) — Stmt 1,3,4")
        void tc10_Invalid_FileName_Dicom() {
            validImport.setFileName("scan.dicom");

            assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));
            verify(dicomImportRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ TC11: fileName = \"virus.dcm.exe\" (giả mạo) → IllegalArgumentException (X5) — Stmt 1,3,4")
        void tc11_Invalid_FileName_FakeDcmExe() {
            validImport.setFileName("virus.dcm.exe");

            assertThrows(IllegalArgumentException.class, () ->
                dicomImportService.saveDicomImport(validImport));
            verify(dicomImportRepository, never()).save(any());
        }
    }

    // =================================================================
    // NHÓM 4 – WHITE-BOX: importDate tự động
    // =================================================================

    @Nested
    @DisplayName("Nhóm 4 – White-box: Statement Coverage")
    class WhiteBoxTests {

        @Test
        @DisplayName("✅ TC12: importDate được gán tự động ≠ null (Stmt 5,6)")
        void tc12_WhiteBox_ImportDate_AutoSet() {
            when(dicomImportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DicomImport result = dicomImportService.saveDicomImport(validImport);

            // Kiểm tra Stmt 5 đã được thực thi: importDate phải được gán
            assertNotNull(result.getImportDate(),
                "Stmt 5: importDate phải được gán tự động bởi saveDicomImport()");
            verify(dicomImportRepository, times(1)).save(any());
        }
    }
}