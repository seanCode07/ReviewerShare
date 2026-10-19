package reviewershare;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Admin management: users, subjects and programs, school records, and the audit log. */
public class AdminManagePanel extends JPanel {
    private final MainFrame main;
    private final Ui.Tabs tabs = new Ui.Tabs();

    private final DefaultTableModel userModel = Ui.model("ID", "Name", "Email", "Program", "Role", "Status", "Shared");
    private final JTable userTable = new JTable(userModel);
    private ArrayList<DataStore.UserInfo> userList = new ArrayList<>();

    private final DefaultTableModel subjectModel = Ui.model("Subject", "Reviewers");
    private final JTable subjectTable = new JTable(subjectModel);
    private ArrayList<String> subjectList = new ArrayList<>();
    private final DefaultTableModel programModel = Ui.model("Program", "Students");
    private final JTable programTable = new JTable(programModel);
    private ArrayList<String> programList = new ArrayList<>();

    private final DefaultTableModel rosterModel = Ui.model("Student ID", "Full Name", "Program", "Account");
    private final JTable rosterTable = new JTable(rosterModel);
    private ArrayList<DataStore.RosterEntry> rosterList = new ArrayList<>();
    private final JLabel rosterNote = Ui.label(" ", Ui.SMALL, Ui.MUTED);

    private final DefaultTableModel auditModel = Ui.model("When", "Who", "Action", "Details");
    private final JTable auditTable = new JTable(auditModel);

    public AdminManagePanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(Ui.label("Manage accounts, subjects, programs, school records and see what admins did.", Ui.BODY, Ui.MUTED), BorderLayout.NORTH);

        for (JTable t : new JTable[]{userTable, subjectTable, programTable, rosterTable, auditTable}) Ui.styleTable(t);

        // Users
        JButton suspend = Ui.plainButton("Suspend / Reactivate");
        JButton remove = Ui.dangerButton("Remove User");
        suspend.addActionListener(e -> toggleSuspend());
        remove.addActionListener(e -> removeUser());
        tabs.addTab("Users", tabPanel(userTable, suspend, remove));

        // Subjects and programs
        JButton addSub = Ui.primaryButton("Add Subject");
        JButton renSub = Ui.plainButton("Rename");
        JButton delSub = Ui.dangerButton("Remove");
        addSub.addActionListener(e -> {
            String n = Ui.showInput(this, "Name of the new subject:", "Add subject", JOptionPane.PLAIN_MESSAGE);
            if (n != null) report(DataStore.addSubject(n), "Subject added.");
        });
        renSub.addActionListener(e -> {
            String old = selectedName(subjectTable, subjectList, "subject");
            if (old == null) return;
            String n = Ui.showInput(this, "New name for \"" + old + "\" (reviewers using it are updated too):", "Rename subject", JOptionPane.PLAIN_MESSAGE);
            if (n != null) report(DataStore.renameSubject(old, n), "Subject renamed.");
        });
        delSub.addActionListener(e -> {
            String old = selectedName(subjectTable, subjectList, "subject");
            if (old != null && Ui.showConfirm(this, "Remove the subject \"" + old + "\"?", "Remove subject",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
                report(DataStore.removeSubject(old), "Subject removed.");
        });
        JButton addProg = Ui.primaryButton("Add Program");
        JButton delProg = Ui.dangerButton("Remove");
        addProg.addActionListener(e -> {
            String n = Ui.showInput(this, "Name of the new program (for example BS Biology):", "Add program", JOptionPane.PLAIN_MESSAGE);
            if (n != null) report(DataStore.addProgram(n), "Program added.");
        });
        delProg.addActionListener(e -> {
            String old = selectedName(programTable, programList, "program");
            if (old != null && Ui.showConfirm(this, "Remove the program \"" + old + "\"?", "Remove program",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
                report(DataStore.removeProgram(old), "Program removed.");
        });
        JPanel lists = Ui.clear(new JPanel(new GridLayout(1, 2, 14, 0)));
        lists.add(tabPanel(subjectTable, addSub, renSub, delSub));
        lists.add(tabPanel(programTable, addProg, delProg));
        tabs.addTab("Subjects & Programs", lists);

        // School records
        JButton addRec = Ui.primaryButton("Add / Update Record");
        JButton impRec = Ui.plainButton("Import CSV...");
        JButton delRec = Ui.dangerButton("Remove Record");
        addRec.addActionListener(e -> addRecord());
        impRec.addActionListener(e -> importCsv());
        delRec.addActionListener(e -> {
            int row = rosterTable.getSelectedRow();
            if (row < 0 || row >= rosterList.size()) {
                Ui.showMessage(this, "Please select a record first.");
                return;
            }
            DataStore.RosterEntry r = rosterList.get(row);
            if (Ui.showConfirm(this, "Remove the school record of " + r.name + " (" + r.id + ")?\nTheir account, if any, is not deleted.",
                    "Remove record", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
            DataStore.rosterRemove(r.id);
            main.refreshAll();
        });
        JPanel rosterPanel = tabPanel(rosterTable, addRec, impRec, delRec);
        rosterPanel.add(rosterNote, BorderLayout.NORTH);
        tabs.addTab("School Records", rosterPanel);

        // Audit log
        JPanel audit = Ui.card();
        audit.setLayout(new BorderLayout(0, 8));
        audit.add(Ui.label("The latest 300 admin actions and security events.", Ui.SMALL, Ui.MUTED), BorderLayout.NORTH);
        audit.add(Ui.scroll(auditTable), BorderLayout.CENTER);
        tabs.addTab("Audit Log", audit);

        add(tabs, BorderLayout.CENTER);
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

    /** Shows the problem, or a success message, then refreshes every screen. */
    private void report(String problem, String success) {
        if (problem != null) Ui.showMessage(this, problem, "Could not do that", JOptionPane.WARNING_MESSAGE);
        else Ui.showMessage(this, success, "Done", Ui.SUCCESS_MESSAGE);
        main.refreshAll();
    }

    private String selectedName(JTable table, List<String> list, String what) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= list.size()) {
            Ui.showMessage(this, "Please select a " + what + " first.");
            return null;
        }
        return list.get(row);
    }

    // ---------- Users ----------
    private DataStore.UserInfo selectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= userList.size()) {
            Ui.showMessage(this, "Please select a user first.");
            return null;
        }
        return userList.get(row);
    }

    private void toggleSuspend() {
        DataStore.UserInfo u = selectedUser();
        if (u == null) return;
        String problem;
        if (!u.suspended) {
            String reason = Ui.showInput(this, "Reason for suspending " + u.name + " (they will see it when they try to sign in):",
                    "Suspend account", JOptionPane.PLAIN_MESSAGE);
            if (reason == null) return;
            problem = DataStore.setSuspended(u.id, true, reason.trim());
            report(problem, u.name + " was suspended. They are signed out and cannot sign in until you reactivate them.");
        } else {
            if (Ui.showConfirm(this, "Reactivate the account of " + u.name + "?", "Reactivate account", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            problem = DataStore.setSuspended(u.id, false, "");
            report(problem, u.name + " can sign in again.");
        }
    }

    private void removeUser() {
        DataStore.UserInfo u = selectedUser();
        if (u == null) return;
        String what = u.shared > 0 ? "\nTheir " + u.shared + " shared reviewer(s) and files are deleted too." : "";
        if (Ui.showConfirm(this, "Permanently remove " + u.name + " (" + u.id + ")?" + what
                        + "\nTheir requests, reviews and activity are deleted and affected students are notified. This cannot be undone.",
                "Remove user", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        report(DataStore.adminRemoveUser(u.id), u.name + " was removed.");
    }

    // ---------- School records ----------
    private void addRecord() {
        JTextField id = Ui.field(), name = Ui.field();
        Ui.limitInput(id, 12, true);
        ArrayList<String> programs = DataStore.programs();
        programs.add(0, "(none)");
        JComboBox<String> program = Ui.style(new JComboBox<>(programs.toArray(new String[0])));
        JPanel box = Ui.clear(new JPanel());
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        Ui.addField(box, "Student ID (digits only)", id);
        Ui.addField(box, "Full Name", name);
        Ui.addField(box, "Program (optional)", program);
        if (Ui.showConfirm(this, box, "Add school record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String prog = program.getSelectedIndex() <= 0 ? "" : (String) program.getSelectedItem();
        report(DataStore.rosterAdd(id.getText().trim(), name.getText(), prog), "The school record was saved.");
    }

    /** Reads lines like  2511111,Alex Rivera,BS Computer Science  (comma, semicolon or tab separated). */
    private void importCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Choose a CSV file: Student ID, Full Name, Program");
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        try {
            ArrayList<String[]> rows = new ArrayList<>();
            int lines = 0;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.replace("\uFEFF", "").split("[,;\\t]", -1);
                for (int i = 0; i < p.length; i++) p[i] = p[i].trim().replaceAll("^\"|\"$", "");
                if (lines++ == 0 && p[0].toLowerCase().replace(" ", "").matches("id|studentid|studentno|idnumber")) continue; // header
                rows.add(p);
            }
            int saved = DataStore.rosterImport(rows);
            Ui.showMessage(this, saved + " school record(s) saved" + (rows.size() > saved ? ", " + (rows.size() - saved)
                    + " line(s) skipped (ID must be digits and the name cannot be empty)." : "."), "Import finished", Ui.SUCCESS_MESSAGE);
            main.refreshAll();
        } catch (Exception ex) {
            Ui.showMessage(this, "Could not read the file: " + ex.getMessage(), "Import failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void reselectKey(JTable t, List<String> keys, String want) {
        if (want == null) return;
        int i = keys.indexOf(want);
        if (i >= 0 && i < t.getRowCount()) t.setRowSelectionInterval(i, i);
    }

    /** Reloads every table. */
    public void refresh() {
        String keepUser = userTable.getSelectedRow() >= 0 && userTable.getSelectedRow() < userList.size() ? userList.get(userTable.getSelectedRow()).id : null;
        userList = DataStore.allUsers();
        userModel.setRowCount(0);
        ArrayList<String> userKeys = new ArrayList<>();
        for (DataStore.UserInfo u : userList) {
            userKeys.add(u.id);
            userModel.addRow(new Object[]{u.id, u.name, u.email, u.course, u.role.equals("admin") ? "Admin" : "Student",
                    u.suspended ? "Suspended" : "Active", u.shared});
        }
        reselectKey(userTable, userKeys, keepUser);

        String keepSub = selectedKey(subjectTable, subjectList), keepProg = selectedKey(programTable, programList);
        HashMap<String, Integer> sc = DataStore.subjectCounts(), pc = DataStore.programCounts();
        subjectList = DataStore.subjects();
        subjectModel.setRowCount(0);
        for (String s : subjectList) subjectModel.addRow(new Object[]{s, sc.getOrDefault(s, 0)});
        programList = DataStore.programs();
        programModel.setRowCount(0);
        for (String p : programList) programModel.addRow(new Object[]{p, pc.getOrDefault(p, 0)});
        reselectKey(subjectTable, subjectList, keepSub);
        reselectKey(programTable, programList, keepProg);

        String keepRec = rosterTable.getSelectedRow() >= 0 && rosterTable.getSelectedRow() < rosterList.size() ? rosterList.get(rosterTable.getSelectedRow()).id : null;
        rosterList = DataStore.roster();
        rosterModel.setRowCount(0);
        ArrayList<String> recKeys = new ArrayList<>();
        for (DataStore.RosterEntry r : rosterList) {
            recKeys.add(r.id);
            rosterModel.addRow(new Object[]{r.id, r.name, r.program.isEmpty() ? "-" : r.program, r.registered ? "Registered" : "Not yet"});
        }
        reselectKey(rosterTable, recKeys, keepRec);
        rosterNote.setText(rosterList.isEmpty()
                ? "No school records yet, so anyone can create a student account. Add or import records to restrict sign-ups to real students."
                : "Sign-ups are restricted: only these students can create an account, and the ID and full name must match.");

        int top = auditTable.getSelectedRow();
        auditModel.setRowCount(0);
        for (DataStore.AuditEntry a : DataStore.auditLog(300)) auditModel.addRow(new Object[]{a.when, a.who, a.action, a.details});
        if (top >= 0 && top < auditTable.getRowCount()) auditTable.setRowSelectionInterval(top, top);
    }

    private static String selectedKey(JTable t, List<String> list) {
        int r = t.getSelectedRow();
        return r >= 0 && r < list.size() ? list.get(r) : null;
    }
}