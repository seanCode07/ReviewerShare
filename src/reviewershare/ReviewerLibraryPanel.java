package reviewershare;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;

/** Reviewer Library: search (title, subject, uploader, topics), filters, sorting, paging, and a 3-column grid of cards. */
public class ReviewerLibraryPanel extends JPanel {
    private static final int PAGE_SIZE = 9;

    private final MainFrame main;
    private final JTextField search = Ui.field();
    private final JComboBox<String> typeBox = Ui.style(new JComboBox<>(new String[]{"All", "Reviewer", "Notes", "Practice Test"}));
    private final JComboBox<String> subjectBox = Ui.style(new JComboBox<>());
    private final JComboBox<String> sortBox = Ui.style(new JComboBox<>(new String[]{"Recently Added", "Highest Rated", "Most Requested", "Title A-Z"}));
    private final JLabel countLabel = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JPanel grid = Ui.clear(new JPanel(new GridLayout(0, 3, 14, 14)));
    private final JLabel pageLabel = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JButton prev = Ui.plainButton("← Previous");
    private final JButton next = Ui.plainButton("Next →");
    private int page = 0;
    private boolean loadingSubjects = false;
    private String subjectKey = "";

    public ReviewerLibraryPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        loadSubjects();

        // Filter bar
        search.setPreferredSize(new Dimension(280, 38));
        for (JComboBox<?> b : new JComboBox<?>[]{typeBox, subjectBox, sortBox}) b.setPreferredSize(new Dimension(150, 36));
        JPanel filters = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        filters.add(Ui.label("Search:", Ui.BOLD, Ui.TEXT));
        filters.add(search);
        filters.add(typeBox);
        filters.add(subjectBox);
        filters.add(sortBox);

        JPanel north = Ui.clear(new JPanel(new BorderLayout(0, 10)));
        north.add(Ui.label("Find reviewers and study materials shared by your classmates. Search also looks inside topics.", Ui.BODY, Ui.MUTED), BorderLayout.NORTH);
        north.add(filters, BorderLayout.CENTER);
        north.add(countLabel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        // Card grid inside a scroll pane
        JPanel wrapper = Ui.clear(new JPanel(new BorderLayout()));
        wrapper.add(grid, BorderLayout.NORTH);
        JScrollPane sp = Ui.scroll(wrapper);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);

        // Paging bar
        prev.addActionListener(e -> {
            page--;
            refresh();
        });
        next.addActionListener(e -> {
            page++;
            refresh();
        });
        JPanel pager = Ui.clear(new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 4)));
        pager.add(prev);
        pager.add(pageLabel);
        pager.add(next);
        add(pager, BorderLayout.SOUTH);

        // Re-filter whenever something changes (added last so everything exists)
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtersChanged(); }
            public void removeUpdate(DocumentEvent e) { filtersChanged(); }
            public void changedUpdate(DocumentEvent e) { filtersChanged(); }
        });
        typeBox.addActionListener(e -> filtersChanged());
        subjectBox.addActionListener(e -> filtersChanged());
        sortBox.addActionListener(e -> filtersChanged());
    }

    private void filtersChanged() {
        if (loadingSubjects) return;
        page = 0;
        refresh();
    }

    /** Subjects are managed by admins, so the list is reloaded when it changes. */
    private void loadSubjects() {
        String[] subjects = DataStore.subjectArray();
        String key = String.join("|", subjects);
        if (key.equals(subjectKey)) return;
        subjectKey = key;
        loadingSubjects = true;
        Object keep = subjectBox.getSelectedItem();
        subjectBox.removeAllItems();
        subjectBox.addItem("All Subjects");
        for (String s : subjects) subjectBox.addItem(s);
        if (keep != null) subjectBox.setSelectedItem(keep);
        if (subjectBox.getSelectedIndex() < 0) subjectBox.setSelectedIndex(0);
        loadingSubjects = false;
    }

    /** Applies the search, filters, sort and page, then rebuilds the cards. Only approved materials are shown. */
    public void refresh() {
        loadSubjects();
        String q = search.getText().trim().toLowerCase();
        String type = (String) typeBox.getSelectedItem();
        String subject = (String) subjectBox.getSelectedItem();
        if (type == null || subject == null) return;

        ArrayList<Material> list = new ArrayList<>();
        for (Material m : DataStore.approvedMaterials()) {
            if (!type.equals("All") && !m.type.equals(type)) continue;
            if (!subject.equals("All Subjects") && !m.subject.equals(subject)) continue;
            if (!q.isEmpty() && !matches(m, q)) continue;
            list.add(m);
        }
        switch (sortBox.getSelectedIndex()) {
            case 1:
                list.sort((a, b) -> Double.compare(b.rating, a.rating));
                break;
            case 2:
                list.sort((a, b) -> b.requestCount - a.requestCount);
                break;
            case 3:
                list.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
                break;
            default: // the database already returned them newest first
        }

        int pages = Math.max(1, (list.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;
        int from = page * PAGE_SIZE, to = Math.min(list.size(), from + PAGE_SIZE);

        countLabel.setText("Showing " + (list.isEmpty() ? 0 : from + 1) + "-" + to + " of " + list.size() + " materials");
        pageLabel.setText("Page " + (page + 1) + " of " + pages);
        prev.setEnabled(page > 0);
        next.setEnabled(page < pages - 1);
        prev.setVisible(pages > 1);
        next.setVisible(pages > 1);

        grid.removeAll();
        for (Material m : list.subList(from, to)) grid.add(makeCard(m));
        // keep the 3-column grid even when there are only 1 or 2 cards
        for (int i = to - from; i > 0 && i < 3; i++) grid.add(Ui.clear(new JPanel()));
        grid.revalidate();
        grid.repaint();
    }

    private boolean matches(Material m, String q) {
        if (m.title.toLowerCase().contains(q) || m.subject.toLowerCase().contains(q)
                || m.uploaderName.toLowerCase().contains(q) || m.description.toLowerCase().contains(q)) return true;
        for (String t : m.topics) if (t.toLowerCase().contains(q)) return true;
        return false;
    }

    /** One material card (pastel color depends on the material type). */
    private JPanel makeCard(Material m) {
        boolean notes = m.type.equals("Notes"), test = m.type.equals("Practice Test");
        Color c = notes ? Ui.P_PEACH : test ? Ui.P_MINT : Ui.P_PURPLE;
        Color accent = notes ? Ui.CORAL : test ? Ui.ACCENT : Ui.BRAND;
        Ui.RoundPanel card = Ui.pastel(c).setAccent(accent);
        card.setBorder(BorderFactory.createEmptyBorder(20, 16, 16, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        String plain = m.description.length() > 80 ? m.description.substring(0, 80) + "..." : m.description;
        String desc = plain.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String files = m.files.isEmpty() ? "" : "  •  " + m.files.size() + (m.files.size() == 1 ? " file" : " files");
        JLabel stars = Ui.label(m.getStars() + (m.ratingCount > 0 ? "  " + String.format("%.1f", m.rating) : ""), Ui.BODY, Ui.ORANGE);
        JButton view = Ui.primaryButton("View Details");
        view.addActionListener(e -> main.showDetails(m));

        card.add(left(Ui.label(m.title, new Font("SansSerif", Font.BOLD, 15), Ui.TEXT)));
        card.add(Box.createVerticalStrut(2));
        card.add(left(Ui.label(m.type + "  •  " + m.subject + files, Ui.SMALL, accent)));
        card.add(Box.createVerticalStrut(8));
        card.add(left(Ui.label("<html><body style='width:220px'>" + desc + "</body></html>", Ui.SMALL, Ui.MUTED)));
        card.add(Box.createVerticalStrut(8));
        card.add(left(Ui.label("By " + m.uploaderName + "  •  " + m.getDateText(), Ui.SMALL, Ui.TEXT)));
        card.add(Box.createVerticalStrut(6));
        card.add(left(stars));
        card.add(Box.createVerticalStrut(10));
        card.add(left(view));
        return card;
    }

    private JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }
}