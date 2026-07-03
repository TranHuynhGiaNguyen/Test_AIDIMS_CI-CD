package com.aidims.aidimsbackend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompareImagesService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${server.port:8080}")
    private String serverPort;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public CompareImagesService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Lấy danh sách DICOM theo mã bệnh nhân (từ bảng dicom_imports) để so sánh
     */
    public List<Map<String, Object>> searchByPatientCode(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm không được để trống!");
        }

        String sql = """
            SELECT 
                di.id,
                di.body_part,
                di.file_name,
                di.file_path,
                di.file_size,
                di.import_date,
                di.notes,
                di.patient_code,
                di.performed_by,
                di.status,
                di.study_type,
                di.technical_params,
                p.full_name as patient_name,
                u.full_name as performed_by_name
            FROM dicom_imports di
            LEFT JOIN patient p ON di.patient_code = p.patient_code
            LEFT JOIN users u ON di.performed_by = u.user_id
            WHERE di.status = 'imported' 
              AND (di.patient_code LIKE ? OR p.full_name LIKE ?)
              AND di.file_path IS NOT NULL
            ORDER BY di.import_date DESC
            """;

        String pattern = "%" + keyword + "%";

        List<Map<String, Object>> raw = jdbcTemplate.queryForList(sql, pattern, pattern);

        return raw.stream().map(this::transformRow).toList();
    }

    /**
     * Helper: xử lý transform dữ liệu giống như viewer
     */
    private Map<String, Object> transformRow(Map<String, Object> row) {
        Map<String, Object> transformed = new HashMap<>();

        transformed.put("id", row.get("id"));
        transformed.put("fileName", row.get("file_name"));
        transformed.put("patientCode", row.get("patient_code"));
        transformed.put("modality", row.get("study_type"));
        transformed.put("bodyPart", row.get("body_part"));
        transformed.put("technicalParams", row.get("technical_params"));
        transformed.put("description", row.get("notes") != null ? row.get("notes") : "Không có mô tả");

        Object importDate = row.get("import_date");
        if (importDate instanceof LocalDateTime dt) {
            transformed.put("dateTaken", dt.format(dateFormatter));
        } else {
            transformed.put("dateTaken", importDate != null ? importDate.toString() : "N/A");
        }

        String fullName = (String) row.get("patient_name");
        transformed.put("fullName", fullName != null ? fullName : "Bệnh nhân " + row.get("patient_code"));
        transformed.put("performedBy", row.get("performed_by_name"));

        // File & image path
        String fileName = (String) row.get("file_name");
        String filePath = (String) row.get("file_path");
        transformed.put("dicomFilePath", filePath);
        transformed.put("imageUrl", (fileName != null) ? generateImageUrl(fileName) : null);

        return transformed;
    }

    private String generateImageUrl(String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        return "http://localhost:" + serverPort + "/api/dicom-viewer/image/" + encodedFileName;
    }
}
