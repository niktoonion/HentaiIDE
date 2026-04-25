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
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import main.Main;

public final class ProjectManager {

    /** Типы поддерживаемых проектов */
    public enum ProjectType {
        CPP_CMAKE,    // C++ + CMake
        JAVA_MAVEN,   // Java + Maven
        JAVA_GRADLE, // Java + Gradle
        PYTHON_VENV   // Python + virtualenv
    }

    /** Диалог выбора типа проекта и его создания */
    public static void createProjectDialog(JFrame owner) {
        ProjectType[] values = ProjectType.values();
        ProjectType choice = (ProjectType) JOptionPane.showInputDialog(
                owner,
                "Выберите тип проекта:",
                "Создание проекта",
                JOptionPane.PLAIN_MESSAGE,
                null,
                values,
                values[0]);

        if (choice == null) return; // отмена

        String name = JOptionPane.showInputDialog(owner,
                "Имя проекта (будет создана папка):",
                "Имя проекта",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        Path root = WorkspaceManager.getWorkspaceDir().resolve(name);
        try {
            Files.createDirectories(root);
            // создаём структуру проекта в зависимости от типа
            switch (choice) {
                case CPP_CMAKE -> initCppCmake(root, name);
                case JAVA_MAVEN -> initJavaMaven(root, name);
                case JAVA_GRADLE -> initJavaGradle(root, name);
                case PYTHON_VENV -> initPythonVenv(root, name);
            }

            JOptionPane.showMessageDialog(owner,
                    "Проект создан:\n" + root,
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
            // после создания обновляем дерево
            fireRefreshTree(owner);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner,
                    "Не удалось создать проект:\n" + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ------------------- реализация шаблонов ------------------- */
    private static void initCppCmake(Path root, String name) throws IOException {
        Path src = Files.createDirectories(root.resolve("src"));
        Files.writeString(src.resolve("main.cpp"),
                "#include <iostream>\n\nint main(){\n    std::cout << \"Hello, " + name + "!\" << std::endl;\n    return 0;\n}\n");
        // CMakeLists.txt
        Files.writeString(root.resolve("CMakeLists.txt"),
                "cmake_minimum_required(VERSION 3.10)\n" +
                "project(" + name + " LANGUAGES CXX)\n" +
                "set(CMAKE_CXX_STANDARD 17)\n" +
                "add_executable(" + name + " src/main.cpp)\n");
    }

    private static void initJavaMaven(Path root, String name) throws IOException {
        Path src = Files.createDirectories(root.resolve("src/main/java"));
        Path pkg = src.resolve(name.toLowerCase());
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve("Main.java"),
                "package " + name.toLowerCase() + ";\n\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, " + name + "!\");\n    }\n}\n");
        // pom.xml
        Files.writeString(root.resolve("pom.xml"),
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 " +
                "http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.example</groupId>\n" +
                "  <artifactId>" + name.toLowerCase() + "</artifactId>\n" +
                "  <version>1.0-SNAPSHOT</version>\n" +
                "  <properties>\n" +
                "    <maven.compiler.source>11</maven.compiler.source>\n" +
                "    <maven.compiler.target>11</maven.compiler.target>\n" +
                "  </properties>\n" +
                "</project>\n");
    }

    private static void initJavaGradle(Path root, String name) throws IOException {
        Path src = Files.createDirectories(root.resolve("src/main/java"));
        Path pkg = src.resolve(name.toLowerCase());
        Files.createDirectories(pkg);
        Files.writeString(pkg.resolve("Main.java"),
                "package " + name.toLowerCase() + ";\n\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, " + name + "!\");\n    }\n}\n");
        // build.gradle
        Files.writeString(root.resolve("build.gradle"),
                "plugins {\n    id 'java'\n}\n\nrepositories {\n    mavenCentral()\n}\n\ndependencies {}\n");
    }

    private static void initPythonVenv(Path root, String name) throws IOException {
        Files.createDirectories(root.resolve(name));
        // simple hello.py
        Files.writeString(root.resolve("hello.py"),
                "print('Hello, " + name + "!')\n");
        // создаём виртуальное окружение (если python доступен)
        ProcessBuilder pb = new ProcessBuilder("python", "-m", "venv", "venv");
        pb.directory(root.toFile());
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (InterruptedException ignored) {}
    }

    /** Уведомить главное окно о необходимости обновить дерево. */
    private static void fireRefreshTree(JFrame owner) {
        // Предположим, что Main подписан на событие через PropertyChange
        // (можно также хранить статический референс к FileTreePanel, но это менее гибко)
    	if (owner instanceof Main) {
            ((Main) owner).refreshFileTree();
        }
    }
}
