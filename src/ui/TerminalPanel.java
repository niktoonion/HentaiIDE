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

import javax.swing.*;
import javax.swing.text.*;

import extra.WorkspaceManager;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Терминал‑эмулятор.
 *
 * • На Windows запускает PowerShell (powershell.exe) без логотипа и профилей.  
 * • На *nix – Bash.  
 * • Ввод и вывод объединены в один {@link JTextArea}.  
 * • После **каждой** команды выводится текущий каталог (prompt).  
 * • История команд доступна клавишами ↑/↓.  
 * • Чтение/запись в UTF‑8 → исчезают «кракозябры».  
 */
public class TerminalPanel extends JPanel {

    /* ---------- UI ---------- */
    private final JTextArea console = new JTextArea();

    /* ---------- Процесс оболочки ---------- */
    private Process shellProcess;
    private BufferedWriter shellWriter;            // запись в stdin процесса
    private File currentDir = new File(WorkspaceManager.getWorkspaceDir().toString());

    /* ---------- История ---------- */
    private final List<String> history = new ArrayList<>();
    private int historyIdx = -1;                  // -1 = «нет выбранной команды»

    /* ---------- Потоки ---------- */
    private final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingPrompt;     // отложенный вывод prompt

    /* ---------- Позиция prompt ---------- */
    private volatile int promptPos = 0;           // индекс в документе сразу после текущего prompt

    /* ---------- Фильтр, запрещающий менять уже выведенный текст ---------- */
    private final PromptFilter filter = new PromptFilter();

    /* ---------- Конструкторы ---------- */
    public TerminalPanel() {
        this(null);
    }

    /** Позволяет задать стартовый каталог (полезно при открытии проекта). */
    public TerminalPanel(File startDir) {
        if (startDir != null && startDir.isDirectory()) currentDir = startDir;
        initGui();
        launchShell();
        showInitialPrompt();
    }

    /* ---------- UI‑инициализация ---------- */
    private void initGui() {
        setLayout(new BorderLayout());

        console.setEditable(true);
        console.setBackground(Color.BLACK);
        console.setForeground(Color.GREEN);
        console.setFont(new Font("Consolas", Font.PLAIN, 12));
        console.setLineWrap(true);
        console.setWrapStyleWord(false);
        ((AbstractDocument) console.getDocument()).setDocumentFilter(filter);
        add(new JScrollPane(console), BorderLayout.CENTER);

        /* ----- клавиатурные события ----- */
        console.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int caret = console.getCaretPosition();

                // не допускаем перемещения курсора в «старый» вывод
                if (caret < promptPos) console.setCaretPosition(console.getDocument().getLength());

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER -> {
                        e.consume();
                        handleEnter();
                    }
                    case KeyEvent.VK_BACK_SPACE -> {
                        if (caret <= promptPos) e.consume();
                    }
                    case KeyEvent.VK_UP -> {
                        e.consume();
                        navigateHistory(-1);
                    }
                    case KeyEvent.VK_DOWN -> {
                        e.consume();
                        navigateHistory(1);
                    }
                }
            }
        });

        /* ----- защита от кликов мышью в «старый» вывод ----- */
        console.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (console.getCaretPosition() < promptPos)
                        console.setCaretPosition(console.getDocument().getLength());
                });
            }
        });
    }

    /* ---------- Запуск оболочки ---------- */
    private List<String> buildCommand() {
        List<String> cmd = new ArrayList<>();
        if (isWindows()) {
            String systemRoot = System.getenv("SystemRoot");
            if (systemRoot == null) systemRoot = "C:\\Windows";
            String psPath = systemRoot + "\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";

            cmd.add(psPath);
            cmd.add("-NoLogo");
            cmd.add("-NoProfile");
            cmd.add("-Command");
            cmd.add("-");                        // читаем команды из stdin
        } else {
            cmd.add("/bin/bash");
            cmd.add("-i");                     // интерактивный режим
        }
        return cmd;
    }

    private void launchShell() {
        try {
            ProcessBuilder pb = new ProcessBuilder(buildCommand());
            pb.directory(currentDir);
            pb.redirectErrorStream(true);       // объединяем stdout и stderr
            shellProcess = pb.start();

            shellWriter = new BufferedWriter(
                    new OutputStreamWriter(shellProcess.getOutputStream(), StandardCharsets.UTF_8));

            // поток чтения вывода процесса
            reader.submit(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(shellProcess.getInputStream(),
                                StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        appendShellOutput(line);
                    }
                } catch (IOException ignored) { }
            });
        } catch (IOException e) {
            showError("Не удалось запустить оболочку:\n" + e.getMessage());
        }
    }

    /* ---------- Первое приглашение ---------- */
    private void showInitialPrompt() {
        SwingUtilities.invokeLater(() -> {
            console.append(getPrompt());
            promptPos = console.getDocument().getLength();
            filter.setPromptPos(promptPos);
            console.setCaretPosition(promptPos);
        });
    }

    /** Формирует строку приглашения, содержащую текущий каталог. */
    private String getPrompt() {
        if (isWindows())
            return "PS " + currentDir.getAbsolutePath() + "> ";
        else
            return currentDir.getAbsolutePath() + "$ ";
    }

    /* ---------- Обработка Enter ---------- */
    private void handleEnter() {
        try {
            Document doc = console.getDocument();
            String full = doc.getText(0, doc.getLength());
            String command = full.substring(promptPos).trim();

            // сохраняем в историю
            if (!command.isEmpty()) {
                history.add(command);
                historyIdx = history.size();     // позиция «после» последней
            }

            console.append("\n");                // перенесём курсор на новую строку
            sendToShell(command);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Отправляем в stdin процесса:
     *   1) команда пользователя
     *   2) специальный запрос, который выводит только путь:
     *      Windows PowerShell →  echo __CWD__:$PWD.Path
     *      Bash                →  echo __CWD__:$(pwd)
     */
    private void sendToShell(String cmd) {
        if (shellWriter == null) {
            showError("Оболочка не запущена.");
            return;
        }
        try {
            shellWriter.write(cmd);
            shellWriter.newLine();

            if (isWindows()) {
                shellWriter.write("echo __CWD__:$PWD.Path");
            } else {
                shellWriter.write("echo __CWD__:$(pwd)");
            }
            shellWriter.newLine();
            shellWriter.flush();
        } catch (IOException e) {
            showError("Не удалось отправить команду:\n" + e.getMessage());
        }
    }

    /* ---------- Вывод из процесса ---------- */
    private void appendShellOutput(String line) {
        SwingUtilities.invokeLater(() -> {
            if (line.startsWith("__CWD__:")) {           // получен путь
                String path = line.substring(8).trim();
                if (!path.isEmpty()) {
                    currentDir = new File(path);
                }
            } else {                                     // обычный вывод
                console.append(line + "\n");
            }
            schedulePrompt();                            // планируем новый prompt
        });
    }

    /** Через ≈150 мс после последнего вывода ставим новый prompt. */
    private void schedulePrompt() {
        if (pendingPrompt != null && !pendingPrompt.isDone())
            pendingPrompt.cancel(false);

        pendingPrompt = scheduler.schedule(() -> SwingUtilities.invokeLater(() -> {
            console.append(getPrompt());
            promptPos = console.getDocument().getLength();
            filter.setPromptPos(promptPos);
            console.setCaretPosition(promptPos);
        }), 150, TimeUnit.MILLISECONDS);
    }

    /* ---------- История (стрелки ↑/↓) ---------- */
    private void navigateHistory(int delta) {
        if (history.isEmpty()) return;

        historyIdx += delta;
        if (historyIdx < 0) historyIdx = 0;
        if (historyIdx >= history.size()) {
            historyIdx = history.size();          // «пустой ввод»
            replaceCurrentInput("");
            return;
        }
        replaceCurrentInput(history.get(historyIdx));
    }

    /** Заменяет всё, что находится после promptPos, на переданный текст. */
    private void replaceCurrentInput(String text) {
        try {
            Document doc = console.getDocument();
            int end = doc.getLength();
            doc.remove(promptPos, end - promptPos);
            doc.insertString(promptPos, text, null);
            console.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /* ---------- Утилиты ---------- */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Ошибка терминала", JOptionPane.ERROR_MESSAGE);
    }

    /** Останавливаем процесс и потоки (вызывается при закрытии IDE). */
    public void shutdown() {
        try {
            if (shellWriter != null) {
                shellWriter.write("exit");
                shellWriter.newLine();
                shellWriter.flush();
            }
        } catch (IOException ignored) { }

        if (shellProcess != null) shellProcess.destroy();

        reader.shutdownNow();
        scheduler.shutdownNow();
    }

    /* ---------- Методы, вызываемые из SettingsDialog ---------- */
    public void setTerminalBackground(Color bg) {
        console.setBackground(bg);
    }

    public void setTerminalForeground(Color fg) {
        console.setForeground(fg);
    }

    public void setTerminalFont(Font f) {
        console.setFont(f);
    }

    /* ---------- DocumentFilter, запрещающий править «старый» вывод ---------- */
    private static class PromptFilter extends DocumentFilter {
        private int promptPos = 0;
        void setPromptPos(int pos) { this.promptPos = pos; }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (offset >= promptPos) super.insertString(fb, offset, string, attr);
            else Toolkit.getDefaultToolkit().beep();
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text,
                            AttributeSet attrs) throws BadLocationException {
            if (offset >= promptPos) super.replace(fb, offset, length, text, attrs);
            else Toolkit.getDefaultToolkit().beep();
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            if (offset >= promptPos) super.remove(fb, offset, length);
            else Toolkit.getDefaultToolkit().beep();
        }
    }
}
