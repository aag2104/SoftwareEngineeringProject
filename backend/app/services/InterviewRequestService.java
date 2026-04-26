package services;

import models.InterviewRequest;
import models.RAJobApplication;
import models.User;
import play.Logger;
import utils.Constants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * F2 - Schedule RA Job Interview (Student View)
 * Business logic for interview request lifecycle.
 */
public class InterviewRequestService {

    private static final int DEFAULT_CANCEL_WINDOW_HOURS = 24;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Faculty creates a new interview request with up to 3 proposed slots.
     * Called by F1 (faculty view) — included here so student side can read it.
     */
    public InterviewRequest createInterviewRequest(
            RAJobApplication application,
            User faculty,
            User student,
            String slot1,
            String slot2,
            String slot3,
            String location,
            String meetingLink,
            String notes) {

        InterviewRequest request = new InterviewRequest();
        request.setRaJobApplication(application);
        request.setFaculty(faculty);
        request.setStudent(student);
        request.setProposedSlot1(slot1);
        request.setProposedSlot2(slot2);
        request.setProposedSlot3(slot3);
        request.setLocation(location);
        request.setMeetingLink(meetingLink);
        request.setNotes(notes);
        request.setStatus("PENDING");
        request.setIsActive("True");
        request.setCancelWindowHours(DEFAULT_CANCEL_WINDOW_HOURS);
        String now = LocalDateTime.now().format(FORMATTER);
        request.setCreateTime(now);
        request.setUpdateTime(now);
        request.save();
        Logger.info("InterviewRequest created id={} for application={}", request.getId(), application.getId());
        return request;
    }

    // ── Student: Accept a slot ────────────────────────────────────────────────

    /**
     * Student accepts one of the faculty-proposed slots.
     * @param requestId the interview request id
     * @param chosenSlot must match one of proposedSlot1/2/3
     * @return updated InterviewRequest or null if not found / invalid
     */
    public InterviewRequest acceptSlot(long requestId, String chosenSlot) {
        InterviewRequest request = findActiveById(requestId);
        if (request == null) {
            Logger.warn("acceptSlot: InterviewRequest {} not found", requestId);
            return null;
        }
        if (!isValidProposedSlot(request, chosenSlot)) {
            Logger.warn("acceptSlot: chosenSlot '{}' not in proposed slots for request {}", chosenSlot, requestId);
            return null;
        }
        request.setAcceptedSlot(chosenSlot);
        request.setStatus("ACCEPTED");
        request.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        request.save();
        Logger.info("InterviewRequest {} accepted slot {}", requestId, chosenSlot);
        return request;
    }

    // ── Student: Counter-offer ────────────────────────────────────────────────

    /**
     * Student proposes an alternative time slot.
     */
    public InterviewRequest counterOffer(long requestId, String counterSlot, String counterNote) {
        InterviewRequest request = findActiveById(requestId);
        if (request == null) return null;

        request.setCounterSlot(counterSlot);
        request.setCounterNote(counterNote);
        request.setStatus("COUNTERED");
        request.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        request.save();
        Logger.info("InterviewRequest {} counter-offered slot {}", requestId, counterSlot);
        return request;
    }

    // ── Student: Cancel ───────────────────────────────────────────────────────

    /**
     * Student cancels the interview. Only allowed if within the cancellation window.
     * @return updated request, or null if cancellation not allowed
     */
    public InterviewRequest cancelInterview(long requestId, String reason) {
        InterviewRequest request = findActiveById(requestId);
        if (request == null) return null;

        String confirmedSlot = request.getAcceptedSlot();
        if (confirmedSlot == null || confirmedSlot.isEmpty()) {
            // No slot confirmed yet — can always cancel a PENDING request
            return doCancel(request, reason);
        }

        if (!withinCancelWindow(confirmedSlot, request.getCancelWindowHours())) {
            Logger.warn("cancelInterview: request {} is outside the {}h cancel window", requestId, request.getCancelWindowHours());
            return null;
        }
        return doCancel(request, reason);
    }

    private InterviewRequest doCancel(InterviewRequest request, String reason) {
        String now = LocalDateTime.now().format(FORMATTER);
        request.setStatus("CANCELLED");
        request.setCancelledBy("STUDENT");
        request.setCancelReason(reason);
        request.setCancelledTime(now);
        request.setUpdateTime(now);
        request.save();
        Logger.info("InterviewRequest {} cancelled by student", request.getId());
        return request;
    }

    // ── Reminders ─────────────────────────────────────────────────────────────

    /**
     * Mark that the 24h reminder was sent (called by a scheduled job).
     */
    public void markReminder24hSent(long requestId) {
        InterviewRequest request = findActiveById(requestId);
        if (request == null) return;
        request.setReminder24hSentTime(LocalDateTime.now().format(FORMATTER));
        request.save();
    }

    /**
     * Mark that the 1h reminder was sent.
     */
    public void markReminder1hSent(long requestId) {
        InterviewRequest request = findActiveById(requestId);
        if (request == null) return;
        request.setReminder1hSentTime(LocalDateTime.now().format(FORMATTER));
        request.save();
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * All active interview requests for a given student.
     */
    public List<InterviewRequest> findByStudent(long studentId) {
        return InterviewRequest.find.query()
                .where()
                .eq("student.id", studentId)
                .eq("isActive", "True")
                .orderBy("createTime desc")
                .findList();
    }

    /**
     * All active interview requests for a given RA job application.
     */
    public List<InterviewRequest> findByApplication(long applicationId) {
        return InterviewRequest.find.query()
                .where()
                .eq("raJobApplication.id", applicationId)
                .eq("isActive", "True")
                .orderBy("createTime desc")
                .findList();
    }

    /**
     * Fetch a single active interview request by id.
     */
    public InterviewRequest findActiveById(long id) {
        return InterviewRequest.find.query()
                .where()
                .eq("id", id)
                .eq("isActive", "True")
                .findOne();
    }

    /**
     * Soft-delete an interview request.
     */
    public void deleteInterviewRequest(long requestId) {
        InterviewRequest request = findActiveById(requestId);
        if (request == null) return;
        request.setIsActive("False");
        request.setUpdateTime(LocalDateTime.now().format(FORMATTER));
        request.save();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isValidProposedSlot(InterviewRequest request, String slot) {
        if (slot == null) return false;
        return slot.equals(request.getProposedSlot1())
                || slot.equals(request.getProposedSlot2())
                || slot.equals(request.getProposedSlot3());
    }

    /**
     * Returns true if the current time is still within the allowed cancellation window
     * before the confirmed interview slot.
     */
    private boolean withinCancelWindow(String slotStr, int windowHours) {
        try {
            LocalDateTime slot = LocalDateTime.parse(slotStr, FORMATTER);
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilInterview = ChronoUnit.HOURS.between(now, slot);
            return hoursUntilInterview >= windowHours;
        } catch (Exception e) {
            Logger.error("withinCancelWindow parse error for slot '{}': {}", slotStr, e.getMessage());
            return false;
        }
    }
}