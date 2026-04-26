package models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * F2 - Schedule RA Job Interview (Student View)
 * Frontend DTO — mirrors backend InterviewRequest for JSON transport.
 * Not an Ebean entity; used only by the frontend service/controller.
 */
public class InterviewRequest {

    private long id;
    private long raJobApplicationId;
    private long facultyId;
    private String facultyName;
    private long studentId;
    private String studentName;

    // Proposed slots from faculty
    private String proposedSlot1;
    private String proposedSlot2;
    private String proposedSlot3;

    // Accepted slot (after student accepts)
    private String acceptedSlot;

    // Counter-offer from student
    private String counterSlot;
    private String counterNote;

    // Status: PENDING | ACCEPTED | COUNTERED | CONFIRMED | CANCELLED | EXPIRED
    private String status;

    private String location;
    private String meetingLink;
    private String notes;

    private String cancelledBy;
    private String cancelReason;
    private String cancelledTime;

    private String createTime;
    private String updateTime;
    private String isActive;
    private int cancelWindowHours;

    // Reminders
    private String reminder24hSentTime;
    private String reminder1hSentTime;

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getRaJobApplicationId() { return raJobApplicationId; }
    public void setRaJobApplicationId(long raJobApplicationId) { this.raJobApplicationId = raJobApplicationId; }

    public long getFacultyId() { return facultyId; }
    public void setFacultyId(long facultyId) { this.facultyId = facultyId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getProposedSlot1() { return proposedSlot1; }
    public void setProposedSlot1(String proposedSlot1) { this.proposedSlot1 = proposedSlot1; }

    public String getProposedSlot2() { return proposedSlot2; }
    public void setProposedSlot2(String proposedSlot2) { this.proposedSlot2 = proposedSlot2; }

    public String getProposedSlot3() { return proposedSlot3; }
    public void setProposedSlot3(String proposedSlot3) { this.proposedSlot3 = proposedSlot3; }

    public String getAcceptedSlot() { return acceptedSlot; }
    public void setAcceptedSlot(String acceptedSlot) { this.acceptedSlot = acceptedSlot; }

    public String getCounterSlot() { return counterSlot; }
    public void setCounterSlot(String counterSlot) { this.counterSlot = counterSlot; }

    public String getCounterNote() { return counterNote; }
    public void setCounterNote(String counterNote) { this.counterNote = counterNote; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getCancelledTime() { return cancelledTime; }
    public void setCancelledTime(String cancelledTime) { this.cancelledTime = cancelledTime; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }

    public String getIsActive() { return isActive; }
    public void setIsActive(String isActive) { this.isActive = isActive; }

    public int getCancelWindowHours() { return cancelWindowHours; }
    public void setCancelWindowHours(int cancelWindowHours) { this.cancelWindowHours = cancelWindowHours; }

    public String getReminder24hSentTime() { return reminder24hSentTime; }
    public void setReminder24hSentTime(String reminder24hSentTime) { this.reminder24hSentTime = reminder24hSentTime; }

    public String getReminder1hSentTime() { return reminder1hSentTime; }
    public void setReminder1hSentTime(String reminder1hSentTime) { this.reminder1hSentTime = reminder1hSentTime; }

    // ── Deserializers ─────────────────────────────────────────────────────────

    /**
     * Deserialize a single InterviewRequest from a JsonNode returned by the backend.
     */
    public static InterviewRequest deserialize(JsonNode json) {
        if (json == null || json.isNull()) return null;
        InterviewRequest ir = new InterviewRequest();
        ir.setId(json.path("id").asLong());
        ir.setStatus(json.path("status").asText(""));
        ir.setProposedSlot1(json.path("proposedSlot1").asText(""));
        ir.setProposedSlot2(json.path("proposedSlot2").asText(""));
        ir.setProposedSlot3(json.path("proposedSlot3").asText(""));
        ir.setAcceptedSlot(json.path("acceptedSlot").asText(""));
        ir.setCounterSlot(json.path("counterSlot").asText(""));
        ir.setCounterNote(json.path("counterNote").asText(""));
        ir.setLocation(json.path("location").asText(""));
        ir.setMeetingLink(json.path("meetingLink").asText(""));
        ir.setNotes(json.path("notes").asText(""));
        ir.setCancelledBy(json.path("cancelledBy").asText(""));
        ir.setCancelReason(json.path("cancelReason").asText(""));
        ir.setCancelledTime(json.path("cancelledTime").asText(""));
        ir.setCreateTime(json.path("createTime").asText(""));
        ir.setUpdateTime(json.path("updateTime").asText(""));
        ir.setIsActive(json.path("isActive").asText("True"));
        ir.setCancelWindowHours(json.path("cancelWindowHours").asInt(24));
        ir.setReminder24hSentTime(json.path("reminder24hSentTime").asText(""));
        ir.setReminder1hSentTime(json.path("reminder1hSentTime").asText(""));

        // Nested objects
        JsonNode faculty = json.path("faculty");
        if (!faculty.isMissingNode()) {
            ir.setFacultyId(faculty.path("id").asLong());
            ir.setFacultyName(faculty.path("firstName").asText("") + " " + faculty.path("lastName").asText(""));
        }
        JsonNode student = json.path("student");
        if (!student.isMissingNode()) {
            ir.setStudentId(student.path("id").asLong());
            ir.setStudentName(student.path("firstName").asText("") + " " + student.path("lastName").asText(""));
        }
        JsonNode app = json.path("raJobApplication");
        if (!app.isMissingNode()) {
            ir.setRaJobApplicationId(app.path("id").asLong());
        }
        return ir;
    }

    /**
     * Deserialize a JSON array into a list of InterviewRequest DTOs.
     */
    public static List<InterviewRequest> deserializeList(JsonNode jsonArray) {
        List<InterviewRequest> list = new ArrayList<>();
        if (jsonArray == null || !jsonArray.isArray()) return list;
        for (JsonNode node : jsonArray) {
            InterviewRequest ir = deserialize(node);
            if (ir != null) list.add(ir);
        }
        return list;
    }
}