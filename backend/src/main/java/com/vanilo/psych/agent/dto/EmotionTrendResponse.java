package com.vanilo.psych.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmotionTrendResponse {
    private LocalDate date;
    private String emotion;
    private Long count;
    private Double averageConfidence;
}
