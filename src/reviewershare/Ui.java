package reviewershare;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.function.ToIntFunction;

/**
 * The design system: a deep-navy "neon dashboard" look (teal, coral and periwinkle accents).
 * The colors below are the default (navy) theme. "Light Mode" in Settings swaps them for a light version.
 */
public class Ui {
    // ----- Default (navy) theme -----
    public static final Color BG = new Color(0x15173D);        // page background
    public static final Color CARD = new Color(0x1F2250);      // cards and sidebar
    public static final Color FIELD = new Color(0x171942);     // input boxes
    public static final Color TEXT = new Color(0xF1F2FF);
    public static final Color MUTED = new Color(0x9AA0D6);
    public static final Color BORDER = new Color(0x2F3475);
    public static final Color BRAND = new Color(0x7B83F0);     // periwinkle
    public static final Color ACCENT = new Color(0x1ED6C8);    // neon teal (primary buttons)
    public static final Color CORAL = new Color(0xFF7F87);
    public static final Color ORANGE = new Color(0xFFB84D);    // stars
    public static final Color WARN = new Color(0xFFB84D);      // "pending" text
    public static final Color RED = new Color(0xFF6B7A);
    public static final Color GREEN = new Color(0x2DE0A5);
    public static final Color BLUE = new Color(0x6FA8FF);

    // Tinted panels (the old "pastel" names are kept so every screen keeps working)
    public static final Color P_PURPLE = new Color(0x2B3077);
    public static final Color P_PEACH = new Color(0x3C2B5C);
    public static final Color P_BLUE = new Color(0x1B3F66);
    public static final Color P_MINT = new Color(0x15485C);
    public static final Color P_YELLOW = new Color(0x33306E);

    // ----- Light theme (alternative) -----
    private static final Color L_BG = new Color(0xF6F3EE);
    private static final Color L_CARD = Color.WHITE;
    private static final Color L_FIELD = new Color(0xFCFBF9);
    private static final Color L_TEXT = new Color(0x1F2937);
    private static final Color L_MUTED = new Color(0x6B7280);
    private static final Color L_BORDER = new Color(0xECE8E1);
    private static final Color L_BRAND = new Color(0x6C5CE7);
    private static final Color L_RED = new Color(0xDC2626);
    private static final Color L_GREEN = new Color(0x16A34A);
    private static final Color L_BLUE = new Color(0x2563EB);
    private static final Color L_WARN = Ui.WARN;
    private static final Color L_P_PURPLE = new Color(0xEDEAFE);
    private static final Color L_P_PEACH = new Color(0xFFEDE4);
    private static final Color L_P_BLUE = new Color(0xE2F3FC);
    private static final Color L_P_MINT = new Color(0xE4F6EA);
    private static final Color L_P_YELLOW = new Color(0xFFF1CF);

    private static final Color[] BG_BASE = {BG, CARD, FIELD, P_PURPLE, P_PEACH, P_BLUE, P_MINT, P_YELLOW};
    private static final Color[] BG_ALT = {L_BG, L_CARD, L_FIELD, L_P_PURPLE, L_P_PEACH, L_P_BLUE, L_P_MINT, L_P_YELLOW};
    private static final Color[] FG_BASE = {TEXT, MUTED, BRAND, RED, GREEN, BLUE, WARN};
    private static final Color[] FG_ALT = {L_TEXT, L_MUTED, L_BRAND, L_RED, L_GREEN, L_BLUE, L_WARN};

    // ----- Fonts -----
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font H1 = new Font("SansSerif", Font.BOLD, 24);
    public static final Font H2 = new Font("SansSerif", Font.BOLD, 16);
    public static final Font BIG = new Font("SansSerif", Font.BOLD, 30);

    /** True when Light Mode is switched on in Settings (the default is the navy theme). */
    public static boolean light = false;

    public static Color themeBg() { return light ? L_BG : BG; }
    public static Color themeCard() { return light ? L_CARD : CARD; }
    private static Color lineColor() { return light ? L_BORDER : BORDER; }
    public static Color textCol() { return light ? L_TEXT : TEXT; }
    public static Color mutedCol() { return light ? L_MUTED : MUTED; }
    private static Color trackCol() { return light ? new Color(0xE5E7EB) : new Color(0x2C3170); }
    private static Color hoverCol() { return light ? new Color(0xF4F2FB) : new Color(0x282C68); }

    /** Converts one of the default accent text colors to the current theme (used by painted components). */
    public static Color themed(Color c) {
        if (!light) return c;
        for (int i = 0; i < FG_BASE.length; i++) if (c.equals(FG_BASE[i])) return FG_ALT[i];
        return c;
    }

    /** Sets the colors used by pop-up dialogs (JOptionPane, file chooser) so they match the theme. Call again after switching theme. */
    public static void installDefaults() {
        Color bg = themeCard(), fg = textCol(), field = light ? L_FIELD : FIELD;
        Color btn = light ? new Color(0xF4F2FB) : new Color(0x2C3070);
        Color selBg = light ? new Color(0xEDEAFE) : new Color(0x3A3F9A);
        String[] bgKeys = {"Panel.background", "OptionPane.background", "CheckBox.background", "RadioButton.background"};
        for (String k : bgKeys) UIManager.put(k, new ColorUIResource(bg));
        String[] fgKeys = {"Label.foreground", "OptionPane.messageForeground", "OptionPane.foreground", "CheckBox.foreground", "TextField.foreground"};
        for (String k : fgKeys) UIManager.put(k, new ColorUIResource(fg));
        UIManager.put("TextField.background", new ColorUIResource(field));
        UIManager.put("TextField.caretForeground", new ColorUIResource(fg));
        UIManager.put("Button.background", new ColorUIResource(btn));
        UIManager.put("Button.foreground", new ColorUIResource(fg));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(selBg));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(fg));
        UIManager.put("OptionPane.messageFont", BODY);
        UIManager.put("OptionPane.buttonFont", BODY);
        UIManager.put("ButtonUI", DialogButtonUI.class.getName());   // clear, high-contrast OK / Cancel buttons in pop-ups
    }

    // ---------- Basic helpers ----------
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** Makes a panel/component see-through so the parent's color shows. */
    public static <T extends JComponent> T clear(T c) {
        c.setOpaque(false);
        return c;
    }

    public static JCheckBox check(String text) {
        JCheckBox b = new JCheckBox(text);
        b.setOpaque(false);
        b.setFont(BODY);
        b.setForeground(TEXT);
        b.setFocusPainted(false);
        return b;
    }

    /** Gives a drop-down the rounded look with a small chevron. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> JComboBox<T> style(JComboBox<T> b) {
        b.setUI(new ComboUI());
        b.setOpaque(false);
        b.setFont(BODY);
        b.setBackground(FIELD);
        b.setForeground(TEXT);
        b.setBorder(new RoundBorder(12, 3, 4));
        b.setRenderer(new ComboRenderer());
        return b;
    }

    private static class ComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
            super.getListCellRendererComponent(list, value, index, false, false);
            boolean inList = index >= 0;
            setOpaque(inList);
            setFont(BODY);
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setBackground(sel ? (light ? L_P_PURPLE : new Color(0x2F3590)) : themeCard());
            setForeground(textCol());
            return this;
        }
    }

    private static class ComboUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(mutedCol());
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            b.setBorder(null);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(26, 20));
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) { }

        @Override
        public void paint(Graphics g, JComponent c) {
            paintRoundBg(g, c);
            super.paint(g, c);
        }

        @Override
        protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.BasicComboPopup p = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
            p.setBorder(BorderFactory.createLineBorder(light ? new Color(0xDDD8CF) : new Color(0x3A4090)));
            return p;
        }
    }

    // ---------- Rounded panels ----------
    public static class RoundPanel extends JPanel {
        private final int radius;
        private final boolean outline;
        private Color accent;      // optional neon bar at the top-left corner
        private Color gradientTo;  // optional gradient (from the background color to this one)

        public RoundPanel(int radius, boolean outline, Color bg) {
            this.radius = radius;
            this.outline = outline;
            setOpaque(false);
            setBackground(bg);
        }

        public RoundPanel setAccent(Color c) {
            accent = c;
            return this;
        }

        public RoundPanel setGradient(Color to) {
            gradientTo = to;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            if (gradientTo != null) g2.setPaint(new GradientPaint(0, 0, getBackground(), w, h, gradientTo));
            else g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, radius, radius);
            if (outline) {
                g2.setColor(lineColor());
                g2.drawRoundRect(0, 0, w, h, radius, radius);
            }
            if (accent != null) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.fillRoundRect(radius - 2, 0, 52, 8, 8, 8);
                g2.setColor(accent);
                g2.fillRoundRect(radius, 0, 44, 4, 4, 4);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A rounded card with a thin outline and padding. */
    public static RoundPanel card() {
        RoundPanel p = new RoundPanel(22, true, CARD);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        return p;
    }

    /** A rounded tinted card (P_PURPLE, P_PEACH, ...). */
    public static RoundPanel pastel(Color c) {
        RoundPanel p = new RoundPanel(22, false, c);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        return p;
    }

    // ---------- Rounded buttons ----------
    public static class RoundButton extends JButton {
        public static final int PRIMARY = 0, SECONDARY = 1, DANGER = 2, NAV = 3, TAB = 4;
        private final int style;
        private boolean hover, active;
        private String icon;
        private Color fillOverride;   // optional custom color for PRIMARY buttons and active TAB buttons

        public RoundButton(String text, int style) {
            super(text);
            this.style = style;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(style == SECONDARY || style == NAV ? BODY : BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        public RoundButton setFill(Color c) {
            fillOverride = c;
            return this;
        }

        public RoundButton setIconName(String name) {
            icon = name;
            return this;
        }

        public void setActive(boolean on) {
            active = on;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            int extra = style == NAV ? (icon != null ? 72 : 44) : 36;
            return new Dimension(fm.stringWidth(getText()) + extra, style == NAV ? 44 : 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color fill = null, line = null, fg;
            boolean gradientActive = false;
            switch (style) {
                case PRIMARY:
                    fill = fillOverride != null ? (hover ? fillOverride.brighter() : fillOverride) : (hover ? new Color(0x5BF0E4) : ACCENT);
                    fg = new Color(0x0C0E2E);
                    break;
                case NAV:
                    if (active) {
                        if (light) {
                            fill = L_P_PURPLE;
                            fg = L_BRAND;
                        } else {
                            gradientActive = true;
                            fg = Color.WHITE;
                        }
                    } else {
                        fill = hover ? hoverCol() : null;
                        fg = light ? L_TEXT : new Color(0xC9CDF5);
                    }
                    break;
                case TAB:
                    fill = active ? (fillOverride != null ? fillOverride : (light ? L_BRAND : new Color(0x6C74E6))) : (hover ? hoverCol() : null);
                    line = active ? null : (light ? new Color(0xDDD8CF) : new Color(0x4A50B8));
                    fg = active ? (fillOverride != null ? new Color(0x0C0E2E) : Color.WHITE) : textCol();
                    break;
                default: // SECONDARY, DANGER
                    fill = hover ? hoverCol() : null;
                    if (style == DANGER) {
                        line = light ? new Color(0xF1B4B4) : new Color(0xB8525E);
                        fg = light ? L_RED : RED;
                    } else {
                        line = light ? new Color(0xDDD8CF) : new Color(0x5A62D8);
                        fg = textCol();
                    }
            }
            if (!isEnabled()) {
                fill = style == PRIMARY ? (light ? new Color(0xE5E7EB) : new Color(0x3A3F7A)) : fill;
                fg = light ? new Color(0x9CA3AF) : new Color(0x7479AA);
            }
            int r = style == NAV ? 14 : 12;
            if (gradientActive) {
                Color c1 = new Color(0x6F77E8), c2 = new Color(0x6F, 0x77, 0xE8, 70);
                g2.setPaint(new GradientPaint(0, 0, c1, w, 0, c2));
                g2.fillRoundRect(0, 0, w, h, r, r);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, h / 4, 4, h / 2, 4, 4);
            } else if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, r, r);
            }
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx;
            if (style == NAV) {
                tx = icon != null ? 46 : 18;
                if (icon != null) drawIcon(g2, icon, 18, (h - 18) / 2, 18, active && !light ? Color.WHITE : fg);
            } else {
                tx = (w - fm.stringWidth(getText())) / 2;
            }
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(fg);
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    public static RoundButton primaryButton(String text) { return new RoundButton(text, RoundButton.PRIMARY); }
    public static RoundButton plainButton(String text) { return new RoundButton(text, RoundButton.SECONDARY); }
    public static RoundButton dangerButton(String text) { return new RoundButton(text, RoundButton.DANGER); }
    public static RoundButton navButton(String text) { return new RoundButton(text, RoundButton.NAV); }

    /** Draws a simple line icon (no image files or special fonts needed). */
    private static void drawIcon(Graphics2D g, String name, int x, int y, int s, Color c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int half = s / 2;
        switch (name) {
            case "Dashboard": {
                int q = half - 2;
                g2.drawRoundRect(x, y, q, q, 3, 3);
                g2.drawRoundRect(x + q + 4, y, q, q, 3, 3);
                g2.drawRoundRect(x, y + q + 4, q, q, 3, 3);
                g2.drawRoundRect(x + q + 4, y + q + 4, q, q, 3, 3);
                break;
            }
            case "Reviewers":
                g2.drawRoundRect(x, y + 2, half - 1, s - 4, 3, 3);
                g2.drawRoundRect(x + half + 1, y + 2, half - 1, s - 4, 3, 3);
                break;
            case "Add Reviewer":
                g2.drawOval(x + 1, y + 1, s - 2, s - 2);
                g2.drawLine(x + half, y + 5, x + half, y + s - 5);
                g2.drawLine(x + 5, y + half, x + s - 5, y + half);
                break;
            case "Requests":
                g2.drawRoundRect(x, y + 3, s, s - 6, 4, 4);
                g2.drawLine(x + 1, y + 5, x + half, y + half + 1);
                g2.drawLine(x + half, y + half + 1, x + s - 1, y + 5);
                break;
            case "My Library":
                for (int i = 0; i < 3; i++) g2.drawRoundRect(x, y + i * 6, s, 4, 3, 3);
                break;
            case "Moderation": {
                int[] xs = {x + half, x + s - 1, x + s - 2, x + half, x + 2, x + 1};
                int[] ys = {y, y + 3, y + half + 2, y + s, y + half + 2, y + 3};
                g2.drawPolygon(xs, ys, 6);
                g2.drawLine(x + 5, y + half, x + half - 1, y + half + 3);
                g2.drawLine(x + half - 1, y + half + 3, x + s - 5, y + 6);
                break;
            }
            case "Settings":
                for (int i = 0; i < 3; i++) {
                    int ly = y + 3 + i * 6;
                    g2.drawLine(x, ly, x + s, ly);
                    g2.fillOval(x + 2 + ((i * 7) % (s - 8)), ly - 2, 5, 5);
                }
                break;
            case "Logout":
                g2.drawLine(x + 1, y + 1, x + half, y + 1);
                g2.drawLine(x + 1, y + 1, x + 1, y + s - 1);
                g2.drawLine(x + 1, y + s - 1, x + half, y + s - 1);
                g2.drawLine(x + 6, y + half, x + s - 1, y + half);
                g2.drawLine(x + s - 6, y + half - 4, x + s - 1, y + half);
                g2.drawLine(x + s - 6, y + half + 4, x + s - 1, y + half);
                break;
            case "Management": {
                g2.drawOval(x + 4, y + 4, s - 8, s - 8);
                g2.drawOval(x + half - 2, y + half - 2, 4, 4);
                for (int i = 0; i < 4; i++) {
                    double a = i * Math.PI / 2;
                    int cx = x + half, cy = y + half;
                    g2.drawLine(cx + (int) Math.round(Math.cos(a) * (half - 3)), cy + (int) Math.round(Math.sin(a) * (half - 3)),
                            cx + (int) Math.round(Math.cos(a) * half), cy + (int) Math.round(Math.sin(a) * half));
                }
                break;
            }
            default:
                g2.drawOval(x + 3, y + 3, s - 6, s - 6);
        }
        g2.dispose();
    }

    // ---------- Pill tabs (replaces JTabbedPane) ----------
    public static class Tabs extends JPanel {
        private final CardLayout layout = new CardLayout();
        private final JPanel body = new JPanel(layout);
        private final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        private final ArrayList<RoundButton> buttons = new ArrayList<>();

        public Tabs() {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            body.setOpaque(false);
            bar.setOpaque(false);
            add(bar, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
        }

        public void addTab(String title, JComponent comp) {
            final int index = buttons.size();
            RoundButton b = new RoundButton(title, RoundButton.TAB);
            b.addActionListener(e -> select(index));
            buttons.add(b);
            bar.add(b);
            body.add(comp, String.valueOf(index));
            if (index == 0) select(0);
        }

        public void select(int index) {
            layout.show(body, String.valueOf(index));
            for (int i = 0; i < buttons.size(); i++) buttons.get(i).setActive(i == index);
        }

        public void setTitle(int index, String title) {
            buttons.get(index).setText(title);
            bar.revalidate();
            bar.repaint();
        }
    }

    // ---------- Avatar (circle or rounded square with initials) ----------
    public static class Avatar extends JComponent {
        private String initials;
        private final int size;
        private final boolean circle;
        private final Color color;

        public Avatar(String name, int size, boolean circle, Color color) {
            this.size = size;
            this.circle = circle;
            this.color = color;
            setPerson(name);
        }

        public void setPerson(String name) {
            initials = initials(name);
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        @Override public Dimension getMinimumSize() { return new Dimension(size, size); }
        @Override public Dimension getMaximumSize() { return new Dimension(size, size); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, color, size, size, CORAL.equals(color) ? color : new Color(0x1ED6C8)));
            if (circle) g2.fillOval(0, 0, size - 1, size - 1);
            else g2.fillRoundRect(0, 0, size - 1, size - 1, size / 3, size / 3);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, size / 3 + 2));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (size - fm.stringWidth(initials)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[1].substring(0, 1);
        return s.toUpperCase();
    }

    // ---------- Charts and gauges ----------
    /** A glowing circular gauge, e.g. "76%". */
    public static class Ring extends JComponent {
        private final Color color;
        private final int size;
        private double value;
        private String text = "";

        public Ring(Color color, int size) {
            this.color = color;
            this.size = size;
        }

        /** value is 0..1; the text is shown in the middle. */
        public void setValue(double value, String text) {
            this.value = Math.max(0, Math.min(1, value));
            this.text = text;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        @Override public Dimension getMinimumSize() { return new Dimension(size, size); }
        @Override public Dimension getMaximumSize() { return new Dimension(size, size); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int pad = Math.max(7, size / 9);
            int d = size - 2 * pad;
            float sw = Math.max(5f, size / 11f);
            Color c = themed(color);
            g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(trackCol());
            g2.drawOval(pad, pad, d, d);
            if (value > 0.001) {
                Arc2D arc = new Arc2D.Double(pad, pad, d, d, 90, -360 * value, Arc2D.OPEN);
                if (!light) {
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
                    g2.setStroke(new BasicStroke(sw + 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(arc);
                }
                g2.setColor(c);
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(arc);
            }
            g2.setColor(textCol());
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(11, size / 5)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (size - fm.stringWidth(text)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** A horizontal progress bar with a caption on the left and a value on the right. */
    public static class Bar extends JComponent {
        private final Color color;
        private double value;
        private String left = "", right = "";

        public Bar(Color color) {
            this.color = color;
        }

        public void setValue(double value, String left, String right) {
            this.value = Math.max(0, Math.min(1, value));
            this.left = left;
            this.right = right;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(200, 40); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 40); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Color c = themed(color);
            g2.setFont(SMALL);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(mutedCol());
            g2.drawString(left, 0, fm.getAscent());
            g2.setFont(BOLD);
            g2.setColor(textCol());
            g2.drawString(right, w - g2.getFontMetrics().stringWidth(right), fm.getAscent());
            int by = h - 12;
            g2.setColor(trackCol());
            g2.fillRoundRect(0, by, w, 9, 9, 9);
            int fw = (int) Math.round(w * value);
            if (fw > 0) {
                fw = Math.max(fw, 9);
                g2.setPaint(new GradientPaint(0, 0, c, w, 0, c.brighter()));
                g2.fillRoundRect(0, by, fw, 9, 9, 9);
            }
            g2.dispose();
        }
    }

    /** A simple vertical bar chart with value labels and category labels. */
    public static class BarChart extends JComponent {
        private String[] labels = new String[0];
        private double[] values = new double[0];
        private Color[] colors = new Color[0];

        public void setData(String[] labels, double[] values, Color[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(300, 140); }

        @Override
        protected void paintComponent(Graphics g) {
            int n = values.length;
            if (n == 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int top = 20, bottom = h - 22;
            double max = 1;
            for (double v : values) max = Math.max(max, v);
            double slot = (double) w / n;
            int bw = (int) Math.min(46, slot * 0.5);
            g2.setColor(lineColor());
            g2.drawLine(0, bottom + 1, w, bottom + 1);
            for (int i = 0; i < n; i++) {
                int bh = (int) Math.round((bottom - top) * values[i] / max);
                bh = Math.max(bh, values[i] > 0 ? 4 : 2);
                int x = (int) Math.round(i * slot + (slot - bw) / 2);
                int y = bottom - bh;
                Color c = themed(colors[i % colors.length]);
                if (!light) {
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
                    g2.fillRoundRect(x - 3, y - 3, bw + 6, bh + 6, 12, 12);
                }
                g2.setPaint(new GradientPaint(0, y, c, 0, bottom, new Color(c.getRed(), c.getGreen(), c.getBlue(), 120)));
                g2.fillRoundRect(x, y, bw, bh, 8, 8);
                g2.setFont(BOLD);
                g2.setColor(textCol());
                String v = String.valueOf((int) values[i]);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(v, x + (bw - fm.stringWidth(v)) / 2, y - 5);
                g2.setFont(SMALL);
                g2.setColor(mutedCol());
                fm = g2.getFontMetrics();
                g2.drawString(labels[i], (int) Math.round(i * slot + (slot - fm.stringWidth(labels[i])) / 2), h - 6);
            }
            g2.dispose();
        }
    }

    /** A bell icon with a red/coral counter badge. */
    public static class BellButton extends JComponent {
        private int count;

        public BellButton(Runnable onClick) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
            });
        }

        public void setCount(int count) {
            this.count = count;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(40, 40); }
        @Override public Dimension getMinimumSize() { return new Dimension(40, 40); }
        @Override public Dimension getMaximumSize() { return new Dimension(40, 40); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(light ? new Color(0xEDEAFE) : new Color(0x2B3077));
            g2.fillOval(0, 0, 39, 39);
            g2.setColor(light ? L_BRAND : new Color(0xC9CDF5));
            g2.fillArc(12, 10, 16, 16, 0, 180);
            g2.fillRect(12, 18, 16, 6);
            g2.fillRoundRect(10, 23, 20, 3, 3, 3);
            g2.fillOval(17, 27, 6, 4);
            if (count > 0) {
                g2.setColor(CORAL);
                g2.fillOval(23, 3, 15, 15);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                String s = count > 9 ? "9+" : String.valueOf(count);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, 30 - fm.stringWidth(s) / 2, 14);
            }
            g2.dispose();
        }
    }

    // ---------- Text inputs ----------
    private static class RoundBorder extends AbstractBorder {
        private final int radius, padV, padH;

        RoundBorder(int radius, int padV, int padH) {
            this.radius = radius;
            this.padV = padV;
            this.padH = padH;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(light ? new Color(0xDDD8CF) : new Color(0x3A4090));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1 + padV, 1 + padH, 1 + padV, 1 + padH);
        }
    }

    private static void paintRoundBg(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(c.getBackground());
        g2.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 12, 12);
        g2.dispose();
    }

    private static void prepareField(JTextComponent f) {
        f.setOpaque(false);
        f.setFont(BODY);
        f.setBackground(FIELD);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setBorder(new RoundBorder(12, 7, 10));
    }

    public static JTextField field() {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                paintRoundBg(g, this);
                super.paintComponent(g);
            }
        };
        prepareField(f);
        return f;
    }

    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                paintRoundBg(g, this);
                super.paintComponent(g);
            }
        };
        prepareField(f);
        return f;
    }

    public static JTextArea textArea(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setFont(BODY);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBackground(FIELD);
        a.setForeground(TEXT);
        a.setCaretColor(TEXT);
        a.setBorder(new EmptyBorder(8, 10, 8, 10));
        return a;
    }

    /** Limits a text box to a maximum length (and optionally digits only). Pasted text is cleaned too. */
    public static void limitInput(JTextComponent c, int max, boolean digitsOnly) {
        ((AbstractDocument) c.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException {
                replace(fb, off, 0, s, a);
            }

            @Override
            public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException {
                if (s == null) s = "";
                if (digitsOnly) s = s.replaceAll("\\D", "");
                int room = max - (fb.getDocument().getLength() - len);
                if (room < s.length()) s = s.substring(0, Math.max(room, 0));
                super.replace(fb, off, len, s, a);
            }
        });
    }

    // ---------- Scroll panes ----------
    private static class SlimScrollBarUI extends BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }

        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(light ? new Color(0xD6D1C7) : new Color(0x4A50B8));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    /** Gives a scroll pane the slim, see-through modern look. */
    public static JScrollPane styleScroll(JScrollPane sp) {
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        for (JScrollBar sb : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
            sb.setUI(new SlimScrollBarUI());
            sb.setOpaque(false);
        }
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    /** A borderless scroll pane (good for tables inside cards). */
    public static JScrollPane scroll(Component c) {
        return styleScroll(new JScrollPane(c));
    }

    /** A scroll pane with a rounded outline (good for text areas inside forms). */
    public static JScrollPane scrollBox(Component c) {
        JScrollPane sp = new JScrollPane(c) {
            @Override protected void paintComponent(Graphics g) {
                paintRoundBg(g, this);
                super.paintComponent(g);
            }
        };
        styleScroll(sp);
        sp.setBackground(FIELD);
        sp.setBorder(new RoundBorder(12, 0, 0));
        if (c instanceof JComponent) ((JComponent) c).setOpaque(false);
        return sp;
    }

    // ---------- Tables ----------
    /** A table model whose cells cannot be edited. */
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    private static Color statusColor(String s) {
        switch (s) {
            case "Pending":
            case "Pending Review":
                return WARN;
            case "Approved":
                return GREEN;
            case "Rejected":
            case "Declined":
            case "Cancelled":
                return RED;
            case "Completed":
            case "Registered":
            case "Resolved":
            case "Dismissed":
                return BLUE;
            case "Open":
            case "Suspended":
                return s.equals("Open") ? WARN : RED;
            case "Action Taken":
            case "Active":
                return GREEN;
            default:
                return (s.startsWith("★") || s.startsWith("☆")) ? ORANGE : null;
        }
    }

    private static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            setOpaque(true);
            setBackground(sel ? (light ? L_P_PURPLE : new Color(0x2F3590)) : themeCard());
            setForeground(textCol());
            setFont(BODY);
            String s = v == null ? "" : v.toString();
            Color sc = statusColor(s);
            if (sc != null) {
                setForeground(themed(sc));
                setFont(BOLD);
            }
            setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, lineColor()), new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(), false, false, r, c);
            setOpaque(true);
            setBackground(themeCard());
            setForeground(mutedCol());
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, lineColor()), new EmptyBorder(0, 12, 0, 12)));
            return this;
        }
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(42);
        t.setFont(BODY);
        t.setBackground(CARD);
        t.setForeground(TEXT);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class, new CellRenderer());
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setBackground(CARD);
        h.setDefaultRenderer(new HeaderRenderer());
        h.setPreferredSize(new Dimension(0, 38));
    }

    /** The id of the selected row (or -1), so a refresh can re-select it. */
    public static <T> int selId(JTable t, java.util.List<T> list, ToIntFunction<T> id) {
        int r = t.getSelectedRow();
        return (r >= 0 && r < list.size()) ? id.applyAsInt(list.get(r)) : -1;
    }

    public static <T> void reselect(JTable t, java.util.List<T> list, ToIntFunction<T> id, int want) {
        if (want < 0) return;
        for (int i = 0; i < list.size(); i++) {
            if (id.applyAsInt(list.get(i)) == want) {
                t.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    // ---------- Pop-up dialog buttons (OK, Cancel, Yes, No) ----------
    /** Gives plain JButtons that have text (like the OK button in message boxes) a clear, easy-to-see look. */
    public static class DialogButtonUI extends BasicButtonUI {
        public static ComponentUI createUI(JComponent c) {
            return new DialogButtonUI();
        }

        private static boolean secondary(String t) {
            return t.equals("Cancel") || t.equals("No") || t.equals("Close");
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            String t = b.getText();
            if (t == null || t.isEmpty()) return;   // icon-only buttons keep the normal look
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            b.setBorder(new EmptyBorder(9, 30, 9, 30));
            b.setFont(BOLD);
            b.setForeground(secondary(t) ? textCol() : new Color(0x0C0E2E));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            String t = b.getText();
            if (t != null && !t.isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel m = b.getModel();
                int w = c.getWidth(), h = c.getHeight();
                if (secondary(t)) {
                    g2.setColor(m.isRollover() ? hoverCol() : themeCard());
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                    g2.setColor(light ? new Color(0xB9B3A8) : new Color(0x7B83F0));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                } else {
                    Color fill = !b.isEnabled() ? new Color(0x3A3F7A) : m.isPressed() ? new Color(0x14A89D)
                            : m.isRollover() ? new Color(0x5BF0E4) : ACCENT;
                    g2.setColor(fill);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                }
                g2.dispose();
            }
            super.paint(g, c);
        }

        @Override
        protected void paintText(Graphics g, JComponent c, Rectangle tr, String text) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(b.getFont());
            g2.setColor(b.isEnabled() ? b.getForeground() : new Color(0x7479AA));
            g2.drawString(text, tr.x, tr.y + g2.getFontMetrics().getAscent());
            g2.dispose();
        }
    }

    // ---------- Logo ----------
    private static BufferedImage logoImg;
    private static boolean logoTried;

    /**
     * Finds logo.jpg. It is looked up inside the project (next to the .java files or in the project folder),
     * and finally in the ReviewerShareData folder. Returns null if it cannot be found.
     */
    public static synchronized BufferedImage logoImage() {
        if (logoTried) return logoImg;
        logoTried = true;
        String[] resources = {"/reviewershare/logo.jpg", "/logo.jpg", "/images/logo.jpg", "/reviewershare/images/logo.jpg"};
        for (String r : resources) {
            try (java.io.InputStream in = Ui.class.getResourceAsStream(r)) {
                if (in != null) {
                    logoImg = ImageIO.read(in);
                    if (logoImg != null) return logoImg;
                }
            } catch (Exception ignored) {
            }
        }
        File[] files = {new File("logo.jpg"), new File("src/reviewershare/logo.jpg"), new File("src/logo.jpg"),
                new File("reviewershare/logo.jpg"), new File(Db.DIR, "logo.jpg")};
        for (File f : files) {
            try {
                if (f.exists()) {
                    logoImg = ImageIO.read(f);
                    if (logoImg != null) return logoImg;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** The logo as a rounded square (the empty dark border of the picture is cropped away). */
    public static class LogoView extends JComponent {
        private final int size;
        private Image scaled;

        public LogoView(int size) {
            this.size = size;
        }

        @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        @Override public Dimension getMinimumSize() { return new Dimension(size, size); }
        @Override public Dimension getMaximumSize() { return new Dimension(size, size); }

        @Override
        protected void paintComponent(Graphics g) {
            BufferedImage img = logoImage();
            if (img == null) return;
            if (scaled == null) {
                int cx = (int) (img.getWidth() * 0.18), cy = (int) (img.getHeight() * 0.18);
                BufferedImage crop = img.getSubimage(cx, cy, img.getWidth() - 2 * cx, img.getHeight() - 2 * cy);
                scaled = crop.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = size / 3;
            g2.setClip(new RoundRectangle2D.Float(0, 0, size, size, arc, arc));
            g2.drawImage(scaled, 0, 0, null);
            g2.setClip(null);
            g2.setColor(new Color(0x1E, 0xD6, 0xC8, 110));
            g2.drawRoundRect(0, 0, size - 1, size - 1, arc, arc);
            g2.dispose();
        }
    }

    /** The logo if logo.jpg was found, otherwise the old letter avatar. */
    public static JComponent logoComponent(int size) {
        return logoImage() != null ? new LogoView(size) : new Avatar("R", size, false, BRAND);
    }

    /** Uses the logo as the window icon (if it exists). */
    public static void applyIcon(Window w) {
        BufferedImage img = logoImage();
        if (img != null) w.setIconImage(img);
    }

    // ---------- Form helpers ----------
    /** Adds "caption + input" below each other to a panel that uses a vertical BoxLayout. */
    public static void addField(JPanel form, String caption, JComponent c) {
        JLabel l = label(caption, BOLD, TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        form.add(l);
        form.add(Box.createVerticalStrut(4));
        form.add(c);
        form.add(Box.createVerticalStrut(10));
    }

    /** Opens an attached file of a material if the current user is allowed to see it (otherwise explains why not). */
    public static void openMaterialFile(Component parent, Material m) {
        openMaterialFile(parent, m, null);
    }

    /** Same, for one chosen file. When which is null and the material has several files, the user picks one. */
    public static void openMaterialFile(Component parent, Material m, Material.FileInfo which) {
        if (m == null) {
            showMessage(parent, "This reviewer is no longer available.");
            return;
        }
        if (!DataStore.canViewFile(m)) {
            showMessage(parent, "You can view this file after the owner approves your request.",
                    "Access needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (m.files.isEmpty()) {
            showMessage(parent, "This material has no attached file.");
            return;
        }
        Material.FileInfo fi = which != null ? which : pickFile(parent, m);
        if (fi == null) return;
        File f = fi.path == null || fi.path.isEmpty() ? null : new File(fi.path);
        if (f == null || !f.exists()) {
            showMessage(parent, "\"" + fi.name + "\" could not be found in the data folder.", "File missing", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) throw new UnsupportedOperationException("Opening files is not supported here.");
            Desktop.getDesktop().open(f);
        } catch (Exception ex) {
            showMessage(parent, "Could not open the file: " + ex.getMessage(), "Cannot open file", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Material.FileInfo pickFile(Component parent, Material m) {
        if (m.files.size() == 1) return m.files.get(0);
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Material.FileInfo f : m.files) model.addElement(f.name + "  (" + f.size + ")");
        JList<String> list = new JList<>(model);
        list.setBackground(FIELD);
        list.setForeground(TEXT);
        list.setFont(BODY);
        list.setSelectionBackground(new Color(0x2F3590));
        list.setSelectionForeground(Color.WHITE);
        list.setSelectedIndex(0);
        JScrollPane sp = scrollBox(list);
        sp.setPreferredSize(new Dimension(380, 150));
        int r = showConfirm(parent, sp, "Choose a file to open", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        int i = list.getSelectedIndex();
        return r == JOptionPane.OK_OPTION && i >= 0 ? m.files.get(i) : null;
    }

    // ---------- Pop-up dialogs (replace the plain JOptionPane boxes) ----------
    public static final int SUCCESS_MESSAGE = 100;

    private static Color kindColor(int kind) {
        switch (kind) {
            case SUCCESS_MESSAGE: return GREEN;
            case JOptionPane.ERROR_MESSAGE: return RED;
            case JOptionPane.WARNING_MESSAGE: return WARN;
            case JOptionPane.INFORMATION_MESSAGE: return BLUE;
            default: return BRAND;
        }
    }

    private static String kindTitle(int kind) {
        switch (kind) {
            case SUCCESS_MESSAGE: return "Done";
            case JOptionPane.ERROR_MESSAGE: return "Something went wrong";
            case JOptionPane.WARNING_MESSAGE: return "Please note";
            case JOptionPane.QUESTION_MESSAGE: return "Please confirm";
            default: return "Message";
        }
    }

    /** Picks a look for messages that did not say which kind they are. */
    private static int guessKind(String text) {
        String t = text.toLowerCase();
        for (String w : new String[]{"please", "cannot", "can't", "can no longer", "only requests", "no longer", "no file",
                "no attached", "not available", "already", "not allowed", "not correct", "too many", "could not"})
            if (t.contains(w)) return JOptionPane.WARNING_MESSAGE;
        for (String w : new String[]{"submitted", "saved", "was sent", "sent to", "created", "changed", "deleted", "approved",
                "was reset", "cancelled", "updated", "posted", "removed", "reported", "registered", "request "})
            if (t.contains(w)) return SUCCESS_MESSAGE;
        return JOptionPane.INFORMATION_MESSAGE;
    }

    private static String html(String text) {
        if (text.startsWith("<html")) return text;
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        return "<html><body style='width:330px'>" + safe + "</body></html>";
    }

    /** The round colored icon on the left of a pop-up. */
    private static class KindBadge extends JComponent {
        private final int kind;

        KindBadge(int kind) {
            this.kind = kind;
        }

        @Override public Dimension getPreferredSize() { return new Dimension(48, 48); }
        @Override public Dimension getMaximumSize() { return new Dimension(48, 48); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = kindColor(kind);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 55));
            g2.fillOval(0, 0, 47, 47);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(1, 1, 45, 45);
            switch (kind) {
                case SUCCESS_MESSAGE:
                    g2.drawLine(14, 25, 21, 32);
                    g2.drawLine(21, 32, 34, 16);
                    break;
                case JOptionPane.ERROR_MESSAGE:
                    g2.drawLine(16, 16, 31, 31);
                    g2.drawLine(31, 16, 16, 31);
                    break;
                case JOptionPane.WARNING_MESSAGE:
                    g2.drawLine(24, 12, 24, 26);
                    g2.fillOval(22, 32, 4, 4);
                    break;
                case JOptionPane.QUESTION_MESSAGE:
                    g2.setFont(new Font("SansSerif", Font.BOLD, 26));
                    g2.drawString("?", 17, 34);
                    break;
                default:
                    g2.drawLine(24, 21, 24, 35);
                    g2.fillOval(22, 11, 4, 4);
            }
            g2.dispose();
        }
    }

    /** Shows a styled dialog and returns the code of the clicked button (or CLOSED_OPTION). */
    private static int open(Component parent, Object msg, String title, int kind, String[] labels, int[] codes,
                            boolean dangerYes, JComponent focusOn) {
        Window owner = parent == null ? null : (parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent));
        JDialog d = new JDialog(owner, title == null ? "ReviewerShare" : title, Dialog.ModalityType.APPLICATION_MODAL);
        int[] result = {JOptionPane.CLOSED_OPTION};

        JPanel text = clear(new JPanel());
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel heading = label(title == null || title.isEmpty() ? kindTitle(kind) : title, H2, TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(heading);
        text.add(Box.createVerticalStrut(8));
        if (msg instanceof JComponent) {
            ((JComponent) msg).setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add((JComponent) msg);
        } else {
            JLabel m = label(html(String.valueOf(msg)), BODY, TEXT);
            m.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(m);
        }
        JPanel body = clear(new JPanel(new BorderLayout(18, 0)));
        body.setBorder(new EmptyBorder(24, 26, 8, 26));
        if (kind != JOptionPane.PLAIN_MESSAGE) {
            JPanel badge = clear(new JPanel(new BorderLayout()));
            badge.add(new KindBadge(kind), BorderLayout.NORTH);
            body.add(badge, BorderLayout.WEST);
        }
        body.add(text, BorderLayout.CENTER);

        JPanel row = clear(new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)));
        row.setBorder(new EmptyBorder(10, 26, 18, 26));
        RoundButton first = null;
        for (int i = labels.length - 1; i >= 0; i--) {
            final int code = codes[i];
            boolean main = i == 0;
            RoundButton b = main ? (dangerYes ? dangerButton(labels[i]) : primaryButton(labels[i])) : plainButton(labels[i]);
            b.addActionListener(e -> {
                result[0] = code;
                d.dispose();
            });
            row.add(b);
            if (main) first = b;
        }
        // buttons were added from last to first; show the main one on the right
        d.getContentPane().setBackground(themeCard());
        d.add(body, BorderLayout.CENTER);
        d.add(row, BorderLayout.SOUTH);
        if (light) applyTheme(d.getContentPane(), true);
        d.getRootPane().setDefaultButton(first);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.pack();
        d.setSize(Math.max(d.getWidth(), 460), d.getHeight() + 6);
        d.setLocationRelativeTo(parent);
        if (focusOn != null)
            d.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowOpened(java.awt.event.WindowEvent e) { focusOn.requestFocusInWindow(); }
            });
        d.setVisible(true);
        return result[0];
    }

    public static void showMessage(Component parent, Object msg) {
        showMessage(parent, msg, null, -1);
    }

    public static void showMessage(Component parent, Object msg, String title, int type) {
        int kind = type;
        if (type == -1 || type == JOptionPane.PLAIN_MESSAGE && !(msg instanceof JComponent))
            kind = msg instanceof JComponent ? JOptionPane.INFORMATION_MESSAGE : guessKind(String.valueOf(msg));
        if (type == JOptionPane.PLAIN_MESSAGE && msg instanceof JComponent) kind = JOptionPane.PLAIN_MESSAGE;
        open(parent, msg, title, kind, new String[]{"OK"}, new int[]{JOptionPane.OK_OPTION}, false, null);
    }

    public static int showConfirm(Component parent, Object msg, String title, int optionType) {
        return showConfirm(parent, msg, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }

    public static int showConfirm(Component parent, Object msg, String title, int optionType, int messageType) {
        boolean yesNo = optionType == JOptionPane.YES_NO_OPTION;
        String[] labels = yesNo ? new String[]{"Yes", "No"} : new String[]{"OK", "Cancel"};
        int[] codes = yesNo ? new int[]{JOptionPane.YES_OPTION, JOptionPane.NO_OPTION}
                : new int[]{JOptionPane.OK_OPTION, JOptionPane.CANCEL_OPTION};
        int r = open(parent, msg, title, messageType, labels, codes, messageType == JOptionPane.WARNING_MESSAGE && yesNo, null);
        return r == JOptionPane.CLOSED_OPTION ? (yesNo ? JOptionPane.NO_OPTION : JOptionPane.CANCEL_OPTION) : r;
    }

    /** Asks for one line of text. Returns null if cancelled. */
    public static String showInput(Component parent, Object msg, String title, int type) {
        JTextField f = field();
        f.setPreferredSize(new Dimension(340, 38));
        JPanel box = clear(new JPanel());
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel l = label(html(String.valueOf(msg)), BODY, TEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        box.add(l);
        box.add(Box.createVerticalStrut(8));
        box.add(f);
        int r = open(parent, box, title, JOptionPane.QUESTION_MESSAGE, new String[]{"OK", "Cancel"},
                new int[]{JOptionPane.OK_OPTION, JOptionPane.CANCEL_OPTION}, false, f);
        return r == JOptionPane.OK_OPTION ? f.getText() : null;
    }

    // ---------- Misc ----------
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public static boolean isValidEmail(String email) {
        return email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    // ---------- Light mode ----------
    /** Walks through every component and swaps the navy colors for light ones (or back). Buttons paint themselves. */
    public static void applyTheme(Container root, boolean toLight) {
        for (Component c : root.getComponents()) {
            if (!(c instanceof JButton)) {
                c.setBackground(swap(c.getBackground(), BG_BASE, BG_ALT, toLight));
                Color fg = c.getForeground();
                if (fg != null) {
                    fg = swap(fg, FG_BASE, FG_ALT, toLight);
                    if (!toLight && fg.equals(Color.BLACK)) fg = TEXT;
                    c.setForeground(fg);
                    if (c instanceof JTextComponent) ((JTextComponent) c).setCaretColor(fg);
                }
            }
            if (c instanceof Container) applyTheme((Container) c, toLight);
        }
    }

    private static Color swap(Color c, Color[] base, Color[] alt, boolean toAlt) {
        if (c == null) return null;
        Color[] from = toAlt ? base : alt;
        Color[] to = toAlt ? alt : base;
        for (int i = 0; i < from.length; i++) if (c.equals(from[i])) return to[i];
        return c;
    }
}