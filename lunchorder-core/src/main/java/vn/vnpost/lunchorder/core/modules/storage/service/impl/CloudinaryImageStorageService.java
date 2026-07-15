package vn.vnpost.lunchorder.core.modules.storage.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.storage.service.ImageStorageService;

import java.util.Map;

/**
 * Adapter cụ thể dùng Cloudinary. Chỉ lớp này biết chi tiết Cloudinary,
 * giúp dễ thay thế nhà cung cấp khác về sau.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryImageStorageService implements ImageStorageService {

    private final Cloudinary cloudinary;

    @Override
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMAGE_INVALID);
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image"));
            return (String) result.get("secure_url");
        } catch (Exception e) {
            // Bắt cả IOException lẫn lỗi runtime từ Cloudinary (sai cloud_name/api_key/secret...)
            log.error("Upload ảnh lên Cloudinary thất bại: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }
}
