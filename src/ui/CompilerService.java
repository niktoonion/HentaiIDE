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

import extra.WorkspaceManager;

import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

public final class CompilerService {

    /** Выполняет компиляцию/запуск текущего активного файла в редакторе. */
    public static void compileAndRun(EditorPanel editor, JFrame owner) {
        File file = editor.getFile();
        if (file == null) {
            JOptionPane.showMessageDialog(owner,
                    "Файл не сохранён. Сначала сохраните.", "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String ext = getExtension(file.getName()).toLowerCase();
        switch (ext) {
            case "cpp":
            case "c":
            case "cc":
            case "cxx":
            case "c++":
                compileCpp(file, owner);
                break;
            case "java":
                compileJava(file, owner);
                break;
            case "py":
            case "pyw":
                runPython(file, owner);
                break;
            default:
                JOptionPane.showMessageDialog(owner,
                        "Не поддерживаемый тип файла: " + ext,
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    private static void compileCpp(File source, JFrame owner) {
        // предполагаем, что в каталоге проекта присутствует CMakeLists.txt
        Path projectDir = source.toPath().getParent();
        while (projectDir != null && !Files.exists(projectDir.resolve("CMakeLists.txt"))) {
            projectDir = projectDir.getParent();
        }
        if (projectDir == null) {
            JOptionPane.showMessageDialog(owner,
                    "CMakeLists.txt не найден. Создайте CMake‑проект.", "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // создаём build‑каталог (если его ещё нет)
        Path buildDir = projectDir.resolve("build");
        try {
            Files.createDirectories(buildDir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner,
                    "Не удалось создать каталог build:\n" + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // cmake
        runCommand(buildDir, List.of("cmake", ".."), owner);
        // make
        runCommand(buildDir, List.of("cmake", "--build", "."), owner);
    }

    private static void compileJava(File source, JFrame owner) {
        // ищем pom.xml или build.gradle
        Path projectDir = source.toPath().getParent();
        while (projectDir != null &&
                !(Files.exists(projectDir.resolve("pom.xml")) ||
                  Files.exists(projectDir.resolve("build.gradle")))) {
            projectDir = projectDir.getParent();
        }

        if (projectDir == null) {
            // fallback – простая компиляция javac
            runCommand(source.getParentFile().toPath(),
                    List.of("javac", source.getName()),
                    owner);
            // автозапуск
            String className = source.getName().replaceAll("\\.java$", "");
            runCommand(source.getParentFile().toPath(),
                    List.of("java", className), owner);
            return;
        }

        if (Files.exists(projectDir.resolve("pom.xml"))) {
            runCommand(projectDir, List.of("mvn", "clean", "compile", "exec:java"), owner);
        } else {
            runCommand(projectDir, List.of("gradle", "run"), owner);
        }
    }

    private static void runPython(File source, JFrame owner) {
        runCommand(source.getParentFile().toPath(),
                List.of("python", source.getName()),
                owner);
    }

    private static void runCommand(Path workDir, List<String> cmd, JFrame owner) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            // вывод в отдельном окне
            JTextArea txt = new JTextArea();
            txt.setEditable(false);
            txt.setBackground(Color.BLACK);
            txt.setForeground(Color.GREEN);
            JScrollPane sp = new JScrollPane(txt);
            JDialog dlg = new JDialog(owner, "Выполнение: " + String.join(" ", cmd), false);
            dlg.add(sp);
            dlg.setSize(600, 400);
            dlg.setLocationRelativeTo(owner);
            dlg.setVisible(true);

            new Thread(() -> {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        final String l = line;
                        SwingUtilities.invokeLater(() -> txt.append(l + "\n"));
                    }
                } catch (IOException ignored) {}
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner,
                    "Ошибка запуска команды:\n" + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return (i >= 0) ? name.substring(i + 1) : "";
    }
}
