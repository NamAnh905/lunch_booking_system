package vn.vnpost.lunchorder.common.base;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse<T> {
    int currentPage;
    int totalPages;
    int pageSize;
    long totalElements;

    @Builder.Default
    List<T> data = Collections.emptyList();

    public static <T> PageResponse<T> of(Page<?> source, List<T> data, int currentPage, int pageSize) {
        return PageResponse.<T>builder()
                .currentPage(currentPage)
                .totalPages(source.getTotalPages())
                .pageSize(pageSize)
                .totalElements(source.getTotalElements())
                .data(data)
                .build();
    }
}
