package com.group12.ciserver.model.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Repository {

    private String name;

    @JsonProperty("clone_url")
    private String cloneUrl;

    private Owner owner;
}
