package reviewershare;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The main window: sidebar on the left, a CardLayout with all screens on the right.
 * A timer checks the data files every 2 seconds, so changes made in another window show up automatically.
 */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = Ui.clear(new JPanel(cards));
    private final JLabel pageTitle = Ui.label("", Ui.H1, Ui.TEXT);
    private final JLabel pageSub = Ui.label(" ", Ui.SMALL, Ui.MUTED);
    private final Map<String, Ui.RoundButton> navButtons = new LinkedHashMap<>();
    private final JLabel userName = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JLabel userRole = Ui.label("", Ui.SMALL, Ui.MUTED);
    private final Ui.Avatar avatar = new Ui.Avatar("", 42, true, Ui.CORAL);
    private final JLabel toastLabel = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final Ui.RoundPanel toast = Ui.pastel(Ui.P_MINT);
    private final Ui.BellButton bell = new Ui.BellButton(this::showNotifications);
    private final javax.swing.Timer toastTimer;
    private final javax.swing.Timer poll;
    private final boolean admin;
    private long lastVersion;
    private long lastActivityId;
    private int unread;

    // Student screens
    private DashboardPanel dashboard;
    private AddReviewerPanel addPanel;
    private RequestsPanel requests;
    private MyLibraryPanel myLibrary;
    // Admin screen
    private AdminPanel adminPanel;
    private AdminManagePanel managePanel;
    // Shared screens
    private final ReviewerLibraryPanel library;
    private final ReviewerDetailsPanel details;
    private final SettingsPanel settings;

    public MainFrame() {
        super("ReviewerShare - " + DataStore.currentUser.name + (DataStore.currentUser.isAdmin() ? " (Admin)" : ""));
        admin = DataStore.currentUser.isAdmin();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Ui.applyIcon(this);
        setSize(1340, 840);
        setMinimumSize(new Dimension(1120, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Ui.BG);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setLayout(new BorderLayout(18, 0));

        library = new ReviewerLibraryPanel(this);
        details = new ReviewerDetailsPanel(this);
        settings = new SettingsPanel(this);
        content.add(library, "Reviewers");
        content.add(details, "Details");
        content.add(settings, "Settings");
        if (admin) {
            adminPanel = new AdminPanel(this);
            content.add(adminPanel, "Moderation");
            managePanel = new AdminManagePanel(this);
            content.add(managePanel, "Management");
        } else {
            dashboard = new DashboardPanel(this);
            addPanel = new AddReviewerPanel(this);
            requests = new RequestsPanel(this);
            myLibrary = new MyLibraryPanel(this);
            content.add(dashboard, "Dashboard");
            content.add(addPanel, "Add Reviewer");
            content.add(requests, "Requests");
            content.add(myLibrary, "My Library");
        }

        // Top bar: page title on the left; toast, bell and user on the right
        toast.setLayout(new BorderLayout());
        toast.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        toast.add(toastLabel, BorderLayout.CENTER);
        toast.setVisible(false);
        JPanel nameBox = Ui.clear(new JPanel(new GridLayout(2, 1)));
        nameBox.add(userName);
        nameBox.add(userRole);
        JPanel rightBox = Ui.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)));
        rightBox.add(toast);
        rightBox.add(bell);
        rightBox.add(avatar);
        rightBox.add(nameBox);
        JPanel titleBox = Ui.clear(new JPanel(new GridLayout(2, 1, 0, 0)));
        titleBox.add(pageTitle);
        titleBox.add(pageSub);
        JPanel top = Ui.clear(new JPanel(new BorderLayout(12, 0)));
        top.add(titleBox, BorderLayout.WEST);
        top.add(rightBox, BorderLayout.EAST);
        top.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));

        JPanel right = Ui.clear(new JPanel(new BorderLayout(0, 8)));
        right.add(top, BorderLayout.NORTH);
        right.add(content, BorderLayout.CENTER);

        add(buildSidebar(), BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        toastTimer = new javax.swing.Timer(7000, e -> toast.setVisible(false));
        toastTimer.setRepeats(false);

        lastVersion = Db.version();
        lastActivityId = DataStore.latestActivityId(DataStore.currentUser.id);
        showPage(admin ? "Moderation" : "Dashboard");

        poll = new javax.swing.Timer(2000, e -> pollChanges());
        poll.start();
    }

    /** Builds the sidebar with navigation buttons. */
    private JPanel buildSidebar() {
        Ui.RoundPanel side = Ui.card();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(250, 0));
        side.setBorder(BorderFactory.createEmptyBorder(22, 14, 16, 14));

        JPanel brand = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)));
        brand.add(Ui.logoComponent(42));
        JPanel names = Ui.clear(new JPanel(new GridLayout(2, 1)));
        names.add(Ui.label("ReviewerShare", Ui.H2, Ui.TEXT));
        names.add(Ui.label("Quality Education", new Font("SansSerif", Font.PLAIN, 11), Ui.MUTED));
        brand.add(names);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        side.add(brand);
        side.add(Box.createVerticalStrut(26));

        side.add(caption("MAIN MENU"));
        String[] pages = admin ? new String[]{"Moderation", "Management", "Reviewers"}
                : new String[]{"Dashboard", "Reviewers", "Add Reviewer", "Requests", "My Library"};
        for (String name : pages) addNav(side, name);

        side.add(Box.createVerticalGlue());

        Ui.RoundPanel promo = Ui.pastel(new Color(0x5A62E0));
        promo.setGradient(new Color(0x1BB5B0));
        promo.setLayout(new GridLayout(0, 1, 0, 2));
        promo.add(Ui.label("SDG 4", new Font("SansSerif", Font.BOLD, 22), Color.WHITE));
        promo.add(Ui.label("<html><body style='width:150px'>Quality Education for all students.</body></html>", Ui.SMALL, new Color(0xEEF0FF)));
        promo.setAlignmentX(Component.LEFT_ALIGNMENT);
        promo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
        side.add(promo);
        side.add(Box.createVerticalStrut(18));

        side.add(caption("ACCOUNT"));
        addNav(side, "Settings");
        Ui.RoundButton logout = Ui.navButton("Logout").setIconName("Logout");
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        logout.addActionListener(e -> logout());
        side.add(logout);
        return side;
    }

    private JLabel caption(String text) {
        JLabel l = Ui.label(text, new Font("SansSerif", Font.BOLD, 11), Ui.MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 0));
        return l;
    }

    private void addNav(JPanel side, String name) {
        Ui.RoundButton b = Ui.navButton(name).setIconName(name);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.addActionListener(e -> showPage(name));
        navButtons.put(name, b);
        side.add(b);
        side.add(Box.createVerticalStrut(4));
    }

    /** Shows one screen, refreshes the data, and highlights the sidebar button. */
    public void showPage(String name) {
        refreshAll(true);
        cards.show(content, name);

        String active = name.equals("Details") ? "Reviewers" : name;
        for (Map.Entry<String, Ui.RoundButton> e : navButtons.entrySet()) {
            e.getValue().setActive(e.getKey().equals(active));
        }
        switch (name) {
            case "Dashboard":
                pageTitle.setText("Dashboard");
                pageSub.setText("Your sharing activity at a glance");
                break;
            case "Reviewers":
                pageTitle.setText("Reviewer Library");
                pageSub.setText("Approved materials from your classmates");
                break;
            case "Details":
                pageTitle.setText("Reviewer Details");
                pageSub.setText("Everything about this material");
                break;
            case "Add Reviewer":
                pageTitle.setText("Share a Reviewer");
                pageSub.setText("Submitted materials are checked by an admin first");
                break;
            case "Requests":
                pageTitle.setText("Reviewer Requests");
                pageSub.setText("Incoming, sent and completed requests");
                break;
            case "My Library":
                pageTitle.setText("My Library");
                pageSub.setText("What you shared and what you requested");
                break;
            case "Moderation":
                pageTitle.setText("Admin Moderation");
                pageSub.setText("Approve or reject new submissions in real time");
                break;
            case "Management":
                pageTitle.setText("Management");
                pageSub.setText("Users, subjects, programs, school records and the audit log");
                break;
            case "Settings":
                pageTitle.setText("Settings");
                pageSub.setText("Your profile and preferences");
                break;
            default:
                pageTitle.setText(name);
                pageSub.setText(" ");
        }
    }

    /** Opens the details screen for one material. */
    public void showDetails(Material m) {
        details.showMaterial(m);
        showPage("Details");
    }

    public void refreshAll() {
        refreshAll(true);
    }

    /** Reloads every screen from the data files. (The Settings form is skipped by the timer so it never erases what you are typing.) */
    public void refreshAll(boolean includeSettings) {
        Student me = DataStore.currentUser;
        if (admin) {
            adminPanel.refresh();
            managePanel.refresh();
        } else {
            addPanel.refresh();
            dashboard.refresh();
            requests.refresh();
            myLibrary.refresh();
        }
        library.refresh();
        details.refresh();
        if (includeSettings) settings.refresh();

        userName.setText(me.name);
        userRole.setText(admin ? "Administrator" : me.program() + " • " + me.id);
        avatar.setPerson(me.name);
        bell.setCount(unread);

        if (admin) {
            int n = DataStore.countMaterials(Material.PENDING) + DataStore.pendingAdminRequestCount() + DataStore.openReportCount();
            navButtons.get("Moderation").setText(n > 0 ? "Moderation (" + n + ")" : "Moderation");
        } else {
            int n = DataStore.pendingCount();
            navButtons.get("Requests").setText(n > 0 ? "Requests (" + n + ")" : "Requests");
        }
        Ui.installDefaults();
        Ui.applyTheme(getContentPane(), Ui.light);
        getContentPane().setBackground(Ui.themeBg());
        revalidate();
        repaint();
    }

    /** Runs every 2 seconds: if anything in the data files changed, refresh and show new notifications. */
    private void pollChanges() {
        try {
            long v = Db.version();
            if (v == lastVersion) return;
            lastVersion = v;

            // An admin may have removed or suspended this account while it was open
            String myId = DataStore.currentUser.id;
            if (DataStore.findStudent(myId) == null) {
                poll.stop();
                Ui.showMessage(this, "Your account no longer exists, so you were signed out.", "Signed out", JOptionPane.WARNING_MESSAGE);
                signOut();
                return;
            }
            if (DataStore.isSuspended(myId)) {
                poll.stop();
                String why = DataStore.suspensionReason(myId);
                Ui.showMessage(this, "Your account was suspended by an administrator." + (why.isEmpty() ? "" : "\nReason: " + why),
                        "Account suspended", JOptionPane.WARNING_MESSAGE);
                signOut();
                return;
            }

            long latest = DataStore.latestActivityId(DataStore.currentUser.id);
            ArrayList<DataStore.Activity> fresh = DataStore.newNotifications(DataStore.currentUser.id, lastActivityId, latest);
            lastActivityId = latest;
            if (!fresh.isEmpty()) unread += fresh.size();
            refreshAll(false);

            if (!fresh.isEmpty() && DataStore.notifications) {
                String text = fresh.get(fresh.size() - 1).text;
                if (fresh.size() > 1) text += "  (+" + (fresh.size() - 1) + " more)";
                showToast(text);
            }
        } catch (RuntimeException ex) {
            // The files may be busy for a moment - just try again on the next tick.
        }
    }

    private void showToast(String text) {
        if (text.length() > 80) text = text.substring(0, 77) + "...";
        toastLabel.setText(text);
        toast.setVisible(true);
        toast.revalidate();
        toastTimer.restart();
        Toolkit.getDefaultToolkit().beep();
    }

    /** Clicking the bell lists the most recent activity. */
    private void showNotifications() {
        unread = 0;
        bell.setCount(0);
        StringBuilder sb = new StringBuilder();
        for (String a : DataStore.activitiesFor(DataStore.currentUser.id, 12)) sb.append("• ").append(a).append("\n\n");
        JTextArea area = Ui.textArea(12);
        area.setEditable(false);
        area.setText(sb.length() == 0 ? "No activity yet." : sb.toString());
        area.setCaretPosition(0);
        JScrollPane sp = Ui.scrollBox(area);
        sp.setPreferredSize(new Dimension(460, 300));
        Ui.showMessage(this, sp, "Recent activity", JOptionPane.PLAIN_MESSAGE);
    }

    private void logout() {
        if (Ui.showConfirm(this, "Log out of ReviewerShare?", "Logout", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            signOut();
        }
    }

    /** Closes this window and goes back to the sign-in screen (also used after an account is deleted). */
    public void signOut() {
        poll.stop();
        toastTimer.stop();
        DataStore.currentUser = null;
        Ui.light = false; // the login screen always uses the default navy look
        Ui.installDefaults();
        dispose();
        new LoginFrame().setVisible(true);
    }
}