package com.pos.service;
import com.pos.domain.Shift;
import com.pos.dto.report.ShiftSummaryReport;
import java.math.BigDecimal;
public interface ShiftService {
    Shift openShift(long cashierId, BigDecimal openingCash, long managerId);
    ShiftSummaryReport closeShift(long shiftId, BigDecimal actualCash, long managerId);
}
