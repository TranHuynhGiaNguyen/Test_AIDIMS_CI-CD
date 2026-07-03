package com.aidims.aidimsbackend.doctor.service;

import com.aidims.aidimsbackend.dto.RequestPhotoDTO;
import com.aidims.aidimsbackend.entity.RequestPhoto;
import com.aidims.aidimsbackend.repository.RequestPhotoRepository;
import com.aidims.aidimsbackend.service.RequestPhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestPhotoService - White Box & Unit Tests (Doctor Role)")
public class RequestPhotoServiceTest {

    @Mock
    private RequestPhotoRepository requestPhotoRepository;

    @InjectMocks
    private RequestPhotoService photoService;

    private RequestPhotoDTO sampleDTO;
    private RequestPhoto sampleEntity;

    @BeforeEach
    void setUp() {
        sampleDTO = new RequestPhotoDTO();
        sampleDTO.setRequestId(1L);
        sampleDTO.setRequestCode("REQ20260624001");
        sampleDTO.setPatientId(10L);
        sampleDTO.setImagingType("X-Ray");
        sampleDTO.setBodyPart("Chest");
        sampleDTO.setClinicalIndication("Cough");
        sampleDTO.setNotes("Notes");
        sampleDTO.setPriorityLevel("high");
        sampleDTO.setRequestDate(LocalDate.now());
        sampleDTO.setStatus("pending");

        sampleEntity = new RequestPhoto();
        sampleEntity.setRequestId(1L);
        sampleEntity.setRequestCode("REQ20260624001");
        sampleEntity.setPatientId(10L);
        sampleEntity.setImagingType("X-Ray");
        sampleEntity.setBodyPart("Chest");
        sampleEntity.setClinicalIndication("Cough");
        sampleEntity.setNotes("Notes");
        sampleEntity.setPriorityLevel("high");
        sampleEntity.setRequestDate(LocalDate.now());
        sampleEntity.setStatus("pending");
    }

    // =========================================================================
    // 1. Tests for createRequest(RequestPhotoDTO dto) - Branch & Condition Coverage
    // =========================================================================

    @Test
    @DisplayName("TC_B1_01 / TC_C1_01: createRequest - requestCode is null (Auto Generate Code)")
    void createRequest_NullRequestCode_ShouldAutoGenerate() {
        sampleDTO.setRequestCode(null);
        
        when(requestPhotoRepository.existsByRequestCode(anyString())).thenReturn(false);
        when(requestPhotoRepository.save(any(RequestPhoto.class))).thenReturn(sampleEntity);

        RequestPhotoDTO result = photoService.createRequest(sampleDTO);

        assertNotNull(result);
        verify(requestPhotoRepository, times(2)).existsByRequestCode(anyString());
        verify(requestPhotoRepository, times(1)).save(any(RequestPhoto.class));
    }

    @Test
    @DisplayName("TC_C1_02: createRequest - requestCode is empty string (Auto Generate Code)")
    void createRequest_EmptyRequestCode_ShouldAutoGenerate() {
        sampleDTO.setRequestCode("");

        when(requestPhotoRepository.existsByRequestCode(anyString())).thenReturn(false);
        when(requestPhotoRepository.save(any(RequestPhoto.class))).thenReturn(sampleEntity);

        RequestPhotoDTO result = photoService.createRequest(sampleDTO);

        assertNotNull(result);
        verify(requestPhotoRepository, times(2)).existsByRequestCode(anyString());
        verify(requestPhotoRepository, times(1)).save(any(RequestPhoto.class));
    }

    @Test
    @DisplayName("TC_B1_02 / TC_C1_04: createRequest - Valid non-existent requestCode")
    void createRequest_ValidNewRequestCode_ShouldSaveSuccessfully() {
        when(requestPhotoRepository.existsByRequestCode("REQ20260624001")).thenReturn(false);
        when(requestPhotoRepository.save(any(RequestPhoto.class))).thenReturn(sampleEntity);

        RequestPhotoDTO result = photoService.createRequest(sampleDTO);

        assertNotNull(result);
        assertEquals("REQ20260624001", result.getRequestCode());
        verify(requestPhotoRepository, times(1)).save(any(RequestPhoto.class));
    }

    @Test
    @DisplayName("TC_B1_03 / TC_C1_03: createRequest - Existing requestCode (Duplicate Code)")
    void createRequest_DuplicateRequestCode_ShouldThrowRuntimeException() {
        when(requestPhotoRepository.existsByRequestCode("REQ20260624001")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> photoService.createRequest(sampleDTO));
        assertTrue(ex.getMessage().contains("Lỗi khi tạo yêu cầu chụp") || ex.getMessage().contains("Mã yêu cầu đã tồn tại"));
        verify(requestPhotoRepository, never()).save(any(RequestPhoto.class));
    }

    @Test
    @DisplayName("Path 4: createRequest - Database Connection / Save Exception")
    void createRequest_DatabaseException_ShouldThrowRuntimeException() {
        when(requestPhotoRepository.existsByRequestCode("REQ20260624001")).thenReturn(false);
        when(requestPhotoRepository.save(any(RequestPhoto.class))).thenThrow(new RuntimeException("DB Connection failure"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> photoService.createRequest(sampleDTO));
        assertTrue(ex.getMessage().contains("Lỗi khi tạo yêu cầu chụp"));
    }

    // =========================================================================
    // 2. Tests for generateRequestCode() - Loop Path Coverage
    // =========================================================================

    @Test
    @DisplayName("TC_B2_01 / TC_C2_01: generateRequestCode - Loop runs 0 times (No duplicate)")
    void generateRequestCode_NoDuplicate_ShouldReturnOriginalCode() {
        sampleDTO.setRequestCode(null);
        when(requestPhotoRepository.existsByRequestCode(anyString())).thenReturn(false);
        when(requestPhotoRepository.save(any(RequestPhoto.class))).thenReturn(sampleEntity);

        RequestPhotoDTO result = photoService.createRequest(sampleDTO);

        assertNotNull(result);
        verify(requestPhotoRepository, times(2)).existsByRequestCode(anyString());
    }

    @Test
    @DisplayName("TC_B2_02 / TC_C2_02: generateRequestCode - Loop runs multiple times (Simulates duplicates)")
    void generateRequestCode_MultipleDuplicates_ShouldAppendSuffix() {
        sampleDTO.setRequestCode(null);
        
        when(requestPhotoRepository.existsByRequestCode(anyString()))
            .thenReturn(true)  // Original timestamp code exists
            .thenReturn(true)  // Timestamp + "01" exists
            .thenReturn(false) // Timestamp + "02" is free
            .thenReturn(false); // Second check inside createRequest

        when(requestPhotoRepository.save(any(RequestPhoto.class))).thenReturn(sampleEntity);

        RequestPhotoDTO result = photoService.createRequest(sampleDTO);

        assertNotNull(result);
        // Checked 4 times: original (true), "01" (true), "02" (false) in generateRequestCode, then check in createRequest
        verify(requestPhotoRepository, times(4)).existsByRequestCode(anyString());
    }

    // =========================================================================
    // Additional Service Helper Tests
    // =========================================================================

    @Test
    @DisplayName("getAllRequests - Success")
    void getAllRequests_Success() {
        when(requestPhotoRepository.findAll()).thenReturn(Arrays.asList(sampleEntity));

        List<RequestPhotoDTO> result = photoService.getAllRequests();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("REQ20260624001", result.get(0).getRequestCode());
    }

    @Test
    @DisplayName("getRequestsByPatientId - Success")
    void getRequestsByPatientId_Success() {
        when(requestPhotoRepository.findByPatientIdOrderByCreatedAtDesc(10L))
                .thenReturn(Arrays.asList(sampleEntity));

        List<RequestPhotoDTO> result = photoService.getRequestsByPatientId(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getPatientId());
    }

    @Test
    @DisplayName("Loi nghiep vu: Service khong duoc phep tao yeu cau chup phim voi ngay chi dinh trong qua khu")
    void createRequest_withPastDate_shouldThrowException() {
        sampleDTO.setRequestDate(LocalDate.now().minusDays(10));

        assertThrows(IllegalArgumentException.class, () -> photoService.createRequest(sampleDTO),
            "Ngày chỉ định trong quá khứ phải ném ra IllegalArgumentException");
    }
}
