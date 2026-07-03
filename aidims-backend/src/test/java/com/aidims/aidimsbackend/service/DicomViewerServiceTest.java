package com.aidims.aidimsbackend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.aidims.aidimsbackend.repository.ImagingResultRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DicomViewerService - Unit Test")
class DicomViewerServiceTest {

    @Mock
    private ImagingResultRepository imagingResultRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DicomViewerService dicomViewerService;

    private Map<String, Object> dbRow;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                dicomViewerService,
                "serverPort",
                "8080"
        );

        dbRow = new HashMap<>();

        dbRow.put("id", 1L);
        dbRow.put("body_part", "CHEST");
        dbRow.put("file_name", "chest_xray.dcm");
        dbRow.put("file_path", "dicom_uploads/chest_xray.dcm");

        dbRow.put("patient_code", "BN999");
        dbRow.put("patient_name", "Bệnh Nhân Nguyễn Văn A");

        dbRow.put(
                "import_date",
                LocalDateTime.of(
                        2026,
                        7,
                        1,
                        10,
                        30
                )
        );

        dbRow.put("status", "imported");
        dbRow.put("study_type", "X-Ray");
        dbRow.put("notes", "Test");
        dbRow.put("performed_by_name", "Admin");
    }

    @Test
    @DisplayName("Get all DICOM viewer successfully")
    void testGetAllDicomViewer_Success() {

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(dbRow);

        when(
                jdbcTemplate.queryForList(anyString())
        ).thenReturn(rows);

        List<Map<String, Object>> result =
                dicomViewerService.getAllDicomViewer();

        assertNotNull(result);

        assertEquals(
                1,
                result.size()
        );

        Map<String, Object> item = result.get(0);

        assertEquals(
                "Bệnh Nhân Nguyễn Văn A",
                item.get("fullName")
        );

        assertEquals(
                "01/07/2026 10:30",
                item.get("dateTaken")
        );

        assertEquals(
                "http://localhost:8080/api/dicom-viewer/image/chest_xray.dcm",
                item.get("imageUrl")
        );

        verify(
                jdbcTemplate,
                times(1)
        ).queryForList(anyString());
    }

    @Test
    @DisplayName("Get DICOM by ID successfully")
    void testGetDicomViewerById_Found() {

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(dbRow);

        when(
                jdbcTemplate.queryForList(
                        anyString(),
                        eq(1L)
                )
        ).thenReturn(rows);

        Map<String, Object> result =
                dicomViewerService.getDicomViewerById(1L);

        assertNotNull(result);

        assertEquals(
                1L,
                result.get("id")
        );

        assertEquals(
                "Bệnh Nhân Nguyễn Văn A",
                result.get("fullName")
        );

        assertEquals(
                "http://localhost:8080/api/dicom-viewer/image/chest_xray.dcm",
                result.get("imageUrl")
        );

        verify(
                jdbcTemplate,
                times(1)
        ).queryForList(
                anyString(),
                eq(1L)
        );
    }

    @Test
    @DisplayName("Missing file name should return null image url")
    void testGetDicomViewerById_MissingFileName() {

        dbRow.put("file_name", "");

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(dbRow);

        when(
                jdbcTemplate.queryForList(
                        anyString(),
                        eq(1L)
                )
        ).thenReturn(rows);

        Map<String, Object> result =
                dicomViewerService.getDicomViewerById(1L);

        assertNotNull(result);

        assertNull(
                result.get("imageUrl")
        );
    }

    @Test
    @DisplayName("Get DICOM file path successfully")
    void testGetDicomViewerFilePath_Success() {

        List<Map<String, Object>> rows = new ArrayList<>();

        Map<String, Object> row = new HashMap<>();

        row.put(
                "file_path",
                "dicom_uploads/chest_xray.dcm"
        );

        rows.add(row);

        when(
                jdbcTemplate.queryForList(
                        anyString(),
                        eq("chest_xray.dcm")
                )
        ).thenReturn(rows);

        String filePath =
                dicomViewerService.getDicomViewerFilePath(
                        "chest_xray.dcm"
                );

        assertEquals(
                "dicom_uploads/chest_xray.dcm",
                filePath
        );

        verify(
                jdbcTemplate,
                times(1)
        ).queryForList(
                anyString(),
                eq("chest_xray.dcm")
        );
    }
}