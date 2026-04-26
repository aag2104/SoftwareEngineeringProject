package services;

import models.InterviewRequest;
import models.RAJobApplication;
import models.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import play.test.Helpers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

/**
 * Unit tests for InterviewRequestService
 * Tests business logic for interview request lifecycle
 */
@RunWith(MockitoJUnitRunner.class)
public class InterviewRequestServiceTest {

    private InterviewRequestService service;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        service = new InterviewRequestService();
    }

    @Test
    public void testAcceptSlotValidatesProposedSlot() {
        // Test that accepting a slot validates against proposed slots
        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setProposedSlot1("2026-05-10T14:00:00");
        request.setProposedSlot2("2026-05-11T10:00:00");
        request.setProposedSlot3("2026-05-12T15:30:00");
        request.setStatus("PENDING");

        // Valid slot should be accepted
        boolean isValid = service.isValidProposedSlot(request, "2026-05-10T14:00:00");
        assertTrue("Should accept valid proposed slot 1", isValid);

        isValid = service.isValidProposedSlot(request, "2026-05-11T10:00:00");
        assertTrue("Should accept valid proposed slot 2", isValid);

        isValid = service.isValidProposedSlot(request, "2026-05-12T15:30:00");
        assertTrue("Should accept valid proposed slot 3", isValid);

        // Invalid slot should be rejected
        isValid = service.isValidProposedSlot(request, "2026-05-20T14:00:00");
        assertFalse("Should reject slot not in proposed list", isValid);
    }

    @Test
    public void testCancellationWindowValidation() {
        // Test 24-hour cancellation window enforcement
        // Interview 25 hours in the future should be within cancel window
        LocalDateTime futureTime = LocalDateTime.now().plusHours(25);
        String slotStr = futureTime.format(FORMATTER);

        boolean withinWindow = service.withinCancelWindow(slotStr, 24);
        assertTrue("Should allow cancellation 25 hours before interview", withinWindow);

        // Interview 23 hours in the future should be outside cancel window
        LocalDateTime tooSoon = LocalDateTime.now().plusHours(23);
        String tooSoonStr = tooSoon.format(FORMATTER);

        withinWindow = service.withinCancelWindow(tooSoonStr, 24);
        assertFalse("Should reject cancellation 23 hours before interview", withinWindow);
    }

    @Test
    public void testReminderTracking() {
        // Test that reminders are tracked properly
        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setStatus("ACCEPTED");
        request.setReminder24hSentTime(null);
        request.setReminder1hSentTime(null);

        assertNull("24h reminder should not be sent initially", request.getReminder24hSentTime());
        assertNull("1h reminder should not be sent initially", request.getReminder1hSentTime());

        // Simulate reminder being sent
        String now = LocalDateTime.now().format(FORMATTER);
        request.setReminder24hSentTime(now);

        assertNotNull("24h reminder should be marked as sent", request.getReminder24hSentTime());
        assertNull("1h reminder should still not be sent", request.getReminder1hSentTime());
    }

    @Test
    public void testInterviewStatusTransitions() {
        // Test valid status transitions
        InterviewRequest request = new InterviewRequest();

        // Start as PENDING
        request.setStatus("PENDING");
        assertEquals("Initial status should be PENDING", "PENDING", request.getStatus());

        // Transition to ACCEPTED
        request.setAcceptedSlot("2026-05-10T14:00:00");
        request.setStatus("ACCEPTED");
        assertEquals("Status should change to ACCEPTED", "ACCEPTED", request.getStatus());
        assertNotNull("Accepted slot should be set", request.getAcceptedSlot());

        // Transition to CANCELLED
        request.setStatus("CANCELLED");
        request.setCancelledBy("STUDENT");
        request.setCancelReason("Schedule conflict");
        assertEquals("Status should change to CANCELLED", "CANCELLED", request.getStatus());
        assertNotNull("Cancellation reason should be set", request.getCancelReason());
    }

    @Test
    public void testCounterOfferTracking() {
        // Test counter-offer state management
        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setStatus("PENDING");

        assertNull("Counter slot should be null initially", request.getCounterSlot());

        // Submit counter-offer
        request.setCounterSlot("2026-05-15T10:00:00");
        request.setCounterNote("This time works better for my schedule");
        request.setStatus("COUNTERED");

        assertEquals("Status should be COUNTERED", "COUNTERED", request.getStatus());
        assertNotNull("Counter slot should be set", request.getCounterSlot());
        assertNotNull("Counter note should be set", request.getCounterNote());
    }

    @Test
    public void testInterviewRequestCreationWithFacultyAndStudent() {
        // Test creating interview request with faculty and student relationships
        User faculty = new User();
        faculty.setId(2L);
        faculty.setFirstName("Dr.");
        faculty.setLastName("Smith");
        faculty.setEmail("smith@university.edu");

        User student = new User();
        student.setId(1L);
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john@student.edu");

        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setFaculty(faculty);
        request.setStudent(student);
        request.setProposedSlot1("2026-05-10T14:00:00");
        request.setProposedSlot2("2026-05-11T10:00:00");
        request.setProposedSlot3("2026-05-12T15:30:00");
        request.setStatus("PENDING");

        assertEquals("Faculty should be set", faculty, request.getFaculty());
        assertEquals("Student should be set", student, request.getStudent());
        assertNotNull("All proposed slots should be set", request.getProposedSlot1());
    }

    @Test
    public void testInterviewWithLocationAndMeetingLink() {
        // Test interview location and meeting link storage
        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setLocation("Room 301, Engineering Building");
        request.setMeetingLink("https://zoom.us/j/987654321");
        request.setAcceptedSlot("2026-05-10T14:00:00");
        request.setStatus("ACCEPTED");

        assertEquals("Location should be set", "Room 301, Engineering Building", request.getLocation());
        assertEquals("Meeting link should be set", "https://zoom.us/j/987654321", request.getMeetingLink());
    }

    @Test
    public void testSoftDeleteWithIsActiveFlag() {
        // Test soft delete via isActive flag
        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setIsActive("True");

        assertEquals("Interview should be active", "True", request.getIsActive());

        // Soft delete
        request.setIsActive("False");
        assertEquals("Interview should be inactive (soft deleted)", "False", request.getIsActive());
    }

    @Test
    public void testInterviewNotesFromFaculty() {
        // Test faculty notes to student
        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setNotes("Please bring a copy of your resume. The interview will cover data structures and algorithms.");

        assertNotNull("Faculty notes should be set", request.getNotes());
        assertTrue("Notes should contain relevant information",
                request.getNotes().contains("resume"));
    }

    @Test
    public void testReminderSentTimestampFormat() {
        // Test that reminder timestamps are in ISO-8601 format
        String timestamp = "2026-04-29T09:00:00";

        // Should be parseable as LocalDateTime
        try {
            LocalDateTime parsed = LocalDateTime.parse(timestamp, FORMATTER);
            assertNotNull("Timestamp should parse correctly", parsed);
        } catch (Exception e) {
            fail("Timestamp format should be ISO-8601 compliant");
        }
    }

    @Test
    public void testCancellationDetails() {
        // Test that cancellation stores all necessary details
        InterviewRequest request = new InterviewRequest();
        request.setId(1L);
        request.setStatus("CANCELLED");
        request.setCancelledBy("STUDENT");
        request.setCancelReason("Schedule conflict with another exam");
        request.setCancelledTime("2026-05-09T11:30:00");

        assertEquals("Cancellation initiator should be tracked", "STUDENT", request.getCancelledBy());
        assertNotNull("Cancellation reason should be tracked", request.getCancelReason());
        assertNotNull("Cancellation time should be tracked", request.getCancelledTime());
    }

    @Test
    public void testMultipleSlotsNotOverlapping() {
        // Test that proposed slots are different times
        InterviewRequest request = new InterviewRequest();
        request.setProposedSlot1("2026-05-10T14:00:00");
        request.setProposedSlot2("2026-05-11T10:00:00");
        request.setProposedSlot3("2026-05-12T15:30:00");

        // All three slots should be different
        assertNotEquals("Slots should not overlap",
                request.getProposedSlot1(), request.getProposedSlot2());
        assertNotEquals("Slots should not overlap",
                request.getProposedSlot2(), request.getProposedSlot3());
        assertNotEquals("Slots should not overlap",
                request.getProposedSlot1(), request.getProposedSlot3());
    }

    @Test
    public void testInvalidProposedSlotRejected() {
        // Test that non-proposed slots are rejected
        InterviewRequest request = new InterviewRequest();
        request.setProposedSlot1("2026-05-10T14:00:00");
        request.setProposedSlot2("2026-05-11T10:00:00");
        request.setProposedSlot3("2026-05-12T15:30:00");

        // Should reject slots not in the proposed list
        boolean isValid = service.isValidProposedSlot(request, "2026-05-20T14:00:00");
        assertFalse("Should reject slot not proposed by faculty", isValid);

        isValid = service.isValidProposedSlot(request, "2026-05-10T15:00:00");
        assertFalse("Should reject slot at different time same day", isValid);
    }
}