package com.nmk.junitmcp.dto;

import lombok.Data;

@Data
public class TestResult {
    private String test;
    private String status;
    private String error;
    private long durationMs;
}
