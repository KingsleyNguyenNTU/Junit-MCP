package com.nmk.junitmcp.service;

import com.nmk.junitmcp.util.ProjectPathResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ExternalProjectService {
    
    public void buildProject(ProjectPathResolver pathResolver) throws IOException, InterruptedException {
        String buildCommand = pathResolver.getBuildCommand();
        String[] buildArgs = pathResolver.getBuildArgs();
        String projectPath = pathResolver.getProjectPath();
        
        List<String> command = new ArrayList<>();
        command.add(buildCommand);
        Collections.addAll(command, buildArgs);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(projectPath));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Build process timed out after 5 minutes");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Build failed with exit code " + exitCode + ". Output: " + output);
        }
    }

}