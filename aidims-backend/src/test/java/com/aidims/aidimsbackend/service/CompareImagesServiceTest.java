package com.aidims.aidimsbackend.service;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompareImagesService - Unit Tests")
class CompareImagesServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CompareImagesService compareImagesService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(compareImagesService, "serverPort", "8080");
    }

    @Test
    @DisplayName("searchByPatientCode - Anh xa va chuyen doi du lieu dong DB thanh cong")
    void searchByPatientCode_Success() {
        List<Map<String, Object>> rawRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1L);
        row.put("file_name", "chest scan.dcm");
        row.put("file_path", "dicom_uploads/chest scan.dcm");
        row.put("file_size", 1024L);
        row.put("import_date", LocalDateTime.of(2026, 6, 24, 10, 30));
        row.put("notes", "Scan notes");
        row.put("patient_code", "BN001");
        row.put("performed_by", 7L);
        row.put("status", "imported");
        row.put("study_type", "X-Ray");
        row.put("technical_params", "{\"kVp\":\"120\"}");
        row.put("patient_name", "Nguyen Van A");
        row.put("performed_by_name", "Dr. Jane Smith");
        rawRows.add(row);

        when(jdbcTemplate.queryForList(anyString(), eq("%BN001%"), eq("%BN001%")))
                .thenReturn(rawRows);

        List<Map<String, Object>> result = compareImagesService.searchByPatientCode("BN001");

        assertNotNull(result);
        assertEquals(1, result.size());

        Map<String, Object> transformed = result.get(0);
        assertEquals(1L, transformed.get("id"));
        assertEquals("chest scan.dcm", transformed.get("fileName"));
        assertEquals("BN001", transformed.get("patientCode"));
        assertEquals("X-Ray", transformed.get("modality"));
        assertEquals("Scan notes", transformed.get("description"));
        assertEquals("24/06/2026 10:30", transformed.get("dateTaken"));
        assertEquals("Nguyen Van A", transformed.get("fullName"));
        assertEquals("Dr. Jane Smith", transformed.get("performedBy"));
        assertEquals("dicom_uploads/chest scan.dcm", transformed.get("dicomFilePath"));
        
        String imageUrl = (String) transformed.get("imageUrl");
        assertNotNull(imageUrl);
        assertTrue(imageUrl.contains("chest+scan.dcm") || imageUrl.contains("chest%20scan.dcm"));
    }

    @Test
    @DisplayName("searchByPatientCode - Tu gan ten mac dinh khi thong tin ten benh nhan bi null trong DB")
    void searchByPatientCode_FallbackPatientName() {
        List<Map<String, Object>> rawRows = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 2L);
        row.put("file_name", "mri.dcm");
        row.put("file_path", "dicom_uploads/mri.dcm");
        row.put("patient_code", "BN002");
        row.put("import_date", "2026-06-24T12:00:00");
        row.put("patient_name", null);
        rawRows.add(row);

        when(jdbcTemplate.queryForList(anyString(), eq("%BN002%"), eq("%BN002%")))
                .thenReturn(rawRows);

        List<Map<String, Object>> result = compareImagesService.searchByPatientCode("BN002");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bệnh nhân BN002", result.get(0).get("fullName"));
    }

    @Test
    @DisplayName("Loi nghiep vu: Khong duoc phep tim kiem anh so sanh voi tu khoa null hoac rong")
    void searchByPatientCode_NullOrEmptyKeyword_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            compareImagesService.searchByPatientCode(null),
            "Từ khóa null/rỗng phải ném ra IllegalArgumentException"
        );
    }
}
