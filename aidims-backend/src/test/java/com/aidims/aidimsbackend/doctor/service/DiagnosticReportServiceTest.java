package com.aidims.aidimsbackend.doctor.service;

import com.aidims.aidimsbackend.entity.DiagnosticReport;
import com.aidims.aidimsbackend.repository.DiagnosticReportRepository;
import com.aidims.aidimsbackend.service.DiagnosticReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticReportService - White Box & Unit Tests (Doctor Role)")
public class DiagnosticReportServiceTest {

    @Mock
    private DiagnosticReportRepository diagnosticReportRepository;

    @InjectMocks
    private DiagnosticReportService diagnosticReportService;

    private DiagnosticReport sampleReport;

    @BeforeEach
    void setUp() {
        sampleReport = new DiagnosticReport();
        sampleReport.setReportId(1);
        sampleReport.setResultId(101);
        sampleReport.setReportCode("BC20260624001");
        sampleReport.setFindings("Findings from scan");
        sampleReport.setImpression("Normal");
        sampleReport.setRecommendations("None");
        sampleReport.setReportType(DiagnosticReport.ReportType.SoBo);
        sampleReport.setRadiologistId(2);
        sampleReport.setDictatedAt(LocalDateTime.now());
        sampleReport.setStatus(DiagnosticReport.ReportStatus.BanNhap);
    }

    // =========================================================================
    // 1. Tests for generateReportCode()
    // =========================================================================

    @Test
    @DisplayName("generateReportCode - Success scenario using count")
    void generateReportCode_Success() {
        when(diagnosticReportRepository.count()).thenReturn(5L);
        String code = diagnosticReportService.generateReportCode();
        assertNotNull(code);
        assertTrue(code.startsWith("BC"));
        assertTrue(code.endsWith("006"));
    }

    @Test
    @DisplayName("generateReportCode - Fallback scenario when DB fails")
    void generateReportCode_Fallback() {
        when(diagnosticReportRepository.count()).thenThrow(new RuntimeException("Database error"));
        String code = diagnosticReportService.generateReportCode();
        assertNotNull(code);
        assertTrue(code.startsWith("BC"));
        assertTrue(code.endsWith("001"));
    }

    // =========================================================================
    // 2. Tests for createReport(DiagnosticReport report) - Branch & Condition Coverage
    // =========================================================================

    @Test
    @DisplayName("TC_B3_01 / TC_C3_03: createReport - reportCode already exists (Duplicate report code)")
    void createReport_DuplicateReportCode_ShouldThrowRuntimeException() {
        when(diagnosticReportRepository.existsByReportCode("BC20260624001")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> diagnosticReportService.createReport(sampleReport));
        assertTrue(ex.getMessage().contains("Report code already exists"));
        verify(diagnosticReportRepository, never()).save(any(DiagnosticReport.class));
    }

    @Test
    @DisplayName("TC_B3_02 / TC_C3_01: createReport - reportCode is null (Auto Generate, Defaults applied)")
    void createReport_NullFields_ShouldSetDefaults() {
        sampleReport.setReportCode(null);
        sampleReport.setStatus(null);
        sampleReport.setReportType(null);
        sampleReport.setReferringDoctorName(null);

        when(diagnosticReportRepository.existsByReportCode(anyString())).thenReturn(false);
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);
        when(diagnosticReportRepository.count()).thenReturn(10L);

        DiagnosticReport result = diagnosticReportService.createReport(sampleReport);

        assertNotNull(result);
        assertEquals(DiagnosticReport.ReportStatus.BanNhap, result.getStatus());
        assertEquals(DiagnosticReport.ReportType.SoBo, result.getReportType());
        verify(diagnosticReportRepository, times(1)).save(any(DiagnosticReport.class));
    }

    @Test
    @DisplayName("TC_C3_02: createReport - reportCode and referringDoctorName are empty strings")
    void createReport_EmptyFields_ShouldApplyDefaults() {
        sampleReport.setReportCode("");
        sampleReport.setStatus(DiagnosticReport.ReportStatus.HoanThanh);
        sampleReport.setReportType(DiagnosticReport.ReportType.ChinhThuc);
        sampleReport.setReferringDoctorName("");

        when(diagnosticReportRepository.existsByReportCode(anyString())).thenReturn(false);
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);
        when(diagnosticReportRepository.count()).thenReturn(10L);

        DiagnosticReport result = diagnosticReportService.createReport(sampleReport);

        assertNotNull(result);
        assertEquals(DiagnosticReport.ReportStatus.HoanThanh, result.getStatus());
        assertEquals(DiagnosticReport.ReportType.ChinhThuc, result.getReportType());
        verify(diagnosticReportRepository, times(1)).save(any(DiagnosticReport.class));
    }

    @Test
    @DisplayName("TC_B3_03 / TC_C3_04: createReport - Valid code, full referring doctor info (Trims strings)")
    void createReport_ValidCodeWithReferringDoctor_ShouldTrimAndSave() {
        sampleReport.setReferringDoctorName("  Dr. Huynh  ");
        sampleReport.setReferringDoctorSpecialty("  Tim Mach  ");

        when(diagnosticReportRepository.existsByReportCode("BC20260624001")).thenReturn(false);
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);

        DiagnosticReport result = diagnosticReportService.createReport(sampleReport);

        assertNotNull(result);
        assertEquals("Dr. Huynh", result.getReferringDoctorName());
        assertEquals("Tim Mach", result.getReferringDoctorSpecialty());
        verify(diagnosticReportRepository, times(1)).save(any(DiagnosticReport.class));
    }

    @Test
    @DisplayName("TC_B3_04: createReport - Referring doctor name is set but specialty is null")
    void createReport_DoctorNameSetSpecialtyNull_ShouldTrimNameAndSave() {
        sampleReport.setReferringDoctorName("  Dr. Nguyen  ");
        sampleReport.setReferringDoctorSpecialty(null);

        when(diagnosticReportRepository.existsByReportCode("BC20260624001")).thenReturn(false);
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);

        DiagnosticReport result = diagnosticReportService.createReport(sampleReport);

        assertNotNull(result);
        assertEquals("Dr. Nguyen", result.getReferringDoctorName());
        assertNull(result.getReferringDoctorSpecialty());
        verify(diagnosticReportRepository, times(1)).save(any(DiagnosticReport.class));
    }

    // =========================================================================
    // 3. Tests for updateReport(Integer reportId, DiagnosticReport updatedReport) - Branch & Condition Coverage
    // =========================================================================

    @Test
    @DisplayName("TC_B4_01 / TC_C4_01: updateReport - report ID not found (Throws Exception)")
    void updateReport_IdNotFound_ShouldThrowRuntimeException() {
        when(diagnosticReportRepository.findById(9999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> 
            diagnosticReportService.updateReport(9999, sampleReport)
        );
        assertTrue(ex.getMessage().contains("Diagnostic report not found with ID"));
        verify(diagnosticReportRepository, never()).save(any(DiagnosticReport.class));
    }

    @Test
    @DisplayName("TC_B4_02 / TC_C4_02: updateReport - Update text fields, status is null")
    void updateReport_UpdateTextFieldsOnly_ShouldSaveSuccessfully() {
        DiagnosticReport updated = new DiagnosticReport();
        updated.setFindings("Updated findings");
        updated.setImpression("Updated impression");
        updated.setRecommendations("Updated recommendations");
        updated.setReportType(DiagnosticReport.ReportType.ChinhThuc);
        updated.setStatus(null);

        when(diagnosticReportRepository.findById(1)).thenReturn(Optional.of(sampleReport));
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);

        DiagnosticReport result = diagnosticReportService.updateReport(1, updated);

        assertNotNull(result);
        assertEquals("Updated findings", result.getFindings());
        assertEquals("Updated impression", result.getImpression());
        assertEquals("Updated recommendations", result.getRecommendations());
        assertEquals(DiagnosticReport.ReportType.ChinhThuc, result.getReportType());
        assertEquals(DiagnosticReport.ReportStatus.BanNhap, result.getStatus()); // remains draft
    }

    @Test
    @DisplayName("TC_B4_03 / TC_C4_03: updateReport - Completed status when finalizedAt is null")
    void updateReport_CompletedStatusNullFinalizedAt_ShouldSetFinalizedAt() {
        DiagnosticReport updated = new DiagnosticReport();
        updated.setStatus(DiagnosticReport.ReportStatus.HoanThanh);

        sampleReport.setFinalizedAt(null);

        when(diagnosticReportRepository.findById(1)).thenReturn(Optional.of(sampleReport));
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);

        DiagnosticReport result = diagnosticReportService.updateReport(1, updated);

        assertNotNull(result);
        assertEquals(DiagnosticReport.ReportStatus.HoanThanh, result.getStatus());
        assertNotNull(result.getFinalizedAt());
    }

    @Test
    @DisplayName("TC_B4_04 / TC_C4_04: updateReport - Completed status when finalizedAt is already set")
    void updateReport_CompletedStatusHasFinalizedAt_ShouldNotOverwrite() {
        DiagnosticReport updated = new DiagnosticReport();
        updated.setStatus(DiagnosticReport.ReportStatus.HoanThanh);

        LocalDateTime oldFinalizedTime = LocalDateTime.of(2026, 6, 20, 10, 0);
        sampleReport.setFinalizedAt(oldFinalizedTime);

        when(diagnosticReportRepository.findById(1)).thenReturn(Optional.of(sampleReport));
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);

        DiagnosticReport result = diagnosticReportService.updateReport(1, updated);

        assertNotNull(result);
        assertEquals(DiagnosticReport.ReportStatus.HoanThanh, result.getStatus());
        assertEquals(oldFinalizedTime, result.getFinalizedAt());
    }

    @Test
    @DisplayName("TC_C4_05: updateReport - Change status to draft (BanNhap)")
    void updateReport_ChangeToDraft_ShouldSaveSuccessfully() {
        DiagnosticReport updated = new DiagnosticReport();
        updated.setStatus(DiagnosticReport.ReportStatus.BanNhap);

        sampleReport.setStatus(DiagnosticReport.ReportStatus.HoanThanh);

        when(diagnosticReportRepository.findById(1)).thenReturn(Optional.of(sampleReport));
        when(diagnosticReportRepository.save(any(DiagnosticReport.class))).thenReturn(sampleReport);

        DiagnosticReport result = diagnosticReportService.updateReport(1, updated);

        assertNotNull(result);
        assertEquals(DiagnosticReport.ReportStatus.BanNhap, result.getStatus());
    }

    // =========================================================================
    // Additional Helper Tests
    // =========================================================================

    @Test
    @DisplayName("getAllReports - Success")
    void getAllReports_Success() {
        when(diagnosticReportRepository.findAll()).thenReturn(Arrays.asList(sampleReport));
        List<DiagnosticReport> result = diagnosticReportService.getAllReports();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getReportById - Success")
    void getReportById_Success() {
        when(diagnosticReportRepository.findById(1)).thenReturn(Optional.of(sampleReport));
        Optional<DiagnosticReport> result = diagnosticReportService.getReportById(1);
        assertTrue(result.isPresent());
        assertEquals("BC20260624001", result.get().getReportCode());
    }
}
