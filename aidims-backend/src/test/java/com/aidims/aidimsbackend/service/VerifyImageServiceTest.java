package com.aidims.aidimsbackend.service;

import com.aidims.aidimsbackend.entity.VerifyImage;
import com.aidims.aidimsbackend.repository.VerifyImageRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyImageService - Unit Tests")
class VerifyImageServiceTest {

    @Mock
    private VerifyImageRepository verifyImageRepo;

    @InjectMocks
    private VerifyImageService verifyImageService;

    private VerifyImage sampleVerify;

    @BeforeEach
    void setUp() {
        sampleVerify = new VerifyImage();
        sampleVerify.setId(1L);
        sampleVerify.setImageId(10L);
        sampleVerify.setCheckedBy(5L);
        sampleVerify.setResult("approved");
        sampleVerify.setNote("Image is clear");
    }

    @Test
    @DisplayName("saveVerifyImage - Luu thong tin phe duyet anh thanh cong")
    void saveVerifyImage_Success() {
        when(verifyImageRepo.save(any(VerifyImage.class))).thenAnswer(inv -> inv.getArgument(0));

        VerifyResult resultWrapper = new VerifyResult();
        VerifyImage result = verifyImageService.saveVerifyImage(sampleVerify);

        assertNotNull(result);
        assertEquals("approved", result.getResult());
        assertNotNull(result.getCheckTime());
        verify(verifyImageRepo, times(1)).save(sampleVerify);
    }

    @Test
    @DisplayName("getAllVerifyImages - Lay danh sach toan bo phe duyet anh thanh cong")
    void getAllVerifyImages_Success() {
        when(verifyImageRepo.findAll()).thenReturn(Arrays.asList(sampleVerify));

        List<VerifyImage> result = verifyImageService.getAllVerifyImages();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("approved", result.get(0).getResult());
    }

    @Test
    @DisplayName("getVerifyImageById - Lay chi tiet phe duyet theo ID thanh cong")
    void getVerifyImageById_Found() {
        when(verifyImageRepo.findById(1L)).thenReturn(Optional.of(sampleVerify));

        Optional<VerifyImage> result = verifyImageService.getVerifyImageById(1L);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getImageId());
    }

    @Test
    @DisplayName("Loi nghiep vu: Khong duoc phep luu duyet anh voi ket qua khong thuoc danh sach approved/rejected")
    void saveVerifyImage_InvalidResult_ThrowsException() {
        sampleVerify.setResult("unknown_status");
        assertThrows(IllegalArgumentException.class, () -> 
            verifyImageService.saveVerifyImage(sampleVerify),
            "Giá trị result sai chuẩn phải ném ra IllegalArgumentException"
        );
    }
    
    private static class VerifyResult {}
}
