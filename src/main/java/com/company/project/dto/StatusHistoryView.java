package com.company.project.dto;

import com.company.project.entity.AccountStatus;
import com.company.project.entity.AccountStatusHistory;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StatusHistoryView(
        LocalDateTime changedAt,
        AccountStatus fromStatus,
        AccountStatus toStatus,
        String reason,
        String note,
        LocalDate fromDate,
        LocalDate toDate,
        String changedByName
) implements Serializable {
    public static StatusHistoryView from(AccountStatusHistory history) {
        return new StatusHistoryView(history.getChangedAt(), history.getFromStatus(), history.getToStatus(),
                history.getReason(), history.getNote(), history.getFromDate(), history.getToDate(),
                history.getChangedByName());
    }
}
