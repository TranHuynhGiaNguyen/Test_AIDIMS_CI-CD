package com.aidims.aidimsbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsonTestReportListener implements TestExecutionListener {

    private final Map<String, Long> startTimes = new ConcurrentHashMap<>();
    private final List<TestResultDetails> testResults = Collections.synchronizedList(new ArrayList<>());
    
    private int totalTests = 0;
    private int passedTests = 0;
    private int failedTests = 0;
    private int skippedTests = 0;
    private long planStartTime;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        planStartTime = System.currentTimeMillis();
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            startTimes.put(testIdentifier.getUniqueId(), System.currentTimeMillis());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            long endTime = System.currentTimeMillis();
            Long startTime = startTimes.remove(testIdentifier.getUniqueId());
            long duration = (startTime != null) ? (endTime - startTime) : 0;

            String className = "";
            String methodName = "";
            if (testIdentifier.getSource().isPresent() && testIdentifier.getSource().get() instanceof MethodSource) {
                MethodSource methodSource = (MethodSource) testIdentifier.getSource().get();
                className = methodSource.getClassName();
                methodName = methodSource.getMethodName();
            } else {
                className = testIdentifier.getLegacyReportingName();
            }

            String status = testExecutionResult.getStatus().toString();
            String errorMessage = null;
            String stackTrace = null;

            totalTests++;
            if (testExecutionResult.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                passedTests++;
            } else {
                failedTests++;
                if (testExecutionResult.getThrowable().isPresent()) {
                    Throwable throwable = testExecutionResult.getThrowable().get();
                    errorMessage = throwable.getMessage();
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    stackTrace = sw.toString();
                }
            }

            testResults.add(new TestResultDetails(
                    className,
                    methodName,
                    testIdentifier.getDisplayName(),
                    status,
                    duration,
                    errorMessage,
                    stackTrace
            ));
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.isTest()) {
            String className = "";
            String methodName = "";
            if (testIdentifier.getSource().isPresent() && testIdentifier.getSource().get() instanceof MethodSource) {
                MethodSource methodSource = (MethodSource) testIdentifier.getSource().get();
                className = methodSource.getClassName();
                methodName = methodSource.getMethodName();
            } else {
                className = testIdentifier.getLegacyReportingName();
            }

            totalTests++;
            skippedTests++;

            testResults.add(new TestResultDetails(
                    className,
                    methodName,
                    testIdentifier.getDisplayName(),
                    "SKIPPED",
                    0,
                    reason,
                    null
            ));
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        long planDuration = System.currentTimeMillis() - planStartTime;

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTests", totalTests);
        summary.put("passedTests", passedTests);
        summary.put("failedTests", failedTests);
        summary.put("skippedTests", skippedTests);
        summary.put("durationMs", planDuration);
        summary.put("timestamp", Instant.now().toString());

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", summary);
        report.put("results", testResults);

        try {
            Files.createDirectories(Paths.get("target"));
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File("target/test-report.json"), report);
            System.out.println(">>> [JSON Report] Saved JUnit test report to target/test-report.json");
        } catch (Exception e) {
            System.err.println(">>> [JSON Report] Failed to write test report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class TestResultDetails {
        public String className;
        public String methodName;
        public String displayName;
        public String status;
        public long durationMs;
        public String errorMessage;
        public String stackTrace;

        public TestResultDetails(String className, String methodName, String displayName, 
                                 String status, long durationMs, String errorMessage, String stackTrace) {
            this.className = className;
            this.methodName = methodName;
            this.displayName = displayName;
            this.status = status;
            this.durationMs = durationMs;
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
        }
    }
}
