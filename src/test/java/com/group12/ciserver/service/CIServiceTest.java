package com.group12.ciserver.service;

import com.group12.ciserver.client.GithubClient;
import com.group12.ciserver.database.DatabaseWrapper;
import com.group12.ciserver.model.BuildInfo;
import com.group12.ciserver.model.ci.CIJobResult;
import com.group12.ciserver.model.ci.UnexpectedCIJobErrorException;
import com.group12.ciserver.model.github.CommitState;
import com.group12.ciserver.model.github.Owner;
import com.group12.ciserver.model.github.PushEvent;
import com.group12.ciserver.model.github.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CIServiceTest {

    @Mock
    private CIJobExecutor ciJobExecutor;

    @Mock
    private GithubClient githubClient;

    @Mock
    private DatabaseWrapper databaseWrapper;

    private CIService ciService;

    private PushEvent pushEvent;

    private File workingDir;

    @BeforeEach
    public void init() {
        this.ciService = new CIService(ciJobExecutor, githubClient, databaseWrapper);

        Owner owner = new Owner();
        owner.setName("Owner");

        Repository repository = new Repository();
        repository.setName("Repository");
        repository.setCloneUrl("http://clone.url.repo/branch");
        repository.setOwner(owner);

        pushEvent = new PushEvent();
        pushEvent.setRef("refs/heads/branch");
        pushEvent.setAfter("aabbccddee");
        pushEvent.setRepository(repository);

        workingDir = new File("/path/to/workingDir");
    }

    /**
     * Given:
     * <p>
     * Both "compile" and "test" stages passes.
     * <p>
     * Then:
     * <p>
     * - Commit status goes from PENDING to SUCCESS and no other status is set
     * - Correct build logs and commit sha, and a timestamp is saved in the database.
     */
    @Test
    public void givenNoFailsNorErrorsDuringCompileAndTest_whenStartCIPipeline_thenCommitStatusSuccessAndBuildLogsSaved() throws Exception {
        String compileJobLogs = "compile job logs";
        CIJobResult compileJobResult = CIJobResult.builder()
                .successful(true)
                .logs(compileJobLogs)
                .build();
        when(ciJobExecutor.runMavenCommand("compile", workingDir)).thenReturn(compileJobResult);

        String testJobLogs = "test job logs";
        CIJobResult testJobResults = CIJobResult.builder()
                .successful(true)
                .logs(testJobLogs)
                .build();
        when(ciJobExecutor.runMavenCommand("test", workingDir)).thenReturn(testJobResults);

        when(githubClient.cloneRepoAndSwitchBranch(pushEvent)).thenReturn(workingDir);
        when(githubClient.createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null))
                .thenReturn(ResponseEntity.ok().build());
        when(githubClient.createStatusMsg(pushEvent, CommitState.SUCCESS, "Pipeline successful", 1L))
                .thenReturn(ResponseEntity.ok().build());

        when(databaseWrapper.addBuild(any(BuildInfo.class))).thenReturn(1L);

        String expectedBuildLogs = "Running CI pipeline...\n"
                + "Cloning repo...\n"
                + "mvn compile\n"
                + compileJobLogs
                + "mvn test\n"
                + testJobLogs
                + "Pipeline successful.";

        ciService.startCIPipeline(pushEvent);

        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null);
        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.SUCCESS, "Pipeline successful", 1L);
        verifyNoMoreInteractions(githubClient);

        ArgumentCaptor<BuildInfo> buildInfoCaptor = ArgumentCaptor.forClass(BuildInfo.class);
        verify(databaseWrapper).addBuild(buildInfoCaptor.capture());

        assertThat(buildInfoCaptor.getValue().getContent()).isEqualTo(expectedBuildLogs);
        assertThat(buildInfoCaptor.getValue().getCommitId()).isEqualTo(pushEvent.getAfter());
        assertThat(buildInfoCaptor.getValue().getTimestamp()).isNotNull();
    }

    /**
     * Given:
     * <p>
     * "compile" stage fails.
     * <p>
     * Then:
     * <p>
     * - "test" stage is not executed.
     * - Commit status goes from PENDING to FAILURE and no other status is set
     * - Correct build logs and commit sha, and a timestamp is saved in the database.
     */
    @Test
    public void givenCompileStageFails_whenStartCIPipeline_thenCommitStatusIsFailureAndBuildLogsSaved() throws Exception {
        String compileJobLogs = "compile job logs";
        CIJobResult compileJobResult = CIJobResult.builder()
                .successful(false)
                .logs(compileJobLogs)
                .build();
        when(ciJobExecutor.runMavenCommand("compile", workingDir)).thenReturn(compileJobResult);

        when(githubClient.cloneRepoAndSwitchBranch(pushEvent)).thenReturn(workingDir);
        when(githubClient.createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null))
                .thenReturn(ResponseEntity.ok().build());
        when(githubClient.createStatusMsg(pushEvent, CommitState.FAILURE, "Compilation of project failed", 1L))
                .thenReturn(ResponseEntity.ok().build());

        when(databaseWrapper.addBuild(any(BuildInfo.class))).thenReturn(1L);

        String expectedBuildLogs = "Running CI pipeline...\n"
                + "Cloning repo...\n"
                + "mvn compile\n"
                + compileJobLogs;

        ciService.startCIPipeline(pushEvent);

        verify(ciJobExecutor, times(0)).runMavenCommand("test", workingDir);

        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null);
        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.FAILURE, "Compilation of project failed", 1L);
        verifyNoMoreInteractions(githubClient);

        ArgumentCaptor<BuildInfo> buildInfoCaptor = ArgumentCaptor.forClass(BuildInfo.class);
        verify(databaseWrapper).addBuild(buildInfoCaptor.capture());

        assertThat(buildInfoCaptor.getValue().getContent()).isEqualTo(expectedBuildLogs);
        assertThat(buildInfoCaptor.getValue().getCommitId()).isEqualTo(pushEvent.getAfter());
        assertThat(buildInfoCaptor.getValue().getTimestamp()).isNotNull();
    }

    /**
     * Given:
     * <p>
     * "compile" stage passes but "test" stage fails.
     * <p>
     * Then:
     * <p>
     * - Commit status goes from PENDING to FAILURE and no other status is set
     * - Correct build logs and commit sha, and a timestamp is saved in the database.
     */
    @Test
    public void givenTestSageFails_whenStartCIPipeline_thenCommitStatusIsFailureAndBuildLogsSaved() throws Exception {
        String compileJobLogs = "compile job logs";
        CIJobResult compileJobResult = CIJobResult.builder()
                .successful(true)
                .logs(compileJobLogs)
                .build();
        when(ciJobExecutor.runMavenCommand("compile", workingDir)).thenReturn(compileJobResult);

        String testJobLogs = "test job logs";
        CIJobResult testJobResults = CIJobResult.builder()
                .successful(false)
                .logs(testJobLogs)
                .build();
        when(ciJobExecutor.runMavenCommand("test", workingDir)).thenReturn(testJobResults);

        when(githubClient.cloneRepoAndSwitchBranch(pushEvent)).thenReturn(workingDir);
        when(githubClient.createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null))
                .thenReturn(ResponseEntity.ok().build());
        when(githubClient.createStatusMsg(pushEvent, CommitState.FAILURE, "Tests failed", 1L))
                .thenReturn(ResponseEntity.ok().build());

        when(databaseWrapper.addBuild(any(BuildInfo.class))).thenReturn(1L);

        String expectedBuildLogs = "Running CI pipeline...\n"
                + "Cloning repo...\n"
                + "mvn compile\n"
                + compileJobLogs
                + "mvn test\n"
                + testJobLogs;

        ciService.startCIPipeline(pushEvent);

        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null);
        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.FAILURE, "Tests failed", 1L);
        verifyNoMoreInteractions(githubClient);

        ArgumentCaptor<BuildInfo> buildInfoCaptor = ArgumentCaptor.forClass(BuildInfo.class);
        verify(databaseWrapper).addBuild(buildInfoCaptor.capture());

        assertThat(buildInfoCaptor.getValue().getContent()).isEqualTo(expectedBuildLogs);
        assertThat(buildInfoCaptor.getValue().getCommitId()).isEqualTo(pushEvent.getAfter());
        assertThat(buildInfoCaptor.getValue().getTimestamp()).isNotNull();
    }

    /**
     * Given:
     * <p>
     * Unexpected error in "compile" stage.
     * <p>
     * Then:
     * <p>
     * - "test" stage is not executed.
     * - Commit status goes from PENDING to ERROR and no other status is set
     * - Unexpected error message is saved in the database.
     */
    @Test
    public void givenErrorInCompileStage_whenStartCIPipeline_thenCommitStatusIsErrorAndBuildLogsSaved() throws Exception {
        when(ciJobExecutor.runMavenCommand("compile", workingDir))
                .thenThrow(UnexpectedCIJobErrorException.class);

        when(githubClient.cloneRepoAndSwitchBranch(pushEvent)).thenReturn(workingDir);
        when(githubClient.createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null))
                .thenReturn(ResponseEntity.ok().build());
        when(githubClient.createStatusMsg(pushEvent, CommitState.ERROR, "Unexpected error occurred on server", 1L))
                .thenReturn(ResponseEntity.ok().build());

        when(databaseWrapper.addBuild(any(BuildInfo.class))).thenReturn(1L);

        String expectedBuildLogs = "Unexpected error occurred on server";

        ciService.startCIPipeline(pushEvent);

        verify(ciJobExecutor, times(0)).runMavenCommand("test", workingDir);

        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null);
        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.ERROR, "Unexpected error occurred on server", 1L);
        verifyNoMoreInteractions(githubClient);

        ArgumentCaptor<BuildInfo> buildInfoCaptor = ArgumentCaptor.forClass(BuildInfo.class);
        verify(databaseWrapper).addBuild(buildInfoCaptor.capture());

        assertThat(buildInfoCaptor.getValue().getContent()).isEqualTo(expectedBuildLogs);
        assertThat(buildInfoCaptor.getValue().getCommitId()).isEqualTo(pushEvent.getAfter());
        assertThat(buildInfoCaptor.getValue().getTimestamp()).isNotNull();
    }

    /**
     * Given:
     * <p>
     * "compile" stage passes but "test" stage throws an unexpected error.
     * <p>
     * Then:
     * <p>
     * - Commit status goes from PENDING to ERROR and no other status is set
     * - Unexpected error message is saved in the database.
     */
    @Test
    public void givenErrorInTestStage_whenStartCIPipeline_thenCommitStatusIsErrorAndBuildLogsSaved() throws Exception {
        String compileJobLogs = "compile job logs";
        CIJobResult compileJobResult = CIJobResult.builder()
                .successful(true)
                .logs(compileJobLogs)
                .build();
        when(ciJobExecutor.runMavenCommand("compile", workingDir)).thenReturn(compileJobResult);

        when(ciJobExecutor.runMavenCommand("test", workingDir))
                .thenThrow(UnexpectedCIJobErrorException.class);

        when(githubClient.cloneRepoAndSwitchBranch(pushEvent)).thenReturn(workingDir);
        when(githubClient.createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null))
                .thenReturn(ResponseEntity.ok().build());
        when(githubClient.createStatusMsg(pushEvent, CommitState.ERROR, "Unexpected error occurred on server", 1L))
                .thenReturn(ResponseEntity.ok().build());

        when(databaseWrapper.addBuild(any(BuildInfo.class))).thenReturn(1L);

        String expectedBuildLogs = "Unexpected error occurred on server";

        ciService.startCIPipeline(pushEvent);

        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.PENDING, "Running CI pipeline...", null);
        verify(githubClient, times(1))
                .createStatusMsg(pushEvent, CommitState.ERROR, "Unexpected error occurred on server", 1L);
        verifyNoMoreInteractions(githubClient);

        ArgumentCaptor<BuildInfo> buildInfoCaptor = ArgumentCaptor.forClass(BuildInfo.class);
        verify(databaseWrapper).addBuild(buildInfoCaptor.capture());

        assertThat(buildInfoCaptor.getValue().getContent()).isEqualTo(expectedBuildLogs);
        assertThat(buildInfoCaptor.getValue().getCommitId()).isEqualTo(pushEvent.getAfter());
        assertThat(buildInfoCaptor.getValue().getTimestamp()).isNotNull();
    }
}
