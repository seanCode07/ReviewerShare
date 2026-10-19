package reviewershare;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

/** "Share a Reviewer" form. Submissions go to the admin for approval first. */
public class AddReviewerPanel extends JPanel {
    private final MainFrame main;
    private final JTextField titleField = Ui.field();
    private final JComboBox<String> subjectBox = Ui.style(new JComboBox<>());
    private final JComboBox<String> typeBox = Ui.style(new JComboBox<>(DataStore.TYPES));
    private final JTextArea descArea = Ui.textArea(5);
    private final JTextArea topicsArea = Ui.textArea(3);
    private final DefaultListModel<File> fileModel = new DefaultListModel<>();
    private final JList<File> fileList = new JList<>(fileModel);
    private final JCheckBox declare = Ui.check("I confirm this is my own work, or I have permission to share it.");
    private String subjectKey = "";

    public AddReviewerPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        loadSubjects();

        JPanel form = Ui.card();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        form.add(left(Ui.label("New Study Material", Ui.H2, Ui.TEXT)));
        form.add(Box.createVerticalStrut(10));

        JPanel info = Ui.pastel(Ui.P_YELLOW);
        info.setLayout(new BorderLayout());
        info.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        info.add(Ui.label("<html><body style='width:640px'>Every reviewer is checked by an admin first. You'll be notified as soon as it is approved or rejected. "
                + "Please share only your own work. Copied books, paid courses and leaked exams will be taken down.</body></html>", Ui.SMALL, Ui.TEXT));
        form.add(left(info));
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, info.getPreferredSize().height + 6));
        form.add(Box.createVerticalStrut(4));

        addField(form, "Reviewer Title *", titleField);
        addField(form, "Subject *", subjectBox);
        addField(form, "Material Type", typeBox);
        addField(form, "Description *", Ui.scrollBox(descArea));
        addField(form, "Topics Covered * (separate with commas or new lines)", Ui.scrollBox(topicsArea));

        // Files (one or many)
        fileList.setBackground(Ui.FIELD);
        fileList.setForeground(Ui.TEXT);
        fileList.setFont(Ui.BODY);
        fileList.setFixedCellHeight(26);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, v, i, sel, false);
                File f = (File) v;
                setText("  " + f.getName() + "   (" + Ui.formatSize(f.length()) + ")");
                setBackground(sel ? new Color(0x2F3590) : Ui.light ? Color.WHITE : Ui.FIELD);
                setForeground(sel ? Color.WHITE : Ui.textCol());
                return this;
            }
        });
        JScrollPane fileScroll = Ui.scrollBox(fileList);
        fileScroll.setPreferredSize(new Dimension(300, 112));
        JButton choose = Ui.plainButton("Add Files...");
        JButton remove = Ui.plainButton("Remove Selected");
        choose.addActionListener(e -> chooseFiles());
        remove.addActionListener(e -> {
            for (File f : new ArrayList<>(fileList.getSelectedValuesList())) fileModel.removeElement(f);
        });
        JPanel fileButtons = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)));
        fileButtons.add(choose);
        fileButtons.add(remove);
        fileButtons.add(Ui.label("PDF, Word or PowerPoint. Up to " + DataStore.MAX_FILES + " files, " + DataStore.MAX_FILE_MB + " MB each.", Ui.SMALL, Ui.MUTED));
        addField(form, "Attach Files *", fileScroll);
        form.add(Box.createVerticalStrut(6));
        form.add(left(fileButtons));
        fileButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE, fileButtons.getPreferredSize().height + 4));

        form.add(Box.createVerticalStrut(14));
        form.add(left(declare));

        JButton cancel = Ui.plainButton("Cancel");
        JButton share = Ui.primaryButton("Submit for Approval");
        cancel.addActionListener(e -> {
            clearForm();
            main.showPage("Dashboard");
        });
        share.addActionListener(e -> submit());
        JPanel buttons = Ui.clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)));
        buttons.add(cancel);
        buttons.add(share);
        form.add(Box.createVerticalStrut(10));
        form.add(left(buttons));
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));

        // Keep the form a comfortable width instead of stretching across the whole window
        form.setPreferredSize(new Dimension(780, form.getPreferredSize().height));
        JPanel holder = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        holder.add(form);
        add(Ui.scroll(holder), BorderLayout.CENTER);
    }

    private JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    /** Adds a caption and a component below each other. */
    private void addField(JPanel form, String caption, JComponent comp) {
        form.add(Box.createVerticalStrut(12));
        form.add(left(Ui.label(caption, Ui.BOLD, Ui.TEXT)));
        form.add(Box.createVerticalStrut(4));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, comp.getPreferredSize().height));
        form.add(left(comp));
    }

    /** Fills the subject list (admins can add subjects, so it is reloaded when it changes). */
    private void loadSubjects() {
        String[] subjects = DataStore.subjectArray();
        String key = String.join("|", subjects);
        if (key.equals(subjectKey)) return;
        subjectKey = key;
        Object keep = subjectBox.getSelectedIndex() > 0 ? subjectBox.getSelectedItem() : null;
        subjectBox.removeAllItems();
        subjectBox.addItem("Select subject...");
        for (String s : subjects) subjectBox.addItem(s);
        if (keep != null) subjectBox.setSelectedItem(keep);
    }

    /** Called whenever the data changes. */
    public void refresh() {
        loadSubjects();
    }

    private void chooseFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("Documents (PDF, Word, PowerPoint)", DataStore.ALLOWED_EXT));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        StringBuilder problems = new StringBuilder();
        for (File f : fc.getSelectedFiles()) {
            if (fileModel.contains(f)) continue;
            if (fileModel.size() >= DataStore.MAX_FILES) {
                problems.append("You can attach up to ").append(DataStore.MAX_FILES).append(" files.\n");
                break;
            }
            String err = DataStore.checkFile(f);
            if (err != null) problems.append(err).append("\n");
            else fileModel.addElement(f);
        }
        if (problems.length() > 0) Ui.showMessage(this, problems.toString().trim(), "Some files were not added", JOptionPane.WARNING_MESSAGE);
    }

    /** Validates the form and submits the material for admin approval. */
    private void submit() {
        StringBuilder missing = new StringBuilder();
        if (titleField.getText().trim().isEmpty()) missing.append("- Reviewer Title\n");
        if (subjectBox.getSelectedIndex() <= 0) missing.append("- Subject\n");
        if (descArea.getText().trim().isEmpty()) missing.append("- Description\n");
        if (Material.parseTopics(topicsArea.getText()).isEmpty()) missing.append("- Topics Covered\n");
        if (fileModel.isEmpty()) missing.append("- At least one file\n");
        if (!declare.isSelected()) missing.append("- The ownership confirmation (checkbox)\n");
        if (missing.length() > 0) {
            Ui.showMessage(this, "Please complete the following:\n" + missing, "Missing information", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < fileModel.size(); i++) files.add(fileModel.get(i));
        DataStore.submitMaterial(titleField.getText().trim(), (String) subjectBox.getSelectedItem(), (String) typeBox.getSelectedItem(),
                descArea.getText().trim(), topicsArea.getText(), files);
        Ui.showMessage(this, "Your reviewer was submitted with " + files.size() + (files.size() == 1 ? " file" : " files")
                + ".\nStatus: Pending Review. An admin will check it before it goes live.", "Submitted", Ui.SUCCESS_MESSAGE);
        clearForm();
        main.showPage("My Library");
    }

    private void clearForm() {
        titleField.setText("");
        subjectBox.setSelectedIndex(0);
        typeBox.setSelectedIndex(0);
        descArea.setText("");
        topicsArea.setText("");
        fileModel.clear();
        declare.setSelected(false);
    }
}