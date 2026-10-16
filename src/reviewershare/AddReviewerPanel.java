package reviewershare;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/** "Share a Reviewer" form. Submissions go to the admin for approval first. */
public class AddReviewerPanel extends JPanel {
    private final MainFrame main;
    private final JTextField titleField = Ui.field();
    private final JComboBox<String> subjectBox = Ui.style(new JComboBox<>());
    private final JComboBox<String> typeBox = Ui.style(new JComboBox<>(DataStore.TYPES));
    private final JTextArea descArea = Ui.textArea(5);
    private final JTextArea topicsArea = Ui.textArea(3);
    private final JLabel fileLabel = Ui.label("No file selected", Ui.SMALL, Ui.MUTED);
    private File selectedFile;

    public AddReviewerPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        subjectBox.addItem("Select subject...");
        for (String s : DataStore.SUBJECTS) subjectBox.addItem(s);

        JPanel form = Ui.card();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        form.add(left(Ui.label("New Study Material", Ui.H2, Ui.TEXT)));
        form.add(Box.createVerticalStrut(10));

        JPanel info = Ui.pastel(Ui.P_YELLOW);
        info.setLayout(new BorderLayout());
        info.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        info.add(Ui.label("<html>Every reviewer is checked by an admin first. You'll be notified as soon as it is approved or rejected.</html>", Ui.SMALL, Ui.TEXT));
        form.add(left(info));
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, info.getPreferredSize().height + 6));
        form.add(Box.createVerticalStrut(4));

        addField(form, "Reviewer Title *", titleField);
        addField(form, "Subject *", subjectBox);
        addField(form, "Material Type", typeBox);
        addField(form, "Description *", Ui.scrollBox(descArea));
        addField(form, "Topics Covered * (separate with commas or new lines)", Ui.scrollBox(topicsArea));

        JButton choose = Ui.plainButton("Choose File...");
        choose.addActionListener(e -> chooseFile());
        JPanel fileRow = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)));
        fileLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        fileRow.add(choose);
        fileRow.add(fileLabel);
        addField(form, "Attach File (Optional)", fileRow);

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
        form.add(Box.createVerticalStrut(8));
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

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            fileLabel.setText(selectedFile.getName() + " (" + Ui.formatSize(selectedFile.length()) + ")");
        }
    }

    /** Validates the form and submits the material for admin approval. */
    private void submit() {
        StringBuilder missing = new StringBuilder();
        if (titleField.getText().trim().isEmpty()) missing.append("- Reviewer Title\n");
        if (subjectBox.getSelectedIndex() == 0) missing.append("- Subject\n");
        if (descArea.getText().trim().isEmpty()) missing.append("- Description\n");
        if (Material.parseTopics(topicsArea.getText()).isEmpty()) missing.append("- Topics Covered\n");
        if (missing.length() > 0) {
            JOptionPane.showMessageDialog(this, "Please fill in the required fields:\n" + missing, "Missing information", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DataStore.submitMaterial(titleField.getText().trim(), (String) subjectBox.getSelectedItem(), (String) typeBox.getSelectedItem(),
                descArea.getText().trim(), topicsArea.getText(), selectedFile);
        JOptionPane.showMessageDialog(this, "Your reviewer was submitted!\nStatus: Pending Review - an admin will check it before it goes live.");
        clearForm();
        main.showPage("My Library");
    }

    private void clearForm() {
        titleField.setText("");
        subjectBox.setSelectedIndex(0);
        typeBox.setSelectedIndex(0);
        descArea.setText("");
        topicsArea.setText("");
        selectedFile = null;
        fileLabel.setText("No file selected");
    }
}