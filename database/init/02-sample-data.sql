INSERT INTO contacts (
    first_name, last_name, email, phone_number,
    address, city, state, country, postal_code, company, job_title
)
VALUES
    (
        'Ada', 'Lovelace', 'ada.lovelace@example.com', '+44 20 7946 0101',
        '12 Analytical Engine Way', 'London', 'England', 'United Kingdom',
        'SW1A 1AA', 'Engine Works', 'Mathematician'
    ),
    (
        'Alan', 'Turing', 'alan.turing@example.com', '+44 20 7946 0102',
        '1 Enigma Lane', 'Manchester', 'England', 'United Kingdom',
        'M1 1AE', 'Bletchley Research', 'Computer Scientist'
    ),
    (
        'Grace', 'Hopper', 'grace.hopper@example.com', '+1 212 555 0133',
        '42 Compiler Avenue', 'New York', 'NY', 'United States',
        '10001', 'Navy Computing', 'Software Engineer'
    )
ON CONFLICT DO NOTHING;
