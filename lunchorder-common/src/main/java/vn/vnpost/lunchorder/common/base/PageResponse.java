package vn.vnpost.lunchorder.common.base;

import lombok.Builder;
import lombok.Data;

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
}
