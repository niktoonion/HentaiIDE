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

/**
 * Хранилище листа «Недавние файлы».
 * Файл‑контейнер – ~/.hentaiide_recent (по одному пути в строке).
 * Хранится максимум 10 записей.
 */
public final class RecentFilesManager {

    private static final int MAX_SIZE = 10;
    private static final Path STORAGE = Paths.get(
            System.getProperty("user.home"), ".hentaiide_recent");

    private static final Deque<String> recent = new ArrayDeque<>();

    static {
        load();
    }

    private RecentFilesManager() {}

    private static void load() {
        if (!Files.isRegularFile(STORAGE)) return;
        try (BufferedReader br = Files.newBufferedReader(STORAGE)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) recent.addLast(line);
            }
            while (recent.size() > MAX_SIZE) recent.removeFirst();
        } catch (IOException e) {
            System.err.println("RecentFilesManager: ошибка чтения – " + e.getMessage());
        }
    }

    private static void save() {
        try (BufferedWriter bw = Files.newBufferedWriter(
                STORAGE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String p : recent) bw.write(p + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("RecentFilesManager: ошибка записи – " + e.getMessage());
        }
    }

    /** Добавить путь в начало списка (переместить наверх, если уже был). */
    public static synchronized void addFile(String absolutePath) {
        recent.remove(absolutePath);
        recent.addFirst(absolutePath);
        while (recent.size() > MAX_SIZE) recent.removeLast();
        save();
    }

    /** Возвратить копию списка (самый свежий сверху). */
    public static synchronized List<String> getRecentFiles() {
        return new ArrayList<>(recent);
    }
}
