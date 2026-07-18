package vn.vnpost.lunchorder.core.infrastructure.storage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.vnpost.lunchorder.common.base.ApiResponse;
import vn.vnpost.lunchorder.core.infrastructure.storage.controller.dto.UploadResponse;
import vn.vnpost.lunchorder.core.infrastructure.storage.service.ImageStorageService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/uploads")
public class FileUploadController {

    private final ImageStorageService imageStorageService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_MENUS')")
    public ApiResponse<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = imageStorageService.upload(file, "menus");
        return ApiResponse.<UploadResponse>builder()
                .result(new UploadResponse(url))
                .build();
    }
}
