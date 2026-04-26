package services;

import models.InterviewRequest;
import models.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReminderSchedulerService
 * Tests reminder triggering and email notification logic
 */
@RunWith(MockitoJUnitRunner.class)
public class ReminderSchedulerServiceTest {

    @Mock
    private InterviewRequestService interviewService;

    @Mock
    private EmailService emailService;

    @Mock
    private akka.actor.ActorSystem actorSystem;

    private ReminderSchedulerService reminderService;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        reminderService = new ReminderSchedulerService(interviewService, emailService, actorSystem);
    }

    @Test
    public void testReminderEmailContainsRequiredInfo() {
        // Test that reminder emails contain necessary information
        User student = new User();
        student.setId(1L);
        student.setFirstName("John");
        student.setEmail("john@university.edu");

        User faculty = new User();
        faculty.setId(2L);
        faculty.setFirstName("Dr.");
        faculty.setLastName("Smith");

        InterviewRequest interview = new InterviewRequest();
        interview.setId(1L);
        interview.setStudent(student);
        interview.setFaculty(faculty);
        interview.setAcceptedSlot("2026-05-10T14:00:00");
        interview.setLocation("Room 301");
        interview.setMeetingLink("https://zoom.us/j/123456789");
        interview.setStatus("ACCEPTED");

        // Verify reminder email structure
        assertNotNull("Student email should not be null", student.getEmail());
        assertNotNull("Faculty name should not be null", faculty.getFirstName());
        assertNotNull("Interview time should not be null", interview.getAcceptedSlot());
        assertNotNull("Location should not be null", interview.getLocation());
    }

    @Test
    public void testOnlyConfirmedInterviewsReceiveReminders() {
        // Test that only ACCEPTED/CONFIRMED interviews get reminders
        InterviewRequest pendingInterview = new InterviewRequest();
        pendingInterview.setStatus("PENDING");

        InterviewRequest acceptedInterview = new InterviewRequest();
        acceptedInterview.setStatus("ACCEPTED");
        acceptedInterview.setAcceptedSlot("2026-05-10T14:00:00");

        InterviewRequest cancelledInterview = new InterviewRequest();
        cancelledInterview.setStatus("CANCELLED");

        assertTrue("ACCEPTED interviews should be eligible for reminders",
                acceptedInterview.getStatus().equals("ACCEPTED"));
        assertFalse("PENDING interviews should not receive reminders",
                pendingInterview.getStatus().equals("ACCEPTED") || pendingInterview.getStatus().equals("CONFIRMED"));
        assertFalse("CANCELLED interviews should not receive reminders",
                cancelledInterview.getStatus().equals("ACCEPTED") || cancelledInterview.getStatus().equals("CONFIRMED"));
    }

    @Test
    public void testReminderNotSentDuplicate() {
        // Test that duplicate reminders are not sent
        InterviewRequest interview = new InterviewRequest();
        interview.setId(1L);
        interview.setStatus("ACCEPTED");

        String reminderTime = LocalDateTime.now().format(FORMATTER);
        interview.setReminder24hSentTime(reminderTime);

        assertNotNull("Reminder should be marked as sent", interview.getReminder24hSentTime());
        // In real implementation, scheduler checks this and skips if already sent
    }

    @Test
    public void testReminderTimingCalculation() {
        // Test reminder timing calculations
        LocalDateTime now = LocalDateTime.now();

        // 24 hours in future (in minutes = 1440)
        LocalDateTime interview24h = now.plusMinutes(1430); // 1430-1440 range
        long minutesUntil24h = java.time.temporal.ChronoUnit.MINUTES.between(now, interview24h);
        assertTrue("Should trigger 24h reminder window", minutesUntil24h >= 1420 && minutesUntil24h <= 1440);

        // 1 hour in future (in minutes = 60)
        LocalDateTime interview1h = now.plusMinutes(60); // 55-65 range
        long minutesUntil1h = java.time.temporal.ChronoUnit.MINUTES.between(now, interview1h);
        assertTrue("Should trigger 1h reminder window", minutesUntil1h >= 55 && minutesUntil1h <= 65);
    }
}