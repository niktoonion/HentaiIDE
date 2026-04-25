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

package main;

import extra.Extra;
import extra.FindReplaceDialog;
import extra.RecentFilesManager;
import extra.SettingsManager;
import extra.WorkspaceManager;
import fileMngr.FileMngr;
import plugin.PluginManager;
import ui.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Главное окно IDE – теперь у терминала есть:
 *   • отдельный заголовок с крестиком (✕) – закрыть конкретный терминал;
 *   • панель‑заголовок над вкладками с кнопками «+» (новый терминал) и
 *     «✕» (закрыть текущий терминал);
 *   • поддержка истории команд и корректный вывод пути после каждой команды.
 */
public class Main extends JFrame {

    /* -------------------- UI‑элементы -------------------- */
    private final JTabbedPane tabbedPane   = new JTabbedPane();
    private final JLabel statusLabel       = new JLabel("Готов");
    private final JMenu recentMenu        = new JMenu("Недавние");
    private JMenuItem undoItem;
    private JMenuItem redoItem;

    private boolean wordWrapEnabled = true;       // включено по умолчанию
    private FileTreePanel fileTreePanel;

    // терминалы → несколько вкладок
    private final JTabbedPane terminalTabs = new JTabbedPane();
    private final Map<TerminalPanel, JButton> terminalCloseMap = new HashMap<>();
    private int terminalCounter = 1;               // счётчик имён терминалов

    private JPanel terminalContainer;              // контейнер {header + terminalTabs}
    private JSplitPane vertSplit;                  // вертикальный split (файлы‑таб‑терминал)
    private int storedDividerLocation = -1;        // где был делитель до скрытия терминала

    /* ------------------- вспомогательные структуры ------------------- */
    private final Map<EditorPanel, JButton> closeButtonMap = new HashMap<>();
    private final Set<EditorPanel> pinnedTabs          = new HashSet<>();
    private int dragTabIndex = -1;                 // индекс таба, который тянем

    /* -------------------- Конструктор -------------------- */
    public Main() {
        setTitle("HentaiIDE");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(Extra.sc_getWidth() * 3 / 4, Extra.sc_getHeight() * 3 / 4);
        setLocationRelativeTo(null);

        URL iconURL = getClass().getResource("icon.jpg");
        if (iconURL != null) setIconImage(Toolkit.getDefaultToolkit().getImage(iconURL));
        else System.err.println("Иконка /icon.jpg не найдена в classpath!");

        initUI();
        initListeners();

        // Применяем сохранённые пользователем настройки (цвета, шрифты, word‑wrap)
        applyAllSettings();

        // Загружаем плагины
        PluginManager.loadAllPlugins(this, getJMenuBar());
    }

    /* --------------------------------------------------------------
     *  UI – меню, панели, раскладка
     * -------------------------------------------------------------- */
    private void initUI() {
        JMenuBar menuBar = new JMenuBar();

        /* ====================  Файл  ==================== */
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newItem        = new JMenuItem("Новый");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,
                InputEvent.CTRL_DOWN_MASK));

        JMenuItem createProjItem = new JMenuItem("Создать проект");
        createProjItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));

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
        fileMenu.add(createProjItem);
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

        /* ====================  Правка  ==================== */
        JMenu editMenu = new JMenu("Правка");
        editMenu.setMnemonic(KeyEvent.VK_E);

        undoItem = new JMenuItem("Отменить");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                InputEvent.CTRL_DOWN_MASK));
        redoItem = new JMenuItem("Повторить");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,
                InputEvent.CTRL_DOWN_MASK));

        JMenuItem cutItem   = new JMenuItem("Вырезать");
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem copyItem  = new JMenuItem("Копировать");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem pasteItem = new JMenuItem("Вставить");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                InputEvent.CTRL_DOWN_MASK));
        JMenuItem findItem  = new JMenuItem("Найти…");
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

        /* ====================  Вид  ==================== */
        JMenu viewMenu = new JMenu("Вид");
        JCheckBoxMenuItem wrapItem = new JCheckBoxMenuItem("Перенос строк");
        wrapItem.setSelected(wordWrapEnabled);
        viewMenu.add(wrapItem);

        JMenuItem goToLineItem = new JMenuItem("Перейти к строке…");
        goToLineItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,
                InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(goToLineItem);
        goToLineItem.addActionListener(e -> goToLine());

        // Показ/скрытие терминала (через кнопку‑крестик в заголовке)
        JMenuItem terminalToggleItem = new JMenuItem("Показать/скрыть терминал");
        terminalToggleItem.addActionListener(e -> toggleTerminalVisibility());
        viewMenu.addSeparator();
        viewMenu.add(terminalToggleItem);
        menuBar.add(viewMenu);

        /* ====================  Run  ==================== */
        JMenu runMenu = new JMenu("Run");
        JMenuItem compileRunItem = new JMenuItem("Запустить / Скомпилировать");
        compileRunItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        compileRunItem.addActionListener(e -> runCurrentEditor());
        runMenu.add(compileRunItem);
        menuBar.add(runMenu);

        /* ====================  Плагины  ==================== */
        JMenu pluginsMenu = new JMenu("Плагины");
        JMenuItem loadPluginItem = new JMenuItem("Загрузить плагин…");
        loadPluginItem.addActionListener(e ->
                PluginManager.loadPluginManually(this, getJMenuBar()));
        pluginsMenu.add(loadPluginItem);
        pluginsMenu.addSeparator();

        /* <-- Новая строка: открываем диалог управления плагинами */
        JMenuItem managePluginsItem = new JMenuItem("Управление плагинами…");
        managePluginsItem.addActionListener(e ->
                new PluginManagerDialog(this, getJMenuBar()).setVisible(true));
        pluginsMenu.add(managePluginsItem);
        pluginsMenu.addSeparator();

        JMenuItem reloadAllItem = new JMenuItem("Перезагрузить все");
        reloadAllItem.addActionListener(e ->
                PluginManager.loadAllPlugins(this, getJMenuBar()));
        pluginsMenu.add(reloadAllItem);
        menuBar.add(pluginsMenu);


        /* ====================  Справка  ==================== */
        JMenu helpMenu = new JMenu("Справка");
        JMenuItem shortcutsItem = new JMenuItem("Список горячих клавиш…");
        shortcutsItem.addActionListener(e ->
                new ShortcutHelpDialog(this).setVisible(true));
        helpMenu.add(shortcutsItem);
        helpMenu.addSeparator();
        JMenuItem aboutItem = new JMenuItem("О программе");
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        /* ====================  Настройки  ==================== */
        JMenu settingsMenu = new JMenu("Настройки");
        JMenuItem settingsItem = new JMenuItem("Настройки IDE…");
        settingsItem.addActionListener(e -> new SettingsDialog(this).setVisible(true));
        settingsMenu.add(settingsItem);
        menuBar.add(settingsMenu);

        setJMenuBar(menuBar);

        /* ---------- Статус‑бар ---------- */
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(Color.BLACK);
        statusLabel.setForeground(Color.YELLOW);
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        /* ---------- Дерево‑файлы и табы ---------- */
        Consumer<File> openFromTree = this::openFileFromTree;
        fileTreePanel = new FileTreePanel(openFromTree);

        JSplitPane horizSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, fileTreePanel, tabbedPane);
        horizSplit.setDividerLocation(200);
        horizSplit.setOneTouchExpandable(true);

        /* ---------- Терминалы ---------- */
        // первый терминал (по умолчанию)
        TerminalPanel first = new TerminalPanel();
        terminalTabs.addTab("Terminal 1", first);
        terminalTabs.setTabComponentAt(0, createTerminalHeader("Terminal 1", first));

        // панель‑заголовок над вкладками терминалов (кнопки + и ✕)
        JPanel termHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        JButton btnAdd = new JButton("\u002B");    // плюс
        btnAdd.setToolTipText("Новый терминал");
        btnAdd.setMargin(new Insets(2, 5, 2, 5));
        btnAdd.addActionListener(e -> addNewTerminalTab());
        JButton btnClose = new JButton("\u2715");   // крестик
        btnClose.setToolTipText("Закрыть текущий терминал");
        btnClose.setMargin(new Insets(2, 5, 2, 5));
        btnClose.addActionListener(e -> closeCurrentTerminal());
        termHeader.add(btnAdd);
        termHeader.add(btnClose);

        terminalContainer = new JPanel(new BorderLayout());
        terminalContainer.add(termHeader, BorderLayout.NORTH);
        terminalContainer.add(terminalTabs, BorderLayout.CENTER);

        /* ---------- Вертикальный split (файлы‑таб‑терминал) ---------- */
        vertSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, horizSplit, terminalContainer);
        vertSplit.setDividerLocation(400);
        vertSplit.setOneTouchExpandable(true);
        vertSplit.setResizeWeight(0.7);
        add(vertSplit, BorderLayout.CENTER);

        /* ---------- Связывание действий ---------- */
        // ---- Файл ----
        newItem.addActionListener(e -> createNewTab());
        createProjItem.addActionListener(e -> ProjectManager.createProjectDialog(this));
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile(false));
        saveAsItem.addActionListener(e -> saveFile(true));
        closeTabItem.addActionListener(e -> closeCurrentTab());
        saveAllItem.addActionListener(e -> saveAllFiles());
        exitItem.addActionListener(e -> exitApplication());

        // ---- Правка ----
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

        // ---- Вид ----
        wrapItem.addActionListener(e -> {
            wordWrapEnabled = wrapItem.isSelected();
            setWordWrapForAll(wordWrapEnabled);
            SettingsManager.setWordWrapEnabled(wordWrapEnabled);
        });

        // ---- Справка ----
        aboutItem.addActionListener(e -> showAbout());

        // ---------- Меню «Недавние» ----------
        updateRecentFilesMenu();

        // ---------- Восстановление открытых файлов ----------
        List<String> previous = WorkspaceManager.getOpenFiles();
        for (String p : previous) {
            File f = new File(p);
            if (f.isFile()) openFileFromTree(f);
        }

        /* ---------- Перетаскивание табов редактора ---------- */
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e))
                    dragTabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (dragTabIndex != -1 && SwingUtilities.isLeftMouseButton(e)) {
                    int target = tabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (target != -1 && target != dragTabIndex) {
                        Component comp      = tabbedPane.getComponentAt(dragTabIndex);
                        String   title      = tabbedPane.getTitleAt(dragTabIndex);
                        Icon     icon       = tabbedPane.getIconAt(dragTabIndex);
                        Component tabHeader = tabbedPane.getTabComponentAt(dragTabIndex);

                        tabbedPane.remove(dragTabIndex);
                        tabbedPane.insertTab(title, icon, comp, null, target);
                        tabbedPane.setTabComponentAt(target, tabHeader);
                        tabbedPane.setSelectedIndex(target);
                    }
                    dragTabIndex = -1;
                }
            }
        });

        /* ---------- Правый клик по табу редактора – Pin/Unpin ---------- */
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = tabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (idx != -1) {
                        Component c = tabbedPane.getComponentAt(idx);
                        if (c instanceof EditorPanel)
                            showTabContextMenu(e.getX(), e.getY(), (EditorPanel) c);
                    }
                }
            }
        });

        /* ---------- Слушатель смены workspace (из FileTreePanel) ---------- */
        this.addPropertyChangeListener("workspaceChanged", evt -> fileTreePanel.refreshRoot());
    }

    /* --------------------------------------------------------------
     *  Слушатели окна
     * -------------------------------------------------------------- */
    private void initListeners() {
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exitApplication(); }
        });

        tabbedPane.addChangeListener(e -> {
            updateCaretStatus();
            updateUndoRedoMenuItems();
        });
    }

    /* --------------------------------------------------------------
     *  Основные действия
     * -------------------------------------------------------------- */
    private void createNewTab() {
        EditorPanel ep = new EditorPanel();
        ep.setWordWrap(wordWrapEnabled);
        attachEditorListeners(ep);
        addTab(ep, "Без названия");
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
                    attachEditorListeners(ep);
                    addTab(ep, file.getName());

                    RecentFilesManager.addFile(file.getAbsolutePath());
                    WorkspaceManager.addOpenFile(file.getAbsolutePath());
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
            WorkspaceManager.addOpenFile(file.getAbsolutePath());
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
            if (ep.getFile() != null)
                WorkspaceManager.removeOpenFile(ep.getFile().getAbsolutePath());
            statusLabel.setText("Вкладка закрыта");
        }
    }

    private void exitApplication() {
        if (!confirmAllClosed()) return;
        // останавливаем все терминалы
        for (int i = 0; i < terminalTabs.getTabCount(); i++) {
            Component c = terminalTabs.getComponentAt(i);
            if (c instanceof TerminalPanel) ((TerminalPanel) c).shutdown();
        }
        // Сохраняем пустой .workspace (для корректного закрытия)
        WorkspaceManager.addOpenFile("");
        System.exit(0);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "HentaiIDE\nВерсия 0.0.1\nАвтор: niktoonion\n\nПростой редактор кода.",
                "О программе", JOptionPane.INFORMATION_MESSAGE);
    }

    /* --------------------------------------------------------------
     *  Вспомогательные методы
     * -------------------------------------------------------------- */
    public EditorPanel getCurrentEditor() {
        Component c = tabbedPane.getSelectedComponent();
        return (c instanceof EditorPanel) ? (EditorPanel) c : null;
    }

    private JTextComponent getCurrentTextArea() {
        EditorPanel ep = getCurrentEditor();
        return (ep != null) ? ep.getTextArea() : null;
    }

    private void attachEditorListeners(EditorPanel ep) {
        // заголовок и индикатор изменения
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

        // статус‑бар
        ep.getTextArea().addCaretListener(e -> updateCaretStatus());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirmAllClosed() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component c = tabbedPane.getComponentAt(i);
            /* !!! Было:   EditorPanel ep = (EditorPanel) tabbedPane.getComponentAt(i);
             * Теперь проверяем тип, чтобы не падать на ThemePanel, TerminalPanel и т.п.
             */
            if (c instanceof EditorPanel) {
                EditorPanel ep = (EditorPanel) c;
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
        }
        return true;
    }

    /** Обновление статус‑бара (строка/столбец). */
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

    /** Обновление пунктов меню Undo/Redo. */
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

    /** Обновление меню «Недавние». */
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
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws IOException {
                return FileMngr.readFile(f);
            }
            @Override protected void done() {
                try {
                    String content = get();
                    EditorPanel ep = new EditorPanel(f, content);
                    ep.setWordWrap(wordWrapEnabled);
                    attachEditorListeners(ep);
                    addTab(ep, f.getName());

                    RecentFilesManager.addFile(f.getAbsolutePath());
                    WorkspaceManager.addOpenFile(f.getAbsolutePath());
                    updateRecentFilesMenu();
                    updateUndoRedoMenuItems();
                    statusLabel.setText("Открыт файл: " + f.getAbsolutePath());
                } catch (Exception ex) {
                    showError("Не удалось открыть файл:\n" + ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    public void refreshFileTree() {
        if (fileTreePanel != null) fileTreePanel.refreshRoot();
    }

    private void goToLine() {
        EditorPanel ep = getCurrentEditor();
        if (ep != null) new GoToLineDialog(this, ep).setVisible(true);
    }

    /* ---------------------- Таб‑менеджер редактора ---------------------- */
    private void addTab(EditorPanel ep, String title) {
        tabbedPane.addTab(title, ep);
        int idx = tabbedPane.indexOfComponent(ep);
        tabbedPane.setTabComponentAt(idx, createEditorHeader(title, ep));
        tabbedPane.setSelectedComponent(ep);
    }

    private JPanel createEditorHeader(String title, EditorPanel ep) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        JButton btn = new JButton("\u2715");               // ✕
        btn.setMargin(new Insets(0, 5, 0, 5));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            tabbedPane.setSelectedComponent(ep);
            closeCurrentTab();
        });
        panel.add(lbl);
        panel.add(btn);
        closeButtonMap.put(ep, btn);
        ep.addPropertyChangeListener(evt -> {
            if ("title".equals(evt.getPropertyName())) lbl.setText((String) evt.getNewValue());
            else if ("modified".equals(evt.getPropertyName())) {
                String cur = lbl.getText();
                boolean mod = (boolean) evt.getNewValue();
                if (mod && !cur.startsWith("*")) lbl.setText("*" + cur);
                if (!mod && cur.startsWith("*")) lbl.setText(cur.substring(1));
            }
        });
        btn.setEnabled(!pinnedTabs.contains(ep));
        return panel;
    }

    private void showTabContextMenu(int x, int y, EditorPanel ep) {
        JPopupMenu menu = new JPopupMenu();
        boolean isPinned = pinnedTabs.contains(ep);
        JMenuItem pinItem = new JMenuItem(isPinned ? "Открепить" : "Закрепить");
        pinItem.addActionListener(e -> togglePin(ep));
        menu.add(pinItem);
        menu.show(tabbedPane, x, y);
    }

    private void togglePin(EditorPanel ep) {
        if (pinnedTabs.contains(ep)) pinnedTabs.remove(ep);
        else pinnedTabs.add(ep);
        JButton btn = closeButtonMap.get(ep);
        if (btn != null) btn.setEnabled(!pinnedTabs.contains(ep));
    }

    /* ---------------------- Терминалы ---------------------- */

    /** Показать/скрыть панель терминалов. */
    private void toggleTerminalVisibility() {
        boolean visible = terminalContainer.isVisible();
        if (visible) {
            storedDividerLocation = vertSplit.getDividerLocation();
            terminalContainer.setVisible(false);
            vertSplit.setDividerLocation(vertSplit.getHeight());
        } else {
            terminalContainer.setVisible(true);
            if (storedDividerLocation > 0) vertSplit.setDividerLocation(storedDividerLocation);
            else vertSplit.setDividerLocation((int) (vertSplit.getHeight() * 0.7));
        }
        vertSplit.revalidate();
        vertSplit.repaint();
    }

    /** Добавить новую вкладку терминала (вызывается кнопкой «+»). */
    private void addNewTerminalTab() {
        TerminalPanel tp = new TerminalPanel(new File(WorkspaceManager.getWorkspaceDir().toString()));
        String title = "Terminal " + ++terminalCounter;
        terminalTabs.addTab(title, tp);
        int idx = terminalTabs.indexOfComponent(tp);
        terminalTabs.setTabComponentAt(idx, createTerminalHeader(title, tp));
        terminalTabs.setSelectedComponent(tp);
    }

    /** Закрыть текущую (выбранную) вкладку терминала (кнопка «✕»). */
    private void closeCurrentTerminal() {
        int idx = terminalTabs.getSelectedIndex();
        if (idx != -1) {
            Component c = terminalTabs.getComponentAt(idx);
            if (c instanceof TerminalPanel) ((TerminalPanel) c).shutdown();
            terminalTabs.removeTabAt(idx);
        }
    }

    /** Закрыть конкретный терминал, когда нажимаем крестик в его заголовке. */
    private void closeSpecificTerminal(TerminalPanel tp) {
        int idx = terminalTabs.indexOfComponent(tp);
        if (idx != -1) {
            tp.shutdown();
            terminalTabs.removeTabAt(idx);
        }
    }

    /** Создаём заголовок вкладки терминала – название + крестик. */
    private JPanel createTerminalHeader(String title, TerminalPanel tp) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(title);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        JButton btn = new JButton("\u2715");               // ✕
        btn.setMargin(new Insets(0, 5, 0, 5));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> closeSpecificTerminal(tp));
        panel.add(lbl);
        panel.add(btn);
        terminalCloseMap.put(tp, btn);
        return panel;
    }

    /** Применить все пользовательские настройки (цвета, шрифт, word‑wrap). */
    public void applyAllSettings() {
        // ---------- РЕДАКТОР ----------
        Font editorFont = SettingsManager.getEditorFont();
        Color editorBg = SettingsManager.getEditorBackground();
        Color editorFg = SettingsManager.getEditorForeground();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component c = tabbedPane.getComponentAt(i);
            if (c instanceof EditorPanel) {
                EditorPanel ep = (EditorPanel) c;
                ep.getTextArea().setFont(editorFont);
                ep.getTextArea().setBackground(editorBg);
                ep.getTextArea().setForeground(editorFg);
            }
        }
        // word‑wrap
        boolean wrap = SettingsManager.isWordWrapEnabled();
        wordWrapEnabled = wrap;
        setWordWrapForAll(wrap);

        // ---------- ТЕРМИНАЛ ----------
        Color termBg = SettingsManager.getTerminalBackground();
        Color termFg = SettingsManager.getTerminalForeground();

        for (int i = 0; i < terminalTabs.getTabCount(); i++) {
            Component c = terminalTabs.getComponentAt(i);
            if (c instanceof TerminalPanel) {
                ((TerminalPanel) c).setTerminalBackground(termBg);
                ((TerminalPanel) c).setTerminalForeground(termFg);
            }
        }
    }

    /* ---------------------- Run (компиляция) ---------------------- */
    private void runCurrentEditor() {
        EditorPanel ep = getCurrentEditor();
        if (ep == null) {
            JOptionPane.showMessageDialog(this,
                    "Нет активного редактора.", "Run", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CompilerService.compileAndRun(ep, this);
    }

    /* ---------------------- Создание проекта ---------------------- */
    private void createProject() {
        ProjectManager.createProjectDialog(this);
    }

    /* ---------------------- Открытие из дерева ---------------------- */
    private void openFileFromTree(File file) {
        if (file == null || !file.isFile()) {
            showError("Выбранный объект не является файлом.");
            return;
        }

        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws IOException {
                return FileMngr.readFile(file);
            }
            @Override protected void done() {
                try {
                    String content = get();
                    EditorPanel ep = new EditorPanel(file, content);
                    ep.setWordWrap(wordWrapEnabled);
                    attachEditorListeners(ep);
                    addTab(ep, file.getName());

                    RecentFilesManager.addFile(file.getAbsolutePath());
                    WorkspaceManager.addOpenFile(file.getAbsolutePath());
                    updateRecentFilesMenu();
                    updateUndoRedoMenuItems();
                    statusLabel.setText("Открыт файл: " + file.getAbsolutePath());
                } catch (Exception ex) {
                    showError("Не удалось открыть файл:\n" + ex.getCause().getMessage());
                }
            }
        }.execute();
    }

    /* --------------------------------------------------------------
     *  Точка входа
     * -------------------------------------------------------------- */
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
