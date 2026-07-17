package vn.vnpost.lunchorder.common.constant;

/**
 * Các hằng số dùng chung cho phân trang.
 *
 * <p>Thiết kế: dự án giữ contract phân trang 1-indexed (tham số {@code page}/{@code size}
 * kiểu {@code int}, service tự trừ 1). Trần kích thước trang KHÔNG còn được đặt bằng
 * {@code @Max} trên tham số controller (gây phải nâng số magic mỗi khi dữ liệu vượt trần).
 * Thay vào đó:
 * <ul>
 *   <li>Dữ liệu bảng có phân trang thật: service tự cắt (clamp) size về {@link #MAX_SIZE}
 *       qua {@link #clampSize(int)} — đây là NƠI DUY NHẤT cấu hình trần, chỉ để bảo vệ DB.</li>
 *   <li>Dữ liệu tra cứu (dropdown/filter): dùng endpoint {@code .../all}, service đặt cứng
 *       trần an toàn {@link #MAX_LOOKUP_SIZE}, client không điều khiển được.</li>
 * </ul>
 */
public final class PaginationConstants {

    public static final String DEFAULT_PAGE = "1";

    public static final String DEFAULT_SIZE = "10";

    public static final int MIN_PAGE = 1;

    public static final int MIN_SIZE = 1;

    /**
     * Trần kích thước trang cho dữ liệu bảng có phân trang thật.
     * Chỉ nhằm bảo vệ DB khỏi request size bất thường; không cần chỉnh khi scale
     * vì dữ liệu tra cứu đã tách sang endpoint {@code .../all}.
     */
    public static final int MAX_SIZE = 100;

    /**
     * Trần an toàn (đặt cứng phía service) cho các endpoint tra cứu {@code .../all}.
     * Client KHÔNG điều khiển được giá trị này.
     */
    public static final int MAX_LOOKUP_SIZE = 5000;

    private PaginationConstants() {
    }

    /**
     * Cắt (clamp) kích thước trang do client gửi về khoảng an toàn
     * [{@link #MIN_SIZE}, {@link #MAX_SIZE}] để bảo vệ DB.
     */
    public static int clampSize(int size) {
        if (size < MIN_SIZE) {
            return MIN_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
