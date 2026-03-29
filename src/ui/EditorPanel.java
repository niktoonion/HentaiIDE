package ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
 * Панель‑редактор.
 *
 * • Word‑wrap включён по умолчанию.
 * • Улучшенный Undo/Redo (группировка быстрых правок, подписи в меню).
 * • Автодополнение – клавиша **Ctrl + Space** открывает {@link CodeCompletion}.
 * • Подсветка RMS (RmsSyntaxHighlighter) включается автоматически,
 *   если файл имеет расширение *.rms*.
 */
public class EditorPanel extends JPanel {

    private final WrapableTextPane textPane;
    private File file;                       // null → новый («Без названия»)
    private boolean modified = false;
    private final UndoManager undoManager = new UndoManager();

    /* ----- Compound‑edit (группировка быстрых правок) ----- */
    private CompoundEdit curCompound;
    private final Timer compoundTimer;
    private static final int COMPOUND_TIMEOUT = 500; // мс

    private boolean rmsMode = false;          // включена ли подсветка RMS

    /** Пустой конструктор – создаёт «Без названия». */
    public EditorPanel() {
        this(null, "");
    }

    /** Конструктор с уже существующим файлом. */
    public EditorPanel(File file, String content) {
        setLayout(new BorderLayout());

        textPane = new WrapableTextPane();
        textPane.setText(content);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 14));
        textPane.setBackground(Color.BLACK);
        textPane.setForeground(Color.GREEN);
        textPane.setCaretColor(Color.WHITE);
        textPane.setSelectionColor(new Color(0x444444));
        textPane.setWordWrap(true);            // **Перенос строк включён по‑умолчанию**

        /* ---------- Undo/Redo + compound‑edit ---------- */
        compoundTimer = new Timer(COMPOUND_TIMEOUT, e -> closeCompoundEdit());
        compoundTimer.setRepeats(false);

        textPane.getDocument().addUndoableEditListener(e -> {
            if (curCompound == null || !curCompound.isInProgress()) {
                curCompound = new CompoundEdit();
                undoManager.addEdit(curCompound);
            }
            curCompound.addEdit(e.getEdit());
            compoundTimer.restart();            // продлеваем таймер при каждом вводе
        });

        installUndoRedoKeyBindings();          // Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z
        installCodeCompletionKeyBinding();      // **Ctrl+Space → автодополнение**

        /* ---------- Модификация (for title «*» и т.п.) ---------- */
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { setModified(true); firePropertyChange("undoRedo", null, null); }
            @Override public void removeUpdate(DocumentEvent e) { setModified(true); firePropertyChange("undoRedo", null, null); }
            @Override public void changedUpdate(DocumentEvent e) { setModified(true); firePropertyChange("undoRedo", null, null); }
        });

        /* ---------- Прокрутка + линейка строк ---------- */
        JScrollPane scroll = new JScrollPane(textPane);
        scroll.setRowHeaderView(new LineNumberComponent(textPane));
        add(scroll, BorderLayout.CENTER);

        // Очистка истории undo при открытии нового файла
        undoManager.discardAllEdits();

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
     *  Undo/Redo key bindings (Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z)
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
     *  **Ctrl+Space → автодополнение**
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

    // -----------------------------------------------------------------
    //  Свойства
    // -----------------------------------------------------------------
    public JTextComponent getTextArea() { return textPane; }

    public String getText() { return textPane.getText(); }

    public File getFile() { return file; }

    public void setFile(File f) {
        this.file = f;
        firePropertyChange("title", null,
                (f != null) ? f.getName() : "Без названия");
        if (f != null && f.getName().toLowerCase().endsWith(".rms")) {
            enableRmsHighlighting();
        }
    }

    public boolean isModified() { return modified; }

    public void setModified(boolean m) {
        boolean old = this.modified;
        this.modified = m;
        firePropertyChange("modified", old, m);
    }

    /** Переключить/отключить word‑wrap. */
    public void setWordWrap(boolean wrap) { textPane.setWordWrap(wrap); }

    // -----------------------------------------------------------------
    //  Undo / Redo
    // -----------------------------------------------------------------
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

    // -----------------------------------------------------------------
    //  Подсветка RMS
    // -----------------------------------------------------------------
    private void enableRmsHighlighting() {
        if (rmsMode) return;
        rmsMode = true;
        RmsSyntaxHighlighter.install(textPane);
    }
}
