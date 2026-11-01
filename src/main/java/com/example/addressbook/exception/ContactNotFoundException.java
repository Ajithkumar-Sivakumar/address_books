package com.example.addressbook.exception;

public class ContactNotFoundException extends RuntimeException {

    public ContactNotFoundException(Long id) {
        super("Contact with ID " + id + " was not found");
    }
}
