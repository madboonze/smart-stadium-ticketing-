package com.smartstadium.service;

public class MatchNotFoundException extends RuntimeException {
    public MatchNotFoundException(String id) {
        super("No match found with ID: " + id);
    }
}
