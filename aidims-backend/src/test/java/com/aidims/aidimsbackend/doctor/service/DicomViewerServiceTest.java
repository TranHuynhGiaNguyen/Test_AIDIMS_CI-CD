package com.aidims.aidimsbackend.doctor.service;

import com.aidims.aidimsbackend.service.DicomViewerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DicomViewerService - White Box & Unit Tests (Doctor Role)")
public class DicomViewerServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DicomViewerService dicomViewerService;

    private Map<String, Object> sampleDicomRow;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dicomViewerService, "serverPort", "8080");

        sampleDicomRow = new HashMap<>();
        sampleDicomRow.put("id", 1L);
        sampleDicomRow.put("file_name", "test_file.dcm");
        sampleDicomRow.put("file_path", "dicom_uploads/test_file.dcm");
        sampleDicomRow.put("file_size", 2048L);
        sampleDicomRow.put("import_date", LocalDateTime.of(2026, 6, 24, 11, 0));
        sampleDicomRow.put("notes", "My notes");
        sampleDicomRow.put("patient_code", "BN001");
        sampleDicomRow.put("performed_by", 5L);
        sampleDicomRow.put("status", "imported");
        sampleDicomRow.put("study_type", "CT");
        sampleDicomRow.put("technical_params", "{\"kVp\":\"120\"}");
        sampleDicomRow.put("patient_name", "John Smith");
        sampleDicomRow.put("gender", "Male");
        sampleDicomRow.put("patient_phone", "0123456789");
        sampleDicomRow.put("performed_by_name", "Dr. Alice");
    }

    // =========================================================================
    // 1. Tests for transformDicomViewerData (Branch & Condition Coverage)
    // =========================================================================

    @Test
    @DisplayName("TC_B5_01 / TC_C5_03: transformDicomViewerData - All fields valid, LocalDateTime import date")
    void transformDicomViewerData_AllFieldsValid_ShouldTransformSuccessfully() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(sampleDicomRow);

        when(jdbcTemplate.queryForList(anyString())).thenReturn(rows);

        List<Map<String, Object>> result = dicomViewerService.getAllDicomViewer();

        assertNotNull(result);
        assertEquals(1, result.size());
        Map<String, Object> record = result.get(0);
        assertEquals(1L, record.get("id"));
        assertEquals("test_file.dcm", record.get("fileName"));
        assertEquals("My notes", record.get("description"));
        assertEquals("CT", record.get("modality"));
        assertEquals("BN001", record.get("patientCode"));
        assertEquals("imported", record.get("status"));
        assertEquals("Dr. Alice", record.get("performedBy"));
        assertEquals(2048L, record.get("fileSize"));
        assertEquals("24/06/2026 11:00", record.get("dateTaken"));
        assertEquals("John Smith", record.get("fullName"));
        assertEquals("http://localhost:8080/api/dicom-viewer/image/test_file.dcm", record.get("imageUrl"));
    }

    @Test
    @DisplayName("TC_B5_02 / TC_C5_01: transformDicomViewerData - Null fields, fallback values used")
    void transformDicomViewerData_NullFields_ShouldUseFallbacks() {
        Map<String, Object> nullRow = new HashMap<>();
        nullRow.put("id", 2L);
        nullRow.put("file_name", null);
        nullRow.put("file_path", "dicom_uploads/null_file.dcm");
        nullRow.put("file_size", 0L);
        nullRow.put("import_date", null);
        nullRow.put("notes", null);
        nullRow.put("patient_code", "PAT_002");
        nullRow.put("performed_by", null);
        nullRow.put("status", "pending");
        nullRow.put("study_type", null);
        nullRow.put("technical_params", null);
        nullRow.put("patient_name", null);
        nullRow.put("performed_by_name", null);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(nullRow);

        when(jdbcTemplate.queryForList(anyString())).thenReturn(rows);

        List<Map<String, Object>> result = dicomViewerService.getAllDicomViewer();

        assertNotNull(result);
        Map<String, Object> record = result.get(0);
        assertEquals(2L, record.get("id"));
        assertNull(record.get("fileName"));
        assertEquals("Không có mô tả", record.get("description"));
        assertEquals("N/A", record.get("modality"));
        assertEquals("N/A", record.get("performedBy"));
        assertEquals("N/A", record.get("dateTaken"));
        assertEquals("Bệnh nhân PAT_002", record.get("fullName"));
        assertNull(record.get("imageUrl"));
    }

    @Test
    @DisplayName("TC_B5_03 / TC_C5_02: transformDicomViewerData - Empty strings, string import date, patient code null")
    void transformDicomViewerData_EmptyStringsAndStringDate_ShouldHandleAndSave() {
        Map<String, Object> emptyRow = new HashMap<>();
        emptyRow.put("id", 3L);
        emptyRow.put("file_name", "");
        emptyRow.put("file_path", "dicom_uploads/empty_file.dcm");
        emptyRow.put("import_date", "22/06/2026 10:00:00");
        emptyRow.put("notes", null);
        emptyRow.put("study_type", null);
        emptyRow.put("body_part", null);
        emptyRow.put("performed_by_name", null);
        emptyRow.put("patient_name", "");
        emptyRow.put("patient_code", null);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(emptyRow);

        when(jdbcTemplate.queryForList(anyString())).thenReturn(rows);

        List<Map<String, Object>> result = dicomViewerService.getAllDicomViewer();

        assertNotNull(result);
        Map<String, Object> record = result.get(0);
        assertEquals("22/06/2026 10:00:00", record.get("dateTaken"));
        assertEquals("Bệnh nhân N/A", record.get("fullName"));
        assertNull(record.get("imageUrl"));
    }

    // =========================================================================
    // 2. Original Service Tests
    // =========================================================================

    @Test
    @DisplayName("getDicomViewerById - Lay chi tiet DICOM theo ID thanh cong")
    void getDicomViewerById_Found() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(sampleDicomRow);

        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(rows);

        Map<String, Object> result = dicomViewerService.getDicomViewerById(1L);

        assertNotNull(result);
        assertEquals("test_file.dcm", result.get("fileName"));
        assertEquals("BN001", result.get("patientCode"));
    }

    @Test
    @DisplayName("getDicomViewerById - Tra ve null khi khong tim thay ID")
    void getDicomViewerById_NotFound() {
        when(jdbcTemplate.queryForList(anyString(), eq(999L))).thenReturn(new ArrayList<>());

        Map<String, Object> result = dicomViewerService.getDicomViewerById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("getDicomViewerByPatient - Lay danh sach DICOM cua benh nhan thanh cong")
    void getDicomViewerByPatient_Success() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(sampleDicomRow);

        when(jdbcTemplate.queryForList(anyString(), eq("BN001"))).thenReturn(rows);

        List<Map<String, Object>> result = dicomViewerService.getDicomViewerByPatient("BN001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BN001", result.get(0).get("patientCode"));
    }

    @Test
    @DisplayName("searchDicomViewer - Tim kiem DICOM theo tu khoa thanh cong")
    void searchDicomViewer_Success() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(sampleDicomRow);

        String keyword = "John";
        String pattern = "%" + keyword + "%";

        when(jdbcTemplate.queryForList(anyString(),
                eq(pattern), eq(pattern), eq(pattern),
                eq(pattern), eq(pattern), eq(pattern)))
                .thenReturn(rows);

        List<Map<String, Object>> result = dicomViewerService.searchDicomViewer(keyword);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getDicomViewerFilePath - Lay duong dan tep tin DICOM luu trong DB thanh cong")
    void getDicomViewerFilePath_Success() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("file_path", "dicom_uploads/test_file.dcm");
        rows.add(row);

        when(jdbcTemplate.queryForList(anyString(), eq("test_file.dcm"))).thenReturn(rows);

        String path = dicomViewerService.getDicomViewerFilePath("test_file.dcm");

        assertEquals("dicom_uploads/test_file.dcm", path);
    }

    @Test
    @DisplayName("getDicomViewerStats - Thong ke chinh xac so lieu luu tru DICOM")
    void getDicomViewerStats_Success() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> statsRow = new HashMap<>();
        statsRow.put("total_count", 15L);
        statsRow.put("mri_count", 5L);
        statsRow.put("ct_count", 5L);
        statsRow.put("xray_count", 5L);
        statsRow.put("total_size", 102400L);
        statsRow.put("unique_patients", 3L);
        rows.add(statsRow);

        when(jdbcTemplate.queryForList(anyString())).thenReturn(rows);

        Map<String, Object> stats = dicomViewerService.getDicomViewerStats();

        assertNotNull(stats);
        assertEquals(15L, stats.get("total_count"));
        assertEquals(5L, stats.get("mri_count"));
        assertEquals(102400L, stats.get("total_size"));
    }

    @Test
    @DisplayName("Loi nghiep vu: Duong dan imageUrl cua DICOM Viewer chua khoang trang phai duoc ma hoa URL thanh %20")
    void getAllDicomViewer_shouldUrlEncodeImageUrls() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>(sampleDicomRow);
        row.put("file_name", "chest scan.dcm");
        rows.add(row);

        when(jdbcTemplate.queryForList(anyString())).thenReturn(rows);

        List<Map<String, Object>> result = dicomViewerService.getAllDicomViewer();
        String imageUrl = (String) result.get(0).get("imageUrl");

        assertNotNull(imageUrl);
        assertTrue(imageUrl.contains("chest%20scan.dcm"), "Đường dẫn ảnh phải được URL encode: " + imageUrl);
    }
}
