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

package extra;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Менеджер «рабочих пространств».
 *
 * <p>В отличие от прежней версии, теперь можно создавать и переключать
 * произвольные рабочие каталоги. Для каждого каталога хранится собственный
 * файл <code>.workspace</code>, в котором сохраняется список открытых файлов.
 * Текущий активный workspace сохраняется в файле
 * <code>%USERHOME%/.hentaiide_workspace/active_workspace</code>.</p>
 */
public final class WorkspaceManager {

    /** Папка, где находятся все workspace‑директории */
    private static final Path GLOBAL_ROOT =
            Paths.get(System.getProperty("user.home"), ".hentaiide_workspace");

    /** Путь к файлу‑маркеру активного workspace */
    private static final Path ACTIVE_MARKER = GLOBAL_ROOT.resolve("active_workspace");

    /** Папка текущего workspace */
    private static Path currentRoot = GLOBAL_ROOT;            // по‑умолчанию – глобальная

    /** Путь к .workspace внутри текущего каталога */
    private static Path workspaceFile = currentRoot.resolve(".workspace");

    /** Ключ свойства, в котором сохраняется список открытых файлов */
    private static final String KEY_OPEN_FILES = "openFiles";

    /** Список открытых файлов (полные пути) */
    private static final List<String> openFiles = new ArrayList<>();

    static {
        try {
            Files.createDirectories(GLOBAL_ROOT);
        } catch (IOException e) {
            System.err.println("WorkspaceManager: cannot create global root – " + e.getMessage());
        }
        loadActiveWorkspace();          // загрузка последнего workspace
    }

    private WorkspaceManager() { /* utils‑класс */ }

    /* --------------------------------------------------------------
     *  Работа с текущим workspace
     * -------------------------------------------------------------- */
    public static Path getWorkspaceDir() {
        return currentRoot;
    }

    /** Переключить активный workspace на указанный путь (создаёт, если нет). */
    public static void setActiveWorkspace(Path newRoot) {
        if (newRoot == null) throw new IllegalArgumentException("null workspace");
        try {
            Files.createDirectories(newRoot);
        } catch (IOException e) {
            System.err.println("WorkspaceManager: cannot create workspace – " + e.getMessage());
            return;
        }
        currentRoot = newRoot;
        workspaceFile = currentRoot.resolve(".workspace");
        saveActiveMarker();
        load();                         // загрузить список открытых файлов нового workspace
    }

    /** Возвратить список всех известных workspace‑директорий (только имена). */
    public static List<String> listAllWorkspaces() {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(GLOBAL_ROOT)) {
            return StreamSupport.stream(ds.spliterator(), false)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /* --------------------------------------------------------------
     *  Внутренние методы – загрузка/сохранение .workspace
     * -------------------------------------------------------------- */
    private static void load() {
        openFiles.clear();
        if (!Files.isRegularFile(workspaceFile)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(workspaceFile)) {
            p.load(in);
            String v = p.getProperty(KEY_OPEN_FILES);
            if (v != null && !v.isEmpty()) {
                String[] arr = v.split(File.pathSeparator);
                openFiles.addAll(Arrays.asList(arr));
            }
        } catch (IOException e) {
            System.err.println("WorkspaceManager: error loading workspace – " + e.getMessage());
        }
    }

    private static void save() {
        Properties p = new Properties();
        if (!openFiles.isEmpty()) {
            String joined = String.join(File.pathSeparator, openFiles);
            p.setProperty(KEY_OPEN_FILES, joined);
        }
        try (OutputStream out = Files.newOutputStream(workspaceFile,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            p.store(out, "IDE workspace – opened files");
        } catch (IOException e) {
            System.err.println("WorkspaceManager: error saving workspace – " + e.getMessage());
        }
    }

    private static void saveActiveMarker() {
        try {
            Files.write(ACTIVE_MARKER,
                    Collections.singletonList(currentRoot.toAbsolutePath().toString()),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("WorkspaceManager: cannot write active marker – " + e.getMessage());
        }
    }

    private static void loadActiveWorkspace() {
        if (!Files.isRegularFile(ACTIVE_MARKER)) {
            // первый запуск – оставляем /home/.hentaiide_workspace
            return;
        }
        try {
            List<String> lines = Files.readAllLines(ACTIVE_MARKER);
            if (!lines.isEmpty()) {
                Path p = Paths.get(lines.get(0));
                if (Files.isDirectory(p)) {
                    currentRoot = p;
                    workspaceFile = currentRoot.resolve(".workspace");
                }
            }
        } catch (IOException e) {
            System.err.println("WorkspaceManager: cannot read active marker – " + e.getMessage());
        }
    }

    /* --------------------------------------------------------------
     *  Публичные методы работы со списком открытых файлов
     * -------------------------------------------------------------- */
    public static synchronized void addOpenFile(String absolutePath) {
        openFiles.remove(absolutePath);
        openFiles.add(0, absolutePath);
        save();
    }

    public static synchronized void removeOpenFile(String absolutePath) {
        openFiles.remove(absolutePath);
        save();
    }

    public static synchronized List<String> getOpenFiles() {
        return new ArrayList<>(openFiles);
    }
}
