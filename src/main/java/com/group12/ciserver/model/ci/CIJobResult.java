package com.group12.ciserver.model.ci;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CIJobResult {

    private boolean successful;

    private String logs;
}
