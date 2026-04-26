-- Test data for interview requests
-- This script creates prerequisite data and interview requests for testing
-- Run this manually in your database

-- 1. Verify User 1 (student) exists - you can check via: SELECT * FROM user WHERE id = 1;

-- 2. Create a faculty user (User ID 2) 
INSERT INTO user (id, email, user_name, password, user_type, is_active, create_time) 
VALUES (2, 'faculty@example.com', 'Dr. Smith', 'hashedpassword', 1, 'True', NOW())
ON DUPLICATE KEY UPDATE user_name='Dr. Smith';

-- 3. Create an RA job (RA Job ID 1)
INSERT INTO rajob (id, title, short_description, rajob_publisher_id, create_time, is_active, status) 
VALUES (1, 'Research Assistant - AI Project', 'Help with AI research project', 2, NOW(), 'True', 'OPEN')
ON DUPLICATE KEY UPDATE title='Research Assistant - AI Project';

-- 4. Create an RA job application (Student 1 applied to RA Job 1)
INSERT INTO rajob_application (id, rajob_id, applicant_id, status, create_time, is_active) 
VALUES (1, 1, 1, 'PENDING', NOW(), 'True')
ON DUPLICATE KEY UPDATE status='PENDING';

-- 5. Create interview requests
-- Interview Request 1: PENDING status
INSERT INTO interview_request (id, ra_job_application_id, faculty_id, student_id, status, 
  proposed_slot1, proposed_slot2, proposed_slot3, create_time, update_time, is_active, cancel_window_hours)
VALUES (1, 1, 2, 1, 'PENDING',
  '2026-05-10T14:00:00', '2026-05-11T10:00:00', '2026-05-12T15:30:00',
  NOW(), NOW(), 'True', 24)
ON DUPLICATE KEY UPDATE status='PENDING';

-- Interview Request 2: ACCEPTED status  
INSERT INTO interview_request (id, ra_job_application_id, faculty_id, student_id, status,
  proposed_slot1, proposed_slot2, proposed_slot3, accepted_slot, location, meeting_link, create_time, update_time, is_active, cancel_window_hours)
VALUES (2, 1, 2, 1, 'ACCEPTED',
  '2026-04-30T09:00:00', '2026-05-01T13:00:00', '2026-05-02T16:00:00',
  '2026-04-30T09:00:00', 'Zoom', 'https://zoom.us/j/123456789', 
  NOW(), NOW(), 'True', 24)
ON DUPLICATE KEY UPDATE status='ACCEPTED';
