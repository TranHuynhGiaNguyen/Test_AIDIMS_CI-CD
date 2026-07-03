package com.aidims.aidimsbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // Seed standard roles into the 'roles' table if they do not exist
        seedRole(1, "admin", "Quản trị viên hệ thống");
        seedRole(2, "doctor", "Bác sĩ");
        seedRole(3, "receptionist", "Nhân viên tiếp nhận");
        seedRole(4, "technician", "Kỹ thuật viên hình ảnh");

        // Seed imaging types if they do not exist
        seedImagingType(1, "X-quang thường", "XR", "Chụp X-quang thông thường", "{\"kVp\": 80, \"mAs\": 10, \"filter\": \"Al\"}");
        seedImagingType(2, "CT Scanner", "CT", "Chụp cắt lớp vi tính", "{\"slice_thickness\": 1.25, \"pitch\": 1.0, \"kVp\": 120}");
        seedImagingType(3, "MRI", "MRI", "Cộng hưởng từ", "{\"field_strength\": \"1.5T\", \"sequence\": \"T1\", \"slice_thickness\": 5}");
        seedImagingType(4, "Siêu âm", "US", "Siêu âm", "{\"frequency\": \"3.5MHz\", \"gain\": 50, \"depth\": 15}");
        seedImagingType(5, "Mammography", "MG", "Chụp tuyến vú", "{\"kVp\": 28, \"mAs\": 63, \"compression\": \"medium\"}");
        seedImagingType(6, "Fluoroscopy", "FL", "Chụp thấu quang", "{\"kVp\": 100, \"mAs\": 5, \"frame_rate\": 15}");
        seedImagingType(7, "PET-CT", "PET", "Chụp PET-CT", "{\"tracer\": \"F18-FDG\", \"uptake_time\": 60, \"slice_thickness\": 2}");
        seedImagingType(8, "SPECT", "SP", "Chụp SPECT", "{\"isotope\": \"Tc99m\", \"matrix\": \"128x128\", \"zoom\": 1.0}");
    }

    private void seedRole(int id, String name, String description) {
        try {
            // Check if role with this ID exists
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE role_id = ?", Integer.class, id);
            if (count == null || count == 0) {
                // Also check if a role with the same name exists to avoid unique constraint violations
                Integer nameCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM roles WHERE role_name = ?", Integer.class, name);
                if (nameCount == null || nameCount == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO roles (role_id, role_name, role_description) VALUES (?, ?, ?)",
                            id, name, description);
                }
            }
        } catch (Exception e) {
            System.err.println("Error seeding role " + name + ": " + e.getMessage());
        }
    }

    private void seedImagingType(int id, String name, String code, String description, String defaultSettings) {
        try {
            // Check if imaging type with this ID exists
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM imaging_types WHERE type_id = ?", Integer.class, id);
            if (count == null || count == 0) {
                // Also check if type name or code exists to avoid unique constraint violations
                Integer nameCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM imaging_types WHERE type_name = ? OR type_code = ?", 
                        Integer.class, name, code);
                if (nameCount == null || nameCount == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO imaging_types (type_id, type_name, type_code, description, default_settings) VALUES (?, ?, ?, ?, ?)",
                            id, name, code, description, defaultSettings);
                }
            }
        } catch (Exception e) {
            System.err.println("Error seeding imaging type " + name + ": " + e.getMessage());
        }
    }
}
