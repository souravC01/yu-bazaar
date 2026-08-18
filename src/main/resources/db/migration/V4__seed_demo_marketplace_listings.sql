-- Seed Demo York Verified & Public Seller Users
INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Alex Chen', 'alex.chen@my.yorku.ca', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 21, 'Male', '2004-03-12', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'alex.chen@my.yorku.ca');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Priya Sharma', 'priya.sharma@my.yorku.ca', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 20, 'Female', '2005-07-22', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'priya.sharma@my.yorku.ca');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Marcus Vance', 'marcus.v@my.yorku.ca', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 22, 'Male', '2003-11-05', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'marcus.v@my.yorku.ca');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Sam Taylor', 'sam.taylor@my.yorku.ca', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 23, 'Non-binary', '2002-09-18', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'sam.taylor@my.yorku.ca');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Emma Watson', 'emma.w@my.yorku.ca', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 19, 'Female', '2006-01-30', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'emma.w@my.yorku.ca');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'David Kim', 'david.kim@my.yorku.ca', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 21, 'Male', '2004-08-14', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'david.kim@my.yorku.ca');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Claire Bennet', 'claire.b@my.yorku.ca', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 22, 'Female', '2003-04-25', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'claire.b@my.yorku.ca');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Dave Miller', 'dave.homefurnishings@gmail.com', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 28, 'Male', '1997-06-10', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'dave.homefurnishings@gmail.com');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Tech Deals Toronto', 'tech.deals.toronto@outlook.com', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 30, 'Prefer not to say', '1995-12-01', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'tech.deals.toronto@outlook.com');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Karen Mitchell', 'karen.m92@gmail.com', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 31, 'Female', '1994-02-17', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'karen.m92@gmail.com');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Kevin Gadgets', 'kevin.gadgets@gmail.com', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 26, 'Male', '1999-10-08', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'kevin.gadgets@gmail.com');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Steve Resale', 'steve.resale@gmail.com', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 29, 'Male', '1996-05-19', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'steve.resale@gmail.com');

INSERT INTO users (name, email, password, age, gender, dob, otp, is_verified)
SELECT 'Cycle Toronto', 'cycle.toronto@gmail.com', '$2a$12$3DO9/.erECuXq3IBNN33/uvvzAqP6EmgrdMYAJD/q5QdSg1fgelda', 27, 'Other', '1998-08-30', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = 'cycle.toronto@gmail.com');


-- Seed York Verified Student Listings
INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'EECS 2030 Advanced OOP Notes + Midterm Prep', 15.00, 'Like New', 'Bergeron Centre', 'Complete handwritten and typed notes with annotated lecture diagrams. Highly useful for exam revision.', 'alex.chen@my.yorku.ca', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'EECS 2030 Advanced OOP Notes + Midterm Prep');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'TI-84 Plus CE Graphing Calculator (Mint Condition)', 65.00, 'Used (like new)', 'Scott Library', 'Used for MATH 1013/1014. Includes charging cable, slide cover, and recent battery replacement.', 'priya.sharma@my.yorku.ca', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'TI-84 Plus CE Graphing Calculator (Mint Condition)');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Linear Algebra with Applications (9th Edition) - Holt', 35.00, 'Good', 'Vari Hall', 'MATH 1025 course text. Minor highlighting on chapter 3, otherwise pages are crisp.', 'marcus.v@my.yorku.ca', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Linear Algebra with Applications (9th Edition) - Holt');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Dell 27-inch 4K USB-C Hub Monitor (U2720Q)', 240.00, 'Used (like new)', 'The Village (Near Campus)', 'Perfect for dual-screen coding and design work in dorms. Includes power cable and high-speed USB-C cable.', 'sam.taylor@my.yorku.ca', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Dell 27-inch 4K USB-C Hub Monitor (U2720Q)');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Keele Campus Parking Permit Holder & Lanyard', 8.00, 'Brand New', 'York Lanes', 'Unused clear windshield suction permit hanger plus official York Lions lanyard.', 'emma.w@my.yorku.ca', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Keele Campus Parking Permit Holder & Lanyard');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'CHEM 1000/1001 Lab Coat (Size M) + Safety Goggles', 20.00, 'Good', 'Petrie Science Building', 'Standard white 100% cotton lab coat with safety splash goggles. Required for first-year chemistry labs.', 'david.kim@my.yorku.ca', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'CHEM 1000/1001 Lab Coat (Size M) + Safety Goggles');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'York Lions Varsity Crewneck Sweater (Size L, Red)', 30.00, 'Like New', 'Tait McKenzie Centre', 'Official York bookstore varsity sweatshirt. Worn twice, super warm fleece interior.', 'claire.b@my.yorku.ca', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'York Lions Varsity Crewneck Sweater (Size L, Red)');


-- Seed Public Seller Listings
INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'IKEA Ergonomic Desk Chair (Flintan Black)', 45.00, 'Good', 'Pioneer Village Station', 'Comfortable high-back swivel chair with lumbar support. Great for student study desks.', 'dave.homefurnishings@gmail.com', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'IKEA Ergonomic Desk Chair (Flintan Black)');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Sony WH-1000XM4 Wireless Noise Cancelling Headphones', 180.00, 'Used (like new)', 'Finch West Station', 'Industry leading active noise cancellation. Excellent battery life, comes with original hard case.', 'tech.deals.toronto@outlook.com', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Sony WH-1000XM4 Wireless Noise Cancelling Headphones');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Dorm Room Compact Mini Fridge (1.7 Cu. Ft.)', 70.00, 'Good', 'York University Heights', 'Danby compact refrigerator with freezer compartment. Clean, quiet, and energy efficient.', 'karen.m92@gmail.com', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Dorm Room Compact Mini Fridge (1.7 Cu. Ft.)');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Logitech MX Master 3S Wireless Performance Mouse', 60.00, 'Used (like new)', 'Keele & Finch', 'Quiet clicks, 8K DPI sensor, works on any surface. USB-C rechargeable.', 'kevin.gadgets@gmail.com', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Logitech MX Master 3S Wireless Performance Mouse');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Breville Compact Electric Kettle & Tea Maker', 25.00, 'Good', 'Near Quad Residence', 'Fast boiling 1L stainless steel electric kettle with auto shut-off. Perfect for instant noodles and tea.', 'steve.resale@gmail.com', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Breville Compact Electric Kettle & Tea Maker');

INSERT INTO item (title, price, wear, location, description, seller_email, image_path)
SELECT 'Foldable 26-inch Commuter Mountain Bike', 110.00, 'Fair', 'Finch Ave West', 'Dual disc brakes, 21-speed Shimano gears. Folds down easily to fit in car trunk or bus.', 'cycle.toronto@gmail.com', 'default-listing.png'
WHERE NOT EXISTS (SELECT 1 FROM item WHERE title = 'Foldable 26-inch Commuter Mountain Bike');
