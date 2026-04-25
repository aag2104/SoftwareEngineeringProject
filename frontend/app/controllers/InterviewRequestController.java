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

/**
 * F2 - Schedule RA Job Interview (Student View)
 * Frontend controller — handles UI routes, renders Twirl views.
 */
public class InterviewRequestController extends Controller {

    private final InterviewRequestService service;

    @Inject
    public InterviewRequestController(InterviewRequestService service) {
        this.service = service;
    }

    public Result interviewList(long applicationId) {
        try {
            List<InterviewRequest> requests = service.getByApplication(applicationId);
            return ok(views.html.interviewRequestList.render(new ArrayList<>(requests), applicationId));
        } catch (Exception e) {
            Logger.error("interviewList error: " + e.getMessage());
            return internalServerError("Error loading interview requests");
        }
    }

    public Result myInterviews() {
        try {
            String studentIdStr = session("id");
            if (studentIdStr == null) return redirect(routes.Application.login());
            long studentId = Long.parseLong(studentIdStr);
            List<InterviewRequest> requests = service.getByStudent(studentId);
            return ok(views.html.interviewRequestList.render(new ArrayList<>(requests), -1L));
        } catch (Exception e) {
            Logger.error("myInterviews error: " + e.getMessage());
            return internalServerError("Error loading your interviews");
        }
    }

    public Result interviewDetail(long id) {
        try {
            InterviewRequest request = service.getById(id);
            if (request == null) return notFound("Interview request not found");
            return ok(views.html.interviewRequestDetail.render(request));
        } catch (Exception e) {
            Logger.error("interviewDetail error: " + e.getMessage());
            return internalServerError("Error loading interview detail");
        }
    }

    public Result acceptSlotPOST(long id) {
        try {
            Map<String, String[]> form = request().body().asFormUrlEncoded();
            String chosenSlot = form.get("chosenSlot")[0];
            InterviewRequest updated = service.acceptSlot(id, chosenSlot);
            if (updated == null) {
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
            if (request == null) return notFound("Interview request not found");
            return ok(views.html.interviewCounterOffer.render(request));
        } catch (Exception e) {
            Logger.error("counterOfferPage error: " + e.getMessage());
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
}