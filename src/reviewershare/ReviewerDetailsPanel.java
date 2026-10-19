package reviewershare;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/** Details screen for one material: files, reviews, Request Access / Cancel Request, and a report button. */
public class ReviewerDetailsPanel extends JPanel {
    private final MainFrame main;
    private int currentId = -1;

    private final JLabel typeSubject = Ui.label("", Ui.SMALL, Ui.BRAND);
    private final JLabel titleLabel = Ui.label("", Ui.H1, Ui.TEXT);
    private final JLabel subjectVal = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel typeVal = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel byVal = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel dateVal = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel ratingLabel = Ui.label("", Ui.BOLD, Ui.ORANGE);
    private final JLabel statusLabel = Ui.label(" ", Ui.BOLD, Ui.WARN);
    private final JTextArea descArea = Ui.textArea(4);
    private final JTextArea topicsArea = Ui.textArea(4);
    private final Ui.Tabs tabs = new Ui.Tabs();

    private final JLabel upName = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel upSection = Ui.label("", Ui.SMALL, Ui.MUTED);
    private final JLabel upShared = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel upAvg = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel fileCount = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel format = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel requests = Ui.label("", Ui.BOLD, Ui.TEXT);

    private final DefaultListModel<String> fileModel = new DefaultListModel<>();
    private final JList<String> fileList = new JList<>(fileModel);
    private ArrayList<Material.FileInfo> shownFiles = new ArrayList<>();
    private String fileSig = "";

    private final CardLayout actionCards = new CardLayout();
    private final JPanel actionHolder = Ui.clear(new JPanel(actionCards));
    private final JLabel actionNote = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final JButton viewFile = Ui.primaryButton("View File");
    private final JLabel fileNote = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final JButton reportBtn = Ui.plainButton("Report this reviewer");

    // Reviews
    private final JLabel reviewSummary = Ui.label(" ", Ui.BOLD, Ui.ORANGE);
    private final JPanel reviewList = Ui.clear(new JPanel());
    private final JComboBox<String> starBox = Ui.style(new JComboBox<>(new String[]{
            "5 stars - Excellent", "4 stars - Very good", "3 stars - Good", "2 stars - Fair", "1 star - Poor"}));
    private final JTextArea commentArea = Ui.textArea(3);
    private final JButton postBtn = Ui.primaryButton("Post Review");
    private final JButton deleteMine = Ui.dangerButton("Delete My Review");
    private final JLabel reviewNote = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final Ui.RoundPanel reviewForm = Ui.pastel(Ui.P_PURPLE);
    private String reviewSig = "";
    private int formFor = -1;

    public ReviewerDetailsPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout(14, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        JButton back = Ui.plainButton("← Back to Library");
        back.addActionListener(e -> main.showPage("Reviewers"));
        JPanel backRow = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        backRow.add(back);
        add(backRow, BorderLayout.NORTH);

        add(buildMainCard(), BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
    }

    // ---------- Main card (header + Overview / Reviews tabs) ----------
    private JPanel buildMainCard() {
        descArea.setEditable(false);
        topicsArea.setEditable(false);

        JPanel tiles = Ui.clear(new JPanel(new GridLayout(1, 4, 10, 0)));
        tiles.add(tile(Ui.P_PURPLE, "Subject", subjectVal));
        tiles.add(tile(Ui.P_PEACH, "Material Type", typeVal));
        tiles.add(tile(Ui.P_BLUE, "Uploaded By", byVal));
        tiles.add(tile(Ui.P_MINT, "Date Added", dateVal));

        JPanel header = Ui.clear(new JPanel());
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{typeSubject, titleLabel, tiles, ratingLabel, statusLabel}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        header.add(typeSubject);
        header.add(Box.createVerticalStrut(2));
        header.add(titleLabel);
        header.add(Box.createVerticalStrut(14));
        header.add(tiles);
        header.add(Box.createVerticalStrut(10));
        header.add(ratingLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(statusLabel);
        header.add(Box.createVerticalStrut(10));

        JPanel desc = Ui.clear(new JPanel(new BorderLayout(0, 4)));
        desc.add(Ui.label("Description", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        desc.add(Ui.scrollBox(descArea), BorderLayout.CENTER);

        JPanel topics = Ui.clear(new JPanel(new BorderLayout(0, 4)));
        topics.add(Ui.label("Topics Covered", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        topics.add(Ui.scrollBox(topicsArea), BorderLayout.CENTER);

        JPanel overview = Ui.clear(new JPanel(new GridLayout(2, 1, 0, 12)));
        overview.add(desc);
        overview.add(topics);

        tabs.addTab("Overview", overview);
        tabs.addTab("Reviews (0)", buildReviewsPanel());

        JPanel card = Ui.card();
        card.setLayout(new BorderLayout(0, 4));
        card.add(header, BorderLayout.NORTH);
        card.add(tabs, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildReviewsPanel() {
        reviewList.setLayout(new BoxLayout(reviewList, BoxLayout.Y_AXIS));
        JPanel wrapper = Ui.clear(new JPanel(new BorderLayout()));
        wrapper.add(reviewList, BorderLayout.NORTH);
        JScrollPane sp = Ui.scroll(wrapper);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // The form to write (or change) my review
        Ui.limitInput(commentArea, 500, false);
        reviewForm.setLayout(new BoxLayout(reviewForm, BoxLayout.Y_AXIS));
        reviewForm.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel caption = Ui.label("Your review", Ui.BOLD, Ui.TEXT);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        starBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        starBox.setMaximumSize(new Dimension(240, 36));
        JScrollPane commentScroll = Ui.scrollBox(commentArea);
        commentScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        commentScroll.setPreferredSize(new Dimension(300, 74));
        commentScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        postBtn.addActionListener(e -> postReview());
        deleteMine.addActionListener(e -> deleteMyReview());
        JPanel btns = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        btns.add(postBtn);
        btns.add(deleteMine);
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        reviewForm.add(caption);
        reviewForm.add(Box.createVerticalStrut(6));
        reviewForm.add(starBox);
        reviewForm.add(Box.createVerticalStrut(6));
        reviewForm.add(commentScroll);
        reviewForm.add(Box.createVerticalStrut(8));
        reviewForm.add(btns);

        JPanel south = Ui.clear(new JPanel(new BorderLayout(0, 6)));
        south.add(reviewForm, BorderLayout.CENTER);
        south.add(reviewNote, BorderLayout.SOUTH);

        JPanel p = Ui.clear(new JPanel(new BorderLayout(0, 8)));
        p.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        p.add(reviewSummary, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    // ---------- Side panel ----------
    /** Adds a component to a vertical BoxLayout panel, left aligned, followed by a small gap. */
    private void put(JPanel p, JComponent c, int gap) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JPanel || c instanceof JButton || c instanceof JScrollPane)
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        p.add(c);
        p.add(Box.createVerticalStrut(gap));
    }

    /** Sets the small note under the file buttons (wraps to two lines when needed). */
    private void setFileNote(String text) {
        fileNote.setText("<html><body style='width:255px'>" + text + "</body></html>");
    }

    private JPanel buildSidePanel() {
        JPanel upl = Ui.card();
        upl.setLayout(new BoxLayout(upl, BoxLayout.Y_AXIS));
        put(upl, Ui.label("About the Uploader", Ui.H2, Ui.TEXT), 8);
        put(upl, upName, 2);
        put(upl, upSection, 8);
        put(upl, row("Materials Shared", upShared), 6);
        put(upl, row("Avg. Rating", upAvg), 0);

        JButton request = Ui.primaryButton("Request Access");
        request.addActionListener(e -> requestAccess());
        JButton cancel = Ui.dangerButton("Cancel My Request");
        cancel.addActionListener(e -> cancelRequest());
        JPanel none = Ui.clear(new JPanel(new BorderLayout()));
        none.add(actionNote, BorderLayout.CENTER);
        actionHolder.add(request, "request");
        actionHolder.add(cancel, "cancel");
        actionHolder.add(none, "none");

        fileList.setBackground(Ui.FIELD);
        fileList.setForeground(Ui.TEXT);
        fileList.setFont(Ui.SMALL);
        fileList.setFixedCellHeight(24);
        fileList.setSelectionBackground(new Color(0x2F3590));
        fileList.setSelectionForeground(Color.WHITE);
        JScrollPane fileScroll = Ui.scrollBox(fileList);
        fileScroll.setPreferredSize(new Dimension(280, 96));
        viewFile.addActionListener(e -> openSelectedFile());

        JPanel file = Ui.card();
        file.setLayout(new BoxLayout(file, BoxLayout.Y_AXIS));
        put(file, Ui.label("Files", Ui.H2, Ui.TEXT), 8);
        put(file, row("Attached", fileCount), 6);
        put(file, row("Format", format), 6);
        put(file, row("Requests", requests), 10);
        put(file, fileScroll, 8);
        put(file, viewFile, 4);
        put(file, fileNote, 10);
        put(file, actionHolder, 0);

        JPanel sdg = Ui.pastel(Ui.P_YELLOW);
        sdg.setLayout(new BoxLayout(sdg, BoxLayout.Y_AXIS));
        put(sdg, Ui.label("Supporting SDG 4", Ui.BOLD, Ui.TEXT), 4);
        put(sdg, Ui.label("<html><body style='width:215px'>This material supports Quality Education for all students.</body></html>", Ui.SMALL, Ui.MUTED), 0);

        reportBtn.addActionListener(e -> reportMaterial());

        JPanel side = Ui.clear(new JPanel());
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        put(side, upl, 14);
        put(side, file, 14);
        put(side, sdg, 10);
        put(side, reportBtn, 0);

        JPanel wrap = Ui.clear(new JPanel(new BorderLayout()));
        wrap.add(side, BorderLayout.NORTH);
        wrap.setPreferredSize(new Dimension(320, 0));
        return wrap;
    }

    private JPanel tile(Color color, String caption, JLabel value) {
        JPanel p = Ui.pastel(color);
        p.setLayout(new GridLayout(2, 1));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 8));
        p.add(Ui.label(caption, Ui.SMALL, Ui.MUTED));
        p.add(value);
        return p;
    }

    private JPanel row(String caption, JLabel value) {
        JPanel p = Ui.clear(new JPanel(new BorderLayout()));
        p.add(Ui.label(caption, Ui.SMALL, Ui.MUTED), BorderLayout.WEST);
        p.add(value, BorderLayout.EAST);
        return p;
    }

    private static String stars(int n) {
        n = Math.max(0, Math.min(5, n));
        return "★".repeat(n) + "☆".repeat(5 - n);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
    }

    /** Remember which material to show, then fill in the labels. */
    public void showMaterial(Material m) {
        currentId = m.id;
        formFor = -1;
        reviewSig = "";
        fileSig = "";
        refresh();
    }

    /** Fills every label from the database copy of the current material. */
    public void refresh() {
        if (currentId < 0) return;
        Material m = DataStore.findMaterial(currentId);
        if (m == null) {
            titleLabel.setText("This reviewer is no longer available.");
            actionNote.setText("It was removed.");
            actionCards.show(actionHolder, "none");
            viewFile.setEnabled(false);
            setFileNote(" ");
            reportBtn.setVisible(false);
            return;
        }
        Student me = DataStore.currentUser;
        typeSubject.setText(m.type + "  •  " + m.subject);
        titleLabel.setText(m.title);
        subjectVal.setText(m.subject);
        typeVal.setText(m.type);
        byVal.setText(m.uploaderName);
        dateVal.setText(m.getDateText());
        ratingLabel.setText(m.ratingCount == 0 ? "No ratings yet"
                : m.getStars() + "  " + String.format("%.1f", m.rating) + " out of 5 - " + m.ratingCount + (m.ratingCount == 1 ? " rating" : " ratings"));
        if (Material.PENDING.equals(m.status)) {
            statusLabel.setForeground(Ui.WARN);
            statusLabel.setText("⏳ Waiting for admin approval - only you and admins can see this.");
        } else if (Material.REJECTED.equals(m.status)) {
            statusLabel.setForeground(Ui.RED);
            statusLabel.setText("Rejected by admin: " + m.rejectReason);
        } else {
            statusLabel.setText(" ");
        }
        descArea.setText(m.description);
        descArea.setCaretPosition(0);

        StringBuilder sb = new StringBuilder();
        for (String t : m.topics) sb.append("✓ ").append(t).append("\n");
        topicsArea.setText(sb.toString());
        topicsArea.setCaretPosition(0);

        Student up = DataStore.findStudent(m.uploaderId);
        upName.setText(m.uploaderName);
        upSection.setText(up != null ? up.program() : "N/A");
        upShared.setText(String.valueOf(DataStore.materialsSharedBy(m.uploaderId)));
        double avg = DataStore.averageRatingOf(m.uploaderId);
        upAvg.setText(avg == 0 ? "-" : String.format("%.1f ★", avg));
        requests.setText(m.requestCount + " students");

        // Files
        boolean hasFile = !m.files.isEmpty();
        String sig = m.id + "|" + m.fileName;
        for (Material.FileInfo f : m.files) sig += "|" + f.id + f.name;
        if (!sig.equals(fileSig)) {
            fileSig = sig;
            shownFiles = new ArrayList<>(m.files);
            fileModel.clear();
            for (Material.FileInfo f : m.files) fileModel.addElement(" " + f.name + "  (" + f.size + ")");
            if (!hasFile && m.fileName != null && !m.fileName.equals("No file attached"))
                fileModel.addElement(" " + m.fileName + "  (sample, no file attached)");
            if (hasFile) fileList.setSelectedIndex(0);
        }
        fileCount.setText(hasFile ? m.files.size() + (m.files.size() == 1 ? " file" : " files") : "None");
        format.setText(m.getFormat());

        // Who may open the files? Admins and the owner always, others after the owner approved their request.
        boolean canView = DataStore.canViewFile(m);
        boolean owner = m.uploaderId.equals(me.id);
        viewFile.setText(me.isAdmin() || owner ? "Open Selected File" : "View Selected File");
        viewFile.setEnabled(hasFile && canView);
        if (!hasFile) setFileNote("No file is attached to this material.");
        else if (me.isAdmin()) setFileNote("Admins can always view files.");
        else if (owner) setFileNote("These are your own files.");
        else if (canView) setFileNote("Your request was approved. You can view these files.");
        else setFileNote("Request access to view the files once the owner approves.");

        // Which action is available?
        if (me.isAdmin()) {
            actionNote.setText("Admins moderate - they don't request.");
            actionCards.show(actionHolder, "none");
        } else if (owner) {
            actionNote.setText("This is your own material.");
            actionCards.show(actionHolder, "none");
        } else if (!Material.APPROVED.equals(m.status)) {
            actionNote.setText("Not available for requests.");
            actionCards.show(actionHolder, "none");
        } else if (DataStore.pendingRequestFor(m.id) != null) {
            actionCards.show(actionHolder, "cancel");
        } else if (DataStore.hasRequested(m.id)) {
            actionNote.setText("You already requested this material.");
            actionCards.show(actionHolder, "none");
        } else {
            actionCards.show(actionHolder, "request");
        }
        reportBtn.setVisible(!me.isAdmin() && !owner && Material.APPROVED.equals(m.status));

        refreshReviews(m, me, canView);
    }

    // ---------- Reviews ----------
    private void refreshReviews(Material m, Student me, boolean canView) {
        ArrayList<DataStore.Review> list = DataStore.reviewsFor(m.id);
        boolean canReview = DataStore.canReview(m);
        tabs.setTitle(1, "Reviews (" + list.size() + ")");
        reviewSummary.setText(m.ratingCount == 0 ? "No ratings yet"
                : m.getStars() + "  " + String.format("%.1f", m.rating) + " out of 5 from " + m.ratingCount + (m.ratingCount == 1 ? " rating, " : " ratings, ")
                + list.size() + (list.size() == 1 ? " written review" : " written reviews"));

        StringBuilder sig = new StringBuilder(me.id + "|" + me.isAdmin());
        for (DataStore.Review r : list) sig.append('|').append(r.id).append(r.stars).append(r.comment.hashCode()).append(r.userName);
        if (!sig.toString().equals(reviewSig)) {
            reviewSig = sig.toString();
            reviewList.removeAll();
            if (list.isEmpty()) {
                reviewList.add(Ui.label("No written reviews yet. Students can review after the owner approves their request.", Ui.SMALL, Ui.MUTED));
            }
            for (DataStore.Review r : list) {
                JPanel card = reviewCard(r, me);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
                reviewList.add(card);
                reviewList.add(Box.createVerticalStrut(8));
            }
            reviewList.revalidate();
            reviewList.repaint();
        }

        reviewForm.setVisible(canReview);
        reviewNote.setVisible(!canReview);
        if (me.isAdmin()) reviewNote.setText("Admins can remove reviews that break the rules, using the Remove button.");
        else if (m.uploaderId.equals(me.id)) reviewNote.setText("You cannot review your own material.");
        else if (!Material.APPROVED.equals(m.status)) reviewNote.setText("Reviews open once this material is approved.");
        else reviewNote.setText("You can write a review after the owner approves your request.");

        if (canReview && formFor != m.id) {
            formFor = m.id;
            DataStore.Review mine = DataStore.myReview(m.id);
            if (mine != null) {
                starBox.setSelectedIndex(5 - mine.stars);
                commentArea.setText(mine.comment);
                postBtn.setText("Update Review");
                deleteMine.setVisible(true);
            } else {
                starBox.setSelectedIndex(0);
                commentArea.setText("");
                postBtn.setText("Post Review");
                deleteMine.setVisible(false);
            }
        }
    }

    private JPanel reviewCard(DataStore.Review r, Student me) {
        Ui.RoundPanel c = Ui.pastel(Ui.P_PURPLE);
        c.setLayout(new BorderLayout(0, 4));
        c.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        boolean mine = r.userId.equals(me.id);

        JPanel head = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)));
        head.add(Ui.label(r.userName + (mine ? " (you)" : ""), Ui.BOLD, Ui.TEXT));
        head.add(Ui.label(stars(r.stars), Ui.BODY, Ui.ORANGE));
        head.add(Ui.label(r.getDateText(), Ui.SMALL, Ui.MUTED));
        JPanel top = Ui.clear(new JPanel(new BorderLayout()));
        top.add(head, BorderLayout.WEST);
        if (me.isAdmin() || mine) {
            JButton remove = Ui.plainButton("Remove");
            remove.addActionListener(e -> {
                if (Ui.showConfirm(this, "Remove this review? The rating will be recalculated.", "Remove review",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
                if (!DataStore.deleteReview(r.id)) Ui.showMessage(this, "That review no longer exists.");
                formFor = -1;
                main.refreshAll();
            });
            top.add(remove, BorderLayout.EAST);
        }
        c.add(top, BorderLayout.NORTH);
        if (!r.comment.isEmpty())
            c.add(Ui.label("<html><body style='width:430px'>" + esc(r.comment) + "</body></html>", Ui.BODY, Ui.TEXT), BorderLayout.CENTER);
        return c;
    }

    private void postReview() {
        String comment = commentArea.getText().trim();
        int stars = 5 - starBox.getSelectedIndex();
        String problem = DataStore.saveReview(currentId, stars, comment);
        if (problem != null) {
            Ui.showMessage(this, problem, "Cannot post review", JOptionPane.WARNING_MESSAGE);
        } else {
            Ui.showMessage(this, "Your review was posted. Thank you for helping your classmates!", "Review posted", Ui.SUCCESS_MESSAGE);
        }
        formFor = -1;
        main.refreshAll();
    }

    private void deleteMyReview() {
        DataStore.Review mine = DataStore.myReview(currentId);
        if (mine == null) return;
        if (Ui.showConfirm(this, "Delete your review?", "Delete review", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;
        DataStore.deleteReview(mine.id);
        formFor = -1;
        main.refreshAll();
    }

    // ---------- Files and reports ----------
    private void openSelectedFile() {
        Material m = DataStore.findMaterial(currentId);
        if (m == null) return;
        int i = fileList.getSelectedIndex();
        if (i < 0 && m.files.size() == 1) i = 0;
        if (i < 0 || i >= m.files.size()) {
            Ui.showMessage(this, "Please select a file in the list first.", "Select a file", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Ui.openMaterialFile(this, m, m.files.get(i));
    }

    /** Lets a student tell the admins about copyrighted, plagiarized or leaked material. */
    private void reportMaterial() {
        Material m = DataStore.findMaterial(currentId);
        if (m == null) return;
        JComboBox<String> reason = Ui.style(new JComboBox<>(DataStore.REPORT_REASONS));
        JTextArea details = Ui.textArea(4);
        Ui.limitInput(details, 500, false);
        JScrollPane ds = Ui.scrollBox(details);
        ds.setPreferredSize(new Dimension(380, 96));
        JPanel box = Ui.clear(new JPanel());
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{reason, ds}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l1 = Ui.label("What is the problem?", Ui.BOLD, Ui.TEXT);
        JLabel l2 = Ui.label("Tell the admins more (at least 10 characters)", Ui.BOLD, Ui.TEXT);
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);
        reason.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        box.add(l1);
        box.add(Box.createVerticalStrut(4));
        box.add(reason);
        box.add(Box.createVerticalStrut(10));
        box.add(l2);
        box.add(Box.createVerticalStrut(4));
        box.add(ds);
        int r = Ui.showConfirm(this, box, "Report \"" + m.title + "\"", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        String text = details.getText().trim();
        if (text.length() < 10) {
            Ui.showMessage(this, "Please describe the problem so the admins know what to check (at least 10 characters).",
                    "More details needed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String problem = DataStore.reportMaterial(m.id, (String) reason.getSelectedItem(), text);
        if (problem != null) Ui.showMessage(this, problem, "Cannot send report", JOptionPane.WARNING_MESSAGE);
        else Ui.showMessage(this, "Your report was sent to the admins. They will review this reviewer.", "Report sent", Ui.SUCCESS_MESSAGE);
        main.refreshAll();
    }

    // ---------- Requests ----------
    /** Creates a Pending request (blocks own material and duplicates). */
    private void requestAccess() {
        Material m = DataStore.findMaterial(currentId);
        if (m == null) {
            Ui.showMessage(this, "This reviewer is no longer available.");
            main.refreshAll();
            return;
        }
        Student me = DataStore.currentUser;
        if (m.uploaderId.equals(me.id)) {
            Ui.showMessage(this, "You cannot request your own material.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Material.APPROVED.equals(m.status)) {
            Ui.showMessage(this, "This material is not available yet.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (DataStore.hasRequested(m.id)) {
            Ui.showMessage(this, "You already requested this material.", "Duplicate request", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DataStore.addRequest(m);
        Ui.showMessage(this, "Request sent to " + m.uploaderName + "! Status: Pending.", "Request sent", Ui.SUCCESS_MESSAGE);
        main.refreshAll();
    }

    /** Cancels my own Pending request for this material. */
    private void cancelRequest() {
        Request r = DataStore.pendingRequestFor(currentId);
        if (r == null) {
            Ui.showMessage(this, "You have no pending request for this material.");
            main.refreshAll();
            return;
        }
        if (Ui.showConfirm(this, "Cancel your request for \"" + r.materialTitle + "\"?", "Cancel request",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (DataStore.cancelRequest(r)) {
            Ui.showMessage(this, "Your request was cancelled.", "Request cancelled", Ui.SUCCESS_MESSAGE);
        } else {
            Ui.showMessage(this, "This request can no longer be cancelled - its status just changed.");
        }
        main.refreshAll();
    }
}