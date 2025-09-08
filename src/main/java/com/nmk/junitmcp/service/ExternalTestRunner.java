package com.nmk.junitmcp.service;

import com.nmk.junitmcp.dto.TestResult;
import com.nmk.junitmcp.util.ProjectPathResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExternalTestRunner {
    
    private static final Pattern MAVEN_TEST_PATTERN = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");
    private static final Pattern MAVEN_TEST_NAME_PATTERN = Pattern.compile("Running (.+)");
    
    // Gradle patterns
    private static final Pattern GRADLE_TEST_STARTED = Pattern.compile("(.+) > (.+) STARTED");
    private static final Pattern GRADLE_TEST_PASSED = Pattern.compile("(.+) > (.+) PASSED");
    private static final Pattern GRADLE_TEST_FAILED = Pattern.compile("(.+) > (.+) FAILED");
    private static final Pattern GRADLE_TEST_SKIPPED = Pattern.compile("(.+) > (.+) SKIPPED");

    public List<TestResult> runTests(ProjectPathResolver pathResolver, List<String> tests) throws IOException, InterruptedException {
        String buildCommand = pathResolver.getBuildCommand();
        String[] testArgs = pathResolver.getTestArgs(tests);
        String projectPath = pathResolver.getProjectPath();
        
        List<String> command = new ArrayList<>();
        command.add(buildCommand);
        command.addAll(Arrays.asList(testArgs));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(projectPath));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        
        List<TestResult> results = new ArrayList<>();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            if (pathResolver.getProjectType() == ProjectPathResolver.ProjectType.MAVEN) {
                results = parseMavenOutput(reader, output);
            } else if (pathResolver.getProjectType() == ProjectPathResolver.ProjectType.GRADLE) {
                results = parseGradleOutput(reader, output);
            }
        }

        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Test execution timed out after 10 minutes");
        }

        int exitCode = process.exitValue();
        
        // If no results were parsed but exit code indicates failure, create a generic failure result
        if (results.isEmpty() && exitCode != 0) {
            for (String test : tests) {
                TestResult result = new TestResult();
                result.setTest(test);
                result.setStatus("FAILED");
                result.setError("Test execution failed. Exit code: " + exitCode);
                result.setDurationMs(0L);
                results.add(result);
            }
        } else if (results.isEmpty()) {
            // Success but no results parsed - create success results
            for (String test : tests) {
                TestResult result = new TestResult();
                result.setTest(test);
                result.setStatus("PASSED");
                result.setDurationMs(0L);
                results.add(result);
            }
        }
        
        return results;
    }
    
    private List<TestResult> parseMavenOutput(BufferedReader reader, StringBuilder output) throws IOException {
        List<TestResult> results = new ArrayList<>();
        String line;
        String currentTest = null;
        long startTime = System.currentTimeMillis();
        
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
            
            Matcher testNameMatcher = MAVEN_TEST_NAME_PATTERN.matcher(line);
            if (testNameMatcher.find()) {
                currentTest = testNameMatcher.group(1);
                startTime = System.currentTimeMillis();
            }
            
            Matcher testResultMatcher = MAVEN_TEST_PATTERN.matcher(line);
            if (testResultMatcher.find() && currentTest != null) {
                int failures = Integer.parseInt(testResultMatcher.group(2));
                int errors = Integer.parseInt(testResultMatcher.group(3));
                
                TestResult result = new TestResult();
                result.setTest(currentTest);
                result.setDurationMs(System.currentTimeMillis() - startTime);
                
                if (failures > 0 || errors > 0) {
                    result.setStatus("FAILED");
                    result.setError(extractErrorMessage(output.toString(), currentTest));
                } else {
                    result.setStatus("PASSED");
                }
                
                results.add(result);
                currentTest = null;
            }
        }
        
        return results;
    }
    
    private List<TestResult> parseGradleOutput(BufferedReader reader, StringBuilder output) throws IOException {
        List<TestResult> results = new ArrayList<>();
        Map<String, Long> testStartTimes = new HashMap<>();
        String line;
        
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
            
            // Check for test started
            Matcher startedMatcher = GRADLE_TEST_STARTED.matcher(line);
            if (startedMatcher.find()) {
                String testClass = startedMatcher.group(1);
                String testMethod = startedMatcher.group(2);
                String fullTestName = testClass + "#" + testMethod;
                testStartTimes.put(fullTestName, System.currentTimeMillis());
                continue;
            }
            
            // Check for test results
            TestResult result = null;
            String fullTestName = null;
            
            Matcher passedMatcher = GRADLE_TEST_PASSED.matcher(line);
            if (passedMatcher.find()) {
                String testClass = passedMatcher.group(1);
                String testMethod = passedMatcher.group(2);
                fullTestName = testClass + "#" + testMethod;
                
                result = new TestResult();
                result.setTest(fullTestName);
                result.setStatus("PASSED");
            }
            
            Matcher failedMatcher = GRADLE_TEST_FAILED.matcher(line);
            if (failedMatcher.find()) {
                String testClass = failedMatcher.group(1);
                String testMethod = failedMatcher.group(2);
                fullTestName = testClass + "#" + testMethod;
                
                result = new TestResult();
                result.setTest(fullTestName);
                result.setStatus("FAILED");
                result.setError(extractGradleErrorMessage(output.toString(), fullTestName));
            }
            
            Matcher skippedMatcher = GRADLE_TEST_SKIPPED.matcher(line);
            if (skippedMatcher.find()) {
                String testClass = skippedMatcher.group(1);
                String testMethod = skippedMatcher.group(2);
                fullTestName = testClass + "#" + testMethod;
                
                result = new TestResult();
                result.setTest(fullTestName);
                result.setStatus("SKIPPED");
            }
            
            // Set duration and add result
            if (result != null) {
                Long startTime = testStartTimes.get(fullTestName);
                if (startTime != null) {
                    result.setDurationMs(System.currentTimeMillis() - startTime);
                    testStartTimes.remove(fullTestName);
                } else {
                    result.setDurationMs(0L);
                }
                results.add(result);
            }
        }
        
        return results;
    }
    
    private String extractErrorMessage(String output, String testName) {
        String[] lines = output.split("\n");
        StringBuilder errorMsg = new StringBuilder();
        boolean inError = false;
        
        for (String line : lines) {
            if (line.contains(testName) && (line.contains("FAILURE") || line.contains("ERROR"))) {
                inError = true;
            } else if (inError && (line.trim().isEmpty() || line.contains("Tests run:"))) {
                break;
            } else if (inError) {
                errorMsg.append(line).append("\n");
            }
        }
        
        return !errorMsg.isEmpty() ? errorMsg.toString().trim() : "Test failed";
    }
    
    private String extractGradleErrorMessage(String output, String testName) {
        String[] lines = output.split("\n");
        StringBuilder errorMsg = new StringBuilder();
        boolean inTestFailure = false;
        
        for (String line : lines) {
            // Look for test failure section
            if (line.contains(testName) && line.contains("FAILED")) {
                inTestFailure = true;
                continue;
            }
            
            // Stop when we hit another test or summary
            if (inTestFailure && (line.contains(" > ") || line.contains("BUILD FAILED") || line.trim().isEmpty())) {
                break;
            }
            
            // Collect error lines
            if (inTestFailure) {
                errorMsg.append(line.trim()).append("\n");
            }
        }
        
        return !errorMsg.isEmpty() ? errorMsg.toString().trim() : "Test failed";
    }
}