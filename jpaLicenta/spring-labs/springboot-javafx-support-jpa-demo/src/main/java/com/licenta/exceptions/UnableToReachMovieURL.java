package com.licenta.exceptions;

public class UnableToReachMovieURL extends RuntimeException {

    public UnableToReachMovieURL(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }
}
