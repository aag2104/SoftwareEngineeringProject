package services;

import akka.actor.ActorSystem;
import models.InterviewRequest;
import models.User;
import play.Logger;
import scala.concurrent.duration.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * F2 - Schedule RA Job Interview (Reminders)
 * Scheduled job that checks for upcoming interviews and sends reminders.
 *
 * Reminders are sent:
 * - 24 hours before confirmed interview time
 * - 1 hour before confirmed interview time
 *
 * Only interviews with status ACCEPTED or CONFIRMED receive reminders.
 */
@Singleton
public class ReminderSchedulerService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final InterviewRequestService interviewService;
    private final EmailService emailService;
    private final ActorSystem actorSystem;

    @Inject
    public ReminderSchedulerService(
            InterviewRequestService interviewService,
            EmailService emailService,
            ActorSystem actorSystem) {
        this.interviewService = interviewService;
        this.emailService = emailService;
        this.actorSystem = actorSystem;

        // Start the scheduler job
        startReminderScheduler();
    }

    /**
     * Start the reminder scheduler to run every 5 minutes
     */
    private void startReminderScheduler() {
        Logger.info("Starting Interview Reminder Scheduler");

        actorSystem.scheduler().schedule(
                Duration.create(0, TimeUnit.SECONDS),
                Duration.create(1, TimeUnit.HOURS),
                () -> checkAndSendReminders(),
                actorSystem.dispatcher()
        );
    }

    /**
     * Check all confirmed interviews and send reminders as needed
     */
    public void checkAndSendReminders() {
        try {
            Logger.info("Checking for interviews needing reminders...");

            // Get all active interviews with accepted/confirmed slots
            List<InterviewRequest> interviews = InterviewRequest.find.query()
                    .where()
                    .in("status", "ACCEPTED", "CONFIRMED")
                    .eq("isActive", "True")
                    .findList();

            LocalDateTime now = LocalDateTime.now();

            for (InterviewRequest interview : interviews) {
                // Determine which slot to check (acceptedSlot is the confirmed time)
                String confirmedSlot = interview.getAcceptedSlot();
                if (confirmedSlot == null || confirmedSlot.isEmpty()) {
                    continue; // Skip if no confirmed slot
                }

                try {
                    LocalDateTime slotTime = LocalDateTime.parse(confirmedSlot, FORMATTER);
                    long minutesUntil = ChronoUnit.MINUTES.between(now, slotTime);

                    // Check for 24-hour reminder (between 1425 and 1440 minutes before)
                    if (minutesUntil >= 1425 && minutesUntil <= 1440) {
                        if (interview.getReminder24hSentTime() == null ||
                                interview.getReminder24hSentTime().isEmpty()) {
                            send24hReminder(interview);
                        }
                    }

                    // Check for 1-hour reminder (between 55 and 65 minutes before)
                    if (minutesUntil >= 55 && minutesUntil <= 65) {
                        if (interview.getReminder1hSentTime() == null ||
                                interview.getReminder1hSentTime().isEmpty()) {
                            send1hReminder(interview);
                        }
                    }
                } catch (Exception e) {
                    Logger.error("Error parsing slot time for interview {}: {}",
                            interview.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            Logger.error("Error in checkAndSendReminders: {}", e.getMessage());
        }
    }

    /**
     * Send 24-hour reminder to student
     */
    private void send24hReminder(InterviewRequest interview) {
        try {
            User student = interview.getStudent();
            User faculty = interview.getFaculty();

            if (student == null || faculty == null) {
                Logger.warn("Interview {} missing student or faculty", interview.getId());
                return;
            }

            String studentEmail = student.getEmail();
            String subject = "Interview Reminder: " + faculty.getFirstName() + " " +
                    faculty.getLastName() + " - Tomorrow";
            String body = String.format(
                    "Hi %s,\n\n" +
                            "This is a reminder that you have an interview scheduled with %s %s tomorrow at %s.\n\n" +
                            "Location: %s\n" +
                            "Meeting Link: %s\n\n" +
                            "Please arrive 5 minutes early. If you need to cancel or reschedule, " +
                            "please do so in the system as soon as possible.\n\n" +
                            "Best regards,\nRA Job System",
                    student.getFirstName(), faculty.getFirstName(), faculty.getLastName(),
                    interview.getAcceptedSlot(), interview.getLocation(),
                    interview.getMeetingLink()
            );

            // Send email
            emailService.sendEmail(studentEmail, subject, body);
            Logger.info("Sent 24h reminder email for interview {}", interview.getId());

            // Mark reminder as sent
            interviewService.markReminder24hSent(interview.getId());
        } catch (Exception e) {
            Logger.error("Error sending 24h reminder for interview {}: {}",
                    interview.getId(), e.getMessage());
        }
    }

    /**
     * Send 1-hour reminder to student
     */
    private void send1hReminder(InterviewRequest interview) {
        try {
            User student = interview.getStudent();
            User faculty = interview.getFaculty();

            if (student == null || faculty == null) {
                Logger.warn("Interview {} missing student or faculty", interview.getId());
                return;
            }

            String studentEmail = student.getEmail();
            String subject = "Upcoming Interview: " + faculty.getFirstName() + " " +
                    faculty.getLastName() + " (in 1 hour)";
            String body = String.format(
                    "Hi %s,\n\n" +
                            "Your interview with %s %s is starting in 1 hour at %s.\n\n" +
                            "Location: %s\n" +
                            "Meeting Link: %s\n\n" +
                            "Please be ready and on time.\n\n" +
                            "Best regards,\nRA Job System",
                    student.getFirstName(), faculty.getFirstName(), faculty.getLastName(),
                    interview.getAcceptedSlot(), interview.getLocation(),
                    interview.getMeetingLink()
            );

            // Send email
            emailService.sendEmail(studentEmail, subject, body);
            Logger.info("Sent 1h reminder email for interview {}", interview.getId());

            // Mark reminder as sent
            interviewService.markReminder1hSent(interview.getId());
        } catch (Exception e) {
            Logger.error("Error sending 1h reminder for interview {}: {}",
                    interview.getId(), e.getMessage());
        }
    }
}