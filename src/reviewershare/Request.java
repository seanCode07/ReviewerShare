package reviewershare;

import java.time.LocalDate;

/** A request from one student to access another student's material. */
public class Request {
    public int id, materialId;
    public String materialTitle, materialSubject;
    public String requesterName, requesterId, ownerName, ownerId;
    public String status;   // Pending, Approved, Declined, Completed, Cancelled
    public LocalDate date;

    public String getDateText() {
        return date.format(Material.DATE_FORMAT);
    }
}