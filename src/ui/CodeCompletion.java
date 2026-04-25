/*
 * Copyright (C) 2026 Fedotov Vladislav Igorevich (niktoonion)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;

public final class CodeCompletion {

    private static final Map<Language, String[]> KEYWORDS_MAP = new EnumMap<>(Language.class);
    static {
        // C++
        KEYWORDS_MAP.put(Language.CPP, new String[]{
                "int","float","double","char","bool","if","else","for","while","do",
                "switch","case","break","continue","return","class","struct","public",
                "private","protected","static","constexpr","namespace","using",
                "template","typename","new","delete","try","catch","throw",
                "std","cout","cin","cerr","getline","vector","string","map","set"
        });
        // Java
        KEYWORDS_MAP.put(Language.JAVA, new String[]{
                "int","long","float","double","char","boolean","String","if","else","for",
                "while","do","switch","case","break","continue","return","class",
                "interface","public","private","protected","static","final","abstract",
                "synchronized","volatile","transient","new","try","catch","throw",
                "System","System.out","System.err","ArrayList","List","Map","HashMap",
                "Set","HashSet","Collections"
        });
        // Lua
        KEYWORDS_MAP.put(Language.LUA, new String[]{
                "function","local","end","if","then","else","elseif","for","in","while",
                "repeat","until","return","true","false","nil","and","or","not","pairs",
                "ipairs","require","module"
        });
        // Python
        KEYWORDS_MAP.put(Language.PYTHON, new String[]{
                "def","class","import","from","as","if","elif","else","while","for","in",
                "break","continue","return","True","False","None","and","or","not","lambda",
                "with","yield","try","except","finally","raise","print","len","range",
                "list","dict","set","tuple","str","int","float","bool","open","enumerate"
        });
        // RMS – оставляем старый набор
        KEYWORDS_MAP.put(Language.RMS, new String[]{
                "if","else","while","for","int","bool","string","i","b","s"
        });
        KEYWORDS_MAP.put(Language.UNKNOWN, new String[]{});
    }

    public static void showCompletion(JTextComponent comp) {
        Language lang = (Language) comp.getClientProperty("language");
        if (lang == null) lang = Language.UNKNOWN;

        try {
            int caretPos = comp.getCaretPosition();
            Document doc = comp.getDocument();

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

            String[] kw = KEYWORDS_MAP.getOrDefault(lang, new String[]{});
            for (String k : kw) if (k.startsWith(prefix)) candidates.add(k);

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
