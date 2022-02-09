package com.group12.ciserver.model.ci;

public class UnexpectedCIJobErrorException extends RuntimeException {

    public UnexpectedCIJobErrorException(String message) {
        super(message);
    }
}
