package com.adidev.githubsearch.domain;

/** A client-supplied value is invalid. Carries the parameter name so the API can say which one. */
public class InvalidParameterException extends IllegalArgumentException {

    private final String parameter;

    public InvalidParameterException(String parameter, String message) {
        super(message);
        this.parameter = parameter;
    }

    public String getParameter() { return parameter; }
}