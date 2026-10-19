package reviewershare;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

/** A shared study material (Reviewer, Notes, or Practice Test). */
public class Material {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    // Moderation status
    public static final String PENDING = "Pending Review";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";

    public int id;
    public String title, subject, type, uploaderName, uploaderId, description, fileName, fileSize, filePath;
    public String status = APPROVED;
    public String rejectReason = "";
    public LocalDate date;
    public double rating;
    public int ratingCount, requestCount;
    public ArrayList<String> topics = new ArrayList<>();
    public ArrayList<FileInfo> files = new ArrayList<>();

    /** One attached file (a material can have several). */
    public static class FileInfo {
        public int id;
        public String name, size, path;
    }

    /** Splits a text on commas or new lines into a clean list of topics. */
    public static ArrayList<String> parseTopics(String text) {
        ArrayList<String> list = new ArrayList<>();
        if (text == null) return list;
        for (String part : text.split("[,\\n]")) {
            if (!part.trim().isEmpty()) list.add(part.trim());
        }
        return list;
    }

    /** Rating shown as text stars, e.g. ★★★★☆ */
    public String getStars() {
        if (ratingCount == 0) return "No ratings yet";
        int full = Math.max(0, Math.min(5, (int) Math.round(rating)));
        return "★".repeat(full) + "☆".repeat(5 - full);
    }

    /** File format guessed from the file name. */
    public String getFormat() {
        String shown = files.isEmpty() ? fileName : files.get(0).name;
        if (files.size() > 1) return files.size() + " files";
        int dot = shown.lastIndexOf('.');
        if (dot < 0) return "-";
        String ext = shown.substring(dot + 1).toLowerCase();
        if (ext.equals("pdf")) return "PDF Document";
        if (ext.equals("docx") || ext.equals("doc")) return "Word Document";
        if (ext.equals("pptx") || ext.equals("ppt")) return "Presentation";
        return ext.toUpperCase() + " File";
    }

    public String getDateText() {
        return date.format(DATE_FORMAT);
    }
}