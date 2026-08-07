package vn.vnpost.lunchorder.core.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.vnpost.lunchorder.core.modules.systemconfig.entity.SystemConfig;
import vn.vnpost.lunchorder.core.modules.systemconfig.repository.SystemConfigRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CutOffPolicyTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private SystemConfigRepository systemConfigRepository;

    @BeforeEach
    void setUp() {
        systemConfigRepository = mock(SystemConfigRepository.class);
        when(systemConfigRepository.findByConfigKey(anyString())).thenReturn(Optional.empty());
    }

    private CutOffPolicy policyAt(String isoDateTime) {
        Clock clock = Clock.fixed(LocalDateTime.parse(isoDateTime).atZone(ZONE).toInstant(), ZONE);
        return new CutOffPolicy(systemConfigRepository, clock);
    }

    private void givenConfig(String key, String value) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        when(systemConfigRepository.findByConfigKey(key)).thenReturn(Optional.of(config));
    }

    @Test
    void getCutOffTime_khongCoCauHinh_thiDungMacDinh1445() {
        assertThat(policyAt("2026-07-29T09:00").getCutOffTime()).isEqualTo(LocalTime.of(14, 45));
    }

    @Test
    void getCutOffTime_coCauHinh_thiDungGiaTriCauHinh() {
        givenConfig("CUT_OFF_TIME", "16:30");

        assertThat(policyAt("2026-07-29T09:00").getCutOffTime()).isEqualTo(LocalTime.of(16, 30));
    }

    @Test
    void getCutOffTime_cauHinhSaiDinhDang_thiQuayVeMacDinh() {
        givenConfig("CUT_OFF_TIME", "khong-phai-gio");

        assertThat(policyAt("2026-07-29T09:00").getCutOffTime()).isEqualTo(LocalTime.of(14, 45));
    }

    @Test
    void getTicketLockTime_khongCoCauHinh_thiDungMacDinh1230() {
        assertThat(policyAt("2026-07-29T09:00").getTicketLockTime()).isEqualTo(LocalTime.of(12, 30));
    }

    @Test
    void getAutoConfirmTime_khongCoCauHinh_thiDungMacDinh1100() {
        assertThat(policyAt("2026-07-29T09:00").getAutoConfirmTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void macDinh_gioChotSoLuongSomHonGioDongChoVe() {
        CutOffPolicy policy = policyAt("2026-07-29T09:00");

        assertThat(policy.getAutoConfirmTime()).isBefore(policy.getTicketLockTime());
    }

    @Test
    void getAutoConfirmTime_coCauHinh_thiDocDocLapVoiGioKhoaVe() {
        givenConfig("AUTO_CONFIRM_TIME", "10:30");
        givenConfig("TICKET_LOCK_TIME", "12:30");
        CutOffPolicy policy = policyAt("2026-07-29T09:00");

        assertThat(policy.getAutoConfirmTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(policy.getTicketLockTime()).isEqualTo(LocalTime.of(12, 30));
    }

    @Test
    void isCutOffReached_conHaiNgayNua_thiChuaChot() {
        assertThat(policyAt("2026-07-29T09:00").isCutOffReached(LocalDate.parse("2026-07-31"))).isFalse();
    }

    @Test
    void isCutOffReached_dungNgayChotVaTruocGioChot_thiChuaChot() {
        assertThat(policyAt("2026-07-29T14:44").isCutOffReached(LocalDate.parse("2026-07-30"))).isFalse();
    }

    @Test
    void isCutOffReached_dungNgayChotVaDungGioChot_thiVanChuaChot() {
        assertThat(policyAt("2026-07-29T14:45").isCutOffReached(LocalDate.parse("2026-07-30"))).isFalse();
    }

    @Test
    void isCutOffReached_dungNgayChotVaQuaGioChot_thiDaChot() {
        assertThat(policyAt("2026-07-29T14:46").isCutOffReached(LocalDate.parse("2026-07-30"))).isTrue();
    }

    @Test
    void isCutOffReached_datChoChinhHomNay_thiDaChot() {
        assertThat(policyAt("2026-07-29T08:00").isCutOffReached(LocalDate.parse("2026-07-29"))).isTrue();
    }

    @Test
    void isCutOffReached_datChoNgayDaQua_thiDaChot() {
        assertThat(policyAt("2026-07-29T08:00").isCutOffReached(LocalDate.parse("2026-07-28"))).isTrue();
    }

    @Test
    void isWithinExchangeWindow_truocGioMoCua_thiNgoaiKhung() {
        assertThat(policyAt("2026-07-29T14:44").isWithinExchangeWindow(LocalDate.parse("2026-07-30"))).isFalse();
    }

    @Test
    void isWithinExchangeWindow_dungGioMoCua_thiTrongKhung() {
        assertThat(policyAt("2026-07-29T14:45").isWithinExchangeWindow(LocalDate.parse("2026-07-30"))).isTrue();
    }

    @Test
    void isWithinExchangeWindow_giuaKhung_thiTrongKhung() {
        assertThat(policyAt("2026-07-30T08:00").isWithinExchangeWindow(LocalDate.parse("2026-07-30"))).isTrue();
    }

    @Test
    void isWithinExchangeWindow_dungGioKhoaVe_thiVanTrongKhung() {
        assertThat(policyAt("2026-07-30T12:30").isWithinExchangeWindow(LocalDate.parse("2026-07-30"))).isTrue();
    }

    @Test
    void isWithinExchangeWindow_quaGioKhoaVe_thiNgoaiKhung() {
        assertThat(policyAt("2026-07-30T12:31").isWithinExchangeWindow(LocalDate.parse("2026-07-30"))).isFalse();
    }

    @Test
    void isWithinExchangeWindow_quaGioChotSoLuongNhungChuaKhoaVe_thiVanTrongKhung() {
        assertThat(policyAt("2026-07-30T11:30").isWithinExchangeWindow(LocalDate.parse("2026-07-30"))).isTrue();
    }

    @Test
    void isWithinExchangeWindow_theoCauHinhTuyChinh() {
        givenConfig("CUT_OFF_TIME", "16:00");
        givenConfig("TICKET_LOCK_TIME", "10:00");
        CutOffPolicy policy = policyAt("2026-07-29T15:59");

        assertThat(policy.isWithinExchangeWindow(LocalDate.parse("2026-07-30"))).isFalse();
    }

    @Test
    void isWeekend_thuBayVaChuNhat_thiTrue() {
        CutOffPolicy policy = policyAt("2026-07-29T09:00");

        assertThat(policy.isWeekend(LocalDate.parse("2026-08-01"))).isTrue();
        assertThat(policy.isWeekend(LocalDate.parse("2026-08-02"))).isTrue();
    }

    @Test
    void isWeekend_ngayThuongThiFalse() {
        assertThat(policyAt("2026-07-29T09:00").isWeekend(LocalDate.parse("2026-07-31"))).isFalse();
    }

    @Test
    void getHolidays_khongCoCauHinh_thiRong() {
        assertThat(policyAt("2026-07-29T09:00").getHolidays()).isEmpty();
    }

    @Test
    void getHolidayDates_boQuaKhoangTrangVaGiaTriRong() {
        givenConfig("HOLIDAYS", " 2026-09-02 , ,2026-01-01, ");

        assertThat(policyAt("2026-07-29T09:00").getHolidayDates())
                .containsExactlyInAnyOrder(LocalDate.parse("2026-09-02"), LocalDate.parse("2026-01-01"));
    }

    @Test
    void getHolidayDates_boQuaNgayKhongHopLeThayViNemLoi() {
        givenConfig("HOLIDAYS", "2026-09-02,ngay-loi,2026-01-01");

        assertThat(policyAt("2026-07-29T09:00").getHolidayDates())
                .containsExactlyInAnyOrder(LocalDate.parse("2026-09-02"), LocalDate.parse("2026-01-01"));
    }

    @Test
    void getMaxOrderableDate_laCuoiThangThuBaKeTiep() {
        assertThat(policyAt("2026-07-29T09:00").getMaxOrderableDate()).isEqualTo(LocalDate.parse("2026-10-31"));
    }

    @Test
    void getMaxOrderableDate_batCauNamMoi() {
        assertThat(policyAt("2026-11-15T09:00").getMaxOrderableDate()).isEqualTo(LocalDate.parse("2027-02-28"));
    }

    @Test
    void today_layTheoDongHoDuocTiemVao() {
        assertThat(policyAt("2026-07-29T23:30").today()).isEqualTo(LocalDate.parse("2026-07-29"));
    }
}
