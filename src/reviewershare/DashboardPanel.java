package reviewershare;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.util.ArrayList;

/** Dashboard: greeting, metric cards, recent reviewers, glowing gauges, a subject chart, and activity. */
public class DashboardPanel extends JPanel {
    private final MainFrame main;
    private final JLabel greeting = Ui.label("", new Font("SansSerif", Font.BOLD, 20), Ui.TEXT);
    private final JLabel totalVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel sharedVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel pendingVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel completedVal = Ui.label("0", Ui.BIG, Ui.TEXT);
    private final JLabel sharedHint = Ui.label(" ", Ui.SMALL, Ui.WARN);
    private final DefaultTableModel model = Ui.model("Title", "Subject", "Type", "Uploaded By", "Rating");
    private final JTable table = new JTable(model);
    private final JTextArea activityArea = Ui.textArea(6);
    private ArrayList<Material> recent = new ArrayList<>();

    private final Ui.Ring ringLive = new Ui.Ring(Ui.BRAND, 84);
    private final Ui.Ring ringAccepted = new Ui.Ring(Ui.CORAL, 84);
    private final Ui.Ring ringDone = new Ui.Ring(Ui.ACCENT, 84);
    private final Ui.Bar barLibrary = new Ui.Bar(Ui.ACCENT);
    private final Ui.Bar barAnswered = new Ui.Bar(Ui.BRAND);
    private final Ui.BarChart chart = new Ui.BarChart();

    public DashboardPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        // Top: greeting + four metric cards
        JPanel metrics = Ui.clear(new JPanel(new GridLayout(1, 4, 14, 0)));
        metrics.add(metricCard(Ui.P_PURPLE, Ui.BRAND, "Total Reviewers", totalVal, Ui.label("Approved and live", Ui.SMALL, Ui.MUTED)));
        metrics.add(metricCard(Ui.P_PEACH, Ui.CORAL, "My Shared Materials", sharedVal, sharedHint));
        metrics.add(metricCard(Ui.P_BLUE, Ui.BLUE, "Pending Requests", pendingVal, Ui.label("Waiting for your answer", Ui.SMALL, Ui.MUTED)));
        metrics.add(metricCard(Ui.P_MINT, Ui.ACCENT, "Completed Shares", completedVal, Ui.label("Requests finished", Ui.SMALL, Ui.MUTED)));

        JPanel top = Ui.clear(new JPanel(new BorderLayout(0, 12)));
        top.add(greeting, BorderLayout.NORTH);
        top.add(metrics, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Left: Recent Reviewers table (with quick actions) + Reviewers-by-subject chart
        Ui.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        JButton viewBtn = Ui.primaryButton("View Selected");
        JButton share = Ui.plainButton("Share Reviewer");
        JButton browse = Ui.plainButton("Browse Reviewers");
        JButton view = Ui.plainButton("View Requests");
        viewBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a reviewer first.");
            } else {
                main.showDetails(recent.get(row));
            }
        });
        share.addActionListener(e -> main.showPage("Add Reviewer"));
        browse.addActionListener(e -> main.showPage("Reviewers"));
        view.addActionListener(e -> main.showPage("Requests"));
        JPanel btnRow = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6)));
        btnRow.add(viewBtn);
        btnRow.add(share);
        btnRow.add(browse);
        btnRow.add(view);

        Ui.RoundPanel recentCard = Ui.card().setAccent(Ui.BRAND);
        recentCard.setLayout(new BorderLayout(0, 8));
        recentCard.add(Ui.label("Recent Reviewers", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        recentCard.add(Ui.scroll(table), BorderLayout.CENTER);
        recentCard.add(btnRow, BorderLayout.SOUTH);

        Ui.RoundPanel chartCard = Ui.card().setAccent(Ui.CORAL);
        chartCard.setLayout(new BorderLayout(0, 6));
        chartCard.setPreferredSize(new Dimension(0, 200));
        chartCard.add(Ui.label("Live Reviewers by Subject", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        chartCard.add(chart, BorderLayout.CENTER);

        JPanel left = Ui.clear(new JPanel(new BorderLayout(0, 16)));
        left.add(recentCard, BorderLayout.CENTER);
        left.add(chartCard, BorderLayout.SOUTH);

        // Right: Overview (gauges + progress bars) and Recent Activity
        JPanel rings = Ui.clear(new JPanel(new GridLayout(1, 3, 6, 0)));
        rings.add(ringCell(ringLive, "Shares live"));
        rings.add(ringCell(ringAccepted, "Accepted"));
        rings.add(ringCell(ringDone, "Completed"));

        JPanel bars = Ui.clear(new JPanel(new GridLayout(2, 1, 0, 6)));
        bars.add(barLibrary);
        bars.add(barAnswered);

        Ui.RoundPanel overview = Ui.card().setAccent(Ui.ACCENT);
        overview.setLayout(new BorderLayout(0, 12));
        overview.add(Ui.label("Overview", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        overview.add(rings, BorderLayout.CENTER);
        overview.add(bars, BorderLayout.SOUTH);

        activityArea.setEditable(false);
        activityArea.setBackground(Ui.CARD);
        Ui.RoundPanel activity = Ui.card().setAccent(Ui.BLUE);
        activity.setLayout(new BorderLayout(0, 8));
        activity.add(Ui.label("Recent Activity", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        activity.add(Ui.scroll(activityArea), BorderLayout.CENTER);

        JPanel side = Ui.clear(new JPanel(new BorderLayout(0, 16)));
        side.setPreferredSize(new Dimension(350, 0));
        side.add(overview, BorderLayout.NORTH);
        side.add(activity, BorderLayout.CENTER);

        JPanel center = Ui.clear(new JPanel(new BorderLayout(16, 0)));
        center.add(left, BorderLayout.CENTER);
        center.add(side, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
    }

    private JPanel metricCard(Color tint, Color accent, String caption, JLabel value, JLabel hint) {
        Ui.RoundPanel p = Ui.pastel(tint).setAccent(accent);
        p.setLayout(new BorderLayout(0, 2));
        p.setBorder(BorderFactory.createEmptyBorder(18, 16, 12, 16));
        p.add(Ui.label(caption, Ui.BOLD, Ui.MUTED), BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private JPanel ringCell(Ui.Ring ring, String caption) {
        JPanel p = Ui.clear(new JPanel());
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        ring.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel l = Ui.label(caption, Ui.SMALL, Ui.MUTED);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(ring);
        p.add(Box.createVerticalStrut(2));
        p.add(l);
        return p;
    }

    private static String pct(double v) {
        return Math.round(v * 100) + "%";
    }

    private static String shortSubject(String s) {
        return s.equals("Computer Science") ? "Comp. Sci." : s.equals("Mathematics") ? "Math" : s;
    }

    /** Reloads greeting, metrics, table, gauges, chart, and activity from the data files. */
    public void refresh() {
        Student user = DataStore.currentUser;
        int hour = LocalTime.now().getHour();
        String part = hour < 12 ? "morning" : hour < 18 ? "afternoon" : "evening";
        greeting.setText("Good " + part + ", " + user.name.split(" ")[0]);

        ArrayList<Material> approved = DataStore.approvedMaterials();   // already newest first
        ArrayList<Material> mine = DataStore.myMaterials();
        int awaiting = 0, liveMine = 0;
        for (Material m : mine) {
            if (Material.PENDING.equals(m.status)) awaiting++;
            if (Material.APPROVED.equals(m.status)) liveMine++;
        }

        totalVal.setText(String.valueOf(approved.size()));
        sharedVal.setText(String.valueOf(mine.size()));
        sharedHint.setText(awaiting > 0 ? awaiting + " awaiting admin approval" : " ");
        pendingVal.setText(String.valueOf(DataStore.pendingCount()));
        completedVal.setText(String.valueOf(DataStore.completedRequests().size()));

        // Gauges
        double live = mine.isEmpty() ? 0 : liveMine / (double) mine.size();
        ringLive.setValue(live, pct(live));
        int active = 0, accepted = 0, done = 0;
        for (Request r : DataStore.sentRequests()) {
            if (r.status.equals("Cancelled")) continue;
            active++;
            if (r.status.equals("Approved") || r.status.equals("Completed")) accepted++;
            if (r.status.equals("Completed")) done++;
        }
        double acc = active == 0 ? 0 : accepted / (double) active;
        double dn = active == 0 ? 0 : done / (double) active;
        ringAccepted.setValue(acc, pct(acc));
        ringDone.setValue(dn, pct(dn));

        // Progress bars
        int pend = DataStore.countMaterials(Material.PENDING);
        int rej = DataStore.countMaterials(Material.REJECTED);
        int all = approved.size() + pend + rej;
        double lib = all == 0 ? 0 : approved.size() / (double) all;
        barLibrary.setValue(lib, "Library approval rate", pct(lib));
        ArrayList<Request> incoming = DataStore.incomingRequests();
        int answered = 0;
        for (Request r : incoming) if (!r.status.equals("Pending")) answered++;
        double ans = incoming.isEmpty() ? 0 : answered / (double) incoming.size();
        barAnswered.setValue(ans, "Requests you answered", pct(ans));

        // Chart: approved materials per subject
        String[] subjects = DataStore.SUBJECTS;
        String[] labels = new String[subjects.length];
        double[] counts = new double[subjects.length];
        for (int i = 0; i < subjects.length; i++) {
            labels[i] = shortSubject(subjects[i]);
            for (Material m : approved) if (m.subject.equals(subjects[i])) counts[i]++;
        }
        chart.setData(labels, counts, new Color[]{Ui.ACCENT, Ui.CORAL, Ui.BRAND, Ui.BLUE});

        // Table
        int keep = Ui.selId(table, recent, m -> m.id);
        recent = new ArrayList<>(approved.subList(0, Math.min(5, approved.size())));
        model.setRowCount(0);
        for (Material m : recent) {
            model.addRow(new Object[]{m.title, m.subject, m.type, m.uploaderName, m.getStars()});
        }
        Ui.reselect(table, recent, m -> m.id, keep);

        StringBuilder sb = new StringBuilder();
        for (String a : DataStore.activitiesFor(user.id, 20)) sb.append("• ").append(a).append("\n\n");
        activityArea.setText(sb.toString());
        activityArea.setCaretPosition(0);
    }
}