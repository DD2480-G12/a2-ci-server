package com.group12.ciserver.service;

import com.group12.ciserver.GithubClient;
import com.group12.ciserver.database.DatabaseWrapper;
import com.group12.ciserver.model.BuildInfo;
import com.group12.ciserver.model.ci.CIJobResult;
import com.group12.ciserver.model.ci.UnexpectedCIJobErrorException;
import com.group12.ciserver.model.github.CommitState;
import com.group12.ciserver.model.github.PushEvent;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class CIService {

    private final GithubClient githubClient;

    private final DatabaseWrapper databaseWrapper;

    @SneakyThrows
    @Async
    public void startCIPipeline(PushEvent pushEvent) {
        log.info("Running CI pipeline...");
        OffsetDateTime pipelineStartTimestamp = OffsetDateTime.now(ZoneOffset.UTC);
        StringBuilder buildLogs = new StringBuilder();
        buildLogs.append("Running CI pipeline...\n");
        githubClient.createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...");
        log.info("Cloning repo...");
        buildLogs.append("Cloning repo...\n");
        File workingDirectory = githubClient.cloneRepoAndSwitchBranch(pushEvent);
        try {
            log.info("Compiling maven project...");
            buildLogs.append("mvn compile\n");
            CIJobResult compileResults = executeCommand(workingDirectory, "mvn", "compile");
            buildLogs.append(compileResults.getLogs());
            if (!compileResults.isSuccessful()) {
                databaseWrapper.addBuild(new BuildInfo(pushEvent.getAfter(), buildLogs.toString(),
                        pipelineStartTimestamp));
                githubClient.createStatusMsg(pushEvent, CommitState.FAILURE, "Compilation of project failed");
                log.info("Compilation of project failed...");
                return;
            }
            log.info("Running maven project tests...");
            buildLogs.append("mvn test\n");
            CIJobResult testResults = executeCommand(workingDirectory, "mvn", "test");
            buildLogs.append(testResults.getLogs());
            if (!testResults.isSuccessful()) {
                databaseWrapper.addBuild(new BuildInfo(pushEvent.getAfter(), buildLogs.toString(),
                        pipelineStartTimestamp));
                githubClient.createStatusMsg(pushEvent, CommitState.FAILURE, "Tests failed");
                log.info("Tests failed...");
                return;
            }
        } catch (UnexpectedCIJobErrorException e) {
            log.error("Unexpected error occurred, errorMessage={}", e.getMessage());
            databaseWrapper.addBuild(new BuildInfo(pushEvent.getAfter(), "Unexpected error occurred on server",
                    pipelineStartTimestamp));
            githubClient.createStatusMsg(pushEvent, CommitState.ERROR, "Unexpected error occurred on server");
            return;
        }
        buildLogs.append("Pipeline successful.");
        databaseWrapper.addBuild(new BuildInfo(pushEvent.getAfter(), buildLogs.toString(),
                pipelineStartTimestamp));
        githubClient.createStatusMsg(pushEvent, CommitState.SUCCESS, "Pipeline successful");
        log.info("Pipeline successful.");
    }

    private CIJobResult executeCommand(File workingDirectory, String... command) throws UnexpectedCIJobErrorException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        processBuilder.directory(workingDirectory);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder compileLogs = new StringBuilder();
            String logLine;
            while ((logLine = reader.readLine()) != null) {
                compileLogs.append(logLine).append("\n");
            }
            int processExitValue = process.waitFor();
            return CIJobResult.builder()
                    .successful(processExitValue == 0)
                    .logs(compileLogs.toString())
                    .build();
        } catch (IOException | InterruptedException e) {
            throw new UnexpectedCIJobErrorException(e.getMessage());
        }
    }
}
