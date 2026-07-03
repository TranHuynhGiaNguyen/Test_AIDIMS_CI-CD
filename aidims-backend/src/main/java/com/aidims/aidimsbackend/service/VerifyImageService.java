package com.aidims.aidimsbackend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aidims.aidimsbackend.entity.VerifyImage;
import com.aidims.aidimsbackend.repository.VerifyImageRepository;

@Service
public class VerifyImageService {
    @Autowired
    private VerifyImageRepository verifyImageRepository;

    public VerifyImage saveVerifyImage(VerifyImage verifyImage) {
        String result = verifyImage.getResult();
        if (result == null || (!result.equalsIgnoreCase("approved") && !result.equalsIgnoreCase("rejected"))) {
            throw new IllegalArgumentException("Kết quả duyệt ảnh không hợp lệ!");
        }
        verifyImage.setCheckTime(LocalDateTime.now());
        return verifyImageRepository.save(verifyImage);
    }

    public List<VerifyImage> getAllVerifyImages() {
        return verifyImageRepository.findAll();
    }

    public Optional<VerifyImage> getVerifyImageById(Long id) {
        return verifyImageRepository.findById(id);
    }
}
