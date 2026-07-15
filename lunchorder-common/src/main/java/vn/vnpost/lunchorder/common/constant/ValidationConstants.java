package vn.vnpost.lunchorder.common.constant;

/**
 * Tập hợp các mẫu Regex dùng cho việc validate dữ liệu đầu vào (DTO).
 *
 * <p>Mục đích: tránh hardcode Regex rải rác trong các DTO, giúp tái sử dụng và
 * bảo trì tập trung tại một nơi duy nhất.
 */
public final class ValidationConstants {

    /** Chỉ chứa chữ số, không có khoảng trắng (dùng cho tên đăng nhập/tài khoản). */
    public static final String REGEX_ACCOUNT = "^\\d+$";

    /** Không chứa khoảng trắng (dùng cho mật khẩu). */
    public static final String REGEX_PASSWORD = "^\\S+$";

    /**
     * Chữ cái (bao gồm ký tự tiếng Việt) và khoảng trắng; không cho phép số,
     * ký tự đặc biệt (dùng cho họ tên người).
     */
    public static final String REGEX_PERSON_NAME = "^[a-zA-ZÀ-ỹ\\s]+$";

    /**
     * Chữ cái (bao gồm ký tự tiếng Việt), chữ số và khoảng trắng; không cho phép
     * ký tự đặc biệt (dùng cho các loại tên chung).
     */
    public static final String REGEX_GENERAL_NAME = "^[a-zA-ZÀ-ỹ0-9\\s]+$";

    /** Chỉ chữ in hoa và dấu gạch dưới (dùng cho mã code hệ thống). */
    public static final String REGEX_CODE = "^[A-Z_]+$";

    private ValidationConstants() {
    }
}
