package com.example.addressbook.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("A contact with email address " + email + " already exists");
    }
}
