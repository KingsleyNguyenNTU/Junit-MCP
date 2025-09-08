package com.nmk.junitmcp.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RunResponse {
    private List<TestResult> results;
    private Map<String, CoverageResult> coverage;
}
