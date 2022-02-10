package com.group12.ciserver.service;

import com.group12.ciserver.model.ci.CIJobResult;
import com.group12.ciserver.model.ci.UnexpectedCIJobErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
@Slf4j
public class CIJobExecutor {

    /**
     * Runs a maven command on the specified working directory.
     *
     * @param command maven command, e.g. compile, test, etc.
     * @param workingDirectory the directory which the command will be executed on
     * @return {@link CIJobResult} that tells if the maven command was successful or if it failed. It also returns the
     * output log of the process running the command.
     * @throws UnexpectedCIJobErrorException is thrown if the process running the command is interrupted or if there is
     * an IO exception when reading the output logs.
     */
    public CIJobResult runMavenCommand(String command, File workingDirectory) throws UnexpectedCIJobErrorException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("mvn", command);
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
