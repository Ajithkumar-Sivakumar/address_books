package com.example.addressbook.service;

import com.example.addressbook.dto.ContactRequest;
import com.example.addressbook.dto.ContactResponse;
import com.example.addressbook.entity.Contact;
import com.example.addressbook.exception.ContactNotFoundException;
import com.example.addressbook.exception.DuplicateEmailException;
import com.example.addressbook.repository.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContactService {

    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findAll() {
        return contactRepository.findAll()
                .stream()
                .map(ContactService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContactResponse findById(Long id) {
        return toResponse(findContact(id));
    }

    public ContactResponse create(ContactRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailIsAvailable(email, null);

        Contact contact = new Contact();
        applyRequest(contact, request, email);
        Contact savedContact = contactRepository.save(contact);
        logger.info("Created contact with ID {}", savedContact.getId());
        return toResponse(savedContact);
    }

    public ContactResponse update(Long id, ContactRequest request) {
        Contact contact = findContact(id);
        String email = normalizeEmail(request.email());
        ensureEmailIsAvailable(email, id);

        applyRequest(contact, request, email);
        Contact savedContact = contactRepository.saveAndFlush(contact);
        logger.info("Updated contact with ID {}", savedContact.getId());
        return toResponse(savedContact);
    }

    public void delete(Long id) {
        Contact contact = findContact(id);
        contactRepository.delete(contact);
        logger.info("Deleted contact with ID {}", id);
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> search(String name, String email, String phone) {
        int suppliedFilters = countNonBlank(name, email, phone);
        if (suppliedFilters > 1) {
            throw new IllegalArgumentException("Use only one search filter: name, email, or phone");
        }

        List<Contact> contacts;
        if (isNotBlank(name)) {
            contacts = contactRepository.searchByName(name.trim());
        } else if (isNotBlank(email)) {
            contacts = contactRepository.findByEmailContainingIgnoreCase(email.trim());
        } else if (isNotBlank(phone)) {
            contacts = contactRepository.findByPhoneNumberContainingIgnoreCase(phone.trim());
        } else {
            contacts = contactRepository.findAll();
        }

        return contacts.stream().map(ContactService::toResponse).toList();
    }

    private Contact findContact(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
    }

    private void ensureEmailIsAvailable(String email, Long currentContactId) {
        boolean alreadyUsed = currentContactId == null
                ? contactRepository.existsByEmailIgnoreCase(email)
                : contactRepository.existsByEmailIgnoreCaseAndIdNot(email, currentContactId);
        if (alreadyUsed) {
            throw new DuplicateEmailException(email);
        }
    }

    private static void applyRequest(Contact contact, ContactRequest request, String normalizedEmail) {
        contact.setFirstName(request.firstName().trim());
        contact.setLastName(request.lastName().trim());
        contact.setEmail(normalizedEmail);
        contact.setPhoneNumber(request.phoneNumber().trim());
        contact.setAddress(trimToNull(request.address()));
        contact.setCity(trimToNull(request.city()));
        contact.setState(trimToNull(request.state()));
        contact.setCountry(trimToNull(request.country()));
        contact.setPostalCode(trimToNull(request.postalCode()));
        contact.setCompany(trimToNull(request.company()));
        contact.setJobTitle(trimToNull(request.jobTitle()));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static int countNonBlank(String... values) {
        int count = 0;
        for (String value : values) {
            if (isNotBlank(value)) {
                count++;
            }
        }
        return count;
    }

    private static ContactResponse toResponse(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhoneNumber(),
                contact.getAddress(),
                contact.getCity(),
                contact.getState(),
                contact.getCountry(),
                contact.getPostalCode(),
                contact.getCompany(),
                contact.getJobTitle(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}
