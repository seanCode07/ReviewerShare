package reviewershare;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Admin moderation: review submitted reviewers and approve or reject them. Updates live. */
public class AdminPanel extends JPanel {
    private final MainFrame main;
    private final JLabel pendingVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel approvedVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel rejectedVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel studentsVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel adminReqVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final Ui.Ring pendingRing = new Ui.Ring(Ui.CORAL, 64);
    private final Ui.Ring approvedRing = new Ui.Ring(Ui.ACCENT, 64);
    private final Ui.Ring rejectedRing = new Ui.Ring(Ui.BRAND, 64);

    private final DefaultTableModel pendModel = Ui.model("Title", "Subject", "Type", "Uploaded By", "Date Submitted");
    private final DefaultTableModel apprModel = Ui.model("Title", "Subject", "Type", "Uploaded By", "Date");
    private final DefaultTableModel rejModel = Ui.model("Title", "Subject", "Uploaded By", "Date", "Reason");
    private final DefaultTableModel admModel = Ui.model("Admin ID", "Name", "Email", "Requested", "Status");
    private final JTable admTable = new JTable(admModel);
    private ArrayList<DataStore.AdminRequest> admList = new ArrayList<>();
    private final JTable pendTable = new JTable(pendModel);
    private final JTable apprTable = new JTable(apprModel);
    private final JTable rejTable = new JTable(rejModel);
    private ArrayList<Material> pendList = new ArrayList<>();
    private ArrayList<Material> apprList = new ArrayList<>();
    private ArrayList<Material> rejList = new ArrayList<>();
    private final Ui.Tabs tabs = new Ui.Tabs();

    // Preview card
    private final JLabel pvTitle = Ui.label("Select a submission", Ui.H2, Ui.TEXT);
    private final JLabel pvMeta = Ui.label(" ", Ui.SMALL, Ui.BRAND);
    private final JLabel pvBy = Ui.label(" ", Ui.SMALL, Ui.TEXT);
    private final JLabel pvFile = Ui.label(" ", Ui.SMALL, Ui.TEXT);
    private final JLabel pvStatus = Ui.label(" ", Ui.BOLD, Ui.TEXT);
    private final JTextArea pvDesc = Ui.textArea(5);
    private final JTextArea pvTopics = Ui.textArea(4);
    private int previewId = -1;
    private boolean loading = false;

    public AdminPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout(0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // Stat cards
        JPanel stats = Ui.clear(new JPanel(new GridLayout(1, 5, 14, 0)));
        stats.add(stat(Ui.P_PEACH, Ui.CORAL, "Pending Review", pendingVal, pendingRing));
        stats.add(stat(Ui.P_MINT, Ui.ACCENT, "Approved", approvedVal, approvedRing));
        stats.add(stat(Ui.P_PURPLE, Ui.BRAND, "Rejected", rejectedVal, rejectedRing));
        stats.add(stat(Ui.P_BLUE, Ui.BLUE, "Students", studentsVal, null));
        stats.add(stat(Ui.P_YELLOW, Ui.WARN, "Admin Requests", adminReqVal, null));
        JPanel top = Ui.clear(new JPanel(new BorderLayout(0, 8)));
        top.add(Ui.label("Review each submission before it goes live in the library.", Ui.BODY, Ui.MUTED), BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        for (JTable t : new JTable[]{pendTable, apprTable, rejTable, admTable}) Ui.styleTable(t);
        listen(pendTable, () -> pendList);
        listen(apprTable, () -> apprList);
        listen(rejTable, () -> rejList);

        JButton approve = Ui.primaryButton("Approve");
        JButton reject = Ui.dangerButton("Reject...");
        JButton open = Ui.plainButton("Open Attached File");
        approve.addActionListener(e -> approve(pendTable, pendList));
        reject.addActionListener(e -> reject(pendTable, pendList));
        open.addActionListener(e -> openFile(selected(pendTable, pendList)));

        JButton takeDown = Ui.dangerButton("Take Down");
        JButton open2 = Ui.plainButton("Open Attached File");
        takeDown.addActionListener(e -> takeDown(selected(apprTable, apprList)));
        open2.addActionListener(e -> openFile(selected(apprTable, apprList)));

        JButton approve3 = Ui.primaryButton("Approve Instead");
        approve3.addActionListener(e -> approve(rejTable, rejList));

        tabs.addTab("Pending Review", tabPanel(pendTable, approve, reject, open));
        tabs.addTab("Approved", tabPanel(apprTable, takeDown, open2));
        tabs.addTab("Rejected", tabPanel(rejTable, approve3));

        JButton approveAdm = Ui.primaryButton("Approve Admin Access");
        JButton rejectAdm = Ui.dangerButton("Reject");
        approveAdm.addActionListener(e -> decideAdmin(true));
        rejectAdm.addActionListener(e -> decideAdmin(false));
        tabs.addTab("Admin Requests", tabPanel(admTable, approveAdm, rejectAdm));

        // Preview card on the right
        pvDesc.setEditable(false);
        pvTopics.setEditable(false);
        JPanel head = Ui.clear(new JPanel());
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{pvTitle, pvMeta, pvBy, pvFile, pvStatus}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            head.add(c);
            head.add(Box.createVerticalStrut(4));
        }
        JPanel descBox = Ui.clear(new JPanel(new BorderLayout(0, 4)));
        descBox.add(Ui.label("Description", Ui.BOLD, Ui.TEXT), BorderLayout.NORTH);
        descBox.add(Ui.scrollBox(pvDesc), BorderLayout.CENTER);
        JPanel topicBox = Ui.clear(new JPanel(new BorderLayout(0, 4)));
        topicBox.add(Ui.label("Topics Covered", Ui.BOLD, Ui.TEXT), BorderLayout.NORTH);
        topicBox.add(Ui.scrollBox(pvTopics), BorderLayout.CENTER);
        JPanel body = Ui.clear(new JPanel(new GridLayout(2, 1, 0, 10)));
        body.add(descBox);
        body.add(topicBox);

        JPanel preview = Ui.card();
        preview.setLayout(new BorderLayout(0, 10));
        preview.setPreferredSize(new Dimension(350, 0));
        preview.add(head, BorderLayout.NORTH);
        preview.add(body, BorderLayout.CENTER);

        JPanel center = Ui.clear(new JPanel(new BorderLayout(14, 0)));
        center.add(tabs, BorderLayout.CENTER);
        center.add(preview, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
    }

    private JPanel stat(Color tint, Color accent, String caption, JLabel value, Ui.Ring ring) {
        Ui.RoundPanel p = Ui.pastel(tint).setAccent(accent);
        p.setLayout(new BorderLayout(8, 0));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 10, 12));
        JPanel text = Ui.clear(new JPanel(new BorderLayout(0, 2)));
        text.add(Ui.label(caption, Ui.BOLD, Ui.MUTED), BorderLayout.NORTH);
        text.add(value, BorderLayout.CENTER);
        p.add(text, BorderLayout.CENTER);
        if (ring != null) p.add(ring, BorderLayout.EAST);
        return p;
    }

    private JPanel tabPanel(JTable table, JButton... buttons) {
        JPanel p = Ui.card();
        p.setLayout(new BorderLayout(0, 8));
        p.add(Ui.scroll(table), BorderLayout.CENTER);
        JPanel row = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)));
        for (JButton b : buttons) row.add(b);
        p.add(row, BorderLayout.SOUTH);
        return p;
    }

    /** Clicking a row fills the preview card. */
    private void listen(JTable table, Supplier<ArrayList<Material>> list) {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || loading) return;
            int row = table.getSelectedRow();
            ArrayList<Material> l = list.get();
            if (row >= 0 && row < l.size()) fillPreview(l.get(row));
        });
    }

    private Material selected(JTable table, ArrayList<Material> list) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= list.size()) {
            JOptionPane.showMessageDialog(this, "Please select a reviewer first.");
            return null;
        }
        return list.get(row);
    }

    // ---------- Actions ----------
    private void approve(JTable table, ArrayList<Material> list) {
        Material m = selected(table, list);
        if (m == null) return;
        if (!DataStore.approveMaterial(m)) JOptionPane.showMessageDialog(this, "That reviewer no longer exists.");
        main.refreshAll();
    }

    private void reject(JTable table, ArrayList<Material> list) {
        Material m = selected(table, list);
        if (m == null) return;
        String reason = JOptionPane.showInputDialog(this, "Reason for rejecting \"" + m.title + "\" (the student will see this):",
                "Reject reviewer", JOptionPane.PLAIN_MESSAGE);
        if (reason == null) return; // cancelled
        reason = reason.trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type a reason so the student knows what to fix.");
            return;
        }
        if (!DataStore.rejectMaterial(m, reason)) JOptionPane.showMessageDialog(this, "That reviewer no longer exists.");
        main.refreshAll();
    }

    private void takeDown(Material m) {
        if (m == null) return;
        if (JOptionPane.showConfirmDialog(this, "Permanently remove \"" + m.title + "\" and its requests?", "Take down reviewer",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        DataStore.deleteMaterial(m.id);
        DataStore.addActivity(m.uploaderId, "Your reviewer \"" + m.title + "\" was removed by an admin", true);
        main.refreshAll();
    }

    private void openFile(Material m) {
        if (m == null) return;
        Ui.openMaterialFile(this, m);   // admins may always view the files
    }

    /** Approve or reject someone who asked to become an administrator. */
    private void decideAdmin(boolean approve) {
        int row = admTable.getSelectedRow();
        if (row < 0 || row >= admList.size()) {
            JOptionPane.showMessageDialog(this, "Please select an admin request first.");
            return;
        }
        DataStore.AdminRequest r = admList.get(row);
        if (!DataStore.ADMIN_PENDING.equals(r.status)) {
            JOptionPane.showMessageDialog(this, "Only requests with status \"Pending\" can be approved or rejected.");
            return;
        }
        String q = approve
                ? "Allow " + r.name + " (" + r.adminId + ") to register as an administrator?"
                : "Reject the admin request of " + r.name + " (" + r.adminId + ")?";
        if (JOptionPane.showConfirmDialog(this, q, approve ? "Approve admin access" : "Reject admin request",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) return;
        if (!DataStore.decideAdminRequest(r, approve)) JOptionPane.showMessageDialog(this, "That request was already handled.");
        main.refreshAll();
    }

    // ---------- Preview ----------
    private void fillPreview(Material m) {
        previewId = m.id;
        pvTitle.setText(m.title);
        pvMeta.setText(m.type + "  •  " + m.subject);
        pvBy.setText("By " + m.uploaderName + "  •  " + m.getDateText());
        pvFile.setText("File: " + m.fileName + " (" + m.fileSize + ")");
        if (Material.PENDING.equals(m.status)) {
            pvStatus.setForeground(Ui.WARN);
            pvStatus.setText("Status: Pending Review");
        } else if (Material.REJECTED.equals(m.status)) {
            pvStatus.setForeground(Ui.RED);
            pvStatus.setText("Rejected: " + m.rejectReason);
        } else {
            pvStatus.setForeground(Ui.GREEN);
            pvStatus.setText("Status: Approved");
        }
        pvDesc.setText(m.description);
        pvDesc.setCaretPosition(0);
        StringBuilder sb = new StringBuilder();
        for (String t : m.topics) sb.append("✓ ").append(t).append("\n");
        pvTopics.setText(sb.toString());
        pvTopics.setCaretPosition(0);
    }

    private void clearPreview() {
        previewId = -1;
        pvTitle.setText("Select a submission");
        pvMeta.setText(" ");
        pvBy.setText(" ");
        pvFile.setText(" ");
        pvStatus.setText(" ");
        pvDesc.setText("");
        pvTopics.setText("");
    }

    /** Reloads the stats and the three tables from the database (keeping your selection). */
    public void refresh() {
        loading = true;
        int keepP = Ui.selId(pendTable, pendList, m -> m.id);
        int keepA = Ui.selId(apprTable, apprList, m -> m.id);
        int keepR = Ui.selId(rejTable, rejList, m -> m.id);
        int keepAdm = Ui.selId(admTable, admList, r -> r.id);

        pendList = DataStore.materialsByStatus(Material.PENDING);
        apprList = DataStore.materialsByStatus(Material.APPROVED);
        rejList = DataStore.materialsByStatus(Material.REJECTED);

        pendModel.setRowCount(0);
        for (Material m : pendList)
            pendModel.addRow(new Object[]{m.title, m.subject, m.type, m.uploaderName, m.getDateText()});
        apprModel.setRowCount(0);
        for (Material m : apprList)
            apprModel.addRow(new Object[]{m.title, m.subject, m.type, m.uploaderName, m.getDateText()});
        rejModel.setRowCount(0);
        for (Material m : rejList)
            rejModel.addRow(new Object[]{m.title, m.subject, m.uploaderName, m.getDateText(), m.rejectReason});

        admList = DataStore.adminRequests();
        admModel.setRowCount(0);
        for (DataStore.AdminRequest r : admList)
            admModel.addRow(new Object[]{r.adminId, r.name, r.email, r.getDateText(), r.status});
        Ui.reselect(admTable, admList, r -> r.id, keepAdm);
        int pendingAdmins = DataStore.pendingAdminRequestCount();
        adminReqVal.setText(String.valueOf(pendingAdmins));
        tabs.setTitle(3, "Admin Requests (" + pendingAdmins + ")");

        Ui.reselect(pendTable, pendList, m -> m.id, keepP);
        Ui.reselect(apprTable, apprList, m -> m.id, keepA);
        Ui.reselect(rejTable, rejList, m -> m.id, keepR);

        pendingVal.setText(String.valueOf(pendList.size()));
        approvedVal.setText(String.valueOf(apprList.size()));
        rejectedVal.setText(String.valueOf(rejList.size()));
        studentsVal.setText(String.valueOf(DataStore.studentCount()));
        int total = pendList.size() + apprList.size() + rejList.size();
        pendingRing.setValue(total == 0 ? 0 : pendList.size() / (double) total, total == 0 ? "0%" : Math.round(100.0 * pendList.size() / total) + "%");
        approvedRing.setValue(total == 0 ? 0 : apprList.size() / (double) total, total == 0 ? "0%" : Math.round(100.0 * apprList.size() / total) + "%");
        rejectedRing.setValue(total == 0 ? 0 : rejList.size() / (double) total, total == 0 ? "0%" : Math.round(100.0 * rejList.size() / total) + "%");
        tabs.setTitle(0, "Pending Review (" + pendList.size() + ")");
        tabs.setTitle(1, "Approved (" + apprList.size() + ")");
        tabs.setTitle(2, "Rejected (" + rejList.size() + ")");

        Material pm = previewId > 0 ? DataStore.findMaterial(previewId) : null;
        if (pm != null) fillPreview(pm); else clearPreview();
        loading = false;
    }
}