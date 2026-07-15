package vn.vnpost.lunchorder.core.modules.storage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Cổng (port) trừu tượng cho việc lưu trữ ảnh. Nghiệp vụ chỉ phụ thuộc vào
 * interface này, không biết nhà cung cấp cụ thể (Cloudinary, S3, ...).
 */
public interface ImageStorageService {

    /**
     * Tải một ảnh lên kho lưu trữ.
     *
     * @param file   file ảnh nhận từ client
     * @param folder thư mục/nhóm lưu trữ (vd: "menus")
     * @return URL an toàn (https) của ảnh đã tải lên
     */
    String upload(MultipartFile file, String folder);
}
