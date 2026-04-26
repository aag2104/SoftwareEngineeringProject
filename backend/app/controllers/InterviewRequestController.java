package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.InterviewRequest;
import models.RAJobApplication;
import models.User;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.InterviewRequestService;

import javax.inject.Inject;
import java.util.List;

/**
 * F2 - Schedule RA Job Interview (Student View)
 * Backend REST API controller.
 *
 * All endpoints return JSON.
 * Authentication/session checks should be added per project conventions.
 */
public class InterviewRequestController extends Controller {

    private final InterviewRequestService service;

    @Inject
    public InterviewRequestController(InterviewRequestService service) {
        this.service = service;
    }

    // ── POST /interview/add ───────────────────────────────────────────────────
    /**
     * Faculty creates an interview request.
     * Body JSON: { applicationId, facultyId, studentId, slot1, slot2, slot3,
     *              location, meetingLink, notes }
     */
    public Result addInterviewRequest() {
        try {
            JsonNode body = request().body().asJson();
            if (body == null) return badRequest("Expected JSON body");

            long applicationId = body.path("applicationId").asLong();
            long facultyId     = body.path("facultyId").asLong();
            long studentId     = body.path("studentId").asLong();

            RAJobApplication application = RAJobApplication.find.byId(applicationId);
            User faculty = User.find.byId(facultyId);
            User student = User.find.byId(studentId);

            if (application == null || faculty == null || student == null) {
                return badRequest("Invalid applicationId, facultyId, or studentId");
            }

            InterviewRequest created = service.createInterviewRequest(
                    application, faculty, student,
                    body.path("slot1").asText(null),
                    body.path("slot2").asText(null),
                    body.path("slot3").asText(null),
                    body.path("location").asText(""),
                    body.path("meetingLink").asText(""),
                    body.path("notes").asText("")
            );
            return ok(Json.toJson(created));
        } catch (Exception e) {
            Logger.error("addInterviewRequest error: {}", e.getMessage());
            return internalServerError("Error creating interview request");
        }
    }


    // ── GET /interview/detail/:id ─────────────────────────────────────────────
    /**
     * Get a single interview request by id.
     */
    public Result getInterviewRequestById(long id) {
        try {
            InterviewRequest request = service.findActiveById(id);
            if (request == null) return notFound("InterviewRequest not found: " + id);
            return ok(Json.toJson(request));
        } catch (Exception e) {
            Logger.error("getInterviewRequestById error: {}", e.getMessage());
            return internalServerError("Error fetching interview request");
        }
    }

    // ── GET /interview/byApplication/:applicationId ───────────────────────────
    /**
     * Get all interview requests for a given RA job application.
     */
    public Result getByApplication(long applicationId) {
        try {
            List<InterviewRequest> list = service.findByApplication(applicationId);
            return ok(Json.toJson(list));
        } catch (Exception e) {
            Logger.error("getByApplication error: {}", e.getMessage());
            return internalServerError("Error fetching interview requests");
        }
    }

    // ── GET /interview/byStudent/:studentId ───────────────────────────────────
    /**
     * Get all interview requests for a given student.
     */
    public Result getByStudent(long studentId) {
        try {
            List<InterviewRequest> list = service.findByStudent(studentId);
            return ok(Json.toJson(list));
        } catch (Exception e) {
            Logger.error("getByStudent error: {}", e.getMessage());
            return internalServerError("Error fetching interview requests for student");
        }
    }

    // ── POST /interview/accept/:id ────────────────────────────────────────────
    /**
     * Student accepts a proposed slot.
     * Body JSON: { "chosenSlot": "2025-06-10T14:00:00" }
     */
    public Result acceptSlot(long id) {
        try {
            JsonNode body = request().body().asJson();
            if (body == null) return badRequest("Expected JSON body");

            String chosenSlot = body.path("chosenSlot").asText(null);
            if (chosenSlot == null || chosenSlot.isEmpty()) {
                return badRequest("chosenSlot is required");
            }

            InterviewRequest updated = service.acceptSlot(id, chosenSlot);
            if (updated == null) return badRequest("Invalid slot or request not found");
            return ok(Json.toJson(updated));
        } catch (Exception e) {
            Logger.error("acceptSlot error: {}", e.getMessage());
            return internalServerError("Error accepting slot");
        }
    }

    // ── POST /interview/counter/:id ───────────────────────────────────────────
    /**
     * Student proposes a counter-offer slot.
     * Body JSON: { "counterSlot": "2025-06-12T10:00:00", "counterNote": "..." }
     */
    public Result counterOffer(long id) {
        try {
            JsonNode body = request().body().asJson();
            if (body == null) return badRequest("Expected JSON body");

            String counterSlot = body.path("counterSlot").asText(null);
            String counterNote = body.path("counterNote").asText("");

            if (counterSlot == null || counterSlot.isEmpty()) {
                return badRequest("counterSlot is required");
            }

            InterviewRequest updated = service.counterOffer(id, counterSlot, counterNote);
            if (updated == null) return notFound("InterviewRequest not found: " + id);
            return ok(Json.toJson(updated));
        } catch (Exception e) {
            Logger.error("counterOffer error: {}", e.getMessage());
            return internalServerError("Error submitting counter-offer");
        }
    }

    // ── POST /interview/cancel/:id ────────────────────────────────────────────
    /**
     * Student cancels the interview.
     * Body JSON: { "reason": "..." }
     */
    public Result cancelInterview(long id) {
        try {
            JsonNode body = request().body().asJson();
            String reason = (body != null) ? body.path("reason").asText("") : "";

            InterviewRequest updated = service.cancelInterview(id, reason);
            if (updated == null) {
                return badRequest("Cannot cancel: request not found or outside cancellation window");
            }
            return ok(Json.toJson(updated));
        } catch (Exception e) {
            Logger.error("cancelInterview error: {}", e.getMessage());
            return internalServerError("Error cancelling interview");
        }
    }

    // ── POST /interview/reminder24h/:id ──────────────────────────────────────
    /**
     * Mark 24h reminder as sent (called by scheduler job).
     */
    public Result markReminder24h(long id) {
        try {
            service.markReminder24hSent(id);
            return ok("24h reminder marked");
        } catch (Exception e) {
            Logger.error("markReminder24h error: {}", e.getMessage());
            return internalServerError("Error marking reminder");
        }
    }

    // ── POST /interview/reminder1h/:id ───────────────────────────────────────
    /**
     * Mark 1h reminder as sent (called by scheduler job).
     */
    public Result markReminder1h(long id) {
        try {
            service.markReminder1hSent(id);
            return ok("1h reminder marked");
        } catch (Exception e) {
            Logger.error("markReminder1h error: {}", e.getMessage());
            return internalServerError("Error marking reminder");
        }
    }
}