package ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Простейшее автодополнение (Ctrl + Space).
 * Использует modelToView(int) – совместимо со всеми версиями JDK.
 */
public final class CodeCompletion {

    private static final String[] KEYWORDS = {
            "if","else","while","for","return","new","class","static",
            "public","private","protected","void","int","bool","string",
            "true","false","i","b","s"
    };

    public static void showCompletion(JTextComponent comp) {
        try {
            int caretPos = comp.getCaretPosition();
            Document doc   = comp.getDocument();

            // ---------- 1) Префикс ----------
            int start = caretPos - 1;
            while (start >= 0) {
                String ch = doc.getText(start, 1);
                if (!Character.isLetterOrDigit(ch.charAt(0)) && ch.charAt(0) != '_')
                    break;
                start--;
            }
            start++;
            String prefix = doc.getText(start, caretPos - start);

            // ---------- 2) Сбор вариантов ----------
            Set<String> candidates = new LinkedHashSet<>();
            for (String kw : KEYWORDS)
                if (kw.startsWith(prefix)) candidates.add(kw);

            String full = doc.getText(0, doc.getLength());
            Matcher m = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b")
                               .matcher(full);
            while (m.find()) {
                String id = m.group(1);
                if (id.startsWith(prefix)) candidates.add(id);
            }

            if (candidates.isEmpty()) return;

            // ---------- 3) UI ----------
            final int startPos = start;
            final int endPos   = caretPos;

            JList<String> list = new JList<>(candidates.toArray(new String[0]));
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(Math.min(8, candidates.size()));
            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createEmptyBorder());

            JPopupMenu popup = new JPopupMenu();
            popup.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            popup.add(scroll);

            list.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        insertSelection(comp, list.getSelectedValue(),
                                         startPos, endPos);
                        popup.setVisible(false);
                    }
                }
            });
            list.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        insertSelection(comp, list.getSelectedValue(),
                                         startPos, endPos);
                        popup.setVisible(false);
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        popup.setVisible(false);
                    }
                }
            });

            Rectangle r;
            try {
                r = comp.modelToView(caretPos);
                if (r == null) r = new Rectangle(0,0,0,0);
            } catch (BadLocationException ex) {
                r = new Rectangle(0,0,0,0);
            }

            popup.show(comp, r.x, r.y + r.height);
            list.setSelectedIndex(0);
            list.requestFocusInWindow();

        } catch (BadLocationException ignored) { }
    }

    private static void insertSelection(JTextComponent comp,
                                        String selected,
                                        int start,
                                        int end) {
        if (selected == null) return;
        try {
            Document doc = comp.getDocument();
            doc.remove(start, end - start);
            doc.insertString(start, selected, null);
        } catch (BadLocationException ignored) { }
    }
}
