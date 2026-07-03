package com.aidims.aidimsbackend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aidims.aidimsbackend.service.CompareImagesService;

@RestController
@RequestMapping("/api/compare-images")
@CrossOrigin(origins = "*")
public class CompareImagesController {

    @Autowired
    private CompareImagesService compareImagesService;

    /**
     * So sánh tất cả ảnh của bệnh nhân theo keyword (mã hoặc tên)
     */
   @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchPatientImages(@RequestParam String keyword) {
        try {
            List<Map<String, Object>> images = compareImagesService.searchByPatientCode(keyword); // 👈 sửa tại đây
            List<Map<String, Object>> normalizedImages = normalizeImageUrls(images);
            System.out.println("✅ Found " + normalizedImages.size() + " images for keyword: " + keyword);
            return ResponseEntity.ok(normalizedImages);
        } catch (Exception e) {
            System.err.println("❌ Error in compare-images/search: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<Map<String, Object>> normalizeImageUrls(List<Map<String, Object>> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .map(this::normalizeImageUrl)
                .toList();
    }

    private Map<String, Object> normalizeImageUrl(Map<String, Object> image) {
        if (image == null) {
            return null;
        }
        Map<String, Object> normalized = new HashMap<>(image);
        Object url = normalized.get("imageUrl");
        if (url instanceof String imageUrl && imageUrl.contains(" ")) {
            normalized.put("imageUrl", imageUrl.replace(" ", "%20"));
        }
        return normalized;
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("CompareImages API is active.");
    }
}
