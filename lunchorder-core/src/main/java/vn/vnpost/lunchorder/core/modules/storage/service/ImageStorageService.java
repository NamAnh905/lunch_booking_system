package vn.vnpost.lunchorder.core.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Cổng (port) trừu tượng cho việc lưu trữ ảnh. Nghiệp vụ chỉ phụ thuộc vào
 * interface này, không biết nhà cung cấp cụ thể (Cloudinary, S3, ...).
 */
public interface ImageStorageService {

    String upload(MultipartFile file, String folder);
}
