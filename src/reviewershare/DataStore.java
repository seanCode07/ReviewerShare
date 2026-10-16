package reviewershare;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Predicate;

/** All data access. Every method reads from / writes to the shared data files (see Db). */
public class DataStore {
    public static final String[] SUBJECTS = {"Computer Science", "Mathematics", "Physics", "English"};
    public static final String[] TYPES = {"Reviewer", "Notes", "Practice Test"};

    public static Student currentUser;          // the logged-in user
    public static String rememberedId = "";     // used by "Remember me"
    public static boolean notifications = true; // Settings preference

    // ---------- Converters (always hand out copies, never the stored objects) ----------
    private static Student toStudent(Db.UserRow u) {
        return new Student(u.id, u.name, u.email, u.course, u.section, u.role);
    }

    private static Material copy(Db.Data d, Material s) {
        Material m = new Material();
        m.id = s.id;
        m.title = s.title;
        m.subject = s.subject;
        m.type = s.type;
        m.uploaderId = s.uploaderId;
        m.uploaderName = d.userName(s.uploaderId);
        m.date = s.date;
        m.rating = s.rating;
        m.ratingCount = s.ratingCount;
        m.description = s.description;
        m.topics = new ArrayList<>(s.topics);
        m.fileName = s.fileName;
        m.fileSize = s.fileSize;
        m.filePath = s.filePath;
        m.requestCount = s.requestCount;
        m.status = s.status;
        m.rejectReason = s.rejectReason;
        return m;
    }

    private static Request toRequest(Db.Data d, Db.RequestRow r) {
        Material m = d.material(r.materialId);
        Db.UserRow requester = d.user(r.requesterId);
        if (m == null || requester == null) return null;
        Db.UserRow owner = d.user(m.uploaderId);
        if (owner == null) return null;
        Request x = new Request();
        x.id = r.id;
        x.materialId = m.id;
        x.materialTitle = m.title;
        x.materialSubject = m.subject;
        x.requesterId = requester.id;
        x.requesterName = requester.name;
        x.ownerId = owner.id;
        x.ownerName = owner.name;
        x.date = r.date;
        x.status = r.status;
        return x;
    }

    /** Matching materials, newest first. */
    private static ArrayList<Material> materials(Db.Data d, Predicate<Material> test) {
        ArrayList<Material> list = new ArrayList<>();
        for (Material m : d.materials) if (test.test(m)) list.add(copy(d, m));
        list.sort((a, b) -> {
            int c = b.date.compareTo(a.date);
            return c != 0 ? c : b.id - a.id;
        });
        return list;
    }

    /** Matching requests, newest first. */
    private static ArrayList<Request> requests(Db.Data d, Predicate<Request> test) {
        ArrayList<Request> list = new ArrayList<>();
        for (Db.RequestRow r : d.requests) {
            Request x = toRequest(d, r);
            if (x != null && test.test(x)) list.add(x);
        }
        list.sort((a, b) -> {
            int c = b.date.compareTo(a.date);
            return c != 0 ? c : b.id - a.id;
        });
        return list;
    }

    // ---------- Students ----------
    public static Student findStudent(String id) {
        return Db.read(d -> {
            Db.UserRow u = d.user(id);
            return u == null ? null : toStudent(u);
        });
    }

    /** Returns the user if the ID and password match, otherwise null. */
    public static Student login(String id, String password) {
        return Db.read(d -> {
            Db.UserRow u = d.user(id);
            if (u == null) return null;
            String attempt = Db.hash(u.salt, password);
            boolean same = MessageDigest.isEqual(attempt.getBytes(StandardCharsets.UTF_8), u.hash.getBytes(StandardCharsets.UTF_8));
            return same ? toStudent(u) : null;
        });
    }

    /** Adds a new student. Returns false if the ID already exists. */
    public static boolean register(Student s, String password) {
        return Db.write(d -> {
            if (d.user(s.id) != null) return false;
            Db.UserRow u = new Db.UserRow();
            u.id = s.id;
            u.name = s.name;
            u.email = s.email;
            u.course = s.course;
            u.section = s.section;
            u.role = "student";
            u.salt = Db.newSalt();
            u.hash = Db.hash(u.salt, password);
            d.users.add(u);
            d.changed = true;
            return true;
        });
    }

    public static boolean emailMatches(String id, String email) {
        return Db.read(d -> {
            Db.UserRow u = d.user(id);
            return u != null && u.email.equalsIgnoreCase(email);
        });
    }

    public static void resetPassword(String id, String newPassword) {
        Db.write(d -> {
            Db.UserRow u = d.user(id);
            if (u != null) {
                u.salt = Db.newSalt();
                u.hash = Db.hash(u.salt, newPassword);
                d.changed = true;
            }
            return null;
        });
    }

    public static void updateProfile(Student s) {
        Db.write(d -> {
            Db.UserRow u = d.user(s.id);
            if (u != null) {
                u.name = s.name;
                u.email = s.email;
                u.course = s.course;
                d.changed = true;
            }
            return null;
        });
    }

    public static int studentCount() {
        return Db.read(d -> {
            int n = 0;
            for (Db.UserRow u : d.users) if ("student".equals(u.role)) n++;
            return n;
        });
    }

    // ---------- Password and account ----------
    private static boolean passwordMatches(Db.UserRow u, String password) {
        String attempt = Db.hash(u.salt, password);
        return MessageDigest.isEqual(attempt.getBytes(StandardCharsets.UTF_8), u.hash.getBytes(StandardCharsets.UTF_8));
    }

    /** Changes the password after checking the current one. Returns false if the current password is wrong. */
    public static boolean changePassword(String id, String current, String newPassword) {
        return Db.write(d -> {
            Db.UserRow u = d.user(id);
            if (u == null || !passwordMatches(u, current)) return false;
            u.salt = Db.newSalt();
            u.hash = Db.hash(u.salt, newPassword);
            d.addActivity(id, "You changed your password", false, System.currentTimeMillis());
            return true;
        });
    }

    /**
     * Deletes an account and everything that belongs to it (shared materials, their files, requests, activity).
     * The Student ID and email become free, so the person can register again later.
     * Returns null when it worked, otherwise a message that explains the problem.
     */
    public static String deleteAccount(String id, String password) {
        ArrayList<String> files = new ArrayList<>();
        String error = Db.write(d -> {
            Db.UserRow u = d.user(id);
            if (u == null) return "This account no longer exists.";
            if (!passwordMatches(u, password)) return "That password is not correct.";
            if ("admin".equals(u.role) && d.adminIds().size() <= 1)
                return "You are the only administrator. Approve another admin first, then delete this account.";

            HashSet<Integer> mine = new HashSet<>();
            for (Iterator<Material> it = d.materials.iterator(); it.hasNext(); ) {
                Material m = it.next();
                if (m.uploaderId.equals(id)) {
                    mine.add(m.id);
                    if (m.filePath != null && !m.filePath.isEmpty()) files.add(m.filePath);
                    it.remove();
                }
            }
            for (Iterator<Db.RequestRow> it = d.requests.iterator(); it.hasNext(); ) {
                Db.RequestRow r = it.next();
                boolean theirs = r.requesterId.equals(id);
                if (theirs || mine.contains(r.materialId)) {
                    if (theirs && !mine.contains(r.materialId) && !r.status.equals("Cancelled")) {
                        Material other = d.material(r.materialId);
                        if (other != null) other.requestCount = Math.max(other.requestCount - 1, 0);
                    }
                    it.remove();
                }
            }
            d.activities.removeIf(a -> a.userId.equals(id));
            d.adminReqs.removeIf(a -> a.adminId.equals(id));
            d.users.remove(u);
            d.changed = true;
            return null;
        });
        if (error == null) {
            for (String p : files) {
                try {
                    Files.deleteIfExists(new File(p).toPath());
                } catch (IOException ignored) {
                }
            }
            if (id.equals(rememberedId)) rememberedId = "";
        }
        return error;
    }

    // ---------- Admin sign-up (needs approval from a current admin) ----------
    public static final String ADMIN_PENDING = "Pending", ADMIN_APPROVED = "Approved",
            ADMIN_REJECTED = "Rejected", ADMIN_REGISTERED = "Registered";

    public static class AdminRequest {
        public int id;
        public String adminId, name, email, status;
        public LocalDate date;

        public String getDateText() {
            return date.format(Material.DATE_FORMAT);
        }
    }

    private static AdminRequest toAdminRequest(Db.AdminReqRow r) {
        AdminRequest x = new AdminRequest();
        x.id = r.id;
        x.adminId = r.adminId;
        x.name = r.name;
        x.email = r.email;
        x.status = r.status;
        x.date = r.date;
        return x;
    }

    /** Step 1: someone asks to become an admin. Every current admin is notified. Returns null if sent, else a message. */
    public static String requestAdminAccess(String adminId, String name, String email) {
        return Db.write(d -> {
            if (d.user(adminId) != null) return "That ID is already taken. Please choose another one.";
            for (Db.UserRow u : d.users)
                if ("admin".equals(u.role) && u.email.equalsIgnoreCase(email)) return "An administrator account already uses that email.";
            for (Db.AdminReqRow r : d.adminReqs) {
                if (!r.adminId.equals(adminId) && !r.email.equalsIgnoreCase(email)) continue;
                if (ADMIN_PENDING.equals(r.status)) return "A request with that ID or email is still waiting for approval.";
                if (ADMIN_APPROVED.equals(r.status))
                    return "That request is already approved. Use \"Register (approved)\" to create your account.";
            }
            d.adminReqs.removeIf(r -> r.adminId.equals(adminId) || r.email.equalsIgnoreCase(email)); // old rejected or used ones
            Db.AdminReqRow r = new Db.AdminReqRow();
            r.id = d.nextAdminReqId();
            r.adminId = adminId;
            r.name = name;
            r.email = email;
            r.date = LocalDate.now();
            r.status = ADMIN_PENDING;
            d.adminReqs.add(r);
            long now = System.currentTimeMillis();
            for (String admin : d.adminIds())
                d.addActivity(admin, name + " asked for admin access. Review it in Moderation > Admin Requests", true, now);
            d.changed = true;
            return null;
        });
    }

    /** The newest admin request with this ID (any status), or null. */
    public static AdminRequest adminRequestFor(String adminId) {
        return Db.read(d -> {
            AdminRequest found = null;
            for (Db.AdminReqRow r : d.adminReqs) if (r.adminId.equals(adminId)) found = toAdminRequest(r);
            return found;
        });
    }

    /** Step 2: an approved person creates the admin account. Returns null if created, else a message. */
    public static String registerApprovedAdmin(String id, String email, String password) {
        return Db.write(d -> {
            Db.AdminReqRow req = null;
            for (Db.AdminReqRow r : d.adminReqs) if (r.adminId.equals(id) && r.email.equalsIgnoreCase(email)) req = r;
            if (req == null) return "No admin request was found for that ID and email.";
            if (ADMIN_PENDING.equals(req.status)) return "Your request is still waiting for approval by a current admin.";
            if (ADMIN_REJECTED.equals(req.status)) return "Your request was rejected. You can send a new request.";
            if (d.user(id) != null) return "That ID is already registered. Please sign in.";
            Db.UserRow u = new Db.UserRow();
            u.id = id;
            u.name = req.name;
            u.email = req.email;
            u.course = "Administrator";
            u.section = "Admin";
            u.role = "admin";
            u.salt = Db.newSalt();
            u.hash = Db.hash(u.salt, password);
            d.users.add(u);
            req.status = ADMIN_REGISTERED;
            long now = System.currentTimeMillis();
            for (String admin : d.adminIds())
                if (!admin.equals(id)) d.addActivity(admin, req.name + " registered as a new administrator", true, now);
            d.changed = true;
            return null;
        });
    }

    /** All admin requests, newest first. */
    public static ArrayList<AdminRequest> adminRequests() {
        return Db.read(d -> {
            ArrayList<AdminRequest> list = new ArrayList<>();
            for (Db.AdminReqRow r : d.adminReqs) list.add(toAdminRequest(r));
            list.sort((a, b) -> b.id - a.id);
            return list;
        });
    }

    public static int pendingAdminRequestCount() {
        return Db.read(d -> {
            int n = 0;
            for (Db.AdminReqRow r : d.adminReqs) if (ADMIN_PENDING.equals(r.status)) n++;
            return n;
        });
    }

    /** Current admin action. Returns false if the request is no longer pending. */
    public static boolean decideAdminRequest(AdminRequest req, boolean approve) {
        final Student me = currentUser;
        return Db.write(d -> {
            Db.AdminReqRow r = d.adminReq(req.id);
            if (r == null || !ADMIN_PENDING.equals(r.status)) return false;
            r.status = approve ? ADMIN_APPROVED : ADMIN_REJECTED;
            d.addActivity(me.id, "You " + (approve ? "approved" : "rejected") + " the admin request of " + r.name, false, System.currentTimeMillis());
            return true;
        });
    }

    // ---------- Materials ----------
    /** True if I may open the file of this material: admins and the owner always, others once the owner approved their request. */
    public static boolean canViewFile(Material m) {
        final Student me = currentUser;
        if (me == null || m == null) return false;
        if (me.isAdmin() || m.uploaderId.equals(me.id)) return true;
        return Db.read(d -> {
            for (Db.RequestRow r : d.requests)
                if (r.materialId == m.id && r.requesterId.equals(me.id) && (r.status.equals("Approved") || r.status.equals("Completed")))
                    return true;
            return false;
        });
    }

    /** Only approved materials are visible in the library. */
    public static ArrayList<Material> approvedMaterials() {
        return Db.read(d -> materials(d, m -> Material.APPROVED.equals(m.status)));
    }

    public static ArrayList<Material> materialsByStatus(String status) {
        return Db.read(d -> materials(d, m -> m.status.equals(status)));
    }

    /** Everything I uploaded, whatever its moderation status. */
    public static ArrayList<Material> myMaterials() {
        String me = currentUser.id;
        return Db.read(d -> materials(d, m -> m.uploaderId.equals(me)));
    }

    public static Material findMaterial(int id) {
        return Db.read(d -> {
            Material m = d.material(id);
            return m == null ? null : copy(d, m);
        });
    }

    public static int countMaterials(String status) {
        return Db.read(d -> {
            int n = 0;
            for (Material m : d.materials) if (m.status.equals(status)) n++;
            return n;
        });
    }

    public static int materialsSharedBy(String uploaderId) {
        return Db.read(d -> {
            int n = 0;
            for (Material m : d.materials) if (m.uploaderId.equals(uploaderId) && Material.APPROVED.equals(m.status)) n++;
            return n;
        });
    }

    /** Average rating of an uploader's rated materials (0 if none). */
    public static double averageRatingOf(String uploaderId) {
        return Db.read(d -> {
            double sum = 0;
            int n = 0;
            for (Material m : d.materials) {
                if (m.uploaderId.equals(uploaderId) && Material.APPROVED.equals(m.status) && m.ratingCount > 0) {
                    sum += m.rating;
                    n++;
                }
            }
            return n == 0 ? 0.0 : sum / n;
        });
    }

    /** Saves a new submission as "Pending Review" and tells the admins. */
    public static void submitMaterial(String title, String subject, String type, String description, String topicsText, File file) {
        String fName = "No file attached", fSize = "-", fPath = "";
        if (file != null) {
            fName = file.getName();
            fSize = Ui.formatSize(file.length());
            try {
                File dest = new File(Db.FILES_DIR, System.currentTimeMillis() + "_" + file.getName());
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                fPath = dest.getAbsolutePath();
            } catch (IOException e) {
                fPath = ""; // the name is still recorded even if the copy failed
            }
        }
        final String n = fName, sz = fSize, path = fPath;
        final Student me = currentUser;
        Db.write(d -> {
            Material m = new Material();
            m.id = d.nextMaterialId();
            m.title = title;
            m.subject = subject;
            m.type = type;
            m.uploaderId = me.id;
            m.date = LocalDate.now();
            m.rating = 0;
            m.ratingCount = 0;
            m.description = description;
            m.topics = Material.parseTopics(topicsText);
            m.fileName = n;
            m.fileSize = sz;
            m.filePath = path;
            m.requestCount = 0;
            m.status = Material.PENDING;
            m.rejectReason = "";
            d.materials.add(m);
            long now = System.currentTimeMillis();
            d.addActivity(me.id, "You submitted \"" + title + "\" for admin review", false, now);
            for (String admin : d.adminIds())
                d.addActivity(admin, me.name + " submitted \"" + title + "\" for review", true, now);
            d.changed = true;
            return null;
        });
    }

    /** Saves edits. If needsReview is true the material goes back to "Pending Review". */
    public static void updateMaterial(Material m, boolean needsReview) {
        final Student me = currentUser;
        Db.write(d -> {
            Material s = d.material(m.id);
            if (s == null) return null;
            s.title = m.title;
            s.subject = m.subject;
            s.type = m.type;
            s.description = m.description;
            if (needsReview) {
                s.status = Material.PENDING;
                s.rejectReason = "";
                long now = System.currentTimeMillis();
                d.addActivity(me.id, "You resubmitted \"" + s.title + "\" for admin review", false, now);
                for (String admin : d.adminIds())
                    d.addActivity(admin, me.name + " resubmitted \"" + s.title + "\" for review", true, now);
            }
            d.changed = true;
            return null;
        });
        if (needsReview) {
            m.status = Material.PENDING;
            m.rejectReason = "";
        }
    }

    /** Admin action. Returns false if the material no longer exists. */
    public static boolean approveMaterial(Material m) {
        final Student me = currentUser;
        return Db.write(d -> {
            Material s = d.material(m.id);
            if (s == null) return false;
            s.status = Material.APPROVED;
            s.rejectReason = "";
            long now = System.currentTimeMillis();
            d.addActivity(s.uploaderId, "Your reviewer \"" + s.title + "\" was approved and is now live!", true, now);
            d.addActivity(me.id, "You approved \"" + s.title + "\"", false, now);
            d.changed = true;
            return true;
        });
    }

    /** Admin action. Returns false if the material no longer exists. */
    public static boolean rejectMaterial(Material m, String reason) {
        final Student me = currentUser;
        return Db.write(d -> {
            Material s = d.material(m.id);
            if (s == null) return false;
            s.status = Material.REJECTED;
            s.rejectReason = reason;
            long now = System.currentTimeMillis();
            d.addActivity(s.uploaderId, "Your reviewer \"" + s.title + "\" was rejected: " + reason, true, now);
            d.addActivity(me.id, "You rejected \"" + s.title + "\"", false, now);
            d.changed = true;
            return true;
        });
    }

    public static void deleteMaterial(int id) {
        Db.write(d -> {
            d.requests.removeIf(r -> r.materialId == id);
            d.materials.removeIf(m -> m.id == id);
            d.changed = true;
            return null;
        });
    }

    // ---------- Requests ----------
    /** Requests sent TO me that are still open (not completed or cancelled). */
    public static ArrayList<Request> incomingRequests() {
        String me = currentUser.id;
        return Db.read(d -> requests(d, r -> r.ownerId.equals(me) && !r.status.equals("Completed") && !r.status.equals("Cancelled")));
    }

    /** Requests I sent to other students. */
    public static ArrayList<Request> sentRequests() {
        String me = currentUser.id;
        return Db.read(d -> requests(d, r -> r.requesterId.equals(me)));
    }

    /** Completed requests that involve me (as owner or requester). */
    public static ArrayList<Request> completedRequests() {
        String me = currentUser.id;
        return Db.read(d -> requests(d, r -> r.status.equals("Completed") && (r.ownerId.equals(me) || r.requesterId.equals(me))));
    }

    public static int pendingCount() {
        String me = currentUser.id;
        return Db.read(d -> requests(d, r -> r.ownerId.equals(me) && r.status.equals("Pending")).size());
    }

    /** True if I already have an open or finished request for this material (cancelled/declined ones don't count). */
    public static boolean hasRequested(int materialId) {
        String me = currentUser.id;
        return Db.read(d -> !requests(d, r -> r.materialId == materialId && r.requesterId.equals(me)
                && (r.status.equals("Pending") || r.status.equals("Approved") || r.status.equals("Completed"))).isEmpty());
    }

    /** My own Pending request for this material, or null. */
    public static Request pendingRequestFor(int materialId) {
        String me = currentUser.id;
        ArrayList<Request> l = Db.read(d -> requests(d, r -> r.materialId == materialId && r.requesterId.equals(me) && r.status.equals("Pending")));
        return l.isEmpty() ? null : l.get(0);
    }

    public static void addRequest(Material m) {
        final Student me = currentUser;
        Db.write(d -> {
            Material s = d.material(m.id);
            if (s == null) return null;
            Db.RequestRow r = new Db.RequestRow();
            r.id = d.nextRequestId();
            r.materialId = s.id;
            r.requesterId = me.id;
            r.date = LocalDate.now();
            r.status = "Pending";
            d.requests.add(r);
            s.requestCount++;
            long now = System.currentTimeMillis();
            d.addActivity(me.id, "You requested \"" + s.title + "\"", false, now);
            d.addActivity(s.uploaderId, me.name + " requested your \"" + s.title + "\"", true, now);
            d.changed = true;
            return null;
        });
    }

    /** Owner action: changes the status only if it is still "from". Returns false if someone changed it first. */
    public static boolean changeRequestStatus(Request req, String from, String to) {
        final Student me = currentUser;
        return Db.write(d -> {
            Db.RequestRow r = d.request(req.id);
            if (r == null || !r.status.equals(from)) return false;
            r.status = to;
            String verb = to.toLowerCase();
            long now = System.currentTimeMillis();
            d.addActivity(r.requesterId, "Your request for \"" + req.materialTitle + "\" was " + verb, true, now);
            d.addActivity(me.id, "You marked " + req.requesterName + "'s request for \"" + req.materialTitle + "\" as " + verb, false, now);
            d.changed = true;
            return true;
        });
    }

    /** Requester action: only a Pending request can be cancelled. Returns false if it already changed. */
    public static boolean cancelRequest(Request req) {
        final Student me = currentUser;
        return Db.write(d -> {
            Db.RequestRow r = d.request(req.id);
            if (r == null || !r.status.equals("Pending")) return false;
            r.status = "Cancelled";
            Material m = d.material(r.materialId);
            if (m != null) m.requestCount = Math.max(m.requestCount - 1, 0);
            long now = System.currentTimeMillis();
            d.addActivity(me.id, "You cancelled your request for \"" + req.materialTitle + "\"", false, now);
            d.addActivity(req.ownerId, req.requesterName + " cancelled their request for \"" + req.materialTitle + "\"", true, now);
            d.changed = true;
            return true;
        });
    }

    // ---------- Activity ----------
    public static class Activity {
        public int id;
        public String text;
        public long created;
    }

    /** notify = true means "show a pop-up message to that user" (it was caused by somebody else). */
    public static void addActivity(String userId, String text, boolean notify) {
        Db.write(d -> {
            d.addActivity(userId, text, notify, System.currentTimeMillis());
            return null;
        });
    }

    public static ArrayList<String> activitiesFor(String userId, int limit) {
        return Db.read(d -> {
            ArrayList<String> list = new ArrayList<>();
            for (int i = d.activities.size() - 1; i >= 0 && list.size() < limit; i--) {
                Db.ActivityRow a = d.activities.get(i);
                if (a.userId.equals(userId)) list.add(a.text + " (" + ago(a.created) + ")");
            }
            return list;
        });
    }

    public static long latestActivityId(String userId) {
        return Db.read(d -> {
            long max = 0;
            for (Db.ActivityRow a : d.activities) if (a.userId.equals(userId)) max = Math.max(max, a.id);
            return max;
        });
    }

    /** Notification-worthy activities with an id between (afterId, upToId]. */
    public static ArrayList<Activity> newNotifications(String userId, long afterId, long upToId) {
        return Db.read(d -> {
            ArrayList<Activity> list = new ArrayList<>();
            for (Db.ActivityRow a : d.activities) {
                if (a.userId.equals(userId) && a.notify && a.id > afterId && a.id <= upToId) {
                    Activity x = new Activity();
                    x.id = a.id;
                    x.text = a.text;
                    x.created = a.created;
                    list.add(x);
                }
            }
            return list;
        });
    }

    private static String ago(long created) {
        long s = Math.max(0, (System.currentTimeMillis() - created) / 1000);
        if (s < 60) return "just now";
        long m = s / 60;
        if (m < 60) return m + (m == 1 ? " min ago" : " mins ago");
        long h = m / 60;
        if (h < 24) return h + (h == 1 ? " hr ago" : " hrs ago");
        long d = h / 24;
        return d + (d == 1 ? " day ago" : " days ago");
    }
}