package com.aidims.aidimsbackend.service;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.aidims.aidimsbackend.dto.ImageAnalysisRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatService {
    public String testGeminiDirectly(String message) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() ||
                geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new RuntimeException("Gemini API key chưa được cấu hình");
        }

        try {
            Map<String, Object> requestBody = createGeminiRequestBody("Test: " + message);

            String response = webClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);

            if (jsonResponse.has("candidates") &&
                    jsonResponse.get("candidates").size() > 0) {

                JsonNode candidate = jsonResponse.get("candidates").get(0);
                if (candidate.has("content") &&
                        candidate.get("content").has("parts") &&
                        candidate.get("content").get("parts").size() > 0) {

                    return candidate.get("content").get("parts").get(0).get("text").asText();
                }
            }

            throw new RuntimeException("Invalid response format: " + response);

        } catch (Exception e) {
            throw new RuntimeException("Gemini API test failed: " + e.getMessage());
        }
    }

    public String testOpenAIDirectly(String message) {
        // Implement tương tự cho OpenAI nếu cần
        return "OpenAI test placeholder";
    }
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent}")
    private String geminiApiUrl;

    @Value("${openai.api.key:}")
    private String openaiApiKey;
    @Value("${gemini.vision.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent}")
    private String geminiVisionApiUrl;
    private WebClient webClient;
    private final ObjectMapper objectMapper;
    private Map<String, String> symptomDiagnosis;
    private Map<String, String> dicomFindings;

    public ChatService() {
        this.objectMapper = new ObjectMapper();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        this.webClient = WebClient.builder().build();
        this.symptomDiagnosis = initializeSymptomDiagnosis();
        this.dicomFindings = initializeDicomFindings();
    }

    public String getChatResponse(String message) {
        return getChatResponse(message, "default");
    }

    public String getChatResponse(String message, String sessionId) {
        // Ưu tiên sử dụng Gemini API
        if (geminiApiKey != null && !geminiApiKey.trim().isEmpty() &&
                !geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            try {
                return getGeminiResponse(message);
            } catch (Exception e) {
                System.err.println("Gemini API failed: " + e.getMessage());
                // Fallback to OpenAI if Gemini fails
                return tryOpenAIOrFallback(message);
            }
        }

        // Fallback to OpenAI hoặc local logic
        return tryOpenAIOrFallback(message);
    }

    private String getGeminiResponse(String message) {
        try {
            // Tạo system prompt cho y tế
            String medicalPrompt = createMedicalPrompt(message);

            // Tạo request body theo format Gemini API
            Map<String, Object> requestBody = createGeminiRequestBody(medicalPrompt);

            // Gọi Gemini API
            String response = webClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);

            if (jsonResponse.has("candidates") &&
                    jsonResponse.get("candidates").size() > 0) {

                JsonNode candidate = jsonResponse.get("candidates").get(0);
                if (candidate.has("content") &&
                        candidate.get("content").has("parts") &&
                        candidate.get("content").get("parts").size() > 0) {

                    String aiResponse = candidate.get("content").get("parts").get(0).get("text").asText();
                    return formatMedicalResponse(aiResponse);
                }
            }

            throw new RuntimeException("Invalid response format from Gemini");

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Gemini API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Gemini API Error: " + e.getMessage());
        }
    }

    private Map<String, Object> createGeminiRequestBody(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();

        // Tạo parts
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        // Tạo content
        Map<String, Object> content = new HashMap<>();
        content.put("parts", Arrays.asList(part));

        // Tạo contents array
        requestBody.put("contents", Arrays.asList(content));

        // Cấu hình generation
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 1024);

        requestBody.put("generationConfig", generationConfig);

        // Safety settings để tránh bị block
        List<Map<String, Object>> safetySettings = new ArrayList<>();
        String[] categories = {
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
        };

        for (String category : categories) {
            Map<String, Object> safety = new HashMap<>();
            safety.put("category", category);
            safety.put("threshold", "BLOCK_ONLY_HIGH");
            safetySettings.add(safety);
        }

        requestBody.put("safetySettings", safetySettings);

        return requestBody;
    }

    private String createMedicalPrompt(String userMessage) {
        return "Bạn là AI chuyên khoa y tế của bệnh viện AIDIMS, hỗ trợ bác sĩ phân tích triệu chứng.\n\n" +
                "NHIỆM VỤ:\n" +
                "- Phân tích triệu chứng bệnh nhân\n" +
                "- Đưa ra chẩn đoán phân biệt (top 3-5)\n" +
                "- Đề xuất xét nghiệm cần thiết\n" +
                "- Đánh giá mức độ ưu tiên\n" +
                "- Giải thích DICOM findings nếu có\n\n" +

                "ĐỊNH DẠNG TRẢ LỜI:\n" +
                "**PHÂN TÍCH TRIỆU CHỨNG**\n" +
                "- Mô tả ngắn gọn\n\n" +

                "**CHẨN ĐOÁN PHÂN BIỆT:**\n" +
                "1. [Chẩn đoán chính] - [xác suất %]\n" +
                "2. [Chẩn đoán 2] - [xác suất %]\n" +
                "3. [Chẩn đoán 3] - [xác suất %]\n\n" +

                "**XÉT NGHIỆM ĐỀ XUẤT:**\n" +
                "- Cận lâm sàng: [...]\n" +
                "- Hình ảnh: [...]\n" +
                "- Khác: [...]\n\n" +

                "**MỨC ĐỘ ƯU TIÊN:**\n" +
                "Khẩn cấp /  Theo dõi /  Thấp\n\n" +

                "**ĐIỀU TRỊ BAN ĐẦU:**\n" +
                "- Triệu chứng: [...]\n" +
                "- Theo dõi: [...]\n\n" +

                "LƯU Ý:\n" +
                "- Trả lời bằng tiếng Việt\n" +
                "- Ngắn gọn, dễ hiểu\n" +
                "- Tối đa 300 từ\n" +
                "TRIỆU CHỨNG/CÂU HỎI CỦA BỆNH NHÂN:\n" + userMessage;
    }

    private String formatMedicalResponse(String response) {
        // Thêm thông tin bổ sung cho response
        StringBuilder formatted = new StringBuilder();
        formatted.append(response);

        // Thêm disclaimer
        formatted.append("\n\n---\n");
        formatted.append(" **LƯU Ý:** Đây chỉ là tư vấn hỗ trợ. ");
        formatted.append("Quyết định cuối cùng thuộc về bác sĩ điều trị.\n");
        formatted.append(" **Khẩn cấp:** 0777815075");

        return formatted.toString();
    }

    private String tryOpenAIOrFallback(String message) {
        // Thử OpenAI nếu có key
        if (openaiApiKey != null && !openaiApiKey.trim().isEmpty() &&
                !openaiApiKey.equals("your-openai-api-key-here")) {
            try {
                return getOpenAIResponse(message);
            } catch (Exception e) {
                System.err.println("OpenAI API failed: " + e.getMessage());
            }
        }

        // Fallback về logic cứng
        return getSymptomAnalysis(message);
    }

    private String getOpenAIResponse(String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", createMedicalPrompt(message));

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", message);

            messages.add(systemMessage);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.3);

            String response = webClient.post()
                    .uri("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            String aiResponse = jsonResponse.get("choices").get(0).get("message").get("content").asText();
            return formatMedicalResponse(aiResponse);

        } catch (Exception e) {
            throw new RuntimeException("OpenAI Error: " + e.getMessage());
        }
    }

    // Giữ nguyên các method cũ cho fallback
    private Map<String, String> initializeSymptomDiagnosis() {
        Map<String, String> symptoms = new HashMap<>();

        // Tim mạch
        symptoms.put("đau ngực", " **ĐÁNH GIÁ:** ACS, PE, Pneumothorax\n **XÉT NGHIỆM:** ECG, Troponin, D-dimer\n **CẦN KHÁM NGAY**");
        symptoms.put("khó thở", " **ĐÁNH GIÁ:** Suy tim, PE, Pneumonia\n **XÉT NGHIỆM:** BNP, CXR, CTPA\n **ƯU TIÊN CAO**");
        symptoms.put("hồi hộp", " **ĐÁNH GIÁ:** Arrhythmia, Anxiety, Hyperthyroid\n **XÉT NGHIỆM:** ECG, TSH, Echo\n **THEO DÕI**");
        symptoms.put("phù chân", " **ĐÁNH GIÁ:** Suy tim, Suy thận, DVT\n **XÉT NGHIỆM:** BNP, Creatinine, Doppler\n **THEO DÕI**");

        // Hô hấp
        symptoms.put("ho khan", " **ĐÁNH GIÁ:** GERD, ACE-I, Asthma\n **XÉT NGHIỆM:** CXR, PPI trial\n **THẤP**");
        symptoms.put("ho có đờm", " **ĐÁNH GIÁ:** Pneumonia, Bronchitis, COPD\n **XÉT NGHIỆM:** CXR, Sputum culture, CRP\n **THEO DÕI**");
        symptoms.put("thở khò khè", " **ĐÁNH GIÁ:** Asthma, COPD, CHF\n **XÉT NGHIỆM:** PFT, CXR, BNP\n **THEO DÕI**");
        symptoms.put("thở gấp", " **ĐÁNH GIÁ:** PE, Pneumothorax, MI\n **XÉT NGHIỆM:** ABG, CTPA, ECG\n **KHẨN CẤP**");
        symptoms.put("ho ra máu", " **ĐÁNH GIÁ:** TB, Cancer, PE, Bronchiectasis\n **XÉT NGHIỆM:** CT chest, AFB, Bronchoscopy\n **KHẨN CẤP**");

        // Tiêu hóa
        symptoms.put("đau bụng", " **ĐÁNH GIÁ:** Appendicitis, Pancreatitis, Gallstones\n **XÉT NGHIỆM:** CT abdomen, Lipase, WBC\n **THEO DÕI**");
        symptoms.put("buồn nôn", " **ĐÁNH GIÁ:** Gastritis, Pancreatitis, Pregnancy\n **XÉT NGHIỆM:** βhCG, Lipase, H.pylori\n **THẤP**");
        symptoms.put("tiêu chảy", " **ĐÁNH GIÁ:** Gastroenteritis, IBD, C.diff\n **XÉT NGHIỆM:** Stool culture, Calprotectin\n **THEO DÕI**");
        symptoms.put("táo bón", " **ĐÁNH GIÁ:** IBS, Medication, Diet\n **XÉT NGHIỆM:** Colonoscopy nếu >50 tuổi\n **THẤP**");
        symptoms.put("vàng da", " **ĐÁNH GIÁ:** Hepatitis, Gallstones, Hemolysis\n **XÉT NGHIỆM:** LFT, Bilirubin, MRCP\n **THEO DÕI**");

        // Thần kinh
        symptoms.put("đau đầu", " **ĐÁNH GIÁ:** Migraine, Tension, SAH\n **XÉT NGHIỆM:** CT head nếu red flags\n **THEO DÕI**");
        symptoms.put("chóng mặt", " **ĐÁNH GIÁ:** BPPV, Orthostatic, Stroke\n **XÉT NGHIỆM:** MRI nếu focal signs\n **THEO DÕI**");
        symptoms.put("co giật", " **ĐÁNH GIÁ:** Epilepsy, Metabolic, Infection\n **XÉT NGHIỆM:** EEG, Glucose, CT head\n **KHẨN CẤP**");
        symptoms.put("tê liệt", " **ĐÁNH GIÁ:** Stroke, Spinal compression\n **XÉT NGHIỆM:** CT/MRI brain/spine\n **KHẨN CẤP**");
        symptoms.put("yếu cơ", " **ĐÁNH GIÁ:** Myopathy, Neuropathy, MG\n **XÉT NGHIỆM:** CK, EMG, AChR Ab\n **THEO DÕI**");

        // Khác
        symptoms.put("sốt", " **ĐÁNH GIÁ:** Infection, Autoimmune, Malignancy\n **XÉT NGHIỆM:** CBC, Blood culture, CRP\n **THEO DÕI**");
        symptoms.put("sút cân", " **ĐÁNH GIÁ:** Cancer, Hyperthyroid, Depression\n **XÉT NGHIỆM:** CT chest/abdomen, TSH\n **THEO DÕI**");
        symptoms.put("mệt mỏi", " **ĐÁNH GIÁ:** Anemia, Thyroid, Depression\n **XÉT NGHIỆM:** CBC, TSH, Vitamin B12\n **THẤP**");

        return symptoms;
    }

    private Map<String, String> initializeDicomFindings() {
        Map<String, String> findings = new HashMap<>();

        // CT Chest patterns
        findings.put("ground glass", " **Ground Glass Opacity:**\nCOVID-19, PCP, Drug toxicity\n**Phân bố:** Peripheral (COVID), Central (PCP)");
        findings.put("consolidation", " **Consolidation:**\nBacterial pneumonia, Aspiration\n**Đặc điểm:** Air bronchograms, lobar");
        findings.put("cavitary", " **Cavitary lesion:**\nTB (upper lobe), Cancer (thick wall), Abscess\n**Wall >4mm:** Nghi ngờ malignancy");
        findings.put("honeycombing", " **Honeycombing:**\nIPF, End-stage fibrosis\n**Vị trí:** Subpleural, bilateral lower");
        findings.put("tree in bud", " **Tree-in-bud:**\nInfection, Aspiration\n**Phân bố:** Centrilobular nodules");
        findings.put("pneumothorax", " **Pneumothorax:**\n>20% cần chest tube\n**Tension:** Emergency decompression");
        findings.put("pleural effusion", " **Pleural effusion:**\nHeart failure, Infection, Malignancy\n**Bilateral:** CHF. **Unilateral:** Infection/Cancer");
        findings.put("pulmonary embolism", " **Pulmonary embolism:**\nFilling defect, RV strain\n**Massive:** Shock, thrombolysis");

        // CT Abdomen patterns
        findings.put("appendicitis", " **Appendicitis:**\nWall thickening >6mm, fat stranding\n**Perforated:** Free fluid, abscess");
        findings.put("pancreatitis", " **Pancreatitis:**\nPancreatic enlargement, fat stranding\n**Severe:** Necrosis, fluid collections");
        findings.put("gallstones", " **Gallstones:**\nHyperdense stones, wall thickening\n**Complications:** Cholangitis, pancreatitis");
        findings.put("bowel obstruction", " **Bowel obstruction:**\nDilated loops, air-fluid levels\n**Strangulation:** Ischemia, surgery");
        findings.put("free air", " **Pneumoperitoneum:**\nPerforated viscus\n**Emergency surgery** indicated");
        findings.put("liver lesion", " **Liver lesion:**\nHCC, Metastases, Hemangioma\n**Enhancement pattern** key");

        // CT Brain patterns
        findings.put("hypodense", " **Hypodense lesion:**\nIschemic stroke, Edema, Tumor\n**Acute:** <6h invisible on CT");
        findings.put("hyperdense", " **Hyperdense lesion:**\nAcute hemorrhage, Contrast\n**Subarachnoid:** Aneurysm rupture");
        findings.put("midline shift", " **Midline shift:**\n>5mm significant mass effect\n**Neurosurgery consult** needed");
        findings.put("hydrocephalus", " **Hydrocephalus:**\nVentricular enlargement\n**Acute:** Emergent shunt");

        return findings;
    }

    private String getSymptomAnalysis(String message) {
        String lowerMessage = message.toLowerCase().trim();
        List<String> foundSymptoms = new ArrayList<>();
        List<String> foundDicom = new ArrayList<>();

        // Tìm triệu chứng
        for (String symptom : symptomDiagnosis.keySet()) {
            if (lowerMessage.contains(symptom)) {
                foundSymptoms.add(symptom);
            }
        }

        // Tìm DICOM findings
        for (String finding : dicomFindings.keySet()) {
            if (lowerMessage.contains(finding.replace(" ", "")) ||
                    lowerMessage.contains(finding)) {
                foundDicom.add(finding);
            }
        }

        StringBuilder response = new StringBuilder();

        if (!foundSymptoms.isEmpty()) {
            response.append(" **PHÂN TÍCH TRIỆU CHỨNG**\n\n");

            for (String symptom : foundSymptoms) {
                response.append("**").append(symptom.toUpperCase()).append(":**\n");
                response.append(symptomDiagnosis.get(symptom)).append("\n\n");
            }

            // Combination analysis
            if (foundSymptoms.size() > 1) {
                response.append(" **KẾT HỢP TRIỆU CHỨNG:**\n");
                response.append(analyzeCombination(foundSymptoms)).append("\n\n");
            }
        }

        if (!foundDicom.isEmpty()) {
            response.append(" **PHÂN TÍCH DICOM**\n\n");

            for (String finding : foundDicom) {
                response.append("**").append(finding.toUpperCase()).append(":**\n");
                response.append(dicomFindings.get(finding)).append("\n\n");
            }
        }

        if (foundSymptoms.isEmpty() && foundDicom.isEmpty()) {
            response.append(" **TƯ VẤN Y TẾ AIDIMS**\n\n");
            response.append("Xin chào! Tôi là AI hỗ trợ y tế. Vui lòng mô tả triệu chứng cụ thể.\n\n");
            response.append("**Ví dụ:**\n");
            response.append(" **Khẩn cấp:** 0777815075");
        }

        return response.toString();
    }

    private String analyzeCombination(List<String> symptoms) {
        // Common combinations
        if (symptoms.contains("đau ngực") && symptoms.contains("khó thở")) {
            return " **ACS vs PE vs Pneumothorax**\nECG + Troponin + D-dimer + CTPA\n **KHẨN CẤP**";
        }

        if (symptoms.contains("sốt") && symptoms.contains("ho có đờm")) {
            return " **Pneumonia**\nCXR + Blood culture + CRP + PCT\n **KHÁNG SINH**";
        }

        if (symptoms.contains("đau bụng") && symptoms.contains("sốt")) {
            return " **Appendicitis vs Cholecystitis**\nCT abdomen + WBC + Lipase\n **PHẪU THUẬT?**";
        }

        if (symptoms.contains("đau đầu") && symptoms.contains("sốt")) {
            return " **Meningitis vs SAH**\nCT head + LP + Blood culture\n **KHẨN CẤP**";
        }

        if (symptoms.contains("khó thở") && symptoms.contains("phù chân")) {
            return " **Heart Failure**\nBNP + Echo + CXR\n **NHẬP VIỆN**";
        }

        String combo = String.join(" + ", symptoms);
        return " **Đa triệu chứng:** " + combo + "\n **Cần đánh giá toàn diện**";
    }
    public String analyzeImages(ImageAnalysisRequest request) {
        if (request == null || request.getImages() == null || request.getImages().isEmpty()) {
            throw new IllegalArgumentException("Danh sách hình ảnh không được trống!");
        }

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() ||
                geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            throw new RuntimeException("Gemini API key chưa được cấu hình");
        }

        try {
            // Create medical image analysis prompt
            String medicalPrompt = createMedicalImagePrompt(request.getMessage());

            // Create multimodal request body for Gemini Vision
            Map<String, Object> requestBody = createGeminiVisionRequestBody(medicalPrompt, request.getImages());

            // Call Gemini Vision API
            String response = webClient.post()
                    .uri(geminiVisionApiUrl + "?key=" + geminiApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(45)) // Longer timeout for image analysis
                    .block();

            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response);

            if (jsonResponse.has("candidates") &&
                    jsonResponse.get("candidates").size() > 0) {

                JsonNode candidate = jsonResponse.get("candidates").get(0);
                if (candidate.has("content") &&
                        candidate.get("content").has("parts") &&
                        candidate.get("content").get("parts").size() > 0) {

                    String aiResponse = candidate.get("content").get("parts").get(0).get("text").asText();
                    return formatMedicalImageResponse(aiResponse, request.getImages().size());
                }
            }

            throw new RuntimeException("Invalid response format from Gemini Vision");

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Gemini Vision API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Gemini Vision API Error: " + e.getMessage());
        }
    }

    /**
     * Tạo request body cho Gemini Vision API với text và images
     */
    private Map<String, Object> createGeminiVisionRequestBody(String prompt, List<ImageAnalysisRequest.ImageData> images) {
        Map<String, Object> requestBody = new HashMap<>();

        // Create parts array with text and images
        List<Map<String, Object>> parts = new ArrayList<>();

        // Add text part
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);

        // Add image parts
        for (ImageAnalysisRequest.ImageData image : images) {
            Map<String, Object> imagePart = new HashMap<>();
            Map<String, Object> inlineData = new HashMap<>();

            // Remove data URL prefix if present (data:image/jpeg;base64,)
            String base64Data = image.getData();
            if (base64Data.contains(",")) {
                base64Data = base64Data.split(",")[1];
            }

            inlineData.put("mime_type", image.getType());
            inlineData.put("data", base64Data);
            imagePart.put("inline_data", inlineData);
            parts.add(imagePart);
        }

        // Create content
        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        // Create contents array
        requestBody.put("contents", Arrays.asList(content));

        // Generation config for image analysis
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2); // Lower temperature for medical analysis
        generationConfig.put("topK", 32);
        generationConfig.put("topP", 0.9);
        generationConfig.put("maxOutputTokens", 2048); // More tokens for detailed analysis

        requestBody.put("generationConfig", generationConfig);

        // Safety settings
        List<Map<String, Object>> safetySettings = new ArrayList<>();
        String[] categories = {
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
        };

        for (String category : categories) {
            Map<String, Object> safety = new HashMap<>();
            safety.put("category", category);
            safety.put("threshold", "BLOCK_ONLY_HIGH");
            safetySettings.add(safety);
        }

        requestBody.put("safetySettings", safetySettings);

        return requestBody;
    }

    /**
     * Tạo prompt chuyên dụng cho phân tích hình ảnh y tế
     */
    private String createMedicalImagePrompt(String userMessage) {
        return "Bạn là AI chuyên gia phân tích hình ảnh y tế của bệnh viện AIDIMS.\n\n" +
                "NHIỆM VỤ PHÂN TÍCH HÌNH ẢNH Y TẾ:\n" +
                "- Mô tả chi tiết những gì nhìn thấy trong hình ảnh\n" +
                "- Xác định loại hình ảnh y tế (X-quang, CT, MRI, siêu âm, etc.)\n" +
                "- Phân tích các cấu trúc giải phẫu bình thường\n" +
                "- Chỉ ra các bất thường nếu có\n" +
                "- Đưa ra chẩn đoán phân biệt dựa trên hình ảnh\n" +
                "- Đề xuất thêm xét nghiệm nếu cần\n\n" +

                "ĐỊNH DẠNG TRẢ LỜI:\n" +
                " **LOẠI HÌNH ẢNH:** [X-quang/CT/MRI/etc.]\n\n" +

                " **MÔ TẢ HÌNH ẢNH:**\n" +
                "- Vùng chụp: [...]\n" +
                "- Tư thế: [...]\n" +
                "- Chất lượng ảnh: [...]\n\n" +

                " **CẤU TRÚC BÌNH THƯỜNG:**\n" +
                "- [...]\n" +
                "- [...]\n\n" +

                " **PHÁT HIỆN BẤT THƯỜNG:**\n" +
                "- [...]\n" +
                "- [...]\n\n" +

                " **CHẨN ĐOÁN PHÂN BIỆT:**\n" +
                "1. [Chẩn đoán chính] - [độ tin cậy]\n" +
                "2. [Chẩn đoán 2] - [độ tin cậy]\n" +
                "3. [Chẩn đoán 3] - [độ tin cậy]\n\n" +

                " **ĐỀ XUẤT THÊM:**\n" +
                "- Xét nghiệm: [...]\n" +
                "- Hình ảnh bổ sung: [...]\n" +
                "- Tư vấn chuyên khoa: [...]\n\n" +

                " **MỨC ĐỘ ƯU TIÊN:**\n" +
                "Khẩn cấp /  Theo dõi /  Bình thường\n\n" +

                "LƯU Ý QUAN TRỌNG:\n" +
                "- Phân tích khách quan dựa trên hình ảnh\n" +
                "- Không đưa ra chẩn đoán chắc chắn\n" +
                "- Luôn khuyến cáo tham khảo bác sĩ chuyên khoa\n" +
                "- Trả lời bằng tiếng Việt, rõ ràng, dễ hiểu\n\n" +

                "CÂU HỎI CỦA NGƯỜI DÙNG:\n" + (userMessage != null ? userMessage : "Phân tích hình ảnh y tế này");
    }

    /**
     * Format response cho phân tích hình ảnh y tế
     */
    private String formatMedicalImageResponse(String response, int imageCount) {
        StringBuilder formatted = new StringBuilder();

        // Add header
        formatted.append(" **KẾT QUẢ PHÂN TÍCH HÌNH ẢNH Y TẾ**\n");
        formatted.append(" *Đã phân tích ").append(imageCount).append(" hình ảnh*\n\n");

        formatted.append(response);

        // Add important disclaimers
        formatted.append("\n\n---\n");
        formatted.append(" **QUAN TRỌNG:**\n");
        formatted.append("• Đây chỉ là hỗ trợ phân tích sơ bộ\n");
        formatted.append("• Quyết định chẩn đoán cuối cùng thuộc về bác sĩ chuyên khoa\n");
        formatted.append("• Cần kết hợp với triệu chứng lâm sàng và tiền sử bệnh\n");
        formatted.append("• Nếu có triệu chứng nghiêm trọng, hãy đến bệnh viện ngay\n\n");
        formatted.append(" **Khẩn cấp:** 0777815075\n");
        formatted.append(" **Bệnh viện AIDIMS** - Chuyên khoa Chẩn đoán Hình ảnh");

        return formatted.toString();
    }
}