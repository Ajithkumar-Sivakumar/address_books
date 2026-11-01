package com.example.addressbook.repository;

import com.example.addressbook.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("""
            select contact from Contact contact
            where lower(concat(concat(contact.firstName, ' '), contact.lastName))
                like lower(concat('%', :name, '%'))
            """)
    List<Contact> searchByName(@Param("name") String name);

    List<Contact> findByEmailContainingIgnoreCase(String email);

    List<Contact> findByPhoneNumberContainingIgnoreCase(String phoneNumber);
}
