package models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.ebean.Finder;
import io.ebean.Model;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

/**
 * F2 - Schedule RA Job Interview (Student View)
 * Represents an interview request sent by faculty to a student applicant.
 *
 * Status lifecycle:
 *   PENDING   → faculty proposed slots, student has not yet responded
 *   ACCEPTED  → student accepted one of the proposed slots
 *   COUNTERED → student proposed an alternative slot
 *   CONFIRMED → both parties confirmed (after counter-offer accepted by faculty)
 *   CANCELLED → student cancelled within the allowed window
 *   EXPIRED   → no response before deadline
 */
@Entity
@Table(name = "interview_request")
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = InterviewRequest.class)
@ToString
public class InterviewRequest extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // ── Relationships ──────────────────────────────────────────────────────────

    @ManyToOne
    @JoinColumn(name = "ra_job_application_id", referencedColumnName = "id")
    private RAJobApplication raJobApplication;

    @ManyToOne
    @JoinColumn(name = "faculty_id", referencedColumnName = "id")
    private User faculty;

    @ManyToOne
    @JoinColumn(name = "student_id", referencedColumnName = "id")
    private User student;

    // ── Proposed slots (faculty sends up to 3 options) ────────────────────────

    /** ISO-8601 datetime string, e.g. "2025-06-10T14:00:00" */
    private String proposedSlot1;
    private String proposedSlot2;
    private String proposedSlot3;

    // ── Accepted / confirmed slot ─────────────────────────────────────────────

    /** The slot the student accepted (copied from one of the proposed slots) */
    private String acceptedSlot;

    // ── Counter-offer (student proposes alternative) ──────────────────────────

    private String counterSlot;
    private String counterNote;

    // ── Interview metadata ────────────────────────────────────────────────────

    /** PENDING | ACCEPTED | COUNTERED | CONFIRMED | CANCELLED | EXPIRED */
    private String status;

    private String location;       // e.g. "Zoom link / Room 301"
    private String meetingLink;
    private String notes;          // faculty notes to student

    // ── Cancellation ─────────────────────────────────────────────────────────

    private String cancelledBy;    // "STUDENT" or "FACULTY"
    private String cancelReason;
    private String cancelledTime;

    // ── Reminders ────────────────────────────────────────────────────────────

    private String reminder24hSentTime;
    private String reminder1hSentTime;

    // ── Audit fields ─────────────────────────────────────────────────────────

    private String createTime;
    private String updateTime;
    private String isActive;       // "True" / "False" (soft delete)

    // ── Cancellation window config (hours before interview) ───────────────────
    // Default enforced in service layer; stored here for auditability
    private int cancelWindowHours; // e.g. 24

    /****************** Constructors ******************************************/

    public InterviewRequest() {
    }

    public InterviewRequest(long id) {
        this.id = id;
    }

    /****************** Finder ************************************************/

    public static Finder<Long, InterviewRequest> find =
            new Finder<>(InterviewRequest.class);
}