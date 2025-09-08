package com.nmk.junitmcp.junit_platform;

import com.nmk.junitmcp.dto.TestResult;
import lombok.Getter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestResultCollector implements TestExecutionListener {
    @Getter
    private final List<TestResult> results = new ArrayList<>();
    private final Map<String, Long> startTimes = new HashMap<>();

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            startTimes.put(testIdentifier.getUniqueId(), System.currentTimeMillis());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            TestResult result = new TestResult();
            result.setTest(testIdentifier.getDisplayName());
            result.setStatus(testExecutionResult.getStatus().name());
            result.setDurationMs(System.currentTimeMillis() - startTimes.get(testIdentifier.getUniqueId()));

            testExecutionResult.getThrowable().ifPresent(t -> {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);  // Write stack trace to PrintWriter
                result.setError(sw.toString());
            });
            results.add(result);
        }
    }
}
