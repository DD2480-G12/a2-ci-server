package com.group12.ciserver;

import com.group12.ciserver.controller.EventController;
import com.group12.ciserver.model.BuildInfo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.containsString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HistoryEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventController controller;

    @Test
    void contextLoads() {
    }

    // History endpoint tests
    @Test
    public void testEmptyHistoryEndpoint() throws Exception {
        ArrayList<BuildInfo> history = new ArrayList<>();

        when(controller.buildHistory()).thenReturn(history);
        // Expect empty json array as history is an empty list
        this.mockMvc.perform(get("/history")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("[]")));
    }

    @Test
    public void testNonEmptyHistoryEndpoint() throws Exception {
        ArrayList<BuildInfo> history = new ArrayList<>();
        BuildInfo b1 = new BuildInfo(1, "commit1", "content1");
        BuildInfo b2 = new BuildInfo(2, "commit2", "content2");

        history.add(b1);
        history.add(b2);

        when(controller.buildHistory()).thenReturn(history);
        this.mockMvc.perform(get("/history")).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uid").value("1"))
                .andExpect(jsonPath("$[1].uid").value("2"))
                .andExpect(jsonPath("$[0].commitId").value("commit1"))
                .andExpect(jsonPath("$[1].commitId").value("commit2"))
                .andExpect(jsonPath("$[0].content").value("content1"))
                .andExpect(jsonPath("$[1].content").value("content2"))
                .andExpect(jsonPath("$[0].timestamp").exists())
                .andExpect(jsonPath("$[1].timestamp").exists());
    }

    // Specific build endpoint tests
    @Test
    public void testBuildEndpoint_NonExistentBuild() throws Exception {

        when(controller.buildInfo("0")).thenReturn(ResponseEntity.notFound().build());

        this.mockMvc.perform(get("/history/0")).andDo(print()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.uid").doesNotExist())
                .andExpect(jsonPath("$.commitId").doesNotExist())
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());

    }

    @Test
    public void testBuildEndpoint_ExistingBuild() throws Exception {
        BuildInfo build = new BuildInfo(0, "commithash", "content", OffsetDateTime.now());

        when(controller.buildInfo("0")).thenReturn(ResponseEntity.ok(build));

        this.mockMvc.perform(get("/history/0")).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.uid").value("0"))
                .andExpect(jsonPath("$.commitId").value("commithash"))
                .andExpect(jsonPath("$.content").value("content"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testBuildEndpoint_InvalidBuild() throws Exception {

        when(controller.buildInfo("badnumber")).thenReturn(ResponseEntity.badRequest().build());

        this.mockMvc.perform(get("/history/badnumber")).andDo(print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uid").doesNotExist())
                .andExpect(jsonPath("$.commitId").doesNotExist())
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());

    }

}
