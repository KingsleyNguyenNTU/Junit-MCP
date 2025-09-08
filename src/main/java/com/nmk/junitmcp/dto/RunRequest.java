package com.nmk.junitmcp.dto;

import lombok.Data;

import java.util.List;

@Data
public class RunRequest {
    private List<String> tests; // e.g. ["com.example.UserServiceTest#shouldCreateUser"]
    private String projectPath; // optional path to external project directory
}
