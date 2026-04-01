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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.*;
import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

import extra.FindReplaceDialog;          // диалог «Найти / Заменить»
/* GoToLineDialog находится в том же пакете ui, импортировать не нужно */

/**
 * Панель‑редактор.
 *
 * <ul>
 *   <li>Word‑wrap включён по‑умолчанию.</li>
 *   <li>Улучшенный Undo/Redo (группировка быстрых правок, подписи в меню).</li>
 *   <li>Автодополнение – клавиша <b>Ctrl + Space</b> открывает {@link CodeCompletion}.</li>
 *   <li>Подсветка RMS (RmsSyntaxHighlighter) включается автоматически,
 *       если файл имеет расширение *.rms*.</li>
 *   <li>Новые возможности:
 *       <ul>
 *          <li>Ctrl / — переключение однострочного комментария;</li>
 *          <li>Авто‑закрытие скобок/строк;</li>
 *          <li>Подсветка парных скобок;</li>
 *          <li>Диалог «Перейти к строке…» (Ctrl G);</li>
 *          <li>Контекстное меню по правой кнопке мыши.</li>
 *       </ul>
 *   </li>
 * </ul>
 */
public class EditorPanel extends JPanel {

    /* --------------------------------------------------------------
     *  Поля
     * -------------------------------------------------------------- */
    private final WrapableTextPane textPane;
    private File file;                         // null → «Без названия»
    private boolean modified = false;
    private final UndoManager undoManager = new UndoManager();

    /* ----- Compound‑edit (группировка быстрых правок) ----- */
    private CompoundEdit curCompound;
    private final Timer compoundTimer;
    private static final int COMPOUND_TIMEOUT = 500; // мс

    private Language language = Language.UNKNOWN;   // текущий язык

    /* ----- вспомогательные карты ----------------------------------- */
    /** Однострочные комментарии для разных языков. */
    private static final Map<Language, String> LINE_COMMENT;

    /** Пары скобок/строк, которые авто‑добавляются. */
    private static final Map<Character, Character> PAIRS;

    static {
        /* ---- Комментарии ------------------------------------------------ */
        EnumMap<Language, String> commentMap = new EnumMap<>(Language.class);
        commentMap.put(Language.CPP,    "//");
        commentMap.put(Language.JAVA,   "//");
        commentMap.put(Language.RMS,    "//");
        commentMap.put(Language.LUA,    "--");
        commentMap.put(Language.PYTHON, "#");
        LINE_COMMENT = Collections.unmodifiableMap(commentMap);

        /* ---- Пары скобок/строк ------------------------------------------- */
        HashMap<Character, Character> pairsMap = new HashMap<>();
        pairsMap.put('(', ')');
        pairsMap.put('[', ']');
        pairsMap.put('{', '}');
        pairsMap.put('\"', '\"');
        pairsMap.put('\'', '\'');
        PAIRS = Collections.unmodifiableMap(pairsMap);
    }

    /* ----- подсветка парных скобок ----- */
    private final Highlighter highlighter;
    private Object braceTag1 = null;
    private Object braceTag2 = null;

    /* --------------------------------------------------------------
     *  Конструкторы
     * -------------------------------------------------------------- */
    /** Пустой конструктор – создаёт «Без названия». */
    public EditorPanel() {
        this(null, "");
    }

    /** Конструктор с уже существующим файлом. */
    public EditorPanel(File file, String content) {
        setLayout(new BorderLayout());

        /* --------------------- текстовый компонент --------------------- */
        textPane = new WrapableTextPane();
        textPane.setText(content);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 14));
        textPane.setBackground(Color.BLACK);
        textPane.setForeground(Color.GREEN);
        textPane.setCaretColor(Color.WHITE);
        textPane.setSelectionColor(new Color(0x444444));
        textPane.setWordWrap(true);               // перенос строк включён

        highlighter = textPane.getHighlighter();

        /* -------------------- Undo/Redo + compound‑edit -------------------- */
        compoundTimer = new Timer(COMPOUND_TIMEOUT, e -> closeCompoundEdit());
        compoundTimer.setRepeats(false);

        textPane.getDocument().addUndoableEditListener(e -> {
            if (curCompound == null || !curCompound.isInProgress()) {
                curCompound = new CompoundEdit();
                undoManager.addEdit(curCompound);
            }
            curCompound.addEdit(e.getEdit());
            compoundTimer.restart();               // продлеваем таймер при каждом вводе
        });

        installUndoRedoKeyBindings();          // Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z
        installCodeCompletionKeyBinding();      // Ctrl+Space → автодополнение
        installCommentToggleKeyBinding();       // Ctrl+/ → коммент/раскоммент
        installAutoCloseKeyListener();          // авто‑закрытие скобок/строк
        installContextMenu();                  // *** правый клик ***

        /* -------------------- модификация (знак «*» в заголовке) -------------------- */
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { setModified(true); firePropertyChange("undoRedo", null, null); }
            @Override public void removeUpdate(DocumentEvent e) { setModified(true); firePropertyChange("undoRedo", null, null); }
            @Override public void changedUpdate(DocumentEvent e) { setModified(true); firePropertyChange("undoRedo", null, null); }
        });

        /* -------------------- прокрутка + номера строк -------------------- */
        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setRowHeaderView(new LineNumberComponent(textPane));
        add(scroll, BorderLayout.CENTER);

        // подсветка парных скобок при перемещении курсора
        textPane.addCaretListener(e -> highlightMatchingBrace());

        // очистка истории undo при открытии нового файла
        undoManager.discardAllEdits();

        // установить файл → определить язык и подключить подсветку
        setFile(file);
    }

    /* --------------------------------------------------------------
     *  Compound‑edit helpers
     * -------------------------------------------------------------- */
    private void closeCompoundEdit() {
        if (curCompound != null && curCompound.isInProgress()) {
            curCompound.end();
            curCompound = null;
        }
    }

    /* --------------------------------------------------------------
     *  Undo/Redo key bindings
     * -------------------------------------------------------------- */
    private void installUndoRedoKeyBindings() {
        // ---- Undo (Ctrl+Z) ----
        KeyStroke ksUndo = KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK);
        textPane.getInputMap(JComponent.WHEN_FOCUSED).put(ksUndo, "undo");
        textPane.getActionMap().put("undo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                undo();
                firePropertyChange("undoRedo", null, null);
            }
        });

        // ---- Redo (Ctrl+Y) ----
        KeyStroke ksRedo = KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                InputEvent.CTRL_DOWN_MASK);
        textPane.getInputMap(JComponent.WHEN_FOCUSED).put(ksRedo, "redo");
        textPane.getActionMap().put("redo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                redo();
                firePropertyChange("undoRedo", null, null);
            }
        });

        // ---- Redo (Ctrl+Shift+Z) ----
        KeyStroke ksRedoShift = KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        textPane.getInputMap(JComponent.WHEN_FOCUSED).put(ksRedoShift, "redoShift");
        textPane.getActionMap().put("redoShift", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                redo();
                firePropertyChange("undoRedo", null, null);
            }
        });
    }

    /* --------------------------------------------------------------
     *  Ctrl+Space → автодополнение
     * -------------------------------------------------------------- */
    private void installCodeCompletionKeyBinding() {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
                InputEvent.CTRL_DOWN_MASK);
        textPane.getInputMap(JComponent.WHEN_FOCUSED).put(ks, "codeComplete");
        textPane.getActionMap().put("codeComplete", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                CodeCompletion.showCompletion(textPane);
            }
        });
    }

    /* --------------------------------------------------------------
     *  Ctrl+/ → переключение комментариев
     * -------------------------------------------------------------- */
    private void installCommentToggleKeyBinding() {
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH,
                InputEvent.CTRL_DOWN_MASK);
        textPane.getInputMap(JComponent.WHEN_FOCUSED).put(ks, "toggleComment");
        textPane.getActionMap().put("toggleComment", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                toggleComment();
            }
        });
    }

    /**
     * Переключает однострочный комментарий в текущем диапазоне.
     * Если все строки уже закомментированы – раскомментирует их,
     * иначе – закомментирует.
     */
    private void toggleComment() {
        Language lang = (Language) textPane.getClientProperty("language");
        String comment = LINE_COMMENT.getOrDefault(lang, "//");

        Document doc = textPane.getDocument();
        int selStart = textPane.getSelectionStart();
        int selEnd   = textPane.getSelectionEnd();

        Element root = doc.getDefaultRootElement();
        int startLine = root.getElementIndex(selStart);
        int endLine   = root.getElementIndex(selEnd);

        boolean allCommented = true;

        try {
            // 1) проверяем, все ли строки уже закомментированы
            for (int line = startLine; line <= endLine; line++) {
                Element lineElem = root.getElement(line);
                int lineStart = lineElem.getStartOffset();
                int lineEnd   = lineElem.getEndOffset() - 1; // без \n
                String lineText = doc.getText(lineStart,
                        lineEnd - lineStart);
                // убираем ведущие пробелы (Java 8‑compatible)
                String trimmed = lineText.replaceFirst("^\\s+", "");
                if (!trimmed.startsWith(comment)) {
                    allCommented = false;
                    break;
                }
            }

            // 2) применяем изменения снизу‑вверх, чтобы не «съесть» смещения
            for (int line = endLine; line >= startLine; line--) {
                Element lineElem = root.getElement(line);
                int lineStart = lineElem.getStartOffset();
                int lineEnd   = lineElem.getEndOffset() - 1;
                String lineText = doc.getText(lineStart,
                        lineEnd - lineStart);

                // позиция первого НЕ‑пробельного символа
                int wsEnd = 0;
                while (wsEnd < lineText.length()
                        && Character.isWhitespace(lineText.charAt(wsEnd))) {
                    wsEnd++;
                }

                if (allCommented) {
                    // раскомментировать – удалить токен после пробелов
                    if (lineText.startsWith(comment, wsEnd)) {
                        int delPos = lineStart + wsEnd;
                        doc.remove(delPos, comment.length());
                    }
                } else {
                    // закомментировать – вставить токен после пробелов
                    int insPos = lineStart + wsEnd;
                    doc.insertString(insPos, comment + " ", null);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /* --------------------------------------------------------------
     *  Авто‑закрытие скобок/строк
     * -------------------------------------------------------------- */
    private void installAutoCloseKeyListener() {
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();
                Character closing = PAIRS.get(ch);
                if (closing != null) {
                    e.consume();
                    try {
                        Document doc = textPane.getDocument();
                        int caretPos = textPane.getCaretPosition();
                        int selStart = textPane.getSelectionStart();
                        int selEnd   = textPane.getSelectionEnd();

                        if (selStart != selEnd) {
                            // пользователь выделил диапазон – обернём его парой
                            doc.insertString(selEnd,
                                    closing.toString(), null);
                            doc.insertString(selStart,
                                    String.valueOf(ch), null);
                            textPane.select(selStart + 1, selEnd + 1);
                        } else {
                            // просто вставляем пару и оставляем курсор между ними
                            doc.insertString(caretPos,
                                    "" + ch + closing, null);
                            textPane.setCaretPosition(caretPos + 1);
                        }
                    } catch (BadLocationException ex) {
                        // игнорируем – маловероятно
                    }
                }
            }
        });
    }

    /* --------------------------------------------------------------
     *  Подсветка парных скобок
     * -------------------------------------------------------------- */
    private static boolean isOpening(char c)  { return "({[".indexOf(c) >= 0; }
    private static boolean isClosing(char c)  { return ")}]".indexOf(c) >= 0; }

    private static char matchingClosing(char open) {
        switch (open) {
            case '(' : return ')';
            case '[' : return ']';
            case '{' : return '}';
            default  : return '\0';
        }
    }

    private static char matchingOpening(char close) {
        switch (close) {
            case ')' : return '(';
            case ']' : return '[';
            case '}' : return '{';
            default  : return '\0';
        }
    }

    /**
     * Находит позицию парной скобки.
     *
     * @param text    полный текст документа
     * @param pos     позиция скобки (открывающей или закрывающей)
     * @param forward {@code true} – искать вперёд от открывающей,
     *                {@code false} – искать назад от закрывающей
     * @return позицию парной скобки или {@code -1}, если не найдено
     */
    private static int findMatchingBrace(String text, int pos, boolean forward) {
        char startChar = text.charAt(pos);
        char targetChar = forward ? matchingClosing(startChar)
                                   : matchingOpening(startChar);
        if (targetChar == '\0') return -1;

        int depth = 0;
        if (forward) {
            for (int i = pos + 1; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == startChar) depth++;
                else if (c == targetChar) {
                    if (depth == 0) return i;
                    depth--;
                }
            }
        } else {
            for (int i = pos - 1; i >= 0; i--) {
                char c = text.charAt(i);
                if (c == startChar) depth++;
                else if (c == targetChar) {
                    if (depth == 0) return i;
                    depth--;
                }
            }
        }
        return -1;
    }

    /** Подсвечивает парные скобки в текущей позиции курсора. */
    private void highlightMatchingBrace() {
        // убираем предыдущее выделение
        if (braceTag1 != null) {
            highlighter.removeHighlight(braceTag1);
            braceTag1 = null;
        }
        if (braceTag2 != null) {
            highlighter.removeHighlight(braceTag2);
            braceTag2 = null;
        }

        int caret = textPane.getCaretPosition();
        if (caret == 0) return;
        try {
            Document doc = textPane.getDocument();
            String text = doc.getText(0, doc.getLength());

            // часто скобка находится слева от caret
            if (caret > 0) {
                char ch = text.charAt(caret - 1);
                if (isOpening(ch) || isClosing(ch)) {
                    int matchPos;
                    int openPos = caret - 1;

                    if (isOpening(ch)) {
                        matchPos = findMatchingBrace(text, openPos, true);
                    } else {
                        matchPos = findMatchingBrace(text, caret, false);
                        openPos = caret; // позиция закрывающей скобки
                    }

                    if (matchPos >= 0) {
                        // ----- HighlightPainter (используем полный путь) -----
                        Highlighter.HighlightPainter painter =
                                new DefaultHighlighter.DefaultHighlightPainter(
                                        new Color(0x444444));

                        braceTag1 = highlighter.addHighlight(openPos,
                                openPos + 1, painter);
                        braceTag2 = highlighter.addHighlight(matchPos,
                                matchPos + 1, painter);
                    }
                }
            }
        } catch (BadLocationException ignored) {
            // ничего страшного – просто не подсвечиваем
        }
    }

    /* --------------------------------------------------------------
     *  Контекстное меню (правый клик)
     * -------------------------------------------------------------- */
    private void installContextMenu() {
        final JPopupMenu menu = new JPopupMenu();

        // ----- Операции Undo/Redo -----
        final JMenuItem miUndo = new JMenuItem("Отменить");
        miUndo.addActionListener(e -> {
            undo();
            firePropertyChange("undoRedo", null, null);
        });
        menu.add(miUndo);

        final JMenuItem miRedo = new JMenuItem("Повторить");
        miRedo.addActionListener(e -> {
            redo();
            firePropertyChange("undoRedo", null, null);
        });
        menu.add(miRedo);
        menu.addSeparator();

        // ----- Кнопки Cut / Copy / Paste -----
        final JMenuItem miCut = new JMenuItem("Вырезать");
        miCut.addActionListener(e -> textPane.cut());
        menu.add(miCut);

        final JMenuItem miCopy = new JMenuItem("Копировать");
        miCopy.addActionListener(e -> textPane.copy());
        menu.add(miCopy);

        final JMenuItem miPaste = new JMenuItem("Вставить");
        miPaste.addActionListener(e -> textPane.paste());
        menu.add(miPaste);
        menu.addSeparator();

        // ----- Выделить всё -----
        final JMenuItem miSelectAll = new JMenuItem("Выделить всё");
        miSelectAll.addActionListener(e -> textPane.selectAll());
        menu.add(miSelectAll);
        menu.addSeparator();

        // ----- Комментировать / раскомментировать -----
        final JMenuItem miComment = new JMenuItem("Комментировать/раскомментировать");
        miComment.addActionListener(e -> toggleComment());
        menu.add(miComment);
        menu.addSeparator();

        // ----- Поиск / Замена -----
        final JMenuItem miFindReplace = new JMenuItem("Найти / Заменить…");
        miFindReplace.addActionListener(e -> {
            Component window = SwingUtilities.getWindowAncestor(EditorPanel.this);
            new FindReplaceDialog((Frame) SwingUtilities.getWindowAncestor(EditorPanel.this), textPane)
                    .setVisible(true);
        });
        menu.add(miFindReplace);
        menu.addSeparator();

        // ----- Перейти к строке -----
        final JMenuItem miGoToLine = new JMenuItem("Перейти к строке…");
        miGoToLine.addActionListener(e -> {
            Component window = SwingUtilities.getWindowAncestor(EditorPanel.this);
            new GoToLineDialog((Frame) SwingUtilities.getWindowAncestor(EditorPanel.this), EditorPanel.this)
                    .setVisible(true);
        });
        menu.add(miGoToLine);

        /* --------- MouseListener, показывающий меню --------- */
        textPane.addMouseListener(new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // Обновляем enabled‑состояния пунктов перед показом
                    miUndo.setEnabled(undoManager.canUndo());
                    miRedo.setEnabled(undoManager.canRedo());

                    boolean hasSelection = textPane.getSelectionStart() != textPane.getSelectionEnd();
                    miCut.setEnabled(hasSelection);
                    miCopy.setEnabled(hasSelection);
                    // Paste обычно оставляем включенным – если в буфере нет текста,
                    // вставка просто ничего не сделает.
                    miPaste.setEnabled(true);
                    miComment.setEnabled(true);

                    menu.show(e.getComponent(), e.getX(), e.getY());
                    textPane.requestFocusInWindow();   // чтобы дальше работали шорткаты
                }
            }
            @Override public void mousePressed(MouseEvent e)  { maybeShowPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
        });
    }

    /* -----------------------------------------------------------------
     *  Свойства
     * ----------------------------------------------------------------- */
    public JTextComponent getTextArea() { return textPane; }
    public String getText()               { return textPane.getText(); }
    public File getFile()                  { return file; }

    /** Устанавливаем файл, определяем язык и подключаем подсветку. */
    public void setFile(File f) {
        this.file = f;
        firePropertyChange("title", null,
                (f != null) ? f.getName() : "Без названия");

        language = (f != null) ? Language.detect(f.getName())
                               : Language.UNKNOWN;
        textPane.putClientProperty("language", language);
        SyntaxHighlighterFactory.install(textPane, language);
    }

    public boolean isModified() { return modified; }
    public void setModified(boolean m) {
        boolean old = this.modified;
        this.modified = m;
        firePropertyChange("modified", old, m);
    }

    /** Переключить/отключить word‑wrap. */
    public void setWordWrap(boolean wrap) { textPane.setWordWrap(wrap); }

    /* -----------------------------------------------------------------
     *  Undo / Redo
     * ----------------------------------------------------------------- */
    public void undo() {
        closeCompoundEdit();
        if (undoManager.canUndo()) undoManager.undo();
    }

    public void redo() {
        closeCompoundEdit();
        if (undoManager.canRedo()) undoManager.redo();
    }

    /** Текст подписи для пункта меню «Отменить». */
    public String getUndoPresentationName() {
        if (undoManager.canUndo())
            return undoManager.getUndoPresentationName();
        return "Отменить";
    }

    /** Текст подписи для пункта меню «Повторить». */
    public String getRedoPresentationName() {
        if (undoManager.canRedo())
            return undoManager.getRedoPresentationName();
        return "Повторить";
    }
}
