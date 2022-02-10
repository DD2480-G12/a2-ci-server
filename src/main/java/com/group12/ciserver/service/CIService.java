package com.group12.ciserver.service;

import com.group12.ciserver.GithubClient;
import com.group12.ciserver.database.DatabaseWrapper;
import com.group12.ciserver.model.BuildInfo;
import com.group12.ciserver.model.ci.CIJobResult;
import com.group12.ciserver.model.ci.UnexpectedCIJobErrorException;
import com.group12.ciserver.model.github.CommitState;
import com.group12.ciserver.model.github.PushEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
@RequiredArgsConstructor
public class CIService {

    private final CIJobExecutor ciJobExecutor;

    private final GithubClient githubClient;

    private final DatabaseWrapper databaseWrapper;

    /**
     * Starts a CI pipeline containing two stages, compile and test.
     * <p>
     * <b>Note:</b> This pipeline only supports maven projects.
     * <p>
     * The commit status of the commit that triggered the pipeline is updated accordingly.
     * <ul>
     *     <li>PENDING - while the pipeline is running</li>
     *     <li>SUCCESS - if both compile and test stage passes</li>
     *     <li>FAILURE - if one of the stages fails</li>
     *     <li>ERROR - if an unexpected error occurs during one of the stages </li>
     * </ul>
     *
     * @param pushEvent is the <i>push</i> event received from GitHub's webhook.
     */
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
            CIJobResult compileResults = ciJobExecutor.runMavenCommand("compile", workingDirectory);
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
            CIJobResult testResults = ciJobExecutor.runMavenCommand("test", workingDirectory);
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
}
