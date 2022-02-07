package com.group12.ciserver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.JsonNode;
import com.group12.ciserver.model.github.CommitState;
import com.group12.ciserver.model.github.PushEvent;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileReader;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Class for communicating with GitHub
 */
@Component
public class GithubClient {

    @Value("${githubclient.repo-clone-base-directory}")
    private String repoCloneBaseDirectory;
    @Value("${githubclient.app-installation-id}")
    private String appInstallationId;
    @Value("${githubclient.pem-location}")
    private String pemLocation;
    @Value("${githubclient.app-identifier}")
    private String appIdentifier;

    private String token = null;
    private Date tokenCreated = new Date((new Date()).getTime() - 120*60000);

    /**
     * Constructor for the GitHub client.
     */
    public GithubClient() {
        java.security.Security.addProvider(
            new org.bouncycastle.jce.provider.BouncyCastleProvider()
        );
    }

    /**
     * Clones a repository and checks out to the specified branch.
     *
     * @param pushEvent The {@link PushEvent} that contains information about the push event.
     * @return The {@link File} that represents the directory the repository was cloned to.
     * @throws GitAPIException is thrown if the clone fails.
     */
    public File cloneRepoAndSwitchBranch(PushEvent pushEvent) throws GitAPIException {
        File cloneDirectory = getCloneDirectory();

        Git git = Git.cloneRepository()
            .setURI(pushEvent.getRepository().getCloneUrl())
            .setDirectory(cloneDirectory)
            .setBranchesToClone(Arrays.asList(pushEvent.getRef()))
            .setBranch(pushEvent.getRef())
            .call();

        return cloneDirectory;
    }

    /**
     * Sets a status message for the last commit in a specified push event. If the response from GitHub
     * indicates an error, an exception will be thrown.
     *
     * @param pushEvent The {@link PushEvent} that contains information about the push event.
     * @param commitState The {@link CommitState} that is to be set.
     * @param message A message that can be set in addition to the status. Can be an empty string.
     * @return A {@link ResponseEntity} that represents the response received from GitHub.
     * @throws Exception is thrown if the action fails, this can be for several reasons (invalid keys, internet connection,
     *          parameters, etc).
     */
    public ResponseEntity<JsonNode> createStatusMsg(PushEvent pushEvent, CommitState commitState, String message) throws Exception {
        if ((new Date()).getTime() - tokenCreated.getTime() > 30*60000) {
            createInstallationToken();
        }

        String url = "https://api.github.com/repos/"
                + pushEvent.getRepository().getOwner().getName() + "/"
                + pushEvent.getRepository().getName() + "/statuses/"
                + pushEvent.getAfter();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);

        Map<String, Object> map = new HashMap<>();
        map.put("state", commitState.name().toLowerCase());
        map.put("description", message);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForEntity(url, entity, JsonNode.class);
    }

    private String getJwtToken() throws Exception {
        RSAPrivateKey privateRSA = null;
        String token = null;
        privateRSA = readPrivateKey(new File(pemLocation));

        Algorithm algorithm = Algorithm.RSA256(null, privateRSA);
        token = JWT.create()
                .withIssuer(appIdentifier)
                .withExpiresAt(new Date((new Date()).getTime() + 5*60000))
                .withIssuedAt(new Date((new Date()).getTime() - 1*60000))
                .sign(algorithm);

        return token;
    }

    // Taken from the web: https://www.baeldung.com/java-read-pem-file-keys
    private RSAPrivateKey readPrivateKey(File file) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA");

        try (FileReader keyReader = new FileReader(file);
             PemReader pemReader = new PemReader(keyReader)) {

            PemObject pemObject = pemReader.readPemObject();
            byte[] content = pemObject.getContent();
            PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
            return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
        }
    }

    private void createInstallationToken() throws Exception {
        String url = "https://api.github.com/app/installations/" + appInstallationId + "/access_tokens";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String jwtToken = getJwtToken();
        headers.setBearerAuth(jwtToken);

        Map<String, Object> map = new HashMap<>();
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = null;

        response = restTemplate.postForEntity(url, entity, JsonNode.class);
        token = response.getBody().findValue("token").toString();

        if (token != null) {
            token = token.replaceAll("\"", "");
        } else {
            throw new Exception("Could not fetch token: " + response.getBody());
        }
    }

    private File getCloneDirectory() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        LocalDateTime now = LocalDateTime.now();
        String name = dtf.format(now);
        return new File(repoCloneBaseDirectory, name);
    }
}
