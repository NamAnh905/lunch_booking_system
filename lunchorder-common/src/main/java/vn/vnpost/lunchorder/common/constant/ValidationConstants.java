package vn.vnpost.lunchorder.common.constant;

public final class ValidationConstants {
    public static final String REGEX_ACCOUNT = "^\\d+$";
    public static final String REGEX_PASSWORD = "^\\S+$";
    public static final String REGEX_PERSON_NAME = "^[a-zA-ZÀ-ỹ\\s()]+$";
    public static final String REGEX_GENERAL_NAME = "^[a-zA-ZÀ-ỹ0-9\\s]+$";
    public static final String REGEX_CODE = "^[A-Z_]+$";

    private ValidationConstants() {
    }
}
