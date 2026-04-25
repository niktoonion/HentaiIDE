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

package plugin;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

public final class PluginManager {

    private static final Path PLUGINS_DIR =
            Paths.get(System.getProperty("user.home"), ".hentaiide_plugins");

    /** Список загруженных плагинов */
    private static final List<IDEPlugin> loaded = new ArrayList<>();

    /* ------------ Хранилище отключённых плагинов (по имени JAR‑а) ------------ */
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(PluginManager.class);
    private static final String KEY_DISABLED = "plugins.disabled";
    private static final Set<String> disabled = new HashSet<>();

    static {
        try {
            Files.createDirectories(PLUGINS_DIR);
        } catch (IOException e) {
            System.err.println("PluginManager: cannot create plugins dir – " + e.getMessage());
        }
        loadDisabled();
    }

    /** Сканировать директорию, загрузить все JAR‑ы (кроме отключённых) */
    public static List<IDEPlugin> loadAllPlugins(JFrame owner, JMenuBar menuBar) {
        loaded.clear();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(PLUGINS_DIR, "*.jar")) {
            for (Path jar : ds) {
                if (isJarDisabled(jar)) continue;                     // ← ПРОПУСКАЕМ ОТКЛЮЧЕННЫЕ
                try {
                    URLClassLoader cl = new URLClassLoader(
                            new URL[]{jar.toUri().toURL()}, PluginManager.class.getClassLoader());
                    ServiceLoader<IDEPlugin> sl = ServiceLoader.load(IDEPlugin.class, cl);
                    for (IDEPlugin plugin : sl) {
                        plugin.init(owner, menuBar);
                        loaded.add(plugin);
                    }
                } catch (MalformedURLException e) {
                    System.err.println("PluginManager: bad jar – " + jar);
                }
            }
        } catch (IOException e) {
            System.err.println("PluginManager: error scanning plugins – " + e.getMessage());
        }
        return Collections.unmodifiableList(loaded);
    }

    /* ------------------------ Отключение / включение ------------------------ */
    private static void loadDisabled() {
        String s = PREFS.get(KEY_DISABLED, "");
        if (!s.isEmpty()) {
            disabled.clear();
            disabled.addAll(Arrays.asList(s.split(",")));
        }
    }
    private static void saveDisabled() {
        PREFS.put(KEY_DISABLED, String.join(",", disabled));
    }
    public static boolean isJarDisabled(Path jar) {
        return disabled.contains(jar.getFileName().toString());
    }
    public static void setJarEnabled(Path jar, boolean enabled) {
        String name = jar.getFileName().toString();
        if (enabled) disabled.remove(name);
        else          disabled.add(name);
        saveDisabled();
    }

    /* -------------------- Загрузка одного плагина вручную ------------------- */
    public static void loadPluginManually(JFrame owner, JMenuBar menuBar) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите .jar‑плагин");
        int res = chooser.showOpenDialog(owner);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        Path target = PLUGINS_DIR.resolve(file.getName());
        try {
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(owner,
                    "Не удалось скопировать плагин:\n" + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // После копирования сразу пытаемся загрузить (если он не отключён)
        loadAllPlugins(owner, menuBar);
    }

    /** Возвратить список уже *загруженных* плагинов (для UI‑управления) */
    public static List<IDEPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(loaded);
    }

    /** Возвратить путь к директории с плагинами */
    public static Path getPluginsDir() {
        return PLUGINS_DIR;
    }
    
    public static void deletePlugin(JFrame owner, JMenuBar menuBar, Path jar) {
        // 1️⃣ Сначала отключаем плагин (чтобы он больше не использовался)
        setJarEnabled(jar, false);

        // 2️⃣ Перезагружаем список плагинов – тем самым «отпускаем» старый ClassLoader
        loadAllPlugins(owner, menuBar);

        // 3️⃣ Пытаемся удалить файл
        try {
            Files.deleteIfExists(jar);
            JOptionPane.showMessageDialog(owner,
                    "Плагин удалён.", "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            // На Windows файл может быть заблокирован до сборки GC.
            // Отложим удаление до закрытия IDE.
            JOptionPane.showMessageDialog(owner,
                    "Не удалось удалить файл сразу (возможно, он используется).\n" +
                    "Файл будет удалён при следующем запуске IDE.",
                    "Внимание", JOptionPane.WARNING_MESSAGE);
            try {
                jar.toFile().deleteOnExit();   // удаляем при завершении JVM
            } catch (Exception ignored) {}
        }
    }
}
