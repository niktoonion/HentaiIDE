package ui;

import java.awt.Color;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

public final class LuaSyntaxHighlighter {

    private static final String[] KEYWORDS = {
            "and","break","do","else","elseif","end","false","for","function","if","in","local",
            "nil","not","or","repeat","return","then","true","until","while"
    };
    private static final Pattern PATTERN_KEYWORD = Pattern.compile(
            "\\b(?:" + String.join("|", KEYWORDS) + ")\\b");

    private static final Pattern PATTERN_NUMBER = Pattern.compile(
            "\\b0[xX][0-9a-fA-F]+\\b|\\b\\d+(?:\\.\\d*)?(?:[eE][+-]?\\d+)?\\b");

    private static final Pattern PATTERN_STRING = Pattern.compile(
            "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'|\\[\\[(?:.|\\R)*?\\]\\]");

    private static final Pattern PATTERN_COMMENT = Pattern.compile(
            "--\\[\\[(?:.|\\R)*?\\]\\]|--[^\\n]*", Pattern.DOTALL);

    private static final Pattern PATTERN_OPERATOR = Pattern.compile(
            "[=+\\-*/%<>!~^]=?|&&?|\\|\\|?|\\.\\.\\.|::|\\.|,|;|\\(|\\)|\\[|\\]|\\{|\\}");

    private static final AttributeSet ATTR_DEFAULT  = style(Color.GREEN, false);
    private static final AttributeSet ATTR_KEYWORD  = style(new Color(0x569CD6), true);
    private static final AttributeSet ATTR_NUMBER   = style(new Color(0xB5CEA8), false);
    private static final AttributeSet ATTR_STRING   = style(new Color(0xCE9178), false);
    private static final AttributeSet ATTR_COMMENT  = style(new Color(0x6A9955), false);
    private static final AttributeSet ATTR_OPERATOR = style(new Color(0xD4D4D4), false);

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
            apply(PATTERN_COMMENT,  doc, text, ATTR_COMMENT);
            apply(PATTERN_STRING,   doc, text, ATTR_STRING);
            apply(PATTERN_NUMBER,   doc, text, ATTR_NUMBER);
            apply(PATTERN_KEYWORD,  doc, text, ATTR_KEYWORD);
            apply(PATTERN_OPERATOR, doc, text, ATTR_OPERATOR);
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
