package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import models.InterviewRequest;
import play.Logger;
import play.libs.Json;
import utils.Constants;
import utils.RESTfulCalls;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * F2 - Schedule RA Job Interview (Student View)
 * Frontend service — calls backend REST API using RESTfulCalls pattern.
 */
public class InterviewRequestService {

    @Inject
    Config config;

    public List<InterviewRequest> getByApplication(long applicationId) {
        try {
            JsonNode response = RESTfulCalls.getAPI(
                    RESTfulCalls.getBackendAPIUrl(config, Constants.INTERVIEW_BY_APPLICATION + applicationId));
            if (response == null || response.has("error")) return new ArrayList<>();
            return InterviewRequest.deserializeList(response);
        } catch (Exception e) {
            Logger.error("InterviewRequestService.getByApplication error: " + e.toString());
            return new ArrayList<>();
        }
    }

    public List<InterviewRequest> getByStudent(long studentId) {
        try {
            JsonNode response = RESTfulCalls.getAPI(
                    RESTfulCalls.getBackendAPIUrl(config, Constants.INTERVIEW_BY_STUDENT + studentId));
            if (response == null || response.has("error")) return new ArrayList<>();
            return InterviewRequest.deserializeList(response);
        } catch (Exception e) {
            Logger.error("InterviewRequestService.getByStudent error: " + e.toString());
            return new ArrayList<>();
        }
    }

    public InterviewRequest getById(long id) {
        try {
            JsonNode response = RESTfulCalls.getAPI(
                    RESTfulCalls.getBackendAPIUrl(config, Constants.INTERVIEW_DETAIL + id));
            if (response == null || response.has("error")) return null;
            return InterviewRequest.deserialize(response);
        } catch (Exception e) {
            Logger.error("InterviewRequestService.getById error: " + e.toString());
            return null;
        }
    }

    public InterviewRequest acceptSlot(long requestId, String chosenSlot) {
        try {
            JsonNode body = Json.newObject().put("chosenSlot", chosenSlot);
            JsonNode response = RESTfulCalls.postAPI(
                    RESTfulCalls.getBackendAPIUrl(config, Constants.INTERVIEW_ACCEPT + requestId), body);
            if (response == null || response.has("error")) return null;
            return InterviewRequest.deserialize(response);
        } catch (Exception e) {
            Logger.error("InterviewRequestService.acceptSlot error: " + e.toString());
            return null;
        }
    }

    public InterviewRequest counterOffer(long requestId, String counterSlot, String counterNote) {
        try {
            JsonNode body = Json.newObject()
                    .put("counterSlot", counterSlot)
                    .put("counterNote", counterNote);
            JsonNode response = RESTfulCalls.postAPI(
                    RESTfulCalls.getBackendAPIUrl(config, Constants.INTERVIEW_COUNTER + requestId), body);
            if (response == null || response.has("error")) return null;
            return InterviewRequest.deserialize(response);
        } catch (Exception e) {
            Logger.error("InterviewRequestService.counterOffer error: " + e.toString());
            return null;
        }
    }

    public boolean cancelInterview(long requestId, String reason) {
        try {
            JsonNode body = Json.newObject().put("reason", reason);
            JsonNode response = RESTfulCalls.postAPI(
                    RESTfulCalls.getBackendAPIUrl(config, Constants.INTERVIEW_CANCEL + requestId), body);
            return response != null && !response.has("error");
        } catch (Exception e) {
            Logger.error("InterviewRequestService.cancelInterview error: " + e.toString());
            return false;
        }
    }
}