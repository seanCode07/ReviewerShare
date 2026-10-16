package reviewershare;

import javax.swing.*;
import java.awt.*;

/** Settings: profile summary, profile form, change password, preferences, and account deletion. */
public class SettingsPanel extends JPanel {
    private final MainFrame main;
    private final Ui.Avatar avatar = new Ui.Avatar("", 64, true, Ui.CORAL);
    private final JLabel nameLabel = Ui.label("", Ui.H1, Color.WHITE);
    private final JLabel infoLabel = Ui.label("", Ui.BODY, new Color(0xE6E2FF));
    private final JLabel statsLabel = Ui.label("", Ui.BOLD, Color.WHITE);
    private final JTextField nameField = Ui.field();
    private final JTextField idField = Ui.field();
    private final JTextField emailField = Ui.field();
    private final JTextField courseField = Ui.field();
    private final JPasswordField curPw = Ui.passwordField();
    private final JPasswordField newPw = Ui.passwordField();
    private final JPasswordField newPw2 = Ui.passwordField();
    private final JCheckBox notifBox = Ui.check("Notifications - show messages when requests and reviewers change");
    private final JCheckBox darkBox = Ui.check("Light Mode");

    public SettingsPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // ----- Profile summary banner (compact) -----
        Ui.RoundPanel summary = new Ui.RoundPanel(22, false, new Color(0x6C74E6));
        summary.setGradient(new Color(0x1BB5B0));
        summary.setLayout(new BorderLayout(18, 0));
        summary.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        JPanel avatarBox = Ui.clear(new JPanel(new GridBagLayout()));
        avatarBox.add(avatar);
        JPanel text = Ui.clear(new JPanel(new GridLayout(0, 1, 0, 2)));
        text.add(nameLabel);
        text.add(infoLabel);
        text.add(statsLabel);
        summary.add(avatarBox, BorderLayout.WEST);
        summary.add(text, BorderLayout.CENTER);
        finish(summary);

        // ----- Profile card (2 x 2 fields) -----
        idField.setEditable(false);
        JButton save = Ui.primaryButton("Save Changes");
        save.addActionListener(e -> saveProfile());
        Ui.RoundPanel profile = box("Profile Information", "Update how your name and contact details appear.");
        JPanel grid = Ui.clear(new JPanel(new GridLayout(0, 2, 12, 10)));
        grid.add(group("Full Name", nameField));
        grid.add(group("Student ID (cannot be changed)", idField));
        grid.add(group("Email Address", emailField));
        grid.add(group("Course / Program", courseField));
        profile.add(finish(grid));
        profile.add(Box.createVerticalStrut(10));
        profile.add(finish(rightRow(save)));
        finish(profile);

        // ----- Preferences card -----
        notifBox.addActionListener(e -> DataStore.notifications = notifBox.isSelected());
        darkBox.addActionListener(e -> {
            Ui.light = darkBox.isSelected();
            main.refreshAll();
        });
        Ui.RoundPanel prefs = box("Preferences", null);
        prefs.add(left(notifBox));
        prefs.add(Box.createVerticalStrut(4));
        prefs.add(left(darkBox));
        finish(prefs);

        // ----- Change password card -----
        JButton change = Ui.primaryButton("Update Password");
        change.addActionListener(e -> changePassword());
        Ui.RoundPanel pass = box("Change Password", "Use at least 6 characters. You need your current password.");
        Ui.addField(pass, "Current Password", curPw);
        Ui.addField(pass, "New Password (min 6 characters)", newPw);
        Ui.addField(pass, "Confirm New Password", newPw2);
        pass.add(finish(rightRow(change)));
        finish(pass);

        // ----- Danger zone card -----
        boolean isAdmin = DataStore.currentUser != null && DataStore.currentUser.isAdmin();
        JButton delete = Ui.dangerButton("Delete My Account");
        delete.addActionListener(e -> deleteAccount());
        Ui.RoundPanel danger = box("Delete Account", null);
        String warn = "Deleting your account permanently removes your profile, the reviewers you shared, your requests and your activity. "
                + "You can register again later with the same ID and email."
                + (isAdmin ? " An administrator can only delete the account if another administrator exists." : "");
        danger.add(left(Ui.label("<html><body style='width:300px'>" + warn + "</body></html>", Ui.SMALL, Ui.MUTED)));
        danger.add(Box.createVerticalStrut(12));
        danger.add(left(delete));
        finish(danger);

        // ----- Two columns -----
        JPanel leftCol = Ui.clear(new JPanel());
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.add(profile);
        leftCol.add(Box.createVerticalStrut(14));
        leftCol.add(prefs);
        leftCol.add(Box.createVerticalGlue());

        JPanel rightCol = Ui.clear(new JPanel());
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.add(pass);
        rightCol.add(Box.createVerticalStrut(14));
        rightCol.add(danger);
        rightCol.add(Box.createVerticalGlue());

        JPanel cols = Ui.clear(new JPanel(new GridLayout(1, 2, 14, 0)));
        cols.add(leftCol);
        cols.add(rightCol);
        cols.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel content = Ui.clear(new JPanel());
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(summary);
        content.add(Box.createVerticalStrut(14));
        content.add(cols);

        JPanel holder = Ui.clear(new JPanel(new BorderLayout()));
        holder.add(content, BorderLayout.NORTH);
        add(Ui.scroll(holder), BorderLayout.CENTER);
    }

    // ---------- Layout helpers ----------
    private <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Left-aligns a component and stops it from growing taller than it needs to be. */
    private <T extends JComponent> T finish(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    /** A card with a title (and an optional grey hint line) that stacks its content from top to bottom. */
    private Ui.RoundPanel box(String title, String hint) {
        Ui.RoundPanel c = Ui.card();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        c.add(left(Ui.label(title, Ui.H2, Ui.TEXT)));
        if (hint != null) {
            c.add(Box.createVerticalStrut(2));
            c.add(left(Ui.label(hint, Ui.SMALL, Ui.MUTED)));
        }
        c.add(Box.createVerticalStrut(12));
        return c;
    }

    private JPanel group(String caption, JComponent field) {
        JPanel p = Ui.clear(new JPanel(new BorderLayout(0, 4)));
        p.add(Ui.label(caption, Ui.BOLD, Ui.TEXT), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel rightRow(JButton b) {
        JPanel row = Ui.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)));
        row.add(b);
        return row;
    }

    // ---------- Actions ----------
    /** Validates and saves the profile fields. */
    private void saveProfile() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String course = courseField.getText().trim();
        if (name.isEmpty() || course.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full Name and Course cannot be empty.", "Missing information", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Ui.isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address (like name@school.edu.ph).", "Invalid email", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Student s = DataStore.currentUser;
        String oldName = s.name;
        s.name = name;
        s.email = email;
        s.course = course;
        DataStore.updateProfile(s);   // names shown on materials and requests come from the database, so they update everywhere
        JOptionPane.showMessageDialog(this, "Profile saved" + (oldName.equals(name) ? "." : " (name updated)."));
        main.refreshAll();
    }

    /** Changes the password of the logged-in user (students and admins). */
    private void changePassword() {
        String cur = new String(curPw.getPassword());
        String a = new String(newPw.getPassword());
        String b = new String(newPw2.getPassword());
        if (cur.isEmpty() || a.isEmpty() || b.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all three password fields.", "Missing information", JOptionPane.ERROR_MESSAGE);
        } else if (a.length() < 6) {
            JOptionPane.showMessageDialog(this, "The new password must be at least 6 characters.", "Weak password", JOptionPane.ERROR_MESSAGE);
        } else if (!a.equals(b)) {
            JOptionPane.showMessageDialog(this, "The two new passwords do not match.", "Password mismatch", JOptionPane.ERROR_MESSAGE);
        } else if (a.equals(cur)) {
            JOptionPane.showMessageDialog(this, "The new password must be different from the current one.", "Same password", JOptionPane.ERROR_MESSAGE);
        } else if (!DataStore.changePassword(DataStore.currentUser.id, cur, a)) {
            JOptionPane.showMessageDialog(this, "Your current password is not correct.", "Wrong password", JOptionPane.ERROR_MESSAGE);
        } else {
            curPw.setText("");
            newPw.setText("");
            newPw2.setText("");
            JOptionPane.showMessageDialog(this, "Your password was changed. Use the new password the next time you sign in.");
        }
    }

    /** Asks twice, then deletes the account together with all of its data and returns to the sign-in screen. */
    private void deleteAccount() {
        Student me = DataStore.currentUser;
        int first = JOptionPane.showConfirmDialog(this,
                "Delete the account of " + me.name + "?\n\nYour profile, shared reviewers, requests and activity will be\n"
                        + "permanently deleted. This cannot be undone.",
                "Delete account", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (first != JOptionPane.YES_OPTION) return;

        JPasswordField pw = Ui.passwordField();
        pw.setPreferredSize(new Dimension(300, 38));
        JPanel box = Ui.clear(new JPanel());
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(left(Ui.label("Type your password to confirm:", Ui.BOLD, Ui.TEXT)));
        box.add(Box.createVerticalStrut(6));
        box.add(left(pw));
        int second = JOptionPane.showConfirmDialog(this, box, "Confirm deletion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (second != JOptionPane.OK_OPTION) return;

        String error = DataStore.deleteAccount(me.id, new String(pw.getPassword()));
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Could not delete account", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Your account and its data were deleted.\nYou can register again anytime with your ID and email.");
        main.signOut();
    }

    /** Fills the labels and fields from the current user. */
    public void refresh() {
        Student s = DataStore.currentUser;
        nameLabel.setText(s.name);
        avatar.setPerson(s.name);
        if (s.isAdmin()) {
            infoLabel.setText("Administrator - ID: " + s.id);
            statsLabel.setText(DataStore.countMaterials(Material.PENDING) + " Pending   |   "
                    + DataStore.countMaterials(Material.APPROVED) + " Approved   |   "
                    + DataStore.countMaterials(Material.REJECTED) + " Rejected");
        } else {
            infoLabel.setText(s.section + " - Student ID: " + s.id);
            statsLabel.setText(DataStore.myMaterials().size() + " Shared   |   " + DataStore.sentRequests().size()
                    + " Requested   |   " + DataStore.completedRequests().size() + " Completed");
        }
        nameField.setText(s.name);
        idField.setText(s.id);
        emailField.setText(s.email);
        courseField.setText(s.course);
        notifBox.setSelected(DataStore.notifications);
        darkBox.setSelected(Ui.light);
    }
}