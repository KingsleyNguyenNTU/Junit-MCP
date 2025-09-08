package com.nmk.junitmcp.util;

import lombok.Getter;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProjectPathResolver {
    private final String projectPath;
    @Getter
    private final ProjectType projectType;

    public enum ProjectType {
        MAVEN, GRADLE
    }

    public ProjectPathResolver(String projectPath) {
        this.projectPath = projectPath != null ? projectPath.trim() : null;
        this.projectType = detectProjectType();
    }

    private ProjectType detectProjectType() {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new IllegalArgumentException("Project path does not exist or is not a directory: " + projectPath);
        }

        if (new File(projectDir, "pom.xml").exists()) {
            return ProjectType.MAVEN;
        } else if (new File(projectDir, "build.gradle").exists() || 
                   new File(projectDir, "build.gradle.kts").exists()) {
            return ProjectType.GRADLE;
        } else {
            throw new IllegalArgumentException("Project path does not contain pom.xml or build.gradle: " + projectPath);
        }
    }

    public String getProjectPath() {
        return projectPath != null ? projectPath : System.getProperty("user.dir");
    }

    public String getJaCoCoExecPath() {
        return switch (projectType) {
            case MAVEN -> Paths.get(projectPath, "target", "jacoco.exec").toString();
            case GRADLE -> Paths.get(projectPath, "build", "jacoco", "test.exec").toString();
        };
    }

    public String getClassesPath() {
        return switch (projectType) {
            case MAVEN -> Paths.get(projectPath, "target", "classes").toString();
            case GRADLE -> Paths.get(projectPath, "build", "classes", "java", "main").toString();
        };
    }

    public String getBuildCommand() {
        switch (projectType) {
            case MAVEN:
                return isWindows() ? "mvn.cmd" : "mvn";
            case GRADLE:
                String gradlew = Paths.get(projectPath, isWindows() ? "gradlew.bat" : "gradlew").toString();
                if (new File(gradlew).exists()) {
                    return gradlew;
                }
                return isWindows() ? "gradle.bat" : "gradle";
            default:
                return null;
        }
    }

    public String[] getBuildArgs() {
        return switch (projectType) {
            case MAVEN -> new String[]{"clean", "test-compile"};
            case GRADLE -> new String[]{"clean", "testClasses"};
        };
    }

    public String[] getTestArgs(List<String> tests) {
        switch (projectType) {
            case MAVEN:
                List<String> args = new ArrayList<>();
                args.add("test");
                if (tests != null && !tests.isEmpty()) {
                    String testFilter = String.join(",", tests);
                    args.add("-Dtest=" + testFilter);
                }
                args.add("-Djacoco.destFile=" + getJaCoCoExecPath());
                return args.toArray(new String[0]);
            case GRADLE:
                List<String> gradleArgs = new ArrayList<>();
                gradleArgs.add("test");
                if (tests != null && !tests.isEmpty()) {
                    for (String test : tests) {
                        gradleArgs.add("--tests");
                        gradleArgs.add(test);
                    }
                }
                gradleArgs.add("jacocoTestReport");
                return gradleArgs.toArray(new String[0]);
            default:
                return new String[0];
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}