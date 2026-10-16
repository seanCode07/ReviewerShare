package reviewershare;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.function.Function;

/**
 * A small file-based database - no extra libraries needed.
 *
 * Each table is a plain text file (one row per line, fields separated by TAB) inside ~/ReviewerShareData:
 *   students.dat, materials.dat, requests.dat, activities.dat, adminreqs.dat, meta.dat (a version counter)
 *
 * Every running copy of the app uses the SAME files. A lock file makes sure only one window writes at a time,
 * and every write bumps the version number in meta.dat, which the other windows poll to know when to refresh.
 */
public class Db {
    // ---------- Row types ----------
    public static class UserRow {
        public String id, name, email, course, section, salt, hash, role;
    }

    public static class RequestRow {
        public int id, materialId;
        public String requesterId, status;
        public LocalDate date;
    }

    public static class ActivityRow {
        public int id;
        public String userId, text;
        public long created;
        public boolean notify;
    }

    /** A request from a future administrator. Status: Pending, Approved, Rejected, Registered. */
    public static class AdminReqRow {
        public int id;
        public String adminId, name, email, status;
        public LocalDate date;
    }

    /** An in-memory copy of all the tables. Change it only inside Db.write(...). */
    public static class Data {
        public final ArrayList<UserRow> users = new ArrayList<>();
        public final ArrayList<Material> materials = new ArrayList<>();
        public final ArrayList<RequestRow> requests = new ArrayList<>();
        public final ArrayList<ActivityRow> activities = new ArrayList<>();
        public final ArrayList<AdminReqRow> adminReqs = new ArrayList<>();
        public long version;
        /** Set to true by code that modified something, so Db.write knows to save. */
        public boolean changed;

        public UserRow user(String id) {
            for (UserRow u : users) if (u.id.equals(id)) return u;
            return null;
        }

        public String userName(String id) {
            UserRow u = user(id);
            return u == null ? "Unknown" : u.name;
        }

        public Material material(int id) {
            for (Material m : materials) if (m.id == id) return m;
            return null;
        }

        public RequestRow request(int id) {
            for (RequestRow r : requests) if (r.id == id) return r;
            return null;
        }

        public AdminReqRow adminReq(int id) {
            for (AdminReqRow r : adminReqs) if (r.id == id) return r;
            return null;
        }

        public int nextAdminReqId() {
            int max = 0;
            for (AdminReqRow r : adminReqs) max = Math.max(max, r.id);
            return max + 1;
        }

        public ArrayList<String> adminIds() {
            ArrayList<String> l = new ArrayList<>();
            for (UserRow u : users) if ("admin".equals(u.role)) l.add(u.id);
            return l;
        }

        public int nextMaterialId() {
            int max = 0;
            for (Material m : materials) max = Math.max(max, m.id);
            return max + 1;
        }

        public int nextRequestId() {
            int max = 0;
            for (RequestRow r : requests) max = Math.max(max, r.id);
            return max + 1;
        }

        public void addActivity(String userId, String text, boolean notify, long created) {
            int max = 0;
            for (ActivityRow a : activities) max = Math.max(max, a.id);
            ActivityRow a = new ActivityRow();
            a.id = max + 1;
            a.userId = userId;
            a.text = text;
            a.created = created;
            a.notify = notify;
            activities.add(a);
            changed = true;
        }
    }

    public static final File DIR = new File(System.getProperty("user.home"), "ReviewerShareData");
    public static final File FILES_DIR = new File(DIR, "files");
    private static final File LOCK_FILE = new File(DIR, "reviewershare.lock");

    private static Data data;   // the copy currently in memory

    // ---------- Setup ----------
    public static synchronized void init() {
        DIR.mkdirs();
        FILES_DIR.mkdirs();
        write(d -> {
            if (d.users.isEmpty()) seed(d);
            return null;
        });
    }

    // ---------- Reading and writing ----------
    /** Runs f on an up-to-date copy of the data. f must not change anything. */
    public static synchronized <T> T read(Function<Data, T> f) {
        try (Lock lock = new Lock(true)) {
            long v = readVersion();
            if (data == null || data.version != v) data = load(v);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the data files: " + e.getMessage(), e);
        }
        return f.apply(data);
    }

    /** Runs f while holding the exclusive lock. If f sets data.changed = true, everything is saved. */
    public static synchronized <T> T write(Function<Data, T> f) {
        try (Lock lock = new Lock(false)) {
            long v = readVersion();
            if (data == null || data.version != v) data = load(v);
            data.changed = false;
            T result;
            try {
                result = f.apply(data);
            } catch (RuntimeException e) {
                data = null; // throw away half-finished changes
                throw e;
            }
            if (data.changed) save(data);
            return result;
        } catch (IOException e) {
            data = null;
            throw new IllegalStateException("Could not write the data files: " + e.getMessage(), e);
        }
    }

    /** Changes every time anything is written - other windows poll this to know when to refresh. */
    public static synchronized long version() {
        try (Lock lock = new Lock(true)) {
            return readVersion();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- File lock (works between separate running copies of the app) ----------
    private static class Lock implements AutoCloseable {
        private final RandomAccessFile raf;
        private final FileLock lock;

        Lock(boolean shared) throws IOException {
            raf = new RandomAccessFile(LOCK_FILE, "rw");
            FileLock l;
            try {
                l = raf.getChannel().lock(0, Long.MAX_VALUE, shared);
            } catch (IOException | RuntimeException e) {
                raf.close();
                throw e;
            }
            lock = l;
        }

        @Override
        public void close() throws IOException {
            try {
                lock.release();
            } finally {
                raf.close();
            }
        }
    }

    // ---------- Text file format ----------
    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unesc(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static ArrayList<String[]> readRows(String fileName, int columns) throws IOException {
        ArrayList<String[]> rows = new ArrayList<>();
        File f = new File(DIR, fileName);
        if (!f.exists()) return rows;
        for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
            if (line.isEmpty()) continue;
            String[] p = line.split("\t", -1);
            if (p.length < columns) continue; // ignore damaged lines
            for (int i = 0; i < p.length; i++) p[i] = unesc(p[i]);
            rows.add(p);
        }
        return rows;
    }

    private static void writeRows(String fileName, ArrayList<String[]> rows) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        for (String[] r : rows) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.length; i++) {
                if (i > 0) sb.append('\t');
                sb.append(esc(r[i]));
            }
            lines.add(sb.toString());
        }
        Files.write(new File(DIR, fileName).toPath(), lines, StandardCharsets.UTF_8);
    }

    private static long readVersion() throws IOException {
        File f = new File(DIR, "meta.dat");
        if (!f.exists()) return 0;
        String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
        try {
            return s.isEmpty() ? 0 : Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Data load(long version) throws IOException {
        Data d = new Data();
        d.version = version;
        for (String[] p : readRows("students.dat", 8)) {
            UserRow u = new UserRow();
            u.id = p[0]; u.name = p[1]; u.email = p[2]; u.course = p[3];
            u.section = p[4]; u.salt = p[5]; u.hash = p[6]; u.role = p[7];
            d.users.add(u);
        }
        for (String[] p : readRows("materials.dat", 16)) {
            Material m = new Material();
            m.id = Integer.parseInt(p[0]);
            m.title = p[1];
            m.subject = p[2];
            m.type = p[3];
            m.uploaderId = p[4];
            m.date = LocalDate.parse(p[5]);
            m.rating = Double.parseDouble(p[6]);
            m.ratingCount = Integer.parseInt(p[7]);
            m.description = p[8];
            m.topics = Material.parseTopics(p[9]);
            m.fileName = p[10];
            m.fileSize = p[11];
            m.filePath = p[12];
            m.requestCount = Integer.parseInt(p[13]);
            m.status = p[14];
            m.rejectReason = p[15];
            d.materials.add(m);
        }
        for (String[] p : readRows("requests.dat", 5)) {
            RequestRow r = new RequestRow();
            r.id = Integer.parseInt(p[0]);
            r.materialId = Integer.parseInt(p[1]);
            r.requesterId = p[2];
            r.date = LocalDate.parse(p[3]);
            r.status = p[4];
            d.requests.add(r);
        }
        for (String[] p : readRows("activities.dat", 5)) {
            ActivityRow a = new ActivityRow();
            a.id = Integer.parseInt(p[0]);
            a.userId = p[1];
            a.text = p[2];
            a.created = Long.parseLong(p[3]);
            a.notify = "1".equals(p[4]);
            d.activities.add(a);
        }
        for (String[] p : readRows("adminreqs.dat", 6)) {
            AdminReqRow r = new AdminReqRow();
            r.id = Integer.parseInt(p[0]);
            r.adminId = p[1];
            r.name = p[2];
            r.email = p[3];
            r.date = LocalDate.parse(p[4]);
            r.status = p[5];
            d.adminReqs.add(r);
        }
        return d;
    }

    private static void save(Data d) throws IOException {
        ArrayList<String[]> rows = new ArrayList<>();
        for (UserRow u : d.users) rows.add(new String[]{u.id, u.name, u.email, u.course, u.section, u.salt, u.hash, u.role});
        writeRows("students.dat", rows);

        rows = new ArrayList<>();
        for (Material m : d.materials) {
            rows.add(new String[]{String.valueOf(m.id), m.title, m.subject, m.type, m.uploaderId, m.date.toString(),
                    String.valueOf(m.rating), String.valueOf(m.ratingCount), m.description, String.join("\n", m.topics),
                    m.fileName, m.fileSize, m.filePath, String.valueOf(m.requestCount), m.status, m.rejectReason});
        }
        writeRows("materials.dat", rows);

        rows = new ArrayList<>();
        for (RequestRow r : d.requests)
            rows.add(new String[]{String.valueOf(r.id), String.valueOf(r.materialId), r.requesterId, r.date.toString(), r.status});
        writeRows("requests.dat", rows);

        rows = new ArrayList<>();
        for (ActivityRow a : d.activities)
            rows.add(new String[]{String.valueOf(a.id), a.userId, a.text, String.valueOf(a.created), a.notify ? "1" : "0"});
        writeRows("activities.dat", rows);

        rows = new ArrayList<>();
        for (AdminReqRow r : d.adminReqs)
            rows.add(new String[]{String.valueOf(r.id), r.adminId, r.name, r.email, r.date.toString(), r.status});
        writeRows("adminreqs.dat", rows);

        // The version file is written LAST, so other windows never see a half-saved state.
        d.version++;
        Files.write(new File(DIR, "meta.dat").toPath(), String.valueOf(d.version).getBytes(StandardCharsets.UTF_8));
        d.changed = false;
    }

    // ---------- Passwords (salted PBKDF2 hash, never plain text) ----------
    public static String newSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    public static String hash(String saltBase64, String password) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(saltBase64), 65536, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- Sample data (only added to empty data files) ----------
    private static void seed(Data d) {
        student(d, "admin", "Admin Moderator", "admin@university.edu.ph", "Administrator", "Admin", "admin123", "admin");
        String pw = "password123";
        student(d, "2511111", "Alex Rivera", "alex.rivera@university.edu.ph", "BS Computer Science", "BSCS 2-A", pw, "student");
        student(d, "2511112", "Maria Santos", "maria@university.edu.ph", "BS Computer Science", "BSCS 2-A", pw, "student");
        student(d, "2511113", "John Cruz", "john@university.edu.ph", "BS Computer Science", "BSCS 2-B", pw, "student");
        student(d, "2511114", "Angela Reyes", "angela@university.edu.ph", "BS Mathematics", "BSM 2-A", pw, "student");
        student(d, "2511115", "Mark Garcia", "mark@university.edu.ph", "BS Information Technology", "BSIT 2-A", pw, "student");
        student(d, "2511116", "Leila Mendoza", "leila@university.edu.ph", "BS Physics", "BSP 3-A", pw, "student");
        student(d, "2511117", "Carlo Bautista", "carlo@university.edu.ph", "BS Mathematics", "BSM 3-B", pw, "student");
        student(d, "2511118", "Sofia Lopez", "sofia@university.edu.ph", "BS English", "BSE 2-A", pw, "student");
        student(d, "2511119", "Angelo Pascual", "angelo@university.edu.ph", "BS Computer Science", "BSCS 2-C", pw, "student");

        String ok = Material.APPROVED;
        material(d, "Java OOP Reviewer", "Computer Science", "Reviewer", "2511112", LocalDate.of(2026, 9, 18), 4.8, 12,
                "A comprehensive reviewer covering fundamental Object-Oriented Programming concepts including classes, objects, encapsulation, inheritance, and polymorphism. Includes detailed examples, UML diagrams, and practice exercises for each topic.",
                "Classes and Objects, Encapsulation, Inheritance, Polymorphism", "java_oop_reviewer.pdf", "2.4 MB", 8, ok);
        material(d, "Data Structures & Algorithms", "Computer Science", "Notes", "2511113", LocalDate.of(2026, 9, 16), 4.0, 9,
                "Detailed notes on arrays, linked lists, stacks, queues, trees, and graphs with visual explanations.",
                "Arrays, Linked Lists, Stacks and Queues, Trees, Graphs", "ds_algo_notes.pdf", "3.1 MB", 0, ok);
        material(d, "Calculus I Reviewer", "Mathematics", "Reviewer", "2511114", LocalDate.of(2026, 9, 14), 5.0, 15,
                "Complete coverage of limits, derivatives, and integrals with solved practice problems.",
                "Limits, Derivatives, Integrals", "calculus1_reviewer.pdf", "1.8 MB", 0, ok);
        material(d, "Database Fundamentals", "Computer Science", "Reviewer", "2511115", LocalDate.of(2026, 9, 12), 4.0, 7,
                "ER diagrams, relational model, normalization, SQL queries, and database design basics.",
                "ER Diagrams, Relational Model, Normalization, SQL Queries", "database_fundamentals.pdf", "2.0 MB", 0, ok);
        int physics = material(d, "Physics Midterm Practice Test", "Physics", "Practice Test", "2511116", LocalDate.of(2026, 9, 10), 4.0, 6,
                "50-item practice exam covering mechanics, kinematics, and Newton's laws with answer keys.",
                "Mechanics, Kinematics, Newton's Laws", "physics_midterm_test.pdf", "1.2 MB", 0, ok);
        int discrete = material(d, "Discrete Mathematics Notes", "Mathematics", "Notes", "2511117", LocalDate.of(2026, 9, 8), 5.0, 11,
                "Logic, set theory, combinatorics, graph theory, and proof techniques explained simply.",
                "Logic, Set Theory, Combinatorics, Graph Theory, Proof Techniques", "discrete_math_notes.pdf", "2.7 MB", 0, ok);
        int english = material(d, "English Grammar Review", "English", "Reviewer", "2511118", LocalDate.of(2026, 9, 5), 4.5, 4,
                "Grammar rules, sentence structure, and common errors with exercises.",
                "Parts of Speech, Sentence Structure, Subject-Verb Agreement", "english_grammar.pdf", "0.9 MB", 0, ok);

        int alexJava = material(d, "Java OOP Quick Reviewer", "Computer Science", "Reviewer", "2511111", LocalDate.of(2026, 9, 18), 4.6, 5,
                "A short one-page summary of Java OOP for last-minute reviewing.",
                "Classes, Interfaces, Abstract Classes", "java_quick_reviewer.pdf", "0.6 MB", 8, ok);
        int alexDs = material(d, "Data Structures Notes", "Computer Science", "Notes", "2511111", LocalDate.of(2026, 9, 10), 4.2, 3,
                "My class notes on lists, stacks, and queues.",
                "Lists, Stacks, Queues", "ds_notes.pdf", "1.1 MB", 5, ok);
        int alexCalc = material(d, "Calculus Reviewer", "Mathematics", "Reviewer", "2511111", LocalDate.of(2026, 9, 3), 4.0, 2,
                "Formulas and solved examples for derivatives.",
                "Derivatives, Chain Rule", "calc_reviewer.pdf", "0.8 MB", 3, ok);

        // A sample submission that is waiting for the admin
        material(d, "Computer Networks Reviewer", "Computer Science", "Reviewer", "2511115", LocalDate.now(), 0, 0,
                "OSI model, TCP/IP, IP addressing and subnetting, with sample problems.",
                "OSI Model, TCP/IP, Subnetting", "networks_reviewer.pdf", "1.5 MB", 0, Material.PENDING);

        request(d, alexJava, "2511113", LocalDate.of(2026, 9, 18), "Pending");
        request(d, alexCalc, "2511112", LocalDate.of(2026, 9, 17), "Pending");
        request(d, alexDs, "2511119", LocalDate.of(2026, 9, 16), "Approved");
        request(d, physics, "2511111", LocalDate.of(2026, 9, 15), "Pending");
        request(d, discrete, "2511111", LocalDate.of(2026, 9, 14), "Approved");
        request(d, english, "2511111", LocalDate.of(2026, 9, 5), "Completed");

        long now = System.currentTimeMillis();
        d.addActivity("2511111", "John Cruz requested your Java OOP Quick Reviewer", false, now - 120 * 60_000L);
        d.addActivity("2511111", "Your request for Discrete Mathematics Notes was approved", false, now - 300 * 60_000L);
        d.addActivity("2511111", "New reviewer added: Physics Midterm Practice Test", false, now - 1440 * 60_000L);
        d.addActivity("admin", "Welcome, Admin! New submissions will appear under Moderation.", false, now - 5 * 60_000L);
        d.changed = true;
    }

    private static void student(Data d, String id, String name, String email, String course, String section, String pw, String role) {
        UserRow u = new UserRow();
        u.id = id; u.name = name; u.email = email; u.course = course; u.section = section; u.role = role;
        u.salt = newSalt();
        u.hash = hash(u.salt, pw);
        d.users.add(u);
    }

    private static int material(Data d, String title, String subject, String type, String uploaderId, LocalDate date, double rating,
                                int ratingCount, String desc, String topics, String file, String size, int requestCount, String status) {
        Material m = new Material();
        m.id = d.nextMaterialId();
        m.title = title; m.subject = subject; m.type = type; m.uploaderId = uploaderId; m.date = date;
        m.rating = rating; m.ratingCount = ratingCount; m.description = desc;
        m.topics = Material.parseTopics(topics);
        m.fileName = file; m.fileSize = size; m.filePath = "";
        m.requestCount = requestCount; m.status = status; m.rejectReason = "";
        d.materials.add(m);
        return m.id;
    }

    private static void request(Data d, int materialId, String requesterId, LocalDate date, String status) {
        RequestRow r = new RequestRow();
        r.id = d.nextRequestId();
        r.materialId = materialId; r.requesterId = requesterId; r.date = date; r.status = status;
        d.requests.add(r);
    }
}