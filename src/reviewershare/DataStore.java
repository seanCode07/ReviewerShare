package reviewershare;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** All data access. Every method reads from / writes to the shared data files (see Db). */
public class DataStore {
    public static final String[] ALLOWED_EXT = {"pdf", "doc", "docx", "ppt", "pptx"};   // what students may upload
    public static final int MAX_FILES = 10, MAX_FILE_MB = 25;
    public static final int MAX_ATTEMPTS = 5, LOCK_MINUTES = 5;                         // sign-in limit
    public static final String[] REPORT_REASONS = {"Copyrighted material", "Plagiarized content",
            "Leaked exam or confidential material", "Inappropriate content", "Other"};
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
        for (Material.FileInfo f : s.files) {
            Material.FileInfo c = new Material.FileInfo();
            c.id = f.id;
            c.name = f.name;
            c.size = f.size;
            c.path = f.path;
            m.files.add(c);
        }
        if (!s.files.isEmpty()) {
            m.fileName = s.files.get(0).name;
            m.filePath = s.files.get(0).path;
        }
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

    private static String nameKey(String name) {
        String[] words = name.toLowerCase().replaceAll("[^\\p{L}0-9 ]", " ").trim().split("\\s+");
        Arrays.sort(words);   // "Cruz, John" and "John Cruz" are the same person
        return String.join(" ", words);
    }

    private static String registrationProblem(Db.Data d, String id, String name, String email) {
        if (d.user(id) != null) return "That Student ID is already registered.";
        if (!d.roster.isEmpty()) {
            Db.RosterRow r = d.rosterRow(id);
            if (r == null) return "That Student ID is not in the school records. Please check it or ask an administrator.";
            if (!nameKey(r.name).equals(nameKey(name))) return "The name does not match the school record for that Student ID.";
        }
        for (Db.UserRow u : d.users)
            if (u.email.equalsIgnoreCase(email)) return "That email is already used by another account.";
        return null;
    }

    /** Checks the sign-up details against the school records and existing accounts. Returns null if everything is fine. */
    public static String checkRegistration(String id, String name, String email) {
        return Db.read(d -> registrationProblem(d, id, name, email));
    }

    /** Adds a new student. Returns null if it worked, otherwise a message that explains the problem. */
    public static String registerStudent(Student s, String password) {
        return Db.write(d -> {
            String problem = registrationProblem(d, s.id, s.name, s.email);
            if (problem != null) return problem;
            Db.RosterRow r = d.rosterRow(s.id);
            Db.UserRow u = new Db.UserRow();
            u.id = s.id;
            u.name = s.name;
            u.email = s.email;
            u.course = r != null && !r.program.isEmpty() ? r.program : Db.normalizeProgram(d.programs, s.course);
            u.section = s.section;
            u.role = "student";
            u.salt = Db.newSalt();
            u.hash = Db.hash(u.salt, password);
            d.users.add(u);
            d.changed = true;
            return null;
        });
    }

    /** True if another account already uses this email. */
    public static boolean emailInUse(String email, String exceptId) {
        return Db.read(d -> {
            for (Db.UserRow u : d.users) if (u.email.equalsIgnoreCase(email) && !u.id.equals(exceptId)) return true;
            return false;
        });
    }

    // ---------- Sign-in attempt limit ----------
    /** Seconds until this ID may try to sign in again (0 = not locked). */
    public static int lockSecondsLeft(String id) {
        return Db.read(d -> {
            Db.AttemptRow a = d.attempt(id);
            long left = a == null ? 0 : a.lockUntil - System.currentTimeMillis();
            return left > 0 ? (int) ((left + 999) / 1000) : 0;
        });
    }

    /** Counts a wrong password. Returns how many tries are left (0 means the ID is now locked). */
    public static int recordFailedLogin(String id) {
        return Db.write(d -> {
            long now = System.currentTimeMillis();
            Db.AttemptRow a = d.attempt(id);
            if (a == null) {
                a = new Db.AttemptRow();
                a.id = id;
                d.attempts.add(a);
                while (d.attempts.size() > 300) d.attempts.remove(0);
            }
            if (a.lockUntil != 0 && a.lockUntil <= now) {
                a.fails = 0;
                a.lockUntil = 0;
            }
            a.fails++;
            int left = MAX_ATTEMPTS - a.fails;
            if (left <= 0) {
                a.lockUntil = now + LOCK_MINUTES * 60_000L;
                a.fails = 0;
                left = 0;
                d.audit(d.user(id) != null ? id : "", "Sign-in locked", "Too many wrong passwords for ID " + id);
            }
            d.changed = true;
            return left;
        });
    }

    public static void clearFailedLogins(String id) {
        Db.write(d -> {
            Db.AttemptRow a = d.attempt(id);
            if (a != null) {
                d.attempts.remove(a);
                d.changed = true;
            }
            return null;
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
     * Deletes an account and everything that belongs to it (shared materials, their files, requests, reviews, activity).
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
            d.audit(id, "Deleted own account", u.name + " (" + id + ")");
            purgeUser(d, u, files, false);
            return null;
        });
        deleteFromDisk(files);
        if (error == null && id.equals(rememberedId)) rememberedId = "";
        return error;
    }

    /** Admin action: removes a user and all of their data. Returns null if it worked, else a message. */
    public static String adminRemoveUser(String id) {
        final Student me = currentUser;
        ArrayList<String> files = new ArrayList<>();
        String error = Db.write(d -> {
            if (id.equals(me.id)) return "You cannot remove your own account here. Use Settings > Delete Account.";
            Db.UserRow u = d.user(id);
            if (u == null) return "That user no longer exists.";
            if ("admin".equals(u.role) && d.adminIds().size() <= 1) return "You cannot remove the only administrator.";
            d.audit(me.id, "Removed user", u.name + " (" + id + ")");
            purgeUser(d, u, files, true);
            return null;
        });
        deleteFromDisk(files);
        return error;
    }

    private static void deleteFromDisk(List<String> paths) {
        for (String p : paths) {
            if (p == null || p.isEmpty()) continue;
            try {
                Files.deleteIfExists(new File(p).toPath());
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean inLibrary(String status) {
        return status.equals("Pending") || status.equals("Approved") || status.equals("Completed");
    }

    /** Recalculates a material's average after one star rating is taken away. */
    private static void dropRating(Material m, int stars) {
        if (m == null) return;
        if (m.ratingCount <= 1) {
            m.rating = 0;
            m.ratingCount = 0;
        } else {
            m.rating = Math.max(0, Math.min(5, (m.rating * m.ratingCount - stars) / (m.ratingCount - 1)));
            m.ratingCount--;
        }
    }

    /** Removes a user and everything connected to them, and tells the people who are affected. */
    private static void purgeUser(Db.Data d, Db.UserRow u, List<String> files, boolean byAdmin) {
        String id = u.id;
        long now = System.currentTimeMillis();
        HashSet<Integer> mine = new HashSet<>();
        HashMap<Integer, String> titles = new HashMap<>();
        for (Iterator<Material> it = d.materials.iterator(); it.hasNext(); ) {
            Material m = it.next();
            if (m.uploaderId.equals(id)) {
                mine.add(m.id);
                titles.put(m.id, m.title);
                for (Material.FileInfo f : m.files) files.add(f.path);
                if (m.files.isEmpty() && m.filePath != null && !m.filePath.isEmpty()) files.add(m.filePath);
                it.remove();
            }
        }
        String who = byAdmin ? u.name + "'s account was removed" : u.name + " deleted their account";
        for (Iterator<Db.RequestRow> it = d.requests.iterator(); it.hasNext(); ) {
            Db.RequestRow r = it.next();
            boolean theirs = r.requesterId.equals(id);
            boolean onMine = mine.contains(r.materialId);
            if (!theirs && !onMine) continue;
            if (onMine && !theirs && inLibrary(r.status)) {
                d.addActivity(r.requesterId, who + ", so \"" + titles.get(r.materialId) + "\" is no longer in your library", true, now);
            }
            if (theirs && !onMine) {
                Material other = d.material(r.materialId);
                if (other != null) {
                    if (inLibrary(r.status))
                        d.addActivity(other.uploaderId, who + ", so their request for \"" + other.title + "\" was removed", true, now);
                    if (!r.status.equals("Cancelled")) other.requestCount = Math.max(other.requestCount - 1, 0);
                }
            }
            it.remove();
        }
        for (Iterator<Db.ReviewRow> it = d.reviews.iterator(); it.hasNext(); ) {
            Db.ReviewRow r = it.next();
            if (mine.contains(r.materialId)) {
                it.remove();
            } else if (r.userId.equals(id)) {
                dropRating(d.material(r.materialId), r.stars);
                it.remove();
            }
        }
        for (Iterator<Db.ReportRow> it = d.reports.iterator(); it.hasNext(); ) {
            Db.ReportRow r = it.next();
            if (mine.contains(r.materialId) && r.status.equals("Open")) r.status = "Resolved";
            if (r.reporterId.equals(id)) {
                if (r.status.equals("Open")) {
                    r.reporterId = "";
                    r.reporterName = "(deleted user)";
                } else {
                    it.remove();
                }
            }
        }
        d.activities.removeIf(a -> a.userId.equals(id));
        d.adminReqs.removeIf(a -> a.adminId.equals(id));
        d.suspended.removeIf(s -> s.userId.equals(id));
        d.attempts.removeIf(a -> a.id.equals(id));
        d.users.remove(u);
        d.changed = true;
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
                if (u.email.equalsIgnoreCase(email)) return "That email is already used by an account.";
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
            d.audit(id, "Registered as administrator", req.name + " (" + id + ")");
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
            d.audit(me.id, approve ? "Approved admin request" : "Rejected admin request", r.name + " (" + r.adminId + ")");
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

    // ---------- Files ----------
    /** Returns null if the file may be uploaded, otherwise a message. Only PDF, Word and PowerPoint files up to 25 MB. */
    public static String checkFile(File f) {
        String n = f.getName().toLowerCase();
        int dot = n.lastIndexOf('.');
        String ext = dot < 0 ? "" : n.substring(dot + 1);
        if (!Arrays.asList(ALLOWED_EXT).contains(ext))
            return "\"" + f.getName() + "\" is not allowed. Upload PDF, Word (doc, docx) or PowerPoint (ppt, pptx) files only.";
        if (f.length() > MAX_FILE_MB * 1024L * 1024L)
            return "\"" + f.getName() + "\" is bigger than " + MAX_FILE_MB + " MB.";
        if (f.length() == 0) return "\"" + f.getName() + "\" is empty.";
        return null;
    }

    /** Copies the chosen files into the data folder. Their ids are given later, inside the database write. */
    private static ArrayList<Material.FileInfo> storeFiles(List<File> picked) {
        ArrayList<Material.FileInfo> list = new ArrayList<>();
        if (picked == null) return list;
        int n = 0;
        for (File file : picked) {
            try {
                String safe = file.getName().replaceAll("[^A-Za-z0-9._-]", "_");
                File dest = new File(Db.FILES_DIR, System.currentTimeMillis() + "_" + (n++) + "_" + safe);
                Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Material.FileInfo f = new Material.FileInfo();
                f.name = file.getName();
                f.size = Ui.formatSize(file.length());
                f.path = dest.getAbsolutePath();
                list.add(f);
            } catch (IOException e) {
                // this file could not be copied, so it is skipped
            }
        }
        return list;
    }

    /** Saves a new submission as "Pending Review" and tells the admins. */
    public static void submitMaterial(String title, String subject, String type, String description, String topicsText, List<File> picked) {
        ArrayList<Material.FileInfo> stored = storeFiles(picked);
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
            m.fileName = "No file attached";
            m.fileSize = "-";
            m.filePath = "";
            m.requestCount = 0;
            m.status = Material.PENDING;
            m.rejectReason = "";
            d.materials.add(m);
            for (Material.FileInfo f : stored) {
                f.id = d.nextFileId();
                m.files.add(f);
            }
            long now = System.currentTimeMillis();
            d.addActivity(me.id, "You submitted \"" + title + "\" for admin review", false, now);
            for (String admin : d.adminIds())
                d.addActivity(admin, me.name + " submitted \"" + title + "\" for review", true, now);
            d.changed = true;
            return null;
        });
    }

    /** Students who may open the files of a material (their request was approved or completed). */
    private static HashSet<String> usersWithAccess(Db.Data d, int materialId) {
        HashSet<String> set = new HashSet<>();
        for (Db.RequestRow r : d.requests)
            if (r.materialId == materialId && (r.status.equals("Approved") || r.status.equals("Completed"))) set.add(r.requesterId);
        return set;
    }

    /**
     * Saves edits, including added and removed files. If needsReview is true the material goes back to "Pending Review".
     * Everyone who has access to the files is told when the files change.
     */
    public static void updateMaterial(Material m, boolean needsReview, List<File> addFiles, Set<Integer> removeFileIds) {
        final Student me = currentUser;
        ArrayList<Material.FileInfo> added = storeFiles(addFiles);
        ArrayList<String> oldPaths = new ArrayList<>();
        Db.write(d -> {
            Material s = d.material(m.id);
            if (s == null) return null;
            s.title = m.title;
            s.subject = m.subject;
            s.type = m.type;
            s.description = m.description;
            int removed = 0;
            for (Iterator<Material.FileInfo> it = s.files.iterator(); it.hasNext(); ) {
                Material.FileInfo f = it.next();
                if (removeFileIds.contains(f.id)) {
                    oldPaths.add(f.path);
                    it.remove();
                    removed++;
                }
            }
            for (Material.FileInfo f : added) {
                f.id = d.nextFileId();
                s.files.add(f);
            }
            long now = System.currentTimeMillis();
            if (removed > 0 || !added.isEmpty()) {
                ArrayList<String> parts = new ArrayList<>();
                if (!added.isEmpty()) parts.add("added " + added.size() + (added.size() == 1 ? " file" : " files"));
                if (removed > 0) parts.add("removed " + removed + (removed == 1 ? " file" : " files"));
                for (String user : usersWithAccess(d, s.id))
                    d.addActivity(user, "The owner updated the files of \"" + s.title + "\" (" + String.join(" and ", parts) + ")", true, now);
            }
            if (needsReview) {
                s.status = Material.PENDING;
                s.rejectReason = "";
                d.addActivity(me.id, "You resubmitted \"" + s.title + "\" for admin review", false, now);
                for (String admin : d.adminIds())
                    d.addActivity(admin, me.name + " resubmitted \"" + s.title + "\" for review", true, now);
            }
            d.changed = true;
            return null;
        });
        deleteFromDisk(oldPaths);
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
            d.audit(me.id, "Approved reviewer", s.title);
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
            d.audit(me.id, "Rejected reviewer", s.title + " - " + reason);
            d.changed = true;
            return true;
        });
    }

    /** Owner deletes a material. Everyone who has it in their library is told. */
    public static void deleteMaterial(int id) {
        deleteMaterial(id, false);
    }

    /** Deletes a material (and its files, requests and reviews) and notifies the students who had it in their library. */
    public static void deleteMaterial(int id, boolean byAdmin) {
        final Student me = currentUser;
        ArrayList<String> files = new ArrayList<>();
        Db.write(d -> {
            Material gone = d.material(id);
            if (gone != null && byAdmin) {
                d.addActivity(gone.uploaderId, "Your reviewer \"" + gone.title + "\" was removed by an admin", true, System.currentTimeMillis());
                d.audit(me == null ? null : me.id, "Took down reviewer", gone.title + " by " + d.userName(gone.uploaderId));
            }
            removeMaterial(d, id, byAdmin, files);
            return null;
        });
        deleteFromDisk(files);
    }

    /** The shared part of every deletion. Must run inside Db.write. */
    private static void removeMaterial(Db.Data d, int id, boolean byAdmin, List<String> files) {
        Material gone = d.material(id);
        if (gone != null) {
            long now = System.currentTimeMillis();
            HashSet<String> told = new HashSet<>();
            for (Db.RequestRow r : d.requests) {
                if (r.materialId != id || r.requesterId.equals(gone.uploaderId)) continue;
                if (inLibrary(r.status) && told.add(r.requesterId))
                    d.addActivity(r.requesterId, "\"" + gone.title + "\" was removed " + (byAdmin ? "by an admin" : "by its owner")
                            + " and is no longer in your library", true, now);
            }
            for (Material.FileInfo f : gone.files) files.add(f.path);
            if (gone.files.isEmpty() && gone.filePath != null && !gone.filePath.isEmpty()) files.add(gone.filePath);
        }
        for (Db.ReportRow r : d.reports) if (r.materialId == id && r.status.equals("Open")) r.status = "Resolved";
        d.reviews.removeIf(r -> r.materialId == id);
        d.requests.removeIf(r -> r.materialId == id);
        d.materials.removeIf(m -> m.id == id);
        d.changed = true;
    }

    // ---------- Reviews (stars + comments) ----------
    public static class Review {
        public int id, materialId, stars;
        public String userId, userName, comment;
        public LocalDate date;

        public String getDateText() {
            return date.format(Material.DATE_FORMAT);
        }
    }

    private static boolean hasAccess(Db.Data d, int materialId, String userId) {
        for (Db.RequestRow r : d.requests)
            if (r.materialId == materialId && r.requesterId.equals(userId) && (r.status.equals("Approved") || r.status.equals("Completed")))
                return true;
        return false;
    }

    /** Students may review a material after the owner approved their request (never their own, never admins). */
    public static boolean canReview(Material m) {
        final Student me = currentUser;
        if (me == null || m == null || me.isAdmin() || m.uploaderId.equals(me.id) || !Material.APPROVED.equals(m.status)) return false;
        return Db.read(d -> hasAccess(d, m.id, me.id));
    }

    private static Review toReview(Db.Data d, Db.ReviewRow r) {
        Review x = new Review();
        x.id = r.id;
        x.materialId = r.materialId;
        x.userId = r.userId;
        Db.UserRow u = d.user(r.userId);
        x.userName = u != null ? u.name : r.userName;
        x.stars = r.stars;
        x.comment = r.comment;
        x.date = r.date;
        return x;
    }

    /** Reviews of a material, newest first. */
    public static ArrayList<Review> reviewsFor(int materialId) {
        return Db.read(d -> {
            ArrayList<Review> list = new ArrayList<>();
            for (Db.ReviewRow r : d.reviews) if (r.materialId == materialId) list.add(toReview(d, r));
            list.sort((a, b) -> b.id - a.id);
            return list;
        });
    }

    public static Review myReview(int materialId) {
        final Student me = currentUser;
        return Db.read(d -> {
            for (Db.ReviewRow r : d.reviews) if (r.materialId == materialId && r.userId.equals(me.id)) return toReview(d, r);
            return null;
        });
    }

    /** Adds or updates my review. Returns null if saved, otherwise a message. The owner is notified. */
    public static String saveReview(int materialId, int stars, String comment) {
        final Student me = currentUser;
        return Db.write(d -> {
            Material m = d.material(materialId);
            if (m == null) return "This reviewer no longer exists.";
            if (me.isAdmin() || m.uploaderId.equals(me.id) || !Material.APPROVED.equals(m.status))
                return "You cannot review this material.";
            if (!hasAccess(d, materialId, me.id)) return "You can review a reviewer after the owner approves your request.";
            Db.ReviewRow r = null;
            for (Db.ReviewRow x : d.reviews) if (x.materialId == materialId && x.userId.equals(me.id)) r = x;
            boolean existing = r != null;
            if (!existing) {
                r = new Db.ReviewRow();
                r.id = d.nextReviewId();
                r.materialId = materialId;
                r.userId = me.id;
                d.reviews.add(r);
                m.rating = (m.rating * m.ratingCount + stars) / (m.ratingCount + 1);
                m.ratingCount++;
            } else if (m.ratingCount > 0) {
                m.rating = (m.rating * m.ratingCount - r.stars + stars) / m.ratingCount;
            } else {
                m.rating = stars;
                m.ratingCount = 1;
            }
            m.rating = Math.max(0, Math.min(5, m.rating));
            r.userName = me.name;
            r.stars = stars;
            r.comment = comment;
            r.date = LocalDate.now();
            String shown = comment.length() > 60 ? comment.substring(0, 57) + "..." : comment;
            d.addActivity(m.uploaderId, me.name + (existing ? " updated their review of \"" : " reviewed \"") + m.title + "\" - "
                    + stars + (stars == 1 ? " star" : " stars") + (shown.isEmpty() ? "" : ": " + shown), true, System.currentTimeMillis());
            d.changed = true;
            return null;
        });
    }

    /** The author or an admin removes a review. Returns false if it is gone or not allowed. */
    public static boolean deleteReview(int reviewId) {
        final Student me = currentUser;
        return Db.write(d -> {
            Db.ReviewRow r = null;
            for (Db.ReviewRow x : d.reviews) if (x.id == reviewId) r = x;
            if (r == null || !(me.isAdmin() || r.userId.equals(me.id))) return false;
            Material m = d.material(r.materialId);
            dropRating(m, r.stars);
            d.reviews.remove(r);
            if (me.isAdmin() && !r.userId.equals(me.id)) {
                String t = m == null ? "a reviewer" : "\"" + m.title + "\"";
                d.addActivity(r.userId, "Your review of " + t + " was removed by an admin", true, System.currentTimeMillis());
                d.audit(me.id, "Removed review", "Review by " + r.userName + " on " + t);
            }
            d.changed = true;
            return true;
        });
    }

    // ---------- Reports (copyright, plagiarism, ...) ----------
    public static class Report {
        public int id, materialId;
        public String title, reason, details, reporterName, uploaderName, status, handledBy;
        public LocalDate date;

        public String getDateText() {
            return date.format(Material.DATE_FORMAT);
        }
    }

    /** A student reports a material. Every admin is notified. Returns null if sent, otherwise a message. */
    public static String reportMaterial(int materialId, String reason, String details) {
        final Student me = currentUser;
        return Db.write(d -> {
            Material m = d.material(materialId);
            if (m == null) return "This reviewer no longer exists.";
            if (m.uploaderId.equals(me.id)) return "You cannot report your own material.";
            for (Db.ReportRow r : d.reports)
                if (r.materialId == materialId && r.reporterId.equals(me.id) && r.status.equals("Open"))
                    return "You already reported this material. An admin will review it soon.";
            Db.ReportRow r = new Db.ReportRow();
            r.id = d.nextReportId();
            r.materialId = materialId;
            r.title = m.title;
            r.reporterId = me.id;
            r.reporterName = me.name;
            r.reason = reason;
            r.details = details;
            r.date = LocalDate.now();
            r.status = "Open";
            r.handledBy = "";
            d.reports.add(r);
            long now = System.currentTimeMillis();
            for (String admin : d.adminIds())
                d.addActivity(admin, me.name + " reported \"" + m.title + "\" (" + reason + "). Review it in Moderation > Reports", true, now);
            d.addActivity(me.id, "You reported \"" + m.title + "\" (" + reason + ")", false, now);
            d.changed = true;
            return null;
        });
    }

    /** All reports, open ones first, then newest first. */
    public static ArrayList<Report> reports() {
        return Db.read(d -> {
            ArrayList<Report> list = new ArrayList<>();
            for (Db.ReportRow r : d.reports) {
                Report x = new Report();
                x.id = r.id;
                x.materialId = r.materialId;
                x.title = r.title;
                x.reason = r.reason;
                x.details = r.details;
                x.reporterName = r.reporterName;
                Material m = d.material(r.materialId);
                x.uploaderName = m == null ? "-" : d.userName(m.uploaderId);
                x.status = r.status;
                x.handledBy = r.handledBy;
                x.date = r.date;
                list.add(x);
            }
            list.sort((a, b) -> {
                int o = (a.status.equals("Open") ? 0 : 1) - (b.status.equals("Open") ? 0 : 1);
                return o != 0 ? o : b.id - a.id;
            });
            return list;
        });
    }

    public static int openReportCount() {
        return Db.read(d -> {
            int n = 0;
            for (Db.ReportRow r : d.reports) if (r.status.equals("Open")) n++;
            return n;
        });
    }

    /** Admin decision on a report: take the material down, or dismiss the report. Returns false if it was already handled. */
    public static boolean resolveReport(int reportId, boolean takeDown) {
        final Student me = currentUser;
        ArrayList<String> files = new ArrayList<>();
        boolean ok = Db.write(d -> {
            Db.ReportRow r = null;
            for (Db.ReportRow x : d.reports) if (x.id == reportId) r = x;
            if (r == null || !r.status.equals("Open")) return false;
            Material m = d.material(r.materialId);
            long now = System.currentTimeMillis();
            if (takeDown && m != null) {
                for (Db.ReportRow x : d.reports) {
                    if (x.materialId != m.id || !x.status.equals("Open")) continue;
                    x.status = "Action Taken";
                    x.handledBy = me.name;
                    if (!x.reporterId.isEmpty())
                        d.addActivity(x.reporterId, "Thank you. \"" + m.title + "\" was taken down after your report", true, now);
                }
                d.addActivity(m.uploaderId, "Your reviewer \"" + m.title + "\" was taken down by an admin after a report (" + r.reason + ")", true, now);
                d.audit(me.id, "Took down reviewer after report", m.title + " - " + r.reason);
                removeMaterial(d, m.id, true, files);
            } else {
                r.status = "Dismissed";
                r.handledBy = me.name;
                if (!r.reporterId.isEmpty())
                    d.addActivity(r.reporterId, "An admin reviewed your report on \"" + r.title + "\". No action was needed", true, now);
                d.audit(me.id, "Dismissed report", r.title + " - " + r.reason);
            }
            d.changed = true;
            return true;
        });
        deleteFromDisk(files);
        return ok;
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

    // ---------- Users (admin tools) ----------
    public static class UserInfo {
        public String id, name, email, course, role;
        public boolean suspended;
        public int shared;
    }

    public static ArrayList<UserInfo> allUsers() {
        return Db.read(d -> {
            ArrayList<UserInfo> list = new ArrayList<>();
            for (Db.UserRow u : d.users) {
                UserInfo x = new UserInfo();
                x.id = u.id;
                x.name = u.name;
                x.email = u.email;
                x.course = u.course;
                x.role = u.role;
                x.suspended = d.suspension(u.id) != null;
                for (Material m : d.materials) if (m.uploaderId.equals(u.id)) x.shared++;
                list.add(x);
            }
            list.sort((a, b) -> {
                int c = a.role.equals("admin") == b.role.equals("admin") ? 0 : (a.role.equals("admin") ? -1 : 1);
                return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
            });
            return list;
        });
    }

    public static boolean isSuspended(String id) {
        return Db.read(d -> d.suspension(id) != null);
    }

    /** Admin action: suspend or reactivate an account. Returns null if it worked, otherwise a message. */
    public static String setSuspended(String id, boolean suspend, String reason) {
        final Student me = currentUser;
        return Db.write(d -> {
            if (id.equals(me.id)) return "You cannot suspend your own account.";
            Db.UserRow u = d.user(id);
            if (u == null) return "That user no longer exists.";
            Db.SuspendedRow s = d.suspension(id);
            if (suspend && s == null) {
                s = new Db.SuspendedRow();
                s.userId = id;
                s.reason = reason;
                s.date = LocalDate.now();
                d.suspended.add(s);
                d.audit(me.id, "Suspended user", u.name + " (" + id + ")" + (reason.isEmpty() ? "" : " - " + reason));
                d.changed = true;
            } else if (!suspend && s != null) {
                d.suspended.remove(s);
                d.audit(me.id, "Reactivated user", u.name + " (" + id + ")");
                d.changed = true;
            }
            return null;
        });
    }

    public static String suspensionReason(String id) {
        return Db.read(d -> {
            Db.SuspendedRow s = d.suspension(id);
            return s == null ? "" : s.reason;
        });
    }

    // ---------- School records (roster) ----------
    public static class RosterEntry {
        public String id, name, program;
        public boolean registered;
    }

    public static ArrayList<RosterEntry> roster() {
        return Db.read(d -> {
            ArrayList<RosterEntry> list = new ArrayList<>();
            for (Db.RosterRow r : d.roster) {
                RosterEntry x = new RosterEntry();
                x.id = r.id;
                x.name = r.name;
                x.program = r.program;
                x.registered = d.user(r.id) != null;
                list.add(x);
            }
            list.sort((a, b) -> a.id.compareTo(b.id));
            return list;
        });
    }

    public static int rosterSize() {
        return Db.read(d -> d.roster.size());
    }

    /** The school record for an ID, or null. */
    public static RosterEntry rosterLookup(String id) {
        return Db.read(d -> {
            Db.RosterRow r = d.rosterRow(id);
            if (r == null) return null;
            RosterEntry x = new RosterEntry();
            x.id = r.id;
            x.name = r.name;
            x.program = r.program;
            return x;
        });
    }

    private static boolean addRoster(Db.Data d, String id, String name, String program) {
        Db.RosterRow r = d.rosterRow(id);
        boolean isNew = r == null;
        if (isNew) {
            r = new Db.RosterRow();
            r.id = id;
            d.roster.add(r);
        }
        r.name = name;
        r.program = Db.normalizeProgram(d.programs, program);
        d.changed = true;
        return isNew;
    }

    public static String rosterAdd(String id, String name, String program) {
        final Student me = currentUser;
        return Db.write(d -> {
            if (!id.matches("\\d{1,12}")) return "Student ID must be numbers only, up to 12 digits.";
            if (name.trim().isEmpty()) return "Please type the student's full name.";
            boolean isNew = addRoster(d, id, name.trim(), program);
            d.audit(me.id, isNew ? "Added school record" : "Updated school record", name.trim() + " (" + id + ")");
            return null;
        });
    }

    public static boolean rosterRemove(String id) {
        final Student me = currentUser;
        return Db.write(d -> {
            Db.RosterRow r = d.rosterRow(id);
            if (r == null) return false;
            d.roster.remove(r);
            d.audit(me.id, "Removed school record", r.name + " (" + id + ")");
            d.changed = true;
            return true;
        });
    }

    /** Adds or updates many school records at once. Each row is {id, name, program}. Returns how many were saved. */
    public static int rosterImport(List<String[]> rows) {
        final Student me = currentUser;
        return Db.write(d -> {
            int n = 0;
            for (String[] r : rows) {
                if (r.length < 2 || !r[0].matches("\\d{1,12}") || r[1].trim().isEmpty()) continue;
                addRoster(d, r[0], r[1].trim(), r.length > 2 ? r[2] : "");
                n++;
            }
            if (n > 0) d.audit(me.id, "Imported school records", n + " students");
            return n;
        });
    }

    // ---------- Subjects and programs ----------
    public static ArrayList<String> subjects() {
        return Db.read(d -> new ArrayList<>(d.subjects));
    }

    public static String[] subjectArray() {
        return subjects().toArray(new String[0]);
    }

    public static ArrayList<String> programs() {
        return Db.read(d -> new ArrayList<>(d.programs));
    }

    public static String[] programArray() {
        return programs().toArray(new String[0]);
    }

    /** How many materials use each subject. */
    public static HashMap<String, Integer> subjectCounts() {
        return Db.read(d -> {
            HashMap<String, Integer> map = new HashMap<>();
            for (Material m : d.materials) map.merge(m.subject, 1, Integer::sum);
            return map;
        });
    }

    /** How many students use each program. */
    public static HashMap<String, Integer> programCounts() {
        return Db.read(d -> {
            HashMap<String, Integer> map = new HashMap<>();
            for (Db.UserRow u : d.users) if (!"admin".equals(u.role)) map.merge(u.course, 1, Integer::sum);
            return map;
        });
    }

    private static boolean hasName(ArrayList<String> list, String name) {
        for (String x : list) if (x.equalsIgnoreCase(name)) return true;
        return false;
    }

    public static String addSubject(String name) {
        final Student me = currentUser;
        final String n = name.trim();
        return Db.write(d -> {
            if (n.isEmpty()) return "Please type a subject name.";
            if (n.length() > 40) return "The name is too long (40 characters at most).";
            if (hasName(d.subjects, n)) return "That subject already exists.";
            d.subjects.add(n);
            d.audit(me.id, "Added subject", n);
            return null;
        });
    }

    /** Renames a subject everywhere (materials included). */
    public static String renameSubject(String oldName, String newName) {
        final Student me = currentUser;
        final String n = newName.trim();
        return Db.write(d -> {
            if (n.isEmpty()) return "Please type a subject name.";
            if (n.length() > 40) return "The name is too long (40 characters at most).";
            if (!oldName.equalsIgnoreCase(n) && hasName(d.subjects, n)) return "That subject already exists.";
            int i = d.subjects.indexOf(oldName);
            if (i < 0) return "That subject no longer exists.";
            d.subjects.set(i, n);
            for (Material m : d.materials) if (m.subject.equals(oldName)) m.subject = n;
            d.audit(me.id, "Renamed subject", oldName + " to " + n);
            return null;
        });
    }

    public static String removeSubject(String name) {
        final Student me = currentUser;
        return Db.write(d -> {
            for (Material m : d.materials)
                if (m.subject.equals(name)) return "Some reviewers still use \"" + name + "\". Rename it instead, or remove those reviewers first.";
            if (d.subjects.size() <= 1) return "At least one subject must remain.";
            if (!d.subjects.remove(name)) return "That subject no longer exists.";
            d.audit(me.id, "Removed subject", name);
            return null;
        });
    }

    public static String addProgram(String name) {
        final Student me = currentUser;
        final String n = name.trim();
        return Db.write(d -> {
            if (n.isEmpty()) return "Please type a program name.";
            if (n.length() > 60) return "The name is too long (60 characters at most).";
            if (hasName(d.programs, n)) return "That program already exists.";
            d.programs.add(n);
            d.audit(me.id, "Added program", n);
            return null;
        });
    }

    public static String removeProgram(String name) {
        final Student me = currentUser;
        return Db.write(d -> {
            for (Db.UserRow u : d.users)
                if (!"admin".equals(u.role) && u.course.equals(name)) return "Some students still use \"" + name + "\".";
            if (d.programs.size() <= 1) return "At least one program must remain.";
            if (!d.programs.remove(name)) return "That program no longer exists.";
            d.audit(me.id, "Removed program", name);
            return null;
        });
    }

    // ---------- Audit log ----------
    public static class AuditEntry {
        public String when, who, action, details;
    }

    private static final DateTimeFormatter AUDIT_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", java.util.Locale.ENGLISH);

    /** The newest audit lines first. */
    public static ArrayList<AuditEntry> auditLog(int limit) {
        return Db.read(d -> {
            ArrayList<AuditEntry> list = new ArrayList<>();
            for (int i = d.audit.size() - 1; i >= 0 && list.size() < limit; i--) {
                Db.AuditRow a = d.audit.get(i);
                AuditEntry x = new AuditEntry();
                x.when = LocalDateTime.ofInstant(Instant.ofEpochMilli(a.time), ZoneId.systemDefault()).format(AUDIT_FORMAT);
                x.who = a.actorName;
                x.action = a.action;
                x.details = a.details;
                list.add(x);
            }
            return list;
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