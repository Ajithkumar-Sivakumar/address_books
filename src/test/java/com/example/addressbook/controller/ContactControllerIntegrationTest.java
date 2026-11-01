package com.example.addressbook.controller;

import com.example.addressbook.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactControllerIntegrationTest {

    private static final String VALID_CONTACT = """
            {
              "firstName": "Ada",
              "lastName": "Lovelace",
              "email": "ada@example.com",
              "phoneNumber": "+1 555 0100",
              "address": "12 Analytical Engine Way",
              "city": "London",
              "state": "Greater London",
              "country": "United Kingdom",
              "postalCode": "SW1A 1AA",
              "company": "Engine Works",
              "jobTitle": "Mathematician"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void clearContacts() {
        contactRepository.deleteAll();
    }

    @Test
    void contactCanBeCreatedListedSearchedUpdatedAndDeleted() throws Exception {
        String createdResponse = mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONTACT))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/contacts/")))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number idNumber = com.jayway.jsonpath.JsonPath.read(createdResponse, "$.id");
        long id = idNumber.longValue();

        mockMvc.perform(get("/api/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"));
        mockMvc.perform(get("/api/contacts/search").param("name", "ada love"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/contacts/search").param("email", "EXAMPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/contacts/search").param("phone", "555 0101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/contacts/search").param("phone", "555 0100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        String updatedContact = VALID_CONTACT.replace("Ada", "Augusta");
        mockMvc.perform(put("/api/contacts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedContact))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Augusta"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(delete("/api/contacts/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/contacts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidAndDuplicateContactsReturnUsefulErrors() throws Exception {
        String invalidContact = VALID_CONTACT.replace("ada@example.com", "not-an-email");
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidContact))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());

        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONTACT))
                .andExpect(status().isCreated());

        String duplicateContact = VALID_CONTACT.replace("ada@example.com", "ADA@example.com");
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateContact))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void searchRejectsMultipleFilters() throws Exception {
        mockMvc.perform(get("/api/contacts/search")
                        .param("name", "Ada")
                        .param("email", "ada@example.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contactResponseDoesNotIncludeAlternatePhoneNumber() throws Exception {
        mockMvc.perform(post("/api/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CONTACT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alternatePhoneNumber").doesNotExist());
    }

    @Test
    void frontendAndHealthEndpointAreAvailable() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
