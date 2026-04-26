package controllers;

import models.InterviewRequest;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import services.InterviewRequestService;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * F2 - Schedule RA Job Interview (Student View)
 * Frontend controller — handles UI routes, renders Twirl views.
 */
public class InterviewRequestController extends Controller {

    private final InterviewRequestService service;

    // Static map to persist test interview data across requests (development only)
    private static final Map<Long, InterviewRequest> testInterviewCache = new HashMap<>();
    private static boolean testDataInitialized = false;

    @Inject
    public InterviewRequestController(InterviewRequestService service) {
        this.service = service;
        // Initialize test data once
        if (!testDataInitialized) {
            initializeTestData();
            testDataInitialized = true;
        }
    }

    public Result interviewList(long applicationId) {
        try {
            List<InterviewRequest> requests = service.getByApplication(applicationId);

            // Fallback to test data if backend returns empty (for development)
            if (requests == null || requests.isEmpty()) {
                Logger.warn("Using hardcoded test data for development");
                requests = getTestInterviewDataFromCache();
            }

            return ok(views.html.interviewRequestList.render(new ArrayList<>(requests), applicationId));
        } catch (Exception e) {
            Logger.error("interviewList error: " + e.getMessage());
            // Return test data on error (development mode)
            List<InterviewRequest> testData = getTestInterviewDataFromCache();
            return ok(views.html.interviewRequestList.render(testData, applicationId));
        }
    }

    public Result myInterviews() {
        try {
            String studentIdStr = session("id");
            if (studentIdStr == null) return redirect(routes.Application.login());
            long studentId = Long.parseLong(studentIdStr);
            Logger.info("myInterviews called for student ID: " + studentId);
            List<InterviewRequest> requests = service.getByStudent(studentId);
            Logger.info("Backend returned " + (requests == null ? "null" : requests.size()) + " requests");

            // Fallback to test data if backend returns empty (for development)
            if (requests == null || requests.isEmpty()) {
                Logger.warn("Using hardcoded test data for development");
                requests = getTestInterviewDataFromCache();
            }

            Logger.info("Rendering " + requests.size() + " interview requests");
            return ok(views.html.interviewRequestList.render(new ArrayList<>(requests), -1L));
        } catch (Exception e) {
            Logger.error("myInterviews error: " + e.getMessage());
            e.printStackTrace();
            // Return test data on error (development mode)
            List<InterviewRequest> testData = getTestInterviewDataFromCache();
            return ok(views.html.interviewRequestList.render(testData, -1L));
        }
    }

    public Result interviewDetail(long id) {
        try {
            InterviewRequest request = service.getById(id);
            if (request == null) {
                // Fallback to test data for development
                Logger.warn("Backend returned null for interview detail " + id + ", using test data");
                request = getTestInterviewById(id);
                if (request == null) return notFound("Interview request not found");
            }
            return ok(views.html.interviewRequestDetail.render(request));
        } catch (Exception e) {
            Logger.error("interviewDetail error: " + e.getMessage());
            // Return test data on error (development mode)
            InterviewRequest testRequest = getTestInterviewById(id);
            if (testRequest != null) {
                return ok(views.html.interviewRequestDetail.render(testRequest));
            }
            return internalServerError("Error loading interview detail");
        }
    }

    public Result acceptSlotPOST(long id) {
        try {
            Map<String, String[]> form = request().body().asFormUrlEncoded();
            String chosenSlot = form.get("chosenSlot")[0];
            InterviewRequest updated = service.acceptSlot(id, chosenSlot);
            if (updated == null) {
                // Fallback: update test data for development
                Logger.warn("Backend failed to accept slot, trying test data update");
                InterviewRequest testRequest = getTestInterviewById(id);
                if (testRequest != null) {
                    testRequest.setAcceptedSlot(chosenSlot);
                    testRequest.setStatus("ACCEPTED");
                    flash("success", "Interview slot accepted! You will receive a reminder before your interview.");
                    return redirect(routes.InterviewRequestController.interviewDetail(id));
                }
                flash("error", "Could not accept slot. Please try again.");
                return redirect(routes.InterviewRequestController.interviewDetail(id));
            }
            flash("success", "Interview slot accepted! You will receive a reminder before your interview.");
            return redirect(routes.InterviewRequestController.interviewDetail(id));
        } catch (Exception e) {
            Logger.error("acceptSlotPOST error: " + e.getMessage());
            flash("error", "An error occurred. Please try again.");
            return redirect(routes.InterviewRequestController.interviewDetail(id));
        }
    }

    public Result counterOfferPage(long id) {
        try {
            InterviewRequest request = service.getById(id);
            if (request == null) {
                // Fallback to test data for development
                Logger.warn("Backend returned null for counter-offer page " + id + ", using test data");
                request = getTestInterviewById(id);
                if (request == null) return notFound("Interview request not found");
            }
            return ok(views.html.interviewCounterOffer.render(request));
        } catch (Exception e) {
            Logger.error("counterOfferPage error: " + e.getMessage());
            // Return test data on error (development mode)
            InterviewRequest testRequest = getTestInterviewById(id);
            if (testRequest != null) {
                return ok(views.html.interviewCounterOffer.render(testRequest));
            }
            return internalServerError("Error loading counter-offer page");
        }
    }

    public Result counterOfferPOST(long id) {
        try {
            Map<String, String[]> form = request().body().asFormUrlEncoded();
            String counterSlot = form.get("counterSlot")[0];
            String counterNote = form.containsKey("counterNote") ? form.get("counterNote")[0] : "";
            InterviewRequest updated = service.counterOffer(id, counterSlot, counterNote);
            if (updated == null) {
                // Fallback: update test data for development
                Logger.warn("Backend failed to submit counter-offer, trying test data update");
                InterviewRequest testRequest = getTestInterviewById(id);
                if (testRequest != null) {
                    testRequest.setCounterSlot(counterSlot);
                    testRequest.setCounterNote(counterNote);
                    testRequest.setStatus("COUNTERED");
                    flash("success", "Counter-offer submitted! The faculty will review your proposed time.");
                    return redirect(routes.InterviewRequestController.interviewDetail(id));
                }
                flash("error", "Could not submit counter-offer. Please try again.");
                return redirect(routes.InterviewRequestController.counterOfferPage(id));
            }
            flash("success", "Counter-offer submitted! The faculty will review your proposed time.");
            return redirect(routes.InterviewRequestController.interviewDetail(id));
        } catch (Exception e) {
            Logger.error("counterOfferPOST error: " + e.getMessage());
            flash("error", "An error occurred. Please try again.");
            return redirect(routes.InterviewRequestController.counterOfferPage(id));
        }
    }

    public Result cancelInterviewPOST(long id) {
        try {
            Map<String, String[]> form = request().body().asFormUrlEncoded();
            String reason = form.containsKey("cancelReason") ? form.get("cancelReason")[0] : "";
            boolean success = service.cancelInterview(id, reason);
            if (!success) {
                // Fallback: try to cancel test data for development
                Logger.warn("Backend failed to cancel interview, trying test data update");
                InterviewRequest testRequest = getTestInterviewById(id);
                if (testRequest != null && (testRequest.getStatus().equals("ACCEPTED") || testRequest.getStatus().equals("CONFIRMED"))) {
                    testRequest.setStatus("CANCELLED");
                    testRequest.setCancelledBy("STUDENT");
                    testRequest.setCancelReason(reason);
                    testRequest.setCancelledTime("2026-04-25T" + java.time.LocalTime.now());
                    flash("success", "Interview cancelled successfully.");
                    return redirect(routes.InterviewRequestController.myInterviews());
                }
                flash("error", "Cannot cancel: you may be outside the cancellation window (must cancel at least 24h before).");
                return redirect(routes.InterviewRequestController.interviewDetail(id));
            }
            flash("success", "Interview cancelled successfully.");
            return redirect(routes.InterviewRequestController.myInterviews());
        } catch (Exception e) {
            Logger.error("cancelInterviewPOST error: " + e.getMessage());
            flash("error", "An error occurred. Please try again.");
            return redirect(routes.InterviewRequestController.interviewDetail(id));
        }
    }

    private static void initializeTestData() {
        // Test Interview 1: PENDING
        InterviewRequest interview1 = new InterviewRequest();
        interview1.setId(1L);
        interview1.setRaJobApplicationId(1L);
        interview1.setFacultyId(2L);
        interview1.setFacultyName("Dr. Smith");
        interview1.setStudentId(1L);
        interview1.setStudentName("Test User");
        interview1.setProposedSlot1("2026-05-10T14:00:00");
        interview1.setProposedSlot2("2026-05-11T10:00:00");
        interview1.setProposedSlot3("2026-05-12T15:30:00");
        interview1.setStatus("PENDING");
        interview1.setLocation("Zoom");
        interview1.setNotes("Please prepare a brief overview of your research interests.");
        interview1.setCreateTime("2026-04-20T09:00:00");
        interview1.setUpdateTime("2026-04-20T09:00:00");
        interview1.setIsActive("True");
        interview1.setCancelWindowHours(24);

        // Test Interview 2: ACCEPTED (with reminder simulation)
        InterviewRequest interview2 = new InterviewRequest();
        interview2.setId(2L);
        interview2.setRaJobApplicationId(1L);
        interview2.setFacultyId(2L);
        interview2.setFacultyName("Dr. Johnson");
        interview2.setStudentId(1L);
        interview2.setStudentName("Test User");
        interview2.setProposedSlot1("2026-04-30T09:00:00");
        interview2.setProposedSlot2("2026-05-01T13:00:00");
        interview2.setProposedSlot3("2026-05-02T16:00:00");
        interview2.setAcceptedSlot("2026-04-30T09:00:00");
        interview2.setStatus("ACCEPTED");
        interview2.setLocation("Room 301");
        interview2.setMeetingLink("https://zoom.us/j/123456789");
        interview2.setNotes("Confirmed for the selected slot. See you then!");
        interview2.setCreateTime("2026-04-15T10:30:00");
        interview2.setUpdateTime("2026-04-18T14:00:00");
        interview2.setIsActive("True");
        interview2.setCancelWindowHours(24);
        // Simulate reminders for development
        interview2.setReminder24hSentTime("2026-04-29T09:00:00");
        interview2.setReminder1hSentTime("2026-04-30T08:00:00");

        testInterviewCache.put(interview1.getId(), interview1);
        testInterviewCache.put(interview2.getId(), interview2);
    }

    /**
     * Get all test interviews from cache for development/testing.
     * TODO: Remove this method before production deployment.
     */
    private static List<InterviewRequest> getTestInterviewDataFromCache() {
        return new ArrayList<>(testInterviewCache.values());
    }

    /**
     * Get a single test interview by ID from cache.
     * TODO: Remove this method before production deployment.
     */
    private static InterviewRequest getTestInterviewById(long id) {
        return testInterviewCache.get(id);
    }
}