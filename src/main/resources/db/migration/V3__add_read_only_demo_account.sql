-- Reserve a negative ID so this system account never consumes or collides with
-- the identity sequence used by registered users.
INSERT INTO users (id, name, email, password, age, gender, dob, otp, is_verified)
SELECT
    -1,
    'YU Bazaar Demo',
    'demo@yubazaar.app',
    '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda',
    21,
    'Prefer not to say',
    '2005-01-01',
    NULL,
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE LOWER(email) = 'demo@yubazaar.app'
)
AND NOT EXISTS (
    SELECT 1 FROM users WHERE id = -1
);
