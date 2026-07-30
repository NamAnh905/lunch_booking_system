package vn.vnpost.lunchorder.core.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.vnpost.lunchorder.common.exception.AppException;
import vn.vnpost.lunchorder.common.exception.ErrorCode;
import vn.vnpost.lunchorder.core.modules.systemconfig.repository.SystemConfigRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderableDatesTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private OrderableDates orderableDates;

    @BeforeEach
    void setUp() {
        SystemConfigRepository systemConfigRepository = mock(SystemConfigRepository.class);
        when(systemConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());

        Clock clock = Clock.fixed(LocalDateTime.parse("2026-07-29T09:00").atZone(ZONE).toInstant(), ZONE);
        orderableDates = OrderableDates.snapshot(new CutOffPolicy(systemConfigRepository, clock));
    }

    private void assertRejects(String date, ErrorCode expected) {
        assertThatThrownBy(() -> orderableDates.assertOrderable(LocalDate.parse(date)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    void ngayThuongHopLe_thiKhongNemLoi() {
        assertThatCode(() -> orderableDates.assertOrderable(LocalDate.parse("2026-07-31")))
                .doesNotThrowAnyException();
    }

    @Test
    void thuBay_thiBaoNgayKhongDuocPhep() {
        assertRejects("2026-08-01", ErrorCode.ORDER_DATE_NOT_ALLOWED);
    }

    @Test
    void chuNhat_thiBaoNgayKhongDuocPhep() {
        assertRejects("2026-08-02", ErrorCode.ORDER_DATE_NOT_ALLOWED);
    }

    @Test
    void quaXa_thiBaoVuotGioiHan() {
        assertRejects("2026-11-02", ErrorCode.ORDER_DATE_TOO_FAR);
    }

    @Test
    void ngayCuoiCungConDatDuoc_thiKhongNemLoi() {
        assertThatCode(() -> orderableDates.assertOrderable(LocalDate.parse("2026-10-30")))
                .doesNotThrowAnyException();
    }

    @Test
    void datChoChinhHomNay_thiBaoDaQuaGioChot() {
        assertRejects("2026-07-29", ErrorCode.ORDER_CUTOFF_REACHED);
    }

    @Test
    void datChoNgayDaQua_thiBaoDaQuaGioChot() {
        assertRejects("2026-07-28", ErrorCode.ORDER_CUTOFF_REACHED);
    }

    @Test
    void cuoiTuanVaQuaHan_thiUuTienBaoNgayKhongDuocPhep() {
        assertRejects("2026-07-25", ErrorCode.ORDER_DATE_NOT_ALLOWED);
    }

    @Test
    void cuoiTuanVaQuaXa_thiUuTienBaoNgayKhongDuocPhep() {
        assertRejects("2026-11-01", ErrorCode.ORDER_DATE_NOT_ALLOWED);
    }

    @Test
    void snapshot_chupNgayLeMotLanDuyNhat() {
        assertThat(orderableDates.maxOrderableDate()).isEqualTo(LocalDate.parse("2026-10-31"));
        assertThat(orderableDates.holidays()).isEmpty();
    }
}
