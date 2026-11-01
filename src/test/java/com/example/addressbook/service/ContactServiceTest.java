package com.example.addressbook.service;

import com.example.addressbook.dto.ContactRequest;
import com.example.addressbook.dto.ContactResponse;
import com.example.addressbook.entity.Contact;
import com.example.addressbook.exception.DuplicateEmailException;
import com.example.addressbook.repository.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactService contactService;

    @Test
    void createNormalizesEmailAndTrimsNames() {
        ContactRequest request = request(" Ada ", " Lovelace ", " ADA@example.com ");
        when(contactRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.create(request);

        assertEquals("Ada", response.firstName());
        assertEquals("Lovelace", response.lastName());
        assertEquals("ada@example.com", response.email());
    }

    @Test
    void createRejectsDuplicateEmailWithoutSaving() {
        ContactRequest request = request("Ada", "Lovelace", "ada@example.com");
        when(contactRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> contactService.create(request));
        verify(contactRepository, never()).save(any(Contact.class));
    }

    @Test
    void findByIdReportsMissingContact() {
        when(contactRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                com.example.addressbook.exception.ContactNotFoundException.class,
                () -> contactService.findById(99L)
        );
    }

    private static ContactRequest request(String firstName, String lastName, String email) {
        return new ContactRequest(
                firstName, lastName, email, "+1 555 0100",
                null, null, null, null, null, null, null
        );
    }
}
