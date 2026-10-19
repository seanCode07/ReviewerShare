package reviewershare;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Random;

/** Sign-in screen, plus Terms & Privacy, Create Account, and Forgot Password dialogs. */
public class LoginFrame extends JFrame {
    private static final String TERMS =
            "TERMS AND CONDITIONS\n\n"
            + "1. About ReviewerShare\n"
            + "ReviewerShare is a student platform for sharing reviewers, notes, and practice tests with your classmates.\n\n"
            + "2. Your account\n"
            + "Provide accurate information when you sign up. Keep your password private - you are responsible for everything done under your account.\n\n"
            + "3. Content you share\n"
            + "Only share materials that you created or that you have the right to share. Do not upload copyrighted books, leaked exams, or anything offensive or misleading. "
            + "Every reviewer you share is checked by an administrator before it appears in the library. The administrator may reject or remove any material that breaks these terms.\n\n"
            + "4. Requests\n"
            + "You may request access to approved materials. Owners may accept or decline requests, and you may cancel a request while it is still pending.\n\n"
            + "5. Acceptable use\n"
            + "Be respectful. Do not try to break, overload, or misuse the system, or access other people's accounts.\n\n"
            + "6. Account removal\n"
            + "Accounts that repeatedly break these terms may be suspended or removed. You may also delete your own account at any time in Settings. Deleting it removes your data from the system.\n\n\n"
            + "PRIVACY NOTICE\n\n"
            + "What we collect\n"
            + "Your Student ID, full name, email address, course, your password (stored only as a salted hash, never as plain text), the materials you share, your requests, and your activity in the app.\n\n"
            + "Why we collect it\n"
            + "To run your account, show who shared a material, let administrators moderate submissions, and notify you when something happens to your reviewers or requests.\n\n"
            + "Who can see it\n"
            + "Other students can see your name and section next to materials you share. Administrators can see submissions, uploaded files and account details needed for moderation. New administrators must be approved by a current administrator. We do not sell your data.\n\n"
            + "How long we keep it\n"
            + "Your data is kept while your account is active. You can delete your account in Settings at any time, which also deletes your data from the system. You may register again later with the same ID and email.\n\n"
            + "Your rights\n"
            + "Under the Data Privacy Act of 2012 (Republic Act 10173) you have the right to be informed, to access and correct your data, to object to its processing, and to request its deletion.\n";

    private final JTextField sId = Ui.field();
    private final JPasswordField sPw = Ui.passwordField();
    private final JCheckBox sRemember = Ui.check("Remember me");
    private final JCheckBox sShow = Ui.check("Show password");
    private final JLabel sError = Ui.label(" ", Ui.SMALL, Ui.RED);

    private final JTextField aId = Ui.field();
    private final JPasswordField aPw = Ui.passwordField();
    private final JCheckBox aShow = Ui.check("Show password");
    private final JLabel aError = Ui.label(" ", Ui.SMALL, Ui.RED);

    private final CardLayout formCards = new CardLayout();
    private final JPanel formHolder = Ui.clear(new JPanel(formCards));
    private final Ui.RoundButton studentTab = new Ui.RoundButton("Student", Ui.RoundButton.TAB).setFill(Ui.ACCENT);
    private final Ui.RoundButton adminTab = new Ui.RoundButton("Administrator", Ui.RoundButton.TAB).setFill(Ui.CORAL);
    private Ui.RoundButton signInStudent, signInAdmin;
    private Banner banner;

    // Notice shown to someone whose admin request was answered
    private final Ui.RoundPanel notice = Ui.pastel(Ui.P_MINT);
    private final JLabel noticeText = Ui.label(" ", Ui.BOLD, Ui.TEXT);
    private final javax.swing.Timer noticeTimer = new javax.swing.Timer(25000, e -> notice.setVisible(false));
    private final javax.swing.Timer adminWatch = new javax.swing.Timer(2000, e -> checkAdminRequest());
    private final HashSet<String> announced = new HashSet<>();
    private String watchedAdminId = "";
    private long lastVersion = -1;

    public LoginFrame() {
        super("ReviewerShare - Sign In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 780);
        setMinimumSize(new Dimension(1000, 740));
        setLocationRelativeTo(null);
        Ui.applyIcon(this);
        getContentPane().setBackground(Ui.BG);
        setLayout(new GridLayout(1, 2));
        Ui.limitInput(sId, 12, false);   // Student IDs are at most 12 characters
        Ui.limitInput(aId, 12, false);   // Admin IDs too
        banner = new Banner();
        add(banner);
        add(buildRightPanel());
        setRole(false);

        // Every 2 seconds, see if a current admin answered the admin request typed on this screen
        noticeTimer.setRepeats(false);
        adminWatch.start();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                adminWatch.stop();
                noticeTimer.stop();
            }
        });
    }

    /** Tells the person on the sign-in screen when their admin request is approved (or rejected). */
    private void checkAdminRequest() {
        try {
            long v = Db.version();
            if (v == lastVersion) return;
            lastVersion = v;
            for (String id : new String[]{watchedAdminId, aId.getText().trim()}) {
                if (id.isEmpty()) continue;
                DataStore.AdminRequest r = DataStore.adminRequestFor(id);
                if (r == null) continue;
                boolean approved = DataStore.ADMIN_APPROVED.equals(r.status);
                boolean rejected = DataStore.ADMIN_REJECTED.equals(r.status);
                if ((approved || rejected) && announced.add(r.id + ":" + r.status)) {
                    showNotice(approved
                            ? "Good news, " + r.name + "! An admin approved your request. Click \"Register (approved)\" on the Administrator tab."
                            : "Your admin request was not approved. You can send a new request.", approved);
                }
            }
        } catch (RuntimeException ex) {
            // the data files may be busy for a moment - try again on the next tick
        }
    }

    private void showNotice(String message, boolean good) {
        noticeText.setText("<html><body style='width:330px'>" + message + "</body></html>");
        notice.setBackground(good ? Ui.P_MINT : Ui.P_PEACH);
        notice.setVisible(true);
        notice.setMaximumSize(new Dimension(Integer.MAX_VALUE, notice.getPreferredSize().height));
        notice.revalidate();
        notice.repaint();
        noticeTimer.restart();
        Toolkit.getDefaultToolkit().beep();
    }

    private JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    // ---------- Left banner (changes color and text for students / administrators) ----------
    private class Banner extends JPanel {
        private boolean admin;
        private final JLabel badge = Ui.label("", new Font("SansSerif", Font.BOLD, 12), Color.WHITE);
        private final JLabel headline = Ui.label("", new Font("SansSerif", Font.BOLD, 38), Color.WHITE);
        private final JLabel desc = Ui.label("", new Font("SansSerif", Font.PLAIN, 14), new Color(0xF0EAFF));
        private final JLabel b1 = Ui.label("", new Font("SansSerif", Font.BOLD, 14), new Color(0xF3F0FF));
        private final JLabel b2 = Ui.label("", new Font("SansSerif", Font.BOLD, 14), new Color(0xF3F0FF));
        private final JLabel b3 = Ui.label("", new Font("SansSerif", Font.BOLD, 14), new Color(0xF3F0FF));

        Banner() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

            JPanel brand = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0)));
            brand.add(Ui.logoComponent(68));
            brand.add(Ui.label("ReviewerShare", new Font("SansSerif", Font.BOLD, 26), Color.WHITE));
            brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

            add(Box.createVerticalGlue());
            add(left(brand));
            add(Box.createVerticalStrut(30));
            add(left(badge));
            add(Box.createVerticalStrut(10));
            add(left(headline));
            add(Box.createVerticalStrut(16));
            add(left(desc));
            add(Box.createVerticalStrut(26));
            add(left(b1));
            add(Box.createVerticalStrut(8));
            add(left(b2));
            add(Box.createVerticalStrut(8));
            add(left(b3));
            add(Box.createVerticalGlue());
        }

        void setRole(boolean admin) {
            this.admin = admin;
            if (admin) {
                badge.setText("ADMINISTRATOR CONSOLE");
                headline.setText("<html>Keep the library<br>trusted and safe.</html>");
                desc.setText("<html><body style='width:330px'>Review every submission, approve or reject reviewers, and decide who joins the moderation team.</body></html>");
                b1.setText("✓  Approve or reject new submissions");
                b2.setText("✓  View every uploaded file");
                b3.setText("✓  New admins need a current admin's approval");
            } else {
                badge.setText("STUDENT PORTAL");
                headline.setText("<html>Share Knowledge.<br>Learn Together.</html>");
                desc.setText("<html><body style='width:330px'>A collaborative platform for college students to share reviewers, notes, and study materials - supporting quality education for everyone.</body></html>");
                b1.setText("✓  Share reviewers and notes in seconds");
                b2.setText("✓  Every material is verified by an admin");
                b3.setText("✓  Request access with one click");
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color top = admin ? new Color(0xC2476B) : new Color(0x4650D8);
            Color bottom = admin ? new Color(0x2E0F34) : new Color(0x1B1E5C);
            Color glowA = admin ? new Color(0xFF, 0xB8, 0x4D, 40) : new Color(0x1E, 0xD6, 0xC8, 38);
            Color glowB = admin ? new Color(0xFF, 0x7F, 0x87, 50) : new Color(0xFF, 0x7F, 0x87, 34);
            g2.setPaint(new GradientPaint(0, 0, top, getWidth(), getHeight(), bottom));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(glowA);
            g2.fillOval(-140, getHeight() - 280, 460, 460);
            g2.setColor(glowB);
            g2.fillOval(getWidth() - 200, -120, 360, 360);
            g2.setColor(admin ? new Color(0xFF, 0xB8, 0x4D) : new Color(0x1E, 0xD6, 0xC8));
            g2.fillOval(getWidth() - 130, getHeight() - 180, 56, 56);
            g2.setColor(new Color(0xFF, 0x7F, 0x87));
            g2.fillOval(getWidth() - 190, getHeight() - 120, 26, 26);
            g2.dispose();
        }
    }

    // ---------- Right side: role switch + two separate sign-in forms ----------
    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Ui.BG);
        p.setBorder(BorderFactory.createEmptyBorder(36, 64, 36, 64));

        if (!DataStore.rememberedId.isEmpty()) {
            sId.setText(DataStore.rememberedId);
            sRemember.setSelected(true);
        }

        studentTab.addActionListener(e -> setRole(false));
        adminTab.addActionListener(e -> setRole(true));
        Ui.RoundPanel pill = new Ui.RoundPanel(20, true, Ui.CARD);
        pill.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
        pill.add(studentTab);
        pill.add(adminTab);
        pill.setAlignmentX(Component.LEFT_ALIGNMENT);
        pill.setMaximumSize(pill.getPreferredSize());

        formHolder.add(buildForm(false), "student");
        formHolder.add(buildForm(true), "admin");
        formHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
        formHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, formHolder.getPreferredSize().height));

        notice.setLayout(new BorderLayout());
        notice.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        notice.add(noticeText, BorderLayout.CENTER);
        notice.setVisible(false);
        notice.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notice.setToolTipText("Click to dismiss");
        notice.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { notice.setVisible(false); }
        });
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(notice);
        p.add(Box.createVerticalGlue());
        p.add(left(Ui.label("I am signing in as", Ui.SMALL, Ui.MUTED)));
        p.add(Box.createVerticalStrut(6));
        p.add(pill);
        p.add(Box.createVerticalStrut(26));
        p.add(formHolder);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JLabel caption(String text) {
        return Ui.label(text, Ui.BOLD, Ui.TEXT);
    }

    /** One sign-in form. The student form and the administrator form have their own fields, colors and buttons. */
    private JPanel buildForm(boolean admin) {
        Color accent = admin ? Ui.CORAL : Ui.ACCENT;
        JTextField id = admin ? aId : sId;
        JPasswordField pw = admin ? aPw : sPw;
        JCheckBox show = admin ? aShow : sShow;
        JLabel error = admin ? aError : sError;
        char defaultEcho = pw.getEchoChar();
        show.addActionListener(e -> pw.setEchoChar(show.isSelected() ? (char) 0 : defaultEcho));
        error.setVerticalAlignment(SwingConstants.TOP);
        error.setPreferredSize(new Dimension(340, 34));
        error.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        Ui.RoundButton signIn = Ui.primaryButton(admin ? "Sign In as Administrator" : "Sign In");
        if (admin) signIn.setFill(Ui.CORAL);
        signIn.addActionListener(e -> doLogin(admin));
        if (admin) signInAdmin = signIn; else signInStudent = signIn;
        JButton forgot = Ui.plainButton("Forgot password?");
        forgot.addActionListener(e -> showForgotDialog(admin));

        for (JComponent c : new JComponent[]{id, pw, signIn}) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            c.setPreferredSize(new Dimension(c.getPreferredSize().width, 44));
        }
        forgot.setMaximumSize(new Dimension(170, 36));

        JPanel p = Ui.clear(new JPanel());
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(left(Ui.label(admin ? "ADMINISTRATOR ACCESS" : "STUDENT PORTAL", new Font("SansSerif", Font.BOLD, 11), accent)));
        p.add(Box.createVerticalStrut(6));
        p.add(left(Ui.label(admin ? "Administrator sign in" : "Welcome back", new Font("SansSerif", Font.BOLD, 26), Ui.TEXT)));
        p.add(left(Ui.label(admin ? "Restricted area. For approved administrators only." : "Sign in to access your study materials.", Ui.SMALL, Ui.MUTED)));
        p.add(Box.createVerticalStrut(20));
        p.add(left(caption(admin ? "Admin ID" : "Student ID (e.g. 2511111)")));
        p.add(Box.createVerticalStrut(4));
        p.add(left(id));
        p.add(Box.createVerticalStrut(12));
        p.add(left(caption("Password")));
        p.add(Box.createVerticalStrut(4));
        p.add(left(pw));
        p.add(Box.createVerticalStrut(8));
        JPanel options = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        options.add(show);
        if (!admin) {
            options.add(Box.createHorizontalStrut(14));
            options.add(sRemember);
        }
        options.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.add(left(options));
        p.add(Box.createVerticalStrut(4));
        p.add(left(error));
        p.add(Box.createVerticalStrut(4));
        p.add(left(signIn));
        p.add(Box.createVerticalStrut(10));
        p.add(left(forgot));
        p.add(Box.createVerticalStrut(20));

        if (admin) {
            JButton request = Ui.plainButton("Request admin access");
            JButton register = Ui.plainButton("Register (approved)");
            request.addActionListener(e -> showAdminRequestDialog());
            register.addActionListener(e -> showAdminRegisterDialog());
            JPanel row = Ui.clear(new JPanel(new GridLayout(1, 2, 8, 0)));
            row.add(request);
            row.add(register);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            p.add(left(caption("New administrator?")));
            p.add(Box.createVerticalStrut(2));
            p.add(left(Ui.label("A current admin must approve you before you can register.", Ui.SMALL, Ui.MUTED)));
            p.add(Box.createVerticalStrut(8));
            p.add(left(row));
        } else {
            JButton create = Ui.plainButton("Create an account");
            create.addActionListener(e -> showTermsDialog());
            create.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            create.setPreferredSize(new Dimension(create.getPreferredSize().width, 44));
            p.add(left(Ui.label("Don't have an account?", Ui.SMALL, Ui.MUTED)));
            p.add(Box.createVerticalStrut(4));
            p.add(left(create));
        }
        return p;
    }

    /** Switches between the Student form and the Administrator form. */
    private void setRole(boolean admin) {
        formCards.show(formHolder, admin ? "admin" : "student");
        studentTab.setActive(!admin);
        adminTab.setActive(admin);
        banner.setRole(admin);
        getRootPane().setDefaultButton(admin ? signInAdmin : signInStudent);
        sError.setText(" ");
        aError.setText(" ");
        setTitle(admin ? "ReviewerShare - Administrator Sign In" : "ReviewerShare - Student Sign In");
    }

    private void showError(JLabel label, String message) {
        label.setText("<html><body style='width:340px'>" + message + "</body></html>");
    }

    /** Checks the fields and opens the main window if the login is correct for the chosen role. */
    private void doLogin(boolean admin) {
        String id = (admin ? aId : sId).getText().trim();
        String pw = new String((admin ? aPw : sPw).getPassword());
        JLabel error = admin ? aError : sError;
        if (id.isEmpty() || pw.isEmpty()) {
            showError(error, admin ? "Please enter your Admin ID and password." : "Please enter your Student ID and password.");
            return;
        }
        int locked = DataStore.lockSecondsLeft(id);
        if (locked > 0) {
            showError(error, "Too many wrong passwords. This ID is locked for " + (locked / 60) + ":" + String.format("%02d", locked % 60)
                    + " more minutes. Try again later.");
            return;
        }
        Student s = DataStore.login(id, pw);
        if (s == null) {
            int left = DataStore.recordFailedLogin(id);
            DataStore.AdminRequest r = admin ? DataStore.adminRequestFor(id) : null;
            if (left == 0) {
                showError(error, "Too many wrong passwords. This ID is now locked for " + DataStore.LOCK_MINUTES + " minutes.");
            } else if (r != null && DataStore.ADMIN_PENDING.equals(r.status)) {
                showError(error, "Your admin request is still waiting for approval by a current admin.");
            } else if (r != null && DataStore.ADMIN_APPROVED.equals(r.status)) {
                showError(error, "Your request was approved. Click \"Register (approved)\" to create your account.");
            } else if (r != null && DataStore.ADMIN_REJECTED.equals(r.status)) {
                showError(error, "Your admin request was rejected. You may send a new request.");
            } else {
                showError(error, (admin ? "Wrong Admin ID or password. " : "Wrong Student ID or password. ")
                        + left + (left == 1 ? " try left." : " tries left."));
            }
            return;
        }
        DataStore.clearFailedLogins(id);
        if (admin && !s.isAdmin()) {
            showError(error, "That is a student account. Please use the Student tab to sign in.");
            return;
        }
        if (!admin && s.isAdmin()) {
            showError(error, "That is an administrator account. Please use the Administrator tab to sign in.");
            return;
        }
        if (DataStore.isSuspended(s.id)) {
            String why = DataStore.suspensionReason(s.id);
            showError(error, "This account is suspended." + (why.isEmpty() ? "" : " Reason: " + why) + " Please contact an administrator.");
            return;
        }
        DataStore.currentUser = s;
        if (!admin) DataStore.rememberedId = sRemember.isSelected() ? id : "";
        dispose();
        new MainFrame().setVisible(true);
    }

    // ---------- Dialog helpers ----------
    private JPanel formPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Ui.CARD);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));
        return p;
    }

    private void addRow(JPanel p, String caption, JComponent c) {
        JLabel l = Ui.label(caption, Ui.BOLD, Ui.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(c);
        p.add(Box.createVerticalStrut(10));
    }

    private JPanel buttonRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        row.setBackground(Ui.CARD);
        row.setBorder(BorderFactory.createEmptyBorder(0, 16, 10, 16));
        for (JButton b : buttons) row.add(b);
        return row;
    }

    private JDialog dialog(String title, int w, int h) {
        JDialog d = new JDialog(this, title, true);
        d.getContentPane().setBackground(Ui.CARD);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        return d;
    }

    // ---------- Terms & Privacy (shown before student sign up) ----------
    private void showTermsDialog() {
        JDialog d = dialog("Terms and Privacy Notice", 600, 640);
        boolean[] accepted = {false};

        JTextArea text = Ui.textArea(10);
        text.setText(TERMS);
        text.setEditable(false);
        text.setCaretPosition(0);
        JCheckBox agree = Ui.check("I have read and agree to the Terms and Conditions and the Privacy Notice");
        JButton cancel = Ui.plainButton("Cancel");
        JButton next = Ui.primaryButton("Continue");
        next.setEnabled(false);
        agree.addActionListener(e -> next.setEnabled(agree.isSelected()));
        cancel.addActionListener(e -> d.dispose());
        next.addActionListener(e -> {
            accepted[0] = true;
            d.dispose();
        });

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setBackground(Ui.CARD);
        top.setBorder(BorderFactory.createEmptyBorder(18, 20, 8, 20));
        top.add(Ui.label("Before you create an account", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        top.add(Ui.label("Please read the terms below. Scroll to the end.", Ui.SMALL, Ui.MUTED), BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout());
        mid.setBackground(Ui.CARD);
        mid.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        mid.add(Ui.scrollBox(text), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Ui.CARD);
        bottom.setBorder(BorderFactory.createEmptyBorder(8, 20, 0, 0));
        bottom.add(agree, BorderLayout.NORTH);
        bottom.add(buttonRow(cancel, next), BorderLayout.CENTER);

        d.add(top, BorderLayout.NORTH);
        d.add(mid, BorderLayout.CENTER);
        d.add(bottom, BorderLayout.SOUTH);
        d.setVisible(true);

        if (accepted[0]) showRegisterDialog();
    }

    // ---------- Create student account ----------
    private void showRegisterDialog() {
        JDialog d = dialog("Create a student account", 480, 660);
        JTextField id = Ui.field(), name = Ui.field(), email = Ui.field();
        JComboBox<String> course = Ui.style(new JComboBox<>(DataStore.programArray()));
        JPasswordField pw = Ui.passwordField(), pw2 = Ui.passwordField();
        Ui.limitInput(id, 12, true);   // digits only, up to 12

        JPanel form = formPanel();
        if (DataStore.rosterSize() > 0)
            form.add(left(Ui.label("<html><body style='width:380px'>Your Student ID and full name must match the school records.</body></html>", Ui.SMALL, Ui.MUTED)));
        form.add(Box.createVerticalStrut(8));
        addRow(form, "Student ID (digits only, up to 12)", id);
        addRow(form, "Full Name (as in the school records)", name);
        addRow(form, "Email (we send a verification code)", email);
        addRow(form, "Course / Program", course);
        addRow(form, "Password (min 6 characters)", pw);
        addRow(form, "Confirm Password", pw2);

        JButton create = Ui.primaryButton("Create Account");
        JButton cancel = Ui.plainButton("Cancel");
        cancel.addActionListener(e -> d.dispose());
        create.addActionListener(e -> {
            String password = new String(pw.getPassword());
            String confirm = new String(pw2.getPassword());
            String sid = id.getText().trim(), nm = name.getText().trim(), mail = email.getText().trim();
            if (sid.isEmpty() || nm.isEmpty() || mail.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Ui.showMessage(d, "Please fill in all fields.", "Missing information", JOptionPane.ERROR_MESSAGE);
            } else if (!sid.matches("\\d{1,12}")) {
                Ui.showMessage(d, "Student ID must be numbers only, up to 12 digits.", "Invalid Student ID", JOptionPane.ERROR_MESSAGE);
            } else if (!Ui.isValidEmail(mail)) {
                Ui.showMessage(d, "Please enter a valid email address.", "Invalid email", JOptionPane.ERROR_MESSAGE);
            } else if (password.length() < 6) {
                Ui.showMessage(d, "Password must be at least 6 characters.", "Weak password", JOptionPane.ERROR_MESSAGE);
            } else if (!password.equals(confirm)) {
                Ui.showMessage(d, "The two passwords do not match.", "Password mismatch", JOptionPane.ERROR_MESSAGE);
            } else {
                String problem = DataStore.checkRegistration(sid, nm, mail);   // school records + duplicate ID or email
                if (problem != null) {
                    Ui.showMessage(d, problem, "Cannot create account", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!verifyEmail(d, mail)) return;
                String result = DataStore.registerStudent(new Student(sid, nm, mail, (String) course.getSelectedItem(), "N/A", "student"), password);
                if (result != null) {
                    Ui.showMessage(d, result, "Cannot create account", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Ui.showMessage(d, "Account created! You can now sign in.", "Welcome to ReviewerShare", Ui.SUCCESS_MESSAGE);
                sId.setText(sid);
                sPw.setText("");
                d.dispose();
            }
        });

        d.add(Ui.scroll(form), BorderLayout.CENTER);
        d.add(buttonRow(cancel, create), BorderLayout.SOUTH);
        d.setVisible(true);
    }

    /** Sends (shows) a 6-digit code and asks for it. Three tries. Demo mode: a real system would email the code. */
    private boolean verifyEmail(Component parent, String email) {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        showCodeDialog(parent, code);
        for (int attempt = 1; attempt <= 3; attempt++) {
            String typed = Ui.showInput(parent, "Type the 6-digit code that was sent to " + email + ":", "Verify your email", JOptionPane.QUESTION_MESSAGE);
            if (typed == null) return false;
            if (typed.trim().equals(code)) return true;
            Ui.showMessage(parent, attempt < 3 ? "That code is not correct. Please try again (" + (3 - attempt) + " left)."
                    : "That code is not correct. Please start again.", "Wrong code", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    // ---------- Admin sign-up, step 1: ask a current admin ----------
    private void showAdminRequestDialog() {
        JDialog d = dialog("Request admin access", 520, 560);
        JTextField id = Ui.field(), name = Ui.field(), email = Ui.field();
        Ui.limitInput(id, 12, false);

        JPanel form = formPanel();
        form.add(left(Ui.label("Ask a current admin for approval", Ui.H2, Ui.TEXT)));
        form.add(Box.createVerticalStrut(4));
        form.add(left(Ui.label("<html><body style='width:350px'>Every current administrator is notified. After one of them approves you, come back and click \"Register (approved)\" to create your admin account.</body></html>", Ui.SMALL, Ui.MUTED)));
        form.add(Box.createVerticalStrut(16));
        addRow(form, "Admin ID you want (3-12 letters, numbers or _)", id);
        addRow(form, "Full Name", name);
        addRow(form, "Email", email);

        JButton send = Ui.primaryButton("Send Request");
        JButton cancel = Ui.plainButton("Cancel");
        cancel.addActionListener(e -> d.dispose());
        send.addActionListener(e -> {
            String aid = id.getText().trim(), nm = name.getText().trim(), mail = email.getText().trim();
            if (aid.isEmpty() || nm.isEmpty() || mail.isEmpty()) {
                Ui.showMessage(d, "Please fill in all fields.", "Missing information", JOptionPane.ERROR_MESSAGE);
            } else if (!aid.matches("(?=.*[A-Za-z])[A-Za-z0-9_]{3,12}")) {
                Ui.showMessage(d, "The Admin ID must be 3 to 12 letters, numbers or underscores, with at least one letter.", "Invalid Admin ID", JOptionPane.ERROR_MESSAGE);
            } else if (!Ui.isValidEmail(mail)) {
                Ui.showMessage(d, "Please enter a valid email address.", "Invalid email", JOptionPane.ERROR_MESSAGE);
            } else {
                String problem = DataStore.requestAdminAccess(aid, nm, mail);
                if (problem != null) {
                    Ui.showMessage(d, problem, "Could not send request", JOptionPane.ERROR_MESSAGE);
                } else {
                    Ui.showMessage(d, "Your request was sent to the current admins.\nYou can register as soon as one of them approves it.");
                    aId.setText(aid);
                    watchedAdminId = aid;
                    d.dispose();
                }
            }
        });

        d.add(form, BorderLayout.CENTER);
        d.add(buttonRow(cancel, send), BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ---------- Admin sign-up, step 2: register after approval ----------
    private void showAdminRegisterDialog() {
        JDialog d = dialog("Register as administrator", 520, 580);
        JTextField id = Ui.field(), email = Ui.field();
        JPasswordField pw = Ui.passwordField(), pw2 = Ui.passwordField();
        Ui.limitInput(id, 12, false);
        id.setText(aId.getText().trim());

        JPanel form = formPanel();
        form.add(left(Ui.label("Create your admin account", Ui.H2, Ui.TEXT)));
        form.add(Box.createVerticalStrut(4));
        form.add(left(Ui.label("<html><body style='width:350px'>Use the same Admin ID and email you sent in your request. This only works after a current admin approved it.</body></html>", Ui.SMALL, Ui.MUTED)));
        form.add(Box.createVerticalStrut(16));
        addRow(form, "Admin ID", id);
        addRow(form, "Email used in the request", email);
        addRow(form, "Password (min 6 characters)", pw);
        addRow(form, "Confirm Password", pw2);

        JButton create = Ui.primaryButton("Create Admin Account");
        JButton cancel = Ui.plainButton("Cancel");
        cancel.addActionListener(e -> d.dispose());
        create.addActionListener(e -> {
            String aid = id.getText().trim(), mail = email.getText().trim();
            String a = new String(pw.getPassword()), b = new String(pw2.getPassword());
            if (aid.isEmpty() || mail.isEmpty() || a.isEmpty() || b.isEmpty()) {
                Ui.showMessage(d, "Please fill in all fields.", "Missing information", JOptionPane.ERROR_MESSAGE);
            } else if (a.length() < 6) {
                Ui.showMessage(d, "Password must be at least 6 characters.", "Weak password", JOptionPane.ERROR_MESSAGE);
            } else if (!a.equals(b)) {
                Ui.showMessage(d, "The two passwords do not match.", "Password mismatch", JOptionPane.ERROR_MESSAGE);
            } else {
                String problem = DataStore.registerApprovedAdmin(aid, mail, a);
                if (problem != null) {
                    Ui.showMessage(d, problem, "Could not register", JOptionPane.ERROR_MESSAGE);
                } else {
                    Ui.showMessage(d, "Admin account created! You can now sign in.");
                    aId.setText(aid);
                    aPw.setText("");
                    d.dispose();
                }
            }
        });

        d.add(form, BorderLayout.CENTER);
        d.add(buttonRow(cancel, create), BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ---------- Forgot password (students and admins) ----------
    /** Shows the code with a clear warning, because it disappears when the user clicks OK. */
    private void showCodeDialog(Component parent, String code) {
        StringBuilder spaced = new StringBuilder();
        for (char c : code.toCharArray()) spaced.append(c).append(' ');

        JPanel box = Ui.clear(new JPanel());
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(left(Ui.label("DEMO MODE - no real email is sent.", Ui.SMALL, Ui.MUTED)));
        box.add(Box.createVerticalStrut(10));
        box.add(left(Ui.label("Your verification code is:", Ui.BOLD, Ui.TEXT)));
        box.add(Box.createVerticalStrut(4));
        box.add(left(Ui.label(spaced.toString().trim(), new Font("Monospaced", Font.BOLD, 38), Ui.ACCENT)));
        box.add(Box.createVerticalStrut(12));

        Ui.RoundPanel warn = Ui.pastel(Ui.P_PEACH);
        warn.setLayout(new BorderLayout());
        warn.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        warn.add(Ui.label("<html><body style='width:280px'><b>Remember this code clearly!</b><br>Memorize it or write it down before you click OK. "
                + "The code disappears as soon as this message closes and will not be shown again.</body></html>", Ui.SMALL, Ui.TEXT), BorderLayout.CENTER);
        box.add(left(warn));
        Ui.showMessage(parent, box, "Verification code - remember it!", JOptionPane.WARNING_MESSAGE);
    }

    private void showForgotDialog(boolean admin) {
        JDialog d = dialog(admin ? "Reset your admin password" : "Reset your password", 440, 470);
        CardLayout steps = new CardLayout();
        JPanel stack = new JPanel(steps);
        stack.setBackground(Ui.CARD);
        String[] code = new String[1];
        int[] tries = {0};

        // Step 1: who are you?
        JTextField id = Ui.field(), email = Ui.field();
        Ui.limitInput(id, 12, false);
        id.setText((admin ? aId : sId).getText().trim());
        JLabel msg1 = Ui.label(" ", Ui.SMALL, Ui.RED);
        JButton cancel1 = Ui.plainButton("Cancel");
        JButton send = Ui.primaryButton("Send Verification Code");
        cancel1.addActionListener(e -> d.dispose());
        JPanel form1 = formPanel();
        form1.add(Ui.label("Forgot your password?", Ui.H2, Ui.TEXT));
        form1.add(Box.createVerticalStrut(4));
        form1.add(Ui.label("<html>Enter your " + (admin ? "Admin ID" : "Student ID") + " and the email you registered with.</html>", Ui.SMALL, Ui.MUTED));
        form1.add(Box.createVerticalStrut(16));
        addRow(form1, admin ? "Admin ID" : "Student ID", id);
        addRow(form1, "Registered Email", email);
        form1.add(msg1);
        JPanel step1 = new JPanel(new BorderLayout());
        step1.setBackground(Ui.CARD);
        step1.add(form1, BorderLayout.CENTER);
        step1.add(buttonRow(cancel1, send), BorderLayout.SOUTH);

        // Step 2: code + new password
        JTextField codeField = Ui.field();
        Ui.limitInput(codeField, 6, true);
        JPasswordField np = Ui.passwordField(), np2 = Ui.passwordField();
        JLabel msg2 = Ui.label(" ", Ui.SMALL, Ui.RED);
        JButton back = Ui.plainButton("Get a new code");
        JButton reset = Ui.primaryButton("Reset Password");
        back.addActionListener(e -> steps.show(stack, "1"));
        JPanel form2 = formPanel();
        form2.add(Ui.label("Create a new password", Ui.H2, Ui.TEXT));
        form2.add(Box.createVerticalStrut(4));
        form2.add(Ui.label("Forgot the code? Click \"Get a new code\".", Ui.SMALL, Ui.MUTED));
        form2.add(Box.createVerticalStrut(16));
        addRow(form2, "6-digit verification code", codeField);
        addRow(form2, "New Password (min 6 characters)", np);
        addRow(form2, "Confirm New Password", np2);
        form2.add(msg2);
        JPanel step2 = new JPanel(new BorderLayout());
        step2.setBackground(Ui.CARD);
        step2.add(form2, BorderLayout.CENTER);
        step2.add(buttonRow(back, reset), BorderLayout.SOUTH);

        stack.add(step1, "1");
        stack.add(step2, "2");

        send.addActionListener(e -> {
            String sid = id.getText().trim(), mail = email.getText().trim();
            if (sid.isEmpty() || mail.isEmpty()) {
                msg1.setText("Please enter your ID and email.");
            } else if (!DataStore.emailMatches(sid, mail)) {
                msg1.setText("No account matches that ID and email.");
            } else {
                msg1.setText(" ");
                code[0] = String.format("%06d", new Random().nextInt(1_000_000));
                tries[0] = 0;
                showCodeDialog(d, code[0]);
                codeField.setText("");
                msg2.setText(" ");
                steps.show(stack, "2");
            }
        });
        reset.addActionListener(e -> {
            String a = new String(np.getPassword()), b = new String(np2.getPassword());
            if (code[0] == null || !codeField.getText().trim().equals(code[0])) {
                tries[0]++;
                if (tries[0] >= 5) {
                    Ui.showMessage(d, "Too many wrong codes. Please request a new code.", "Too many tries", JOptionPane.WARNING_MESSAGE);
                    code[0] = null;
                    tries[0] = 0;
                    steps.show(stack, "1");
                    return;
                }
                msg2.setText("That verification code is not correct. " + (5 - tries[0]) + " tries left.");
            } else if (a.length() < 6) {
                msg2.setText("Password must be at least 6 characters.");
            } else if (!a.equals(b)) {
                msg2.setText("The two passwords do not match.");
            } else {
                DataStore.resetPassword(id.getText().trim(), a);
                Ui.showMessage(d, "Your password was reset. You can now sign in.");
                (admin ? aId : sId).setText(id.getText().trim());
                (admin ? aPw : sPw).setText("");
                d.dispose();
            }
        });

        d.add(stack, BorderLayout.CENTER);
        d.setVisible(true);
    }
}