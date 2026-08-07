package vn.vnpost.lunchorder.core.modules.ticketexchange.service.dto;

import lombok.Builder;
import lombok.Getter;
import vn.vnpost.lunchorder.tools.excel.ExcelColumn;

@Getter
@Builder
public class TicketExchangeExportResponse {

    @ExcelColumn(name = "STT", width = 2000)
    private String index;

    @ExcelColumn(name = "Ngày ăn", width = 4000)
    private String menuDate;

    @ExcelColumn(name = "Thời gian đăng", width = 6000)
    private String createdAt;

    @ExcelColumn(name = "Người pass", width = 7000)
    private String sellerName;

    @ExcelColumn(name = "Người nhận", width = 7000)
    private String buyerName;

    @ExcelColumn(name = "Trạng thái", width = 5000)
    private String status;
}
