/*
 * Copyright (C) 2026 Федотов Владислав Игоревич (niktoonion)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later   // ← для машинного анализа (необязательно)
 */

package ui;

import java.awt.Color;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public final class CppSyntaxHighlighter {

    /* ---------- Ключевые слова ---------- */
    private static final String[] KEYWORDS = {
        "alignas","alignof","and","and_eq","asm","auto","bitand","bitor","bool","break",
        "case","catch","char","char16_t","char32_t","class","compl","const","constexpr",
        "const_cast","continue","decltype","default","delete","do","double","dynamic_cast",
        "else","enum","explicit","export","extern","false","float","for","friend","goto",
        "if","inline","int","long","mutable","namespace","new","noexcept","not","not_eq",
        "nullptr","operator","or","or_eq","private","protected","public","register",
        "reinterpret_cast","return","short","signed","sizeof","static","static_assert",
        "static_cast","struct","switch","template","this","thread_local","throw","true",
        "try","typedef","typeid","typename","union","unsigned","using","virtual","void",
        "volatile","wchar_t","while","xor","xor_eq"
    };
    private static final Pattern PATTERN_KEYWORD = Pattern.compile(
            "\\b(?:" + String.join("|", KEYWORDS) + ")\\b");

    private static final Pattern PATTERN_PREPROC   = Pattern.compile(
            "^[ \\t]*#[ \\t]*\\w+(?:[^\\n]*)", Pattern.MULTILINE);
    private static final Pattern PATTERN_NUMBER    = Pattern.compile("\\b\\d+(?:\\.\\d*)?(?:[eE][+-]?\\d+)?\\b");
    private static final Pattern PATTERN_STRING    = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'");
    private static final Pattern PATTERN_COMMENT   = Pattern.compile("//[^\\n]*|/\\*(?:.|\\R)*?\\*/", Pattern.DOTALL);
    private static final Pattern PATTERN_OPERATOR  = Pattern.compile("[=+\\-*/%<>!&|^~]=?|&&|\\|\\||\\?\\:|::|\\.|,|;|\\(|\\)|\\[|\\]|\\{|\\}");
    private static final Pattern PATTERN_BRACKETS  = Pattern.compile("[\\[\\]{}()]");

    /* ---------- Стили ---------- */
    private static final AttributeSet ATTR_DEFAULT   = style(Color.GREEN, false);
    private static final AttributeSet ATTR_KEYWORD   = style(new Color(0x569CD6), true);
    private static final AttributeSet ATTR_PREPROC   = style(new Color(0xC586C0), false);
    private static final AttributeSet ATTR_NUMBER    = style(new Color(0xB5CEA8), false);
    private static final AttributeSet ATTR_STRING    = style(new Color(0xCE9178), false);
    private static final AttributeSet ATTR_COMMENT   = style(new Color(0x6A9955), false);
    private static final AttributeSet ATTR_OPERATOR  = style(new Color(0xD4D4D4), false);
    private static final AttributeSet ATTR_BRACKETS  = style(new Color(0xC586C0), false);

    private static SimpleAttributeSet style(Color fg, boolean bold) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setForeground(s, fg);
        StyleConstants.setBold(s, bold);
        return s;
    }

    public static void install(JTextComponent comp) {
        comp.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { highlight(comp); }
            @Override public void removeUpdate(DocumentEvent e) { highlight(comp); }
            @Override public void changedUpdate(DocumentEvent e) {}
        });
        highlight(comp);
    }

    private static void highlight(JTextComponent comp) {
        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = (StyledDocument) comp.getDocument();
            String text;
            try { text = doc.getText(0, doc.getLength()); }
            catch (BadLocationException ex) { return; }

            doc.setCharacterAttributes(0, text.length(), ATTR_DEFAULT, true);
            apply(PATTERN_COMMENT,   doc, text, ATTR_COMMENT);
            apply(PATTERN_STRING,    doc, text, ATTR_STRING);
            apply(PATTERN_PREPROC,   doc, text, ATTR_PREPROC);
            apply(PATTERN_NUMBER,    doc, text, ATTR_NUMBER);
            apply(PATTERN_KEYWORD,   doc, text, ATTR_KEYWORD);
            apply(PATTERN_OPERATOR,  doc, text, ATTR_OPERATOR);
            apply(PATTERN_BRACKETS,  doc, text, ATTR_BRACKETS);
        });
    }

    private static void apply(Pattern p, StyledDocument doc, String text, AttributeSet attr) {
        Matcher m = p.matcher(text);
        while (m.find()) {
            doc.setCharacterAttributes(m.start(),
                    m.end() - m.start(),
                    attr, false);
        }
    }
}
