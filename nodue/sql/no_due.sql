-- MySQL schema for the NoDue student clearance tracker.
CREATE DATABASE IF NOT EXISTS nodue;
USE nodue;

CREATE TABLE IF NOT EXISTS students (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    roll_no VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    department VARCHAR(80) NOT NULL,
    semester TINYINT UNSIGNED NOT NULL,
    cleared BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(100) NULL,
    password_salt VARCHAR(50) NULL,
    CHECK (semester BETWEEN 1 AND 12)
);

-- Optional sample records. A student must register before they can log in.
INSERT INTO students (roll_no, name, department, semester, cleared) VALUES
    ('CSE001', 'A', 'CSE', 3, FALSE),
    ('ECE002', 'Sample Student', 'ECE', 2, FALSE)
ON DUPLICATE KEY UPDATE roll_no = VALUES(roll_no);

CREATE TABLE IF NOT EXISTS dues (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    department VARCHAR(80) NOT NULL,
    cleared BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dues_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT uq_student_department UNIQUE (student_id, department)
);
