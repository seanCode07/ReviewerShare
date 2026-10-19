package reviewershare;

import javax.swing.*;

/** Program entry point. Opens the data files, then starts the login screen on the Event Dispatch Thread. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Cross-platform look so our custom colors show on every OS
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            Ui.installDefaults(); // pop-up dialogs use the same navy colors
            try {
                Db.init(); // creates the data files on first run
            } catch (RuntimeException ex) {
                Ui.showMessage(null,
                        "Could not open the data files.\n\n" + ex.getMessage()
                                + "\n\nMake sure the folder " + Db.DIR.getAbsolutePath() + " can be written to.",
                        "Data error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            new LoginFrame().setVisible(true);
        });
    }
}