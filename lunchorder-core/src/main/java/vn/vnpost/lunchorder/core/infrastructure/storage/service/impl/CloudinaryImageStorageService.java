package vn.vnpost.lunchorder.core.infrastructure.storage.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.infrastructure.storage.service.ImageStorageService;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final Tika tika = new Tika();

    @Override
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMAGE_INVALID);
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new AppException(ErrorCode.IMAGE_TOO_LARGE);
        }
        try {
            byte[] bytes = file.getBytes();
            String detectedType = tika.detect(bytes);
            if (!ALLOWED_CONTENT_TYPES.contains(detectedType)) {
                throw new AppException(ErrorCode.IMAGE_TYPE_NOT_ALLOWED);
            }

            Map<?, ?> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "allowed_formats", ALLOWED_FORMATS));
            return (String) result.get("secure_url");
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Upload ảnh lên Cloudinary thất bại: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }
}