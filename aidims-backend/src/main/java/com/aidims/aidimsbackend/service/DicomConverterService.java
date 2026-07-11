package com.aidims.aidimsbackend.service;

import com.aidims.aidimsbackend.dto.DicomAnalysisResponse.DicomMetadata;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

@Service
public class DicomConverterService {

    private static final Logger logger = LoggerFactory.getLogger(DicomConverterService.class);

    /**
     * Đọc DICOM bytes, trả về:
     *  base64 JPEG string (không prefix) + DicomMetadata object
     */
    public ConvertResult convert(byte[] dicomBytes) throws Exception {
        // ── 1. Đọc metadata + pixel data bằng dcm4che3 DicomInputStream ──
        DicomMetadata meta = new DicomMetadata();
        Attributes attrs;
        String transferSyntax = "unknown";

        try (DicomInputStream dis = new DicomInputStream(
                new ByteArrayInputStream(dicomBytes))) {
            // Đảm bảo đọc cả bulk data (pixel data)
            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.YES);
            transferSyntax = dis.getTransferSyntax();
            logger.info("DICOM Transfer Syntax: {}", transferSyntax);
            attrs = dis.readDataset();
        }

        meta.setPatientId(         safe(attrs, Tag.PatientID));
        meta.setPatientName(       safe(attrs, Tag.PatientName));
        meta.setPatientBirthDate(  safe(attrs, Tag.PatientBirthDate));
        meta.setPatientSex(        safe(attrs, Tag.PatientSex));
        meta.setModality(          safe(attrs, Tag.Modality));
        meta.setBodyPart(          safe(attrs, Tag.BodyPartExamined));
        meta.setStudyDescription(  safe(attrs, Tag.StudyDescription));
        meta.setSeriesDescription( safe(attrs, Tag.SeriesDescription));
        meta.setStudyDate(         safe(attrs, Tag.StudyDate));
        meta.setInstitutionName(   safe(attrs, Tag.InstitutionName));
        meta.setManufacturer(      safe(attrs, Tag.Manufacturer));
        meta.setBitsAllocated(     safe(attrs, Tag.BitsAllocated));
        meta.setKvp(               safe(attrs, Tag.KVP));
        meta.setExposureTime(      safe(attrs, Tag.ExposureTime));

        int rows = attrs.getInt(Tag.Rows, 0);
        int cols = attrs.getInt(Tag.Columns, 0);
        meta.setImageSize(rows + " x " + cols);

        // ── 2. Trích xuất pixel data ────────────────────────────────────
        BufferedImage image = null;

        // Kiểm tra loại pixel data (native vs encapsulated)
        Object pixelDataValue = attrs.getValue(Tag.PixelData);
        logger.info("PixelData value type: {}", pixelDataValue != null ? pixelDataValue.getClass().getSimpleName() : "null");

        // Phương pháp 1: Encapsulated pixel data (JPEG/JPEG2000/RLE compressed)
        if (pixelDataValue instanceof Fragments) {
            logger.info("DICOM uses encapsulated (compressed) pixel data");
            try {
                image = decodeEncapsulatedPixelData((Fragments) pixelDataValue, transferSyntax);
                if (image != null) {
                    logger.info("Successfully decoded encapsulated pixel data");
                }
            } catch (Exception e) {
                logger.warn("Encapsulated pixel data decoding failed: {}", e.getMessage());
            }
        }

        // Phương pháp 2: Native pixel data (uncompressed)
        if (image == null && pixelDataValue instanceof byte[]) {
            logger.info("DICOM uses native (uncompressed) pixel data");
            try {
                image = buildImageFromNativePixels((byte[]) pixelDataValue, attrs, rows, cols);
                if (image != null) {
                    logger.info("Successfully built image from native pixel data");
                }
            } catch (Exception e) {
                logger.warn("Native pixel extraction failed: {}", e.getMessage());
            }
        }

        // Phương pháp 3: Thử getBytes() (một số DICOM lưu dưới dạng OW/OB)
        if (image == null) {
            try {
                byte[] pixelBytes = attrs.getBytes(Tag.PixelData);
                if (pixelBytes == null) {
                    pixelBytes = attrs.getSafeBytes(Tag.PixelData);
                }
                if (pixelBytes != null && pixelBytes.length > 0) {
                    logger.info("Got pixel data via getBytes: {} bytes", pixelBytes.length);
                    image = buildImageFromNativePixels(pixelBytes, attrs, rows, cols);
                    if (image != null) {
                        logger.info("Successfully built image via getBytes fallback");
                    }
                }
            } catch (Exception e) {
                logger.warn("getBytes pixel extraction failed: {}", e.getMessage());
            }
        }

        // Phương pháp 4: Fallback — dùng ImageIO SPI
        if (image == null) {
            try {
                image = readViaImageIO(dicomBytes);
                if (image != null) {
                    logger.info("Successfully read DICOM via ImageIO fallback");
                }
            } catch (Exception e) {
                logger.warn("ImageIO fallback failed: {}", e.getMessage());
            }
        }

        if (image == null) {
            throw new RuntimeException(
                "Không thể giải mã hình ảnh từ tệp DICOM. " +
                "Transfer Syntax: " + transferSyntax + ". " +
                "PixelData type: " + (pixelDataValue != null ? pixelDataValue.getClass().getSimpleName() : "null") + ". " +
                "Vui lòng thử file DICOM khác hoặc file không nén.");
        }

        // ── 3. Normalize + convert sang RGB ─────────────────────────────
        image = normalizeToRgb(image);

        // ── 4. Encode JPEG quality 92% ───────────────────────────────────
        String base64 = encodeJpeg(image, 0.92f);

        return new ConvertResult(base64, meta);
    }

    // ── Decode encapsulated (compressed) pixel data ─────────────────────

    private BufferedImage decodeEncapsulatedPixelData(Fragments fragments, String tsuid) {
        logger.info("Fragments count: {}", fragments.size());

        // Fragment 0 = basic offset table (thường rỗng hoặc chứa offset)
        // Fragment 1..N = compressed frame data
        for (int i = 1; i < fragments.size(); i++) {
            Object fragment = fragments.get(i);
            if (fragment instanceof byte[]) {
                byte[] fragBytes = (byte[]) fragment;
                logger.info("Fragment {}: {} bytes", i, fragBytes.length);

                if (fragBytes.length == 0) continue;

                // Thử decode bằng Java ImageIO (hỗ trợ JPEG Baseline, PNG, etc.)
                try {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(fragBytes));
                    if (img != null) {
                        logger.info("Successfully decoded fragment {} as image ({}x{})",
                                    i, img.getWidth(), img.getHeight());
                        return img;
                    }
                } catch (Exception e) {
                    logger.warn("ImageIO decode of fragment {} failed: {}", i, e.getMessage());
                }

                // Thử xem fragment có phải JPEG không (check JPEG magic bytes)
                if (fragBytes.length > 2 &&
                    (fragBytes[0] & 0xFF) == 0xFF && (fragBytes[1] & 0xFF) == 0xD8) {
                    logger.info("Fragment {} has JPEG header, trying forced JPEG decode", i);
                    try {
                        // Wrap in proper JPEG stream and retry
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(fragBytes));
                        if (img != null) return img;
                    } catch (Exception e) {
                        logger.warn("Forced JPEG decode failed: {}", e.getMessage());
                    }
                }

                // Nếu là JPEG 2000 (magic: 0x00 0x00 0x00 0x0C 0x6A 0x50)
                if (fragBytes.length > 6 &&
                    fragBytes[0] == 0x00 && fragBytes[1] == 0x00 &&
                    fragBytes[2] == 0x00 && fragBytes[3] == 0x0C &&
                    fragBytes[4] == 0x6A && fragBytes[5] == 0x50) {
                    logger.info("Fragment {} appears to be JPEG 2000", i);
                    // Java built-in ImageIO doesn't support J2K, but try anyway
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(fragBytes));
                        if (img != null) return img;
                    } catch (Exception e) {
                        logger.warn("JPEG 2000 decode failed (no J2K plugin): {}", e.getMessage());
                    }
                }
            } else {
                logger.warn("Fragment {} is not byte[] but: {}", i,
                           fragment != null ? fragment.getClass().getSimpleName() : "null");
            }
        }

        // Nếu không decode được từng fragment, thử ghép tất cả fragments lại
        logger.info("Trying to concatenate all fragments and decode...");
        try {
            ByteArrayOutputStream combined = new ByteArrayOutputStream();
            for (int i = 1; i < fragments.size(); i++) {
                Object fragment = fragments.get(i);
                if (fragment instanceof byte[]) {
                    combined.write((byte[]) fragment);
                }
            }
            byte[] allData = combined.toByteArray();
            if (allData.length > 0) {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(allData));
                if (img != null) {
                    logger.info("Successfully decoded concatenated fragments");
                    return img;
                }
            }
        } catch (Exception e) {
            logger.warn("Concatenated fragments decode failed: {}", e.getMessage());
        }

        return null;
    }

    // ── Build image from native (uncompressed) pixel data ───────────────

    private BufferedImage buildImageFromNativePixels(byte[] pixelData, Attributes attrs,
                                                      int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            logger.error("Invalid DICOM dimensions: rows={}, cols={}", rows, cols);
            return null;
        }

        int bitsAllocated  = attrs.getInt(Tag.BitsAllocated, 16);
        int bitsStored     = attrs.getInt(Tag.BitsStored, bitsAllocated);
        int pixelRep       = attrs.getInt(Tag.PixelRepresentation, 0);
        int samplesPerPx   = attrs.getInt(Tag.SamplesPerPixel, 1);
        String photoInterp = safe(attrs, Tag.PhotometricInterpretation);

        logger.info("DICOM pixel info: rows={}, cols={}, bitsAlloc={}, bitsStored={}, " +
                     "pixelRep={}, samples={}, photoInterp={}, pixelDataLen={}",
                     rows, cols, bitsAllocated, bitsStored, pixelRep,
                     samplesPerPx, photoInterp, pixelData.length);

        // Kiểm tra kích thước pixel data
        int expectedSize = rows * cols * samplesPerPx * (bitsAllocated / 8);
        if (pixelData.length < expectedSize / 2) {
            logger.warn("Pixel data too small ({} < expected {}), likely compressed",
                        pixelData.length, expectedSize);
            return null;
        }

        try {
            if (samplesPerPx == 3) {
                return buildRgbImage(pixelData, rows, cols, bitsAllocated);
            } else {
                return buildGrayscaleImage(pixelData, rows, cols,
                        bitsAllocated, bitsStored, pixelRep, photoInterp, attrs);
            }
        } catch (Exception e) {
            logger.error("Error building image: {}", e.getMessage(), e);
            return null;
        }
    }

    private BufferedImage buildGrayscaleImage(byte[] pixelData, int rows, int cols,
                                              int bitsAllocated, int bitsStored,
                                              int pixelRep, String photoInterp,
                                              Attributes attrs) {

        int totalPixels = rows * cols;
        int[] pixelValues = new int[totalPixels];

        if (bitsAllocated == 8) {
            for (int i = 0; i < totalPixels && i < pixelData.length; i++) {
                pixelValues[i] = pixelData[i] & 0xFF;
            }
        } else if (bitsAllocated == 16) {
            ByteBuffer bb = ByteBuffer.wrap(pixelData).order(ByteOrder.LITTLE_ENDIAN);
            int mask = (1 << bitsStored) - 1;
            for (int i = 0; i < totalPixels && bb.remaining() >= 2; i++) {
                int val = bb.getShort() & 0xFFFF;
                val = val & mask;
                if (pixelRep == 1) {
                    int signBit = 1 << (bitsStored - 1);
                    if ((val & signBit) != 0) {
                        val = val - (1 << bitsStored);
                    }
                }
                pixelValues[i] = val;
            }
        } else if (bitsAllocated == 32) {
            ByteBuffer bb = ByteBuffer.wrap(pixelData).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < totalPixels && bb.remaining() >= 4; i++) {
                pixelValues[i] = bb.getInt();
            }
        } else {
            logger.warn("Unsupported bitsAllocated: {}, trying 16-bit fallback", bitsAllocated);
            ByteBuffer bb = ByteBuffer.wrap(pixelData).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < totalPixels && bb.remaining() >= 2; i++) {
                pixelValues[i] = bb.getShort() & 0xFFFF;
            }
        }

        // Áp dụng Rescale Slope / Intercept (cho CT Hounsfield units)
        double slope     = getDoubleAttr(attrs, Tag.RescaleSlope, 1.0);
        double intercept = getDoubleAttr(attrs, Tag.RescaleIntercept, 0.0);
        if (slope != 1.0 || intercept != 0.0) {
            logger.info("Applying rescale: slope={}, intercept={}", slope, intercept);
            for (int i = 0; i < totalPixels; i++) {
                pixelValues[i] = (int) (pixelValues[i] * slope + intercept);
            }
        }

        // Áp dụng Window Center / Width nếu có
        double wc = getDoubleAttr(attrs, Tag.WindowCenter, Double.NaN);
        double ww = getDoubleAttr(attrs, Tag.WindowWidth, Double.NaN);

        int min, max;
        if (!Double.isNaN(wc) && !Double.isNaN(ww) && ww > 0) {
            logger.info("Applying window: center={}, width={}", wc, ww);
            min = (int) (wc - ww / 2.0);
            max = (int) (wc + ww / 2.0);
        } else {
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;
            for (int v : pixelValues) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }

        double range = (max - min) == 0 ? 1.0 : (double)(max - min);
        boolean invert = "MONOCHROME1".equalsIgnoreCase(photoInterp.trim());

        BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int idx = y * cols + x;
                if (idx >= totalPixels) break;
                int gray = (int) Math.max(0, Math.min(255,
                        (pixelValues[idx] - min) / range * 255.0));
                if (invert) gray = 255 - gray;
                image.getRaster().setSample(x, y, 0, gray);
            }
        }

        return image;
    }

    private BufferedImage buildRgbImage(byte[] pixelData, int rows, int cols,
                                        int bitsAllocated) {
        BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        int bytesPerSample = bitsAllocated / 8;
        int bytesPerPixel = bytesPerSample * 3;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int idx = (y * cols + x) * bytesPerPixel;
                if (idx + bytesPerPixel > pixelData.length) break;

                int r, g, b;
                if (bytesPerSample == 1) {
                    r = pixelData[idx] & 0xFF;
                    g = pixelData[idx + 1] & 0xFF;
                    b = pixelData[idx + 2] & 0xFF;
                } else {
                    r = ((pixelData[idx + 1] & 0xFF) << 8 | (pixelData[idx] & 0xFF)) >> 8;
                    g = ((pixelData[idx + 3] & 0xFF) << 8 | (pixelData[idx + 2] & 0xFF)) >> 8;
                    b = ((pixelData[idx + 5] & 0xFF) << 8 | (pixelData[idx + 4] & 0xFF)) >> 8;
                }

                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return image;
    }

    // ── ImageIO fallback ────────────────────────────────────────────────

    private BufferedImage readViaImageIO(byte[] dicomBytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(dicomBytes)) {
            javax.imageio.stream.ImageInputStream iis = ImageIO.createImageInputStream(bais);
            if (iis == null) return null;

            Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                logger.warn("No ImageIO readers found for DICOM format");
                return null;
            }

            javax.imageio.ImageReader reader = readers.next();
            logger.info("Using ImageIO reader: {}", reader.getClass().getName());
            reader.setInput(iis);
            BufferedImage img = reader.read(0);
            reader.dispose();
            iis.close();
            return img;
        } catch (Exception e) {
            logger.warn("ImageIO read failed: {}", e.getMessage());
            return null;
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private double getDoubleAttr(Attributes attrs, int tag, double defaultVal) {
        try {
            String val = attrs.getString(tag);
            if (val != null && !val.isBlank()) {
                val = val.split("\\\\")[0].trim();
                return Double.parseDouble(val);
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultVal;
    }

    private String safe(Attributes attrs, int tag) {
        try {
            String v = attrs.getString(tag);
            return (v != null && !v.isBlank()) ? v.trim() : "N/A";
        } catch (Exception e) {
            return "N/A";
        }
    }

    private BufferedImage normalizeToRgb(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();

        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }

        int numBands = src.getRaster().getNumBands();
        int[] pixels = src.getRaster().getPixels(0, 0, w, h, (int[]) null);

        double min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int p : pixels) {
            if (p < min) min = p;
            if (p > max) max = p;
        }
        double range = (max - min) == 0 ? 1 : (max - min);

        BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = (y * w + x) * numBands;
                int raw = pixels[idx];
                int gray = (int) Math.max(0, Math.min(255,
                        (raw - min) / range * 255.0));
                rgb.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }
        return rgb;
    }

    private String encodeJpeg(BufferedImage image, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new RuntimeException("Không tìm thấy JPEG writer");

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        ios.close();

        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Trích xuất các thông số kỹ thuật từ file DICOM
     */
    public Map<String, String> extractTechnicalParams(byte[] dicomBytes) {
        Map<String, String> params = new HashMap<>();
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomBytes))) {
            dis.setIncludeBulkData(DicomInputStream.IncludeBulkData.NO);
            Attributes attrs = dis.readDataset();
            
            String kvp = safe(attrs, Tag.KVP);
            String mAs = safe(attrs, Tag.ExposureInmAs);
            if ("N/A".equals(mAs)) {
                mAs = safe(attrs, Tag.Exposure);
            }
            if ("N/A".equals(mAs)) {
                mAs = safe(attrs, Tag.ExposureTime);
            }
            String sliceThickness = safe(attrs, Tag.SliceThickness);
            String contrast = safe(attrs, Tag.ContrastBolusAgent);
            
            params.put("kVp", "N/A".equals(kvp) ? "" : kvp);
            params.put("mAs", "N/A".equals(mAs) ? "" : mAs);
            params.put("sliceThickness", "N/A".equals(sliceThickness) ? "" : sliceThickness);
            params.put("contrast", "N/A".equals(contrast) ? "" : contrast);
        } catch (Exception e) {
            logger.error("Error extracting technical params from DICOM", e);
        }
        return params;
    }

    // ── Result wrapper ───────────────────────────────────────────────────
    public static class ConvertResult {
        public final String base64Jpeg;
        public final DicomMetadata metadata;

        public ConvertResult(String base64Jpeg, DicomMetadata metadata) {
            this.base64Jpeg = base64Jpeg;
            this.metadata   = metadata;
        }
    }
}