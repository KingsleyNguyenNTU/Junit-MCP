package com.nmk.junitmcp.controller;

import com.nmk.junitmcp.dto.CoverageResult;
import com.nmk.junitmcp.dto.RunRequest;
import com.nmk.junitmcp.dto.RunResponse;
import com.nmk.junitmcp.dto.TestResult;
import com.nmk.junitmcp.junit_platform.CoverageAnalyzer;
import com.nmk.junitmcp.service.ExternalProjectService;
import com.nmk.junitmcp.service.ExternalTestRunner;
import com.nmk.junitmcp.util.ProjectPathResolver;
import lombok.RequiredArgsConstructor;
import org.jacoco.core.tools.ExecFileLoader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp/junit")
@RequiredArgsConstructor
public class JUnitMcpController {
    private final ExternalProjectService externalProjectService;
    private final ExternalTestRunner externalTestRunner;
    
    @PostMapping
    public RunResponse runTests(@RequestBody RunRequest request) throws Exception {
        ProjectPathResolver pathResolver = new ProjectPathResolver(request.getProjectPath());

        // Use external process approach for external projects
        return runExternalProjectTests(request, pathResolver);
    }

    private RunResponse runExternalProjectTests(RunRequest request, ProjectPathResolver pathResolver) throws Exception {
        // 1. Build external project if needed
        externalProjectService.buildProject(pathResolver);
        
        // 2. Run tests via external process
        List<TestResult> testResults = externalTestRunner.runTests(pathResolver, request.getTests());
        
        // 3. Load coverage data
        Map<String, CoverageResult> coverageMap = loadCoverageData(pathResolver);
        
        // 4. Create response
        RunResponse response = new RunResponse();
        response.setResults(testResults);
        response.setCoverage(coverageMap);
        return response;
    }
    
    private Map<String, CoverageResult> loadCoverageData(ProjectPathResolver pathResolver) throws IOException {
        Map<String, CoverageResult> coverageMap = new HashMap<>();
        ExecFileLoader loader = new ExecFileLoader();
        boolean foundCoverage = false;
        
        // Try primary JaCoCo exec file location
        String execFilePath = pathResolver.getJaCoCoExecPath();
        File execFile = new File(execFilePath);
        if (execFile.exists()) {
            loader.load(execFile);
            foundCoverage = true;
        }
        
        // For Gradle, also try alternative locations
        if (pathResolver.getProjectType() == ProjectPathResolver.ProjectType.GRADLE) {
            // Try test.exec in jacoco folder
            String altPath1 = pathResolver.getProjectPath() + File.separator + "build" + File.separator + "jacoco" + File.separator + "test.exec";
            File altFile1 = new File(altPath1);
            if (altFile1.exists()) {
                loader.load(altFile1);
                foundCoverage = true;
            }
            
            // Try jacoco.exec in build folder
            String altPath2 = pathResolver.getProjectPath() + File.separator + "build" + File.separator + "jacoco.exec";
            File altFile2 = new File(altPath2);
            if (altFile2.exists()) {
                loader.load(altFile2);
                foundCoverage = true;
            }
        }
        
        if (foundCoverage) {
            CoverageAnalyzer analyzer = new CoverageAnalyzer(loader, pathResolver.getClassesPath());
            coverageMap = analyzer.analyze();
        }
        
        return coverageMap;
    }

}
