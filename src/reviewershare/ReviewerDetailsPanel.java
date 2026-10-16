package reviewershare;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** Details screen for one material, with Request Access / Cancel Request. */
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

    private final JLabel upName = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel upSection = Ui.label("", Ui.SMALL, Ui.MUTED);
    private final JLabel upShared = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel upAvg = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel fileName = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel fileSize = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel format = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel requests = Ui.label("", Ui.BOLD, Ui.TEXT);

    private final CardLayout actionCards = new CardLayout();
    private final JPanel actionHolder = Ui.clear(new JPanel(actionCards));
    private final JLabel actionNote = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final JButton viewFile = Ui.primaryButton("View File");
    private final JLabel fileNote = Ui.label(" ", Ui.SMALL, Ui.MUTED);

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

        JPanel body = Ui.clear(new JPanel(new GridLayout(2, 1, 0, 12)));
        body.add(desc);
        body.add(topics);

        JPanel card = Ui.card();
        card.setLayout(new BorderLayout());
        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** Adds a component to a vertical BoxLayout panel, left aligned, followed by a small gap. */
    private void put(JPanel p, JComponent c, int gap) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (c instanceof JPanel || c instanceof JButton) c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        p.add(c);
        p.add(Box.createVerticalStrut(gap));
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

        viewFile.addActionListener(e -> Ui.openMaterialFile(this, DataStore.findMaterial(currentId)));

        JPanel file = Ui.card();
        file.setLayout(new BoxLayout(file, BoxLayout.Y_AXIS));
        put(file, Ui.label("File Information", Ui.H2, Ui.TEXT), 8);
        put(file, row("File Name", fileName), 6);
        put(file, row("File Size", fileSize), 6);
        put(file, row("Format", format), 6);
        put(file, row("Requests", requests), 12);
        put(file, viewFile, 4);
        put(file, fileNote, 10);
        put(file, actionHolder, 0);

        JPanel sdg = Ui.pastel(Ui.P_YELLOW);
        sdg.setLayout(new GridLayout(0, 1, 0, 4));
        sdg.add(Ui.label("Supporting SDG 4", Ui.BOLD, Ui.TEXT));
        sdg.add(Ui.label("<html>This material supports Quality Education for all students.</html>", Ui.SMALL, Ui.MUTED));

        JPanel side = Ui.clear(new JPanel());
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        put(side, upl, 14);
        put(side, file, 14);
        put(side, sdg, 0);

        JPanel wrap = Ui.clear(new JPanel(new BorderLayout()));
        wrap.add(side, BorderLayout.NORTH);
        wrap.setPreferredSize(new Dimension(320, 0));
        return wrap;
    }

    private static String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
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

    /** Remember which material to show, then fill in the labels. */
    public void showMaterial(Material m) {
        currentId = m.id;
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
            fileNote.setText(" ");
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
                : m.getStars() + "  " + String.format("%.1f", m.rating) + " out of 5 - " + m.ratingCount + " ratings");
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
        upSection.setText(up != null ? up.section : "N/A");
        upShared.setText(String.valueOf(DataStore.materialsSharedBy(m.uploaderId)));
        double avg = DataStore.averageRatingOf(m.uploaderId);
        upAvg.setText(avg == 0 ? "-" : String.format("%.1f ★", avg));
        fileName.setText(shorten(m.fileName, 26));
        fileName.setToolTipText(m.fileName);
        fileSize.setText(m.fileSize);
        format.setText(m.getFormat());
        requests.setText(m.requestCount + " students");

        // Who may open the file? Admins and the owner always, others after the owner approved their request.
        boolean hasFile = m.filePath != null && !m.filePath.isEmpty() && new File(m.filePath).exists();
        boolean canView = DataStore.canViewFile(m);
        boolean owner = m.uploaderId.equals(me.id);
        viewFile.setText(me.isAdmin() || owner ? "Open File" : "View File");
        viewFile.setEnabled(hasFile && canView);
        if (!hasFile) fileNote.setText("No file is attached to this material.");
        else if (me.isAdmin()) fileNote.setText("Admins can always view files.");
        else if (owner) fileNote.setText("This is your own file.");
        else if (canView) fileNote.setText("Your request was approved. You can view this file.");
        else fileNote.setText("Request access to view this file once the owner approves.");

        // Which action is available?
        if (me.isAdmin()) {
            actionNote.setText("Admins moderate - they don't request.");
            actionCards.show(actionHolder, "none");
        } else if (m.uploaderId.equals(me.id)) {
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
    }

    /** Creates a Pending request (blocks own material and duplicates). */
    private void requestAccess() {
        Material m = DataStore.findMaterial(currentId);
        if (m == null) {
            JOptionPane.showMessageDialog(this, "This reviewer is no longer available.");
            main.refreshAll();
            return;
        }
        Student me = DataStore.currentUser;
        if (m.uploaderId.equals(me.id)) {
            JOptionPane.showMessageDialog(this, "You cannot request your own material.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!Material.APPROVED.equals(m.status)) {
            JOptionPane.showMessageDialog(this, "This material is not available yet.", "Not allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (DataStore.hasRequested(m.id)) {
            JOptionPane.showMessageDialog(this, "You already requested this material.", "Duplicate request", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DataStore.addRequest(m);
        JOptionPane.showMessageDialog(this, "Request sent to " + m.uploaderName + "! Status: Pending.");
        main.refreshAll();
    }

    /** Cancels my own Pending request for this material. */
    private void cancelRequest() {
        Request r = DataStore.pendingRequestFor(currentId);
        if (r == null) {
            JOptionPane.showMessageDialog(this, "You have no pending request for this material.");
            main.refreshAll();
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Cancel your request for \"" + r.materialTitle + "\"?", "Cancel request",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (DataStore.cancelRequest(r)) {
            JOptionPane.showMessageDialog(this, "Your request was cancelled.");
        } else {
            JOptionPane.showMessageDialog(this, "This request can no longer be cancelled - its status just changed.");
        }
        main.refreshAll();
    }
}