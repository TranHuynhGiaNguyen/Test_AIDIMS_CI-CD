package com.aidims.aidimsbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidims.aidimsbackend.dto.RequestPhotoDTO;
import com.aidims.aidimsbackend.entity.RequestPhoto;
import com.aidims.aidimsbackend.repository.RequestPhotoRepository;

@Service
public class RequestPhotoService {

    @Autowired
    private RequestPhotoRepository requestPhotoRepository;

    @Transactional
    public RequestPhotoDTO createRequest(RequestPhotoDTO dto) {
        if (dto.getRequestDate() != null && dto.getRequestDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày chỉ định không được trong quá khứ!");
        }

        try {
            System.out.println("=== SERVICE CREATE REQUEST ===");
            System.out.println("Input DTO: " + dto.getPatientId() + ", " + dto.getImagingType());

            // Generate unique request code
            if (dto.getRequestCode() == null || dto.getRequestCode().isEmpty()) {
                dto.setRequestCode(generateRequestCode());
            }

            // Validate request code uniqueness
            if (requestPhotoRepository.existsByRequestCode(dto.getRequestCode())) {
                throw new RuntimeException("Mã yêu cầu đã tồn tại: " + dto.getRequestCode());
            }

            RequestPhoto entity = convertToEntity(dto);
            System.out.println("Entity before save: " + entity.getRequestCode() + ", " + entity.getPatientId());

            RequestPhoto savedEntity = requestPhotoRepository.save(entity);
            System.out.println("Entity after save: " + savedEntity.getRequestId() + ", " + savedEntity.getRequestCode());

            RequestPhotoDTO result = convertToDTO(savedEntity);
            System.out.println("Result DTO: " + result.getRequestId() + ", " + result.getRequestCode());

            return result;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Service error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo yêu cầu chụp: " + e.getMessage());
        }
    }

    public List<RequestPhotoDTO> getAllRequests() {
        try {
            List<RequestPhoto> entities = requestPhotoRepository.findAll();
            System.out.println("Found " + entities.size() + " requests in database");

            return entities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách yêu cầu: " + e.getMessage());
        }
    }

    public List<RequestPhotoDTO> getRequestsByPatientId(Long patientId) {
        try {
            return requestPhotoRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy yêu cầu của bệnh nhân: " + e.getMessage());
        }
    }

    private String generateRequestCode() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String code = "REQ" + timestamp;

        // Ensure uniqueness
        int counter = 1;
        String originalCode = code;
        while (requestPhotoRepository.existsByRequestCode(code)) {
            code = originalCode + String.format("%02d", counter++);
        }

        return code;
    }

    private RequestPhoto convertToEntity(RequestPhotoDTO dto) {
        RequestPhoto entity = new RequestPhoto();
        entity.setRequestCode(dto.getRequestCode());
        entity.setPatientId(dto.getPatientId());
        entity.setImagingType(dto.getImagingType());
        entity.setBodyPart(dto.getBodyPart());
        entity.setClinicalIndication(dto.getClinicalIndication());
        entity.setNotes(dto.getNotes());
        entity.setPriorityLevel(dto.getPriorityLevel() != null ? dto.getPriorityLevel() : "normal");
        entity.setRequestDate(dto.getRequestDate() != null ? dto.getRequestDate() : LocalDate.now());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "pending");
        return entity;
    }

    private RequestPhotoDTO convertToDTO(RequestPhoto entity) {
        RequestPhotoDTO dto = new RequestPhotoDTO();
        dto.setRequestId(entity.getRequestId());
        dto.setRequestCode(entity.getRequestCode());
        dto.setPatientId(entity.getPatientId());
        dto.setImagingType(entity.getImagingType());
        dto.setBodyPart(entity.getBodyPart());
        dto.setClinicalIndication(entity.getClinicalIndication());
        dto.setNotes(entity.getNotes());
        dto.setPriorityLevel(entity.getPriorityLevel());
        dto.setRequestDate(entity.getRequestDate());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}