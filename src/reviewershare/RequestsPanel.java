package reviewershare;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

/** Requests screen with Incoming / Sent / Completed tabs. */
public class RequestsPanel extends JPanel {
    private final MainFrame main;
    private final Ui.Tabs tabs = new Ui.Tabs();
    private final DefaultTableModel inModel = Ui.model("Reviewer", "Requester", "Subject", "Date", "Status");
    private final DefaultTableModel sentModel = Ui.model("Reviewer", "Owner", "Subject", "Date", "Status");
    private final DefaultTableModel doneModel = Ui.model("Reviewer", "Requester", "Subject", "Date", "Status");
    private final JTable inTable = new JTable(inModel);
    private final JTable sentTable = new JTable(sentModel);
    private final JTable doneTable = new JTable(doneModel);
    private ArrayList<Request> inList = new ArrayList<>();
    private ArrayList<Request> sentList = new ArrayList<>();
    private ArrayList<Request> doneList = new ArrayList<>();

    public RequestsPanel(MainFrame main) {
        this.main = main;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        add(Ui.label("Manage incoming and outgoing reviewer requests.", Ui.BODY, Ui.MUTED), BorderLayout.NORTH);

        for (JTable t : new JTable[]{inTable, sentTable, doneTable}) Ui.styleTable(t);

        // Incoming tab has action buttons
        JButton accept = Ui.primaryButton("Accept");
        JButton decline = Ui.dangerButton("Decline");
        JButton complete = Ui.plainButton("Mark as Completed");
        accept.addActionListener(e -> change("Pending", "Approved"));
        decline.addActionListener(e -> change("Pending", "Declined"));
        complete.addActionListener(e -> change("Approved", "Completed"));

        // Sent tab: cancel my own pending request
        JButton viewSent = Ui.primaryButton("View File");
        viewSent.addActionListener(e -> viewSentFile());
        JButton cancel = Ui.dangerButton("Cancel Request");
        cancel.addActionListener(e -> cancelSelected());

        tabs.addTab("Incoming", tabPanel(inTable, accept, decline, complete));
        tabs.addTab("Sent", tabPanel(sentTable, viewSent, cancel));
        tabs.addTab("Completed", tabPanel(doneTable));
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel tabPanel(JTable table, JButton... buttons) {
        JPanel p = Ui.card();
        p.setLayout(new BorderLayout(0, 8));
        p.add(Ui.scroll(table), BorderLayout.CENTER);
        if (buttons.length > 0) {
            JPanel row = Ui.clear(new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4)));
            for (JButton b : buttons) row.add(b);
            p.add(row, BorderLayout.SOUTH);
        }
        return p;
    }

    private Request selected(JTable table, ArrayList<Request> list) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= list.size()) {
            Ui.showMessage(this, "Please select a request first.");
            return null;
        }
        return list.get(row);
    }

    /** Changes the status of the selected incoming request (only if it currently has status 'from'). */
    private void change(String from, String to) {
        Request r = selected(inTable, inList);
        if (r == null) return;
        if (!r.status.equals(from)) {
            Ui.showMessage(this, "Only requests with status \"" + from + "\" can be set to " + to + ".");
            return;
        }
        if (!DataStore.changeRequestStatus(r, from, to)) {
            Ui.showMessage(this, "This request was just changed (for example, the student cancelled it). The list has been refreshed.");
            main.refreshAll();
            return;
        }
        if (DataStore.notifications) {
            Ui.showMessage(this, "Request " + to.toLowerCase() + ".");
        }
        main.refreshAll();
    }

    /** Opens the file of a sent request that the owner approved. */
    private void viewSentFile() {
        Request r = selected(sentTable, sentList);
        if (r == null) return;
        if (!r.status.equals("Approved") && !r.status.equals("Completed")) {
            Ui.showMessage(this, "You can view the file once the owner approves your request.");
            return;
        }
        Ui.openMaterialFile(this, DataStore.findMaterial(r.materialId));
    }

    /** Cancels the selected request in the Sent tab (only Pending ones). */
    private void cancelSelected() {
        Request r = selected(sentTable, sentList);
        if (r == null) return;
        if (!r.status.equals("Pending")) {
            Ui.showMessage(this, "Only requests with status \"Pending\" can be cancelled.");
            return;
        }
        if (Ui.showConfirm(this, "Cancel your request for \"" + r.materialTitle + "\"?", "Cancel request",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        if (DataStore.cancelRequest(r)) {
            Ui.showMessage(this, "Your request was cancelled.");
        } else {
            Ui.showMessage(this, "This request can no longer be cancelled - the owner already responded.");
        }
        main.refreshAll();
    }

    /** Reloads the three tables (keeping your selected rows). */
    public void refresh() {
        int keepIn = Ui.selId(inTable, inList, r -> r.id);
        int keepSent = Ui.selId(sentTable, sentList, r -> r.id);
        int keepDone = Ui.selId(doneTable, doneList, r -> r.id);

        inList = DataStore.incomingRequests();
        sentList = DataStore.sentRequests();
        doneList = DataStore.completedRequests();

        inModel.setRowCount(0);
        for (Request r : inList) inModel.addRow(row(r, r.requesterName));
        sentModel.setRowCount(0);
        for (Request r : sentList) sentModel.addRow(row(r, r.ownerName));
        doneModel.setRowCount(0);
        for (Request r : doneList) doneModel.addRow(row(r, r.requesterName));

        Ui.reselect(inTable, inList, r -> r.id, keepIn);
        Ui.reselect(sentTable, sentList, r -> r.id, keepSent);
        Ui.reselect(doneTable, doneList, r -> r.id, keepDone);

        tabs.setTitle(0, "Incoming (" + DataStore.pendingCount() + ")");
        tabs.setTitle(1, "Sent (" + sentList.size() + ")");
        tabs.setTitle(2, "Completed (" + doneList.size() + ")");
    }

    private Object[] row(Request r, String who) {
        return new Object[]{r.materialTitle, who, r.materialSubject, r.getDateText(), r.status};
    }
}