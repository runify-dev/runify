package com.run.handler.application.vo;

import com.run.dao.common.annotations.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageTrendVO {
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "count")
    private Long count;
}
