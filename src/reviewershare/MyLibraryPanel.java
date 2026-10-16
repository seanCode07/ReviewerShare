package reviewershare;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

/** My Library: materials I shared (with their moderation status), and materials I requested. */
public class MyLibraryPanel extends JPanel {
    private final MainFrame main;
    private final DefaultTableModel sharedModel = Ui.model("Title", "Subject", "Type", "Date Shared", "Requests", "Status");
    private final DefaultTableModel requestedModel = Ui.model("Reviewer Title", "Owner", "Request Date", "Status");
    private final JTable sharedTable = new JTable(sharedModel);
    private final JTable requestedTable = new JTable(requestedModel);
    private final JLabel noteLabel = Ui.label(" ", Ui.SMALL, Ui.RED);
    private ArrayList<Material> sharedList = new ArrayList<>();
    private ArrayList<Request> sentList = new ArrayList<>();

    public MyLibraryPanel(MainFrame main) {
        this.main = main;
        setLayout(new GridLayout(2, 1, 0, 14));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        Ui.styleTable(sharedTable);
        Ui.styleTable(requestedTable);
        sharedTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateNote();
        });

        // ----- Shared by Me -----
        JButton addNew = Ui.primaryButton("+ Add New");
        JButton view = Ui.plainButton("View");
        JButton openOwn = Ui.plainButton("Open File");
        JButton edit = Ui.plainButton("Edit");
        JButton delete = Ui.dangerButton("Delete");
        addNew.addActionListener(e -> main.showPage("Add Reviewer"));
        view.addActionListener(e -> {
            Material m = selected();
            if (m != null) main.showDetails(m);
        });
        openOwn.addActionListener(e -> {
            Material m = selected();
            if (m != null) Ui.openMaterialFile(this, m);
        });
        edit.addActionListener(e -> {
            Material m = selected();
            if (m != null) showEditDialog(m);
        });
        delete.addActionListener(e -> {
            Material m = selected();
            if (m != null && JOptionPane.showConfirmDialog(this, "Delete \"" + m.title + "\"? This cannot be undone.",
                    "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                DataStore.deleteMaterial(m.id);
                main.refreshAll();
            }
        });
        JPanel buttons = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)));
        buttons.add(addNew);
        buttons.add(view);
        buttons.add(openOwn);
        buttons.add(edit);
        buttons.add(delete);

        JPanel south = Ui.clear(new JPanel(new BorderLayout(0, 2)));
        south.add(noteLabel, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.CENTER);

        JPanel sharedCard = Ui.card();
        sharedCard.setLayout(new BorderLayout(0, 6));
        sharedCard.add(Ui.label("Shared by Me", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        sharedCard.add(Ui.scroll(sharedTable), BorderLayout.CENTER);
        sharedCard.add(south, BorderLayout.SOUTH);
        add(sharedCard);

        // ----- Requested Materials -----
        JButton viewReq = Ui.primaryButton("View File");
        viewReq.addActionListener(e -> viewRequestedFile());
        JButton cancel = Ui.dangerButton("Cancel Request");
        cancel.addActionListener(e -> cancelSelectedRequest());
        JPanel reqButtons = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)));
        reqButtons.add(viewReq);
        reqButtons.add(cancel);

        JPanel reqCard = Ui.card();
        reqCard.setLayout(new BorderLayout(0, 6));
        reqCard.add(Ui.label("Requested Materials", Ui.H2, Ui.TEXT), BorderLayout.NORTH);
        reqCard.add(Ui.scroll(requestedTable), BorderLayout.CENTER);
        reqCard.add(reqButtons, BorderLayout.SOUTH);
        add(reqCard);
    }

    /** The selected material in the Shared table, or null (no message). */
    private Material selectedQuiet() {
        int row = sharedTable.getSelectedRow();
        return (row >= 0 && row < sharedList.size()) ? sharedList.get(row) : null;
    }

    /** Returns the selected material in the Shared table, or shows a message if none. */
    private Material selected() {
        Material m = selectedQuiet();
        if (m == null) JOptionPane.showMessageDialog(this, "Please select a material first.");
        return m;
    }

    /** Shows the admin's rejection reason (or a waiting note) under the table. */
    private void updateNote() {
        Material m = selectedQuiet();
        if (m == null) {
            noteLabel.setText(" ");
        } else if (Material.REJECTED.equals(m.status)) {
            noteLabel.setForeground(Ui.RED);
            noteLabel.setText("Rejected by admin: " + m.rejectReason + "  -  click Edit and save to resubmit.");
        } else if (Material.PENDING.equals(m.status)) {
            noteLabel.setForeground(Ui.WARN);
            noteLabel.setText("Waiting for admin approval - not visible to other students yet.");
        } else {
            noteLabel.setText(" ");
        }
    }

    /** Opens the file of a request that the owner approved. */
    private void viewRequestedFile() {
        int row = requestedTable.getSelectedRow();
        if (row < 0 || row >= sentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a request first.");
            return;
        }
        Request r = sentList.get(row);
        if (!r.status.equals("Approved") && !r.status.equals("Completed")) {
            JOptionPane.showMessageDialog(this, "You can view the file once the owner approves your request.");
            return;
        }
        Ui.openMaterialFile(this, DataStore.findMaterial(r.materialId));
    }

    private void cancelSelectedRequest() {
        int row = requestedTable.getSelectedRow();
        if (row < 0 || row >= sentList.size()) {
            JOptionPane.showMessageDialog(this, "Please select a request first.");
            return;
        }
        Request r = sentList.get(row);
        if (!r.status.equals("Pending")) {
            JOptionPane.showMessageDialog(this, "Only requests with status \"Pending\" can be cancelled.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Cancel your request for \"" + r.materialTitle + "\"?", "Cancel request",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (DataStore.cancelRequest(r)) {
            JOptionPane.showMessageDialog(this, "Your request was cancelled.");
        } else {
            JOptionPane.showMessageDialog(this, "This request can no longer be cancelled - the owner already responded.");
        }
        main.refreshAll();
    }

    /** A dialog for editing title, subject, type, and description. Edits to a live or rejected material go back to the admin. */
    private void showEditDialog(Material m) {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Edit Material", Dialog.ModalityType.APPLICATION_MODAL);
        JTextField title = Ui.field();
        title.setText(m.title);
        JComboBox<String> subject = Ui.style(new JComboBox<>(DataStore.SUBJECTS));
        JComboBox<String> type = Ui.style(new JComboBox<>(DataStore.TYPES));
        JTextArea desc = Ui.textArea(4);
        desc.setText(m.description);
        subject.setSelectedItem(m.subject);
        type.setSelectedItem(m.type);
        boolean needsReview = !Material.PENDING.equals(m.status);

        JPanel form = Ui.clear(new JPanel());
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(18, 20, 8, 20));
        addRow(form, "Title", title);
        addRow(form, "Subject", subject);
        addRow(form, "Type", type);
        addRow(form, "Description", Ui.scrollBox(desc));
        if (needsReview) {
            JLabel note = Ui.label("<html><body style='width:380px'>Note: after you save, an admin will check this reviewer again before it is shown in the library.</body></html>", Ui.SMALL, Ui.WARN);
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(note);
        }

        JButton save = Ui.primaryButton(needsReview ? "Save & Resubmit" : "Save");
        JButton cancel = Ui.plainButton("Cancel");
        save.addActionListener(e -> {
            if (title.getText().trim().isEmpty() || desc.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(d, "Title and description cannot be empty.", "Missing information", JOptionPane.ERROR_MESSAGE);
                return;
            }
            m.title = title.getText().trim();
            m.subject = (String) subject.getSelectedItem();
            m.type = (String) type.getSelectedItem();
            m.description = desc.getText().trim();
            DataStore.updateMaterial(m, needsReview);
            d.dispose();
            main.refreshAll();
        });
        cancel.addActionListener(e -> d.dispose());
        JPanel btns = Ui.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10)));
        btns.add(cancel);
        btns.add(save);

        d.add(form, BorderLayout.CENTER);
        d.add(btns, BorderLayout.SOUTH);
        d.setSize(500, needsReview ? 520 : 480);
        d.setLocationRelativeTo(this);
        d.getContentPane().setBackground(Ui.themeCard());
        if (Ui.light) Ui.applyTheme(d.getContentPane(), true);
        d.setVisible(true);
    }

    private void addRow(JPanel form, String caption, JComponent c) {
        JLabel l = Ui.label(caption, Ui.BOLD, Ui.TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        form.add(l);
        form.add(Box.createVerticalStrut(4));
        form.add(c);
        form.add(Box.createVerticalStrut(10));
    }

    /** Reloads both tables (keeping your selected rows). */
    public void refresh() {
        int keepShared = Ui.selId(sharedTable, sharedList, m -> m.id);
        int keepReq = Ui.selId(requestedTable, sentList, r -> r.id);

        sharedList = DataStore.myMaterials();
        sharedModel.setRowCount(0);
        for (Material m : sharedList) {
            sharedModel.addRow(new Object[]{m.title, m.subject, m.type, m.getDateText(), m.requestCount, m.status});
        }
        sentList = DataStore.sentRequests();
        requestedModel.setRowCount(0);
        for (Request r : sentList) {
            requestedModel.addRow(new Object[]{r.materialTitle, r.ownerName, r.getDateText(), r.status});
        }
        Ui.reselect(sharedTable, sharedList, m -> m.id, keepShared);
        Ui.reselect(requestedTable, sentList, r -> r.id, keepReq);
        updateNote();
    }
}