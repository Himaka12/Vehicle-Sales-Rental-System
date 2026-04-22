package com.example.Vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAiResult {
    private String sentiment;
    private boolean requiresAdminAttention;
    private String aiReason;
    private String adminAttentionReason;
}
