package com.nmk.junitmcp.dto;

import lombok.Data;

@Data
public class CoverageResult {
    private int coveredLines;
    private int totalLines;
    private double coveragePercent;
}
