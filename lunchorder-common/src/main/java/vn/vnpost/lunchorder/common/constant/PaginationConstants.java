package vn.vnpost.lunchorder.common.constant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PaginationConstants {

    public static final String DEFAULT_PAGE = "1";
    public static final String DEFAULT_SIZE = "10";
    public static final int MIN_PAGE = 1;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
    public static final int MAX_LOOKUP_SIZE = 5000;

    private PaginationConstants() {
    }

    public static int clampSize(int size) {
        if (size < MIN_SIZE) {
            return MIN_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static Pageable toPageable(int page, int size) {
        return PageRequest.of(toPageIndex(page), clampSize(size));
    }
    
    public static Pageable toPageable(int page, int size, Sort sort) {
        return PageRequest.of(toPageIndex(page), clampSize(size), sort);
    }

    private static int toPageIndex(int page) {
        return Math.max(0, page - 1);
    }
}
