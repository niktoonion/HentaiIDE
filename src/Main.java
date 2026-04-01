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


import extra.Extra;
import extra.RecentFilesManager;
import fileMngr.FileMngr;
import ui.EditorPanel;
import extra.FindReplaceDialog;
import ui.CodeCompletion;
import ui.GoToLineDialog;
import ui.ShortcutHelpDialog;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Главный класс IDE.
 * Включает:
 *   – динамические подписи пунктов «Отменить/Повторить»,
 *   – автоматическое обновление их состояния,
 *   – word‑wrap включён по‑умолчанию,
 *   – автодополнение (Ctrl + Space),
 *   – меню «Недавние»,
 *   – панель‑инструментов (toolbar) со всеми часто‑используемыми функциями,
 *   – диалог «Список горячих клавиш».
 */
public class Main extends JFrame {
	

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final JLabel statusLabel = new JLabel("Готов");
    private final JMenu recentMenu = new JMenu("Недавние");

    // пункты Undo/Redo держим в полях, чтобы менять их текст
    private JMenuItem undoItem;
    private JMenuItem redoItem;

    private boolean wordWrapEnabled = true;   // включено по умолчанию

    public Main() {
        setTitle("HentaiIDE");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(Extra.sc_getWidth() * 3 / 4, Extra.sc_getHeight() * 3 / 4);
        setLocationRelativeTo(null);
        
        URL iconURL = getClass().getResource("icon.jpg");   // <-- ваш путь
        if (iconURL != null) {
            Image icon = Toolkit.getDefaultToolkit().getImage(iconURL);
            setIconImage(icon);   // <-- задаём иконку Frame'а
        } else {
            System.err.println("Иконка /icon.jpg не найдена в classpath!");
        }
        initUI();
        initListeners();
    }

    // --------------------------------------------------------------
    //  UI
    // --------------------------------------------------------------
    private void initUI() {
        JMenuBar menuBar = new JMenuBar();

        // ==== Файл ====
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newItem = new JMenuItem("Новый");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem openItem = new JMenuItem("Открыть");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveItem = new JMenuItem("Сохранить");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveAsItem = new JMenuItem("Сохранить как…");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        JMenuItem closeTabItem = new JMenuItem("Закрыть вкладку");
        closeTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveAllItem = new JMenuItem("Сохранить всё");
        saveAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK));
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4,
                InputEvent.ALT_DOWN_MASK));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(closeTabItem);
        fileMenu.add(saveAllItem);
        fileMenu.addSeparator();
        fileMenu.add(recentMenu);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // ==== Правка ====
        JMenu editMenu = new JMenu("Правка");
        editMenu.setMnemonic(KeyEvent.VK_E);

        undoItem = new JMenuItem("Отменить");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK));
        redoItem = new JMenuItem("Повторить");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                InputEvent.CTRL_DOWN_MASK));

        JMenuItem cutItem = new JMenuItem("Вырезать");
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem copyItem = new JMenuItem("Копировать");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem pasteItem = new JMenuItem("Вставить");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem findItem = new JMenuItem("Найти…");
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                InputEvent.CTRL_DOWN_MASK));

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(findItem);
        menuBar.add(editMenu);

        // ==== Вид ====
        JMenu viewMenu = new JMenu("Вид");
        JCheckBoxMenuItem wrapItem = new JCheckBoxMenuItem("Перенос строк");
        wrapItem.setSelected(wordWrapEnabled);
        viewMenu.add(wrapItem);

        // Перейти к строке (Ctrl+G)
        JMenuItem goToLineItem = new JMenuItem("Перейти к строке…");
        goToLineItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(goToLineItem);
        goToLineItem.addActionListener(e -> goToLine());

        menuBar.add(viewMenu);

        // ==== Справка ====
        JMenu helpMenu = new JMenu("Справка");
        JMenuItem shortcutsItem = new JMenuItem("Список горячих клавиш…");
        shortcutsItem.addActionListener(e ->
                new ShortcutHelpDialog(this).setVisible(true));
        helpMenu.add(shortcutsItem);
        helpMenu.addSeparator();

        JMenuItem aboutItem = new JMenuItem("О программе");
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);


        // ---------- Статус‑бар ----------
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(Color.BLACK);
        statusLabel.setForeground(Color.YELLOW);
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        // ---------- Табы ----------
        add(tabbedPane, BorderLayout.CENTER);

        // ---------- Действия ----------
        // Файл
        newItem.addActionListener(e -> createNewTab());
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile(false));
        saveAsItem.addActionListener(e -> saveFile(true));
        closeTabItem.addActionListener(e -> closeCurrentTab());
        saveAllItem.addActionListener(e -> saveAllFiles());
        exitItem.addActionListener(e -> exitApplication());

        // Правка
        cutItem.addActionListener(e -> getCurrentTextArea().cut());
        copyItem.addActionListener(e -> getCurrentTextArea().copy());
        pasteItem.addActionListener(e -> getCurrentTextArea().paste());

        undoItem.addActionListener(e -> {
            EditorPanel ep = getCurrentEditor();
            if (ep != null) ep.undo();
            updateUndoRedoMenuItems();
        });
        redoItem.addActionListener(e -> {
            EditorPanel ep = getCurrentEditor();
            if (ep != null) ep.redo();
            updateUndoRedoMenuItems();
        });

        findItem.addActionListener(e -> {
            EditorPanel ep = getCurrentEditor();
            if (ep != null)
                new FindReplaceDialog(this, ep.getTextArea()).setVisible(true);
        });

        // Вид – Перенос строк
        wrapItem.addActionListener(e -> {
            wordWrapEnabled = wrapItem.isSelected();
            setWordWrapForAll(wordWrapEnabled);
        });

        // Справка
        aboutItem.addActionListener(e -> showAbout());

        // Заполняем меню «Недавние»
        updateRecentFilesMenu();
    }

    

    // --------------------------------------------------------------
    //  Слушатели окна
    // --------------------------------------------------------------
    private void initListeners() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exitApplication(); }
        });

        // Переключение табов → обновляем статус‑бар и подписи Undo/Redo
        tabbedPane.addChangeListener(e -> {
            updateCaretStatus();
            updateUndoRedoMenuItems();
        });
    }

    // --------------------------------------------------------------
    //  Основные действия
    // --------------------------------------------------------------
    private void createNewTab() {
        EditorPanel ep = new EditorPanel();
        ep.setWordWrap(wordWrapEnabled);
        tabbedPane.addTab("Без названия", ep);
        tabbedPane.setSelectedComponent(ep);
        attachEditorListeners(ep);
        updateUndoRedoMenuItems();
        statusLabel.setText("Создан новый файл");
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Открыть файл");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        final File file = chooser.getSelectedFile();

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws IOException {
                return FileMngr.readFile(file);
            }
            @Override protected void done() {
                try {
                    String content = get();
                    EditorPanel ep = new EditorPanel(file, content);
                    ep.setWordWrap(wordWrapEnabled);
                    tabbedPane.addTab(file.getName(), ep);
                    tabbedPane.setSelectedComponent(ep);
                    attachEditorListeners(ep);
                    RecentFilesManager.addFile(file.getAbsolutePath());
                    updateRecentFilesMenu();
                    updateUndoRedoMenuItems();
                    statusLabel.setText("Открыт файл: " + file.getAbsolutePath());
                } catch (Exception ex) {
                    showError("Не удалось открыть файл:\n" + ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void saveFile(boolean saveAs) {
        EditorPanel ep = getCurrentEditor();
        if (ep == null) return;

        File file = ep.getFile();

        if (saveAs || file == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Сохранить как");
            int res = chooser.showSaveDialog(this);
            if (res != JFileChooser.APPROVE_OPTION) return;
            file = chooser.getSelectedFile();
            ep.setFile(file);
        }

        try {
            FileMngr.writeFile(file, ep.getTextArea().getText());
            ep.setModified(false);
            int idx = tabbedPane.indexOfComponent(ep);
            if (idx >= 0) tabbedPane.setTitleAt(idx, file.getName());
            RecentFilesManager.addFile(file.getAbsolutePath());
            updateRecentFilesMenu();
            statusLabel.setText("Сохранён файл: " + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Ошибка при сохранении файла:\n" + ex.getMessage());
        }
    }

    private void saveAllFiles() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component c = tabbedPane.getComponentAt(i);
            if (c instanceof EditorPanel) {
                EditorPanel ep = (EditorPanel) c;
                if (ep.isModified()) {
                    tabbedPane.setSelectedComponent(ep);
                    saveFile(false);
                    if (ep.isModified()) return; // пользователь отменил
                }
                if (ep.getFile() != null)
                    RecentFilesManager.addFile(ep.getFile().getAbsolutePath());
            }
        }
        updateRecentFilesMenu();
        statusLabel.setText("Все изменения сохранены.");
    }

    private void closeCurrentTab() {
        EditorPanel ep = getCurrentEditor();
        if (ep == null) return;

        if (ep.isModified()) {
            int answer = JOptionPane.showConfirmDialog(this,
                    "Файл «" + (ep.getFile() != null ? ep.getFile().getName() : "Без названия")
                            + "» имеет несохранённые изменения.\nСохранить?",
                    "Несохранённые изменения", JOptionPane.YES_NO_CANCEL_OPTION);
            if (answer == JOptionPane.CANCEL_OPTION) return;
            if (answer == JOptionPane.YES_OPTION) {
                tabbedPane.setSelectedComponent(ep);
                saveFile(false);
                if (ep.isModified()) return;
            }
        }
        int idx = tabbedPane.indexOfComponent(ep);
        if (idx >= 0) {
            tabbedPane.remove(idx);
            statusLabel.setText("Вкладка закрыта");
        }
    }

    private void exitApplication() {
        if (!confirmAllClosed()) return;
        System.exit(0);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "HentaiIDE\nВерсия 0.0.1\nАвтор: niktoonion\n\nПростой редактор кода.",
                "О программе", JOptionPane.INFORMATION_MESSAGE);
    }

    // --------------------------------------------------------------
    //  Вспомогательные методы
    // --------------------------------------------------------------
    private EditorPanel getCurrentEditor() {
        Component c = tabbedPane.getSelectedComponent();
        return (c instanceof EditorPanel) ? (EditorPanel) c : null;
    }

    private JTextComponent getCurrentTextArea() {
        EditorPanel ep = getCurrentEditor();
        return (ep != null) ? ep.getTextArea() : null;
    }

    private void attachEditorListeners(EditorPanel ep) {
        // Заголовок вкладки
        ep.addPropertyChangeListener(evt -> {
            if ("title".equals(evt.getPropertyName())) {
                int idx = tabbedPane.indexOfComponent(ep);
                if (idx >= 0) tabbedPane.setTitleAt(idx, (String) evt.getNewValue());
            } else if ("modified".equals(evt.getPropertyName())) {
                int idx = tabbedPane.indexOfComponent(ep);
                if (idx >= 0) {
                    String title = tabbedPane.getTitleAt(idx);
                    boolean mod = (boolean) evt.getNewValue();
                    if (mod && !title.startsWith("*")) tabbedPane.setTitleAt(idx, "*" + title);
                    if (!mod && title.startsWith("*")) tabbedPane.setTitleAt(idx, title.substring(1));
                }
            } else if ("undoRedo".equals(evt.getPropertyName())) {
                updateUndoRedoMenuItems();
            }
        });

        // Обновляем статус‑бар при перемещении курсора
        ep.getTextArea().addCaretListener(e -> updateCaretStatus());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    /** Подтверждение закрытия всех файлов с несохранёнными изменениями. */
    private boolean confirmAllClosed() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            EditorPanel ep = (EditorPanel) tabbedPane.getComponentAt(i);
            if (ep.isModified()) {
                tabbedPane.setSelectedIndex(i);
                int answer = JOptionPane.showConfirmDialog(this,
                        "Файл «" + (ep.getFile() != null ? ep.getFile().getName() : "Без названия")
                                + "» имеет несохранённые изменения.\nСохранить?",
                        "Несохранённые изменения", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION) return false;
                if (answer == JOptionPane.YES_OPTION) {
                    saveFile(false);
                    if (ep.isModified()) return false;
                }
            }
        }
        return true;
    }

    /** Обновляем статус‑бар: строка/столбец. */
    private void updateCaretStatus() {
        JTextComponent comp = getCurrentTextArea();
        if (comp == null) {
            statusLabel.setText("Готов");
            return;
        }
        int caret = comp.getCaretPosition();
        Document doc = comp.getDocument();
        try {
            int line = doc.getDefaultRootElement().getElementIndex(caret) + 1;
            Element lineElem = doc.getDefaultRootElement().getElement(line - 1);
            int col = caret - lineElem.getStartOffset() + 1;
            statusLabel.setText(String.format("Строка: %d, Столбец: %d", line, col));
        } catch (Exception ignored) {
            statusLabel.setText("Готов");
        }
    }

    /** Переключить word‑wrap во всех открытых редакторах. */
    private void setWordWrapForAll(boolean wrap) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component c = tabbedPane.getComponentAt(i);
            if (c instanceof EditorPanel) ((EditorPanel) c).setWordWrap(wrap);
        }
    }

    /** Обновление пунктов меню «Отменить/Повторить». */
    private void updateUndoRedoMenuItems() {
        EditorPanel ep = getCurrentEditor();
        if (ep != null) {
            undoItem.setText(ep.getUndoPresentationName());
            redoItem.setText(ep.getRedoPresentationName());
            undoItem.setEnabled(!ep.getUndoPresentationName().equals("Отменить"));
            redoItem.setEnabled(!ep.getRedoPresentationName().equals("Повторить"));
        } else {
            undoItem.setText("Отменить");
            redoItem.setText("Повторить");
            undoItem.setEnabled(false);
            redoItem.setEnabled(false);
        }
    }

    /** Обновляем меню «Недавние». */
    private void updateRecentFilesMenu() {
        recentMenu.removeAll();
        List<String> recent = RecentFilesManager.getRecentFiles();
        if (recent.isEmpty()) {
            JMenuItem empty = new JMenuItem("(Нет недавних файлов)");
            empty.setEnabled(false);
            recentMenu.add(empty);
        } else {
            for (String p : recent) {
                JMenuItem it = new JMenuItem(new File(p).getName());
                it.setToolTipText(p);
                it.addActionListener(e -> openRecentFile(p));
                recentMenu.add(it);
            }
        }
    }

    private void openRecentFile(String path) {
        File f = new File(path);
        if (!f.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "Файл не найден:\n" + path, "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Тот же код, что и в openFile(), но без диалога
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws IOException {
                return FileMngr.readFile(f);
            }
            @Override protected void done() {
                try {
                    String content = get();
                    EditorPanel ep = new EditorPanel(f, content);
                    ep.setWordWrap(wordWrapEnabled);
                    tabbedPane.addTab(f.getName(), ep);
                    tabbedPane.setSelectedComponent(ep);
                    attachEditorListeners(ep);
                    RecentFilesManager.addFile(f.getAbsolutePath());
                    updateRecentFilesMenu();
                    updateUndoRedoMenuItems();
                    statusLabel.setText("Открыт файл: " + f.getAbsolutePath());
                } catch (Exception ex) {
                    showError("Не удалось открыть файл:\n" + ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    /** Открывает диалог «Перейти к строке…». */
    private void goToLine() {
        EditorPanel ep = getCurrentEditor();
        if (ep != null) new GoToLineDialog(this, ep).setVisible(true);
    }

    // --------------------------------------------------------------
    //  Точка входа
    // --------------------------------------------------------------
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
