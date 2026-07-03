package com.aidims.aidimsbackend.controller;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aidims.aidimsbackend.entity.DicomImport;
import com.aidims.aidimsbackend.service.DicomFileService;
import com.aidims.aidimsbackend.service.DicomImportService;

@RestController
@RequestMapping("/api/dicom-import")
@CrossOrigin(origins = "*")
public class DicomImportController {
    
    @Autowired
    private DicomImportService dicomImportService;

    @Autowired
    private DicomFileService dicomFileService;

    @PostMapping("/import")
    public ResponseEntity<?> importDicom(
            @RequestParam("file") MultipartFile file,
            @RequestParam("patient_code") String patientCode,
            @RequestParam("patient_name") String patientName,
            @RequestParam("study_type") String studyType,
            @RequestParam("body_part") String bodyPart,
            @RequestParam("technical_params") String technicalParams,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "performed_by", required = false) String performedBy
    ) {
        System.out.println("[DicomImport] Nhận file: " + (file != null ? file.getOriginalFilename() : "null") + ", size: " + (file != null ? file.getSize() : "null"));
        
        // ========== VALIDATION ==========
        
        // 1. Kiểm tra file null hoặc rỗng
        if (file == null || file.isEmpty()) {
            System.out.println("[DicomImport] File null hoặc empty!");
            return ResponseEntity.status(400).body("File rỗng!");
        }
        
        // 2. Kiểm tra kích thước file (tối đa 100MB)
        if (file.getSize() > 100 * 1024 * 1024) {
            System.out.println("[DicomImport] File quá lớn: " + file.getSize() + " bytes");
            return ResponseEntity.status(400).body("File quá lớn! Tối đa 100MB");
        }
        
        // 3. Kiểm tra định dạng file
        String originalFileName = file.getOriginalFilename();
        if (originalFileName != null && originalFileName.contains(".")) {
            String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
            Set<String> validExtensions = Set.of("dcm", "dicom", "dc3", "dic");
            if (!validExtensions.contains(extension)) {
                System.out.println("[DicomImport] Định dạng file không hợp lệ: " + extension);
                return ResponseEntity.status(400).body("Chỉ hỗ trợ file DICOM (.dcm, .dicom, .dc3, .dic)");
            }
        }
        // ✅ FIX BUG #1: Validate patient_code
        if (patientCode == null || patientCode.trim().isEmpty()) {
            return ResponseEntity.status(400)
                .body("Ma benh nhan khong duoc de trong!");
        }

        // ✅ FIX BUG #2: Validate study_type
        if (studyType == null || studyType.trim().isEmpty()) {
            return ResponseEntity.status(400)
                .body("Loai chup khong duoc de trong!");
        }
        // ====== PHẦN CODE CŨ ======
        
        // Lưu file vào thư mục dicom_uploads tuyệt đối
        String uploadDir = System.getProperty("user.dir") + "/aidims-backend/dicom_uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        
        // Tạo tên file mới (thêm timestamp và replace khoảng trắng)
        String newFileName = System.currentTimeMillis() + "" + originalFileName.replaceAll("\\s+", "");
        String filePath = uploadDir + newFileName;
        
        try {
            // 1. Lưu file vào backend
            file.transferTo(new File(filePath));

            // 2. Copy file vừa lưu sang frontend/public/dicom_uploads
            dicomFileService.copyFileToFrontend(new File(filePath), newFileName);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Lỗi khi lưu file: " + e.getMessage());
        }
        
        System.out.println("[DicomImport] File saved to: " + filePath);

        // Tạo entity và lưu vào DB
        DicomImport dicomImport = new DicomImport();
        dicomImport.setFileName(originalFileName);
        dicomImport.setFilePath(filePath);
        dicomImport.setFileSize(file.getSize());
        dicomImport.setPatientCode(patientCode);
        dicomImport.setPatientName(patientName);
        dicomImport.setStudyType(studyType);
        dicomImport.setBodyPart(bodyPart);
        dicomImport.setTechnicalParams(technicalParams);
        dicomImport.setNotes(notes);
        dicomImport.setStatus("imported");
        dicomImport.setPerformedBy(performedBy);
        dicomImportService.saveDicomImport(dicomImport);
        
        return ResponseEntity.ok("Import thành công");
    }
}