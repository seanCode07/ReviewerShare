package reviewershare;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;

/** Reviewer Library: search, filters, sorting, and a 3-column grid of approved material cards. */
public class ReviewerLibraryPanel extends JPanel {
    private final MainFrame main;
    private final JTextField search = Ui.field();
    private final JComboBox<String> typeBox = Ui.style(new JComboBox<>(new String[]{"All", "Reviewer", "Notes", "Practice Test"}));
    private final JComboBox<String> subjectBox = Ui.style(new JComboBox<>());
    private final JComboBox<String> sortBox = Ui.style(new JComboBox<>(new String[]{"Recently Added", "Highest Rated"}));
    private final JLabel countLabel = Ui.label("", Ui.BOLD, Ui.TEXT);
    private final JPanel grid = Ui.clear(new JPanel(new GridLayout(0, 3, 14, 14)));

    public ReviewerLibraryPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        subjectBox.addItem("All Subjects");
        for (String s : DataStore.SUBJECTS) subjectBox.addItem(s);

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
        north.add(Ui.label("Find reviewers and study materials shared by your classmates.", Ui.BODY, Ui.MUTED), BorderLayout.NORTH);
        north.add(filters, BorderLayout.CENTER);
        north.add(countLabel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        // Card grid inside a scroll pane
        JPanel wrapper = Ui.clear(new JPanel(new BorderLayout()));
        wrapper.add(grid, BorderLayout.NORTH);
        JScrollPane sp = Ui.scroll(wrapper);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);

        // Re-filter whenever something changes (added last so everything exists)
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        typeBox.addActionListener(e -> refresh());
        subjectBox.addActionListener(e -> refresh());
        sortBox.addActionListener(e -> refresh());
    }

    /** Applies the search, filters, and sort, then rebuilds the cards. Only approved materials are shown. */
    public void refresh() {
        String q = search.getText().trim().toLowerCase();
        String type = (String) typeBox.getSelectedItem();
        String subject = (String) subjectBox.getSelectedItem();

        ArrayList<Material> list = new ArrayList<>();
        for (Material m : DataStore.approvedMaterials()) {
            if (!type.equals("All") && !m.type.equals(type)) continue;
            if (!subject.equals("All Subjects") && !m.subject.equals(subject)) continue;
            if (!q.isEmpty() && !(m.title.toLowerCase().contains(q) || m.subject.toLowerCase().contains(q)
                    || m.uploaderName.toLowerCase().contains(q))) continue;
            list.add(m);
        }
        if (sortBox.getSelectedIndex() == 1) {
            list.sort((a, b) -> Double.compare(b.rating, a.rating));
        } // otherwise the database already returned them newest first

        countLabel.setText("Showing " + list.size() + " materials");
        grid.removeAll();
        for (Material m : list) grid.add(makeCard(m));
        // keep the 3-column grid even when there are only 1 or 2 cards
        for (int i = list.size(); i > 0 && i < 3; i++) grid.add(Ui.clear(new JPanel()));
        grid.revalidate();
        grid.repaint();
    }

    /** One material card (pastel color depends on the material type). */
    private JPanel makeCard(Material m) {
        boolean notes = m.type.equals("Notes"), test = m.type.equals("Practice Test");
        Color c = notes ? Ui.P_PEACH : test ? Ui.P_MINT : Ui.P_PURPLE;
        Color accent = notes ? Ui.CORAL : test ? Ui.ACCENT : Ui.BRAND;
        Ui.RoundPanel card = Ui.pastel(c).setAccent(accent);
        card.setBorder(BorderFactory.createEmptyBorder(20, 16, 16, 16));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        String desc = m.description.length() > 80 ? m.description.substring(0, 80) + "..." : m.description;
        JLabel stars = Ui.label(m.getStars(), Ui.BODY, Ui.ORANGE);
        JButton view = Ui.primaryButton("View Details");
        view.addActionListener(e -> main.showDetails(m));

        card.add(left(Ui.label(m.title, new Font("SansSerif", Font.BOLD, 15), Ui.TEXT)));
        card.add(Box.createVerticalStrut(2));
        card.add(left(Ui.label(m.type + "  •  " + m.subject, Ui.SMALL, accent)));
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