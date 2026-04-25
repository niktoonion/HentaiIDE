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

import java.awt.*;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import ui.UIKey;

/**
 * Управление пользовательскими настройками IDE.
 * Включено хранение произвольных UI‑цветов (для темы) через преференсы.
 */
public final class SettingsManager {

    private static final Preferences PREFS = Preferences.userNodeForPackage(SettingsManager.class);

    /* ---------------------  ПРИВЯЗАННЫЕ КОНФИГИ  --------------------- */
    private static final String KEY_FONT_NAME   = "editor.font.name";
    private static final String KEY_FONT_SIZE   = "editor.font.size";
    private static final String KEY_EDITOR_BG   = "editor.bg";
    private static final String KEY_EDITOR_FG   = "editor.fg";
    private static final String KEY_TERM_BG     = "terminal.bg";
    private static final String KEY_TERM_FG     = "terminal.fg";
    private static final String KEY_WORD_WRAP   = "editor.wordwrap";

    /* --------------------  ПРЕФИКС ДЛЯ UI‑КЛЮЧЕЙ  -------------------- */
    private static final String UI_PREFIX = "theme.ui.";   // theme.ui.EDITOR_BACKGROUND, …

    /* ------------------------  ДЕФОЛТЫ  ------------------------ */
    private static final String  DEF_FONT_NAME = "Consolas";
    private static final int     DEF_FONT_SIZE = 14;
    private static final Color   DEF_EDITOR_BG = Color.BLACK;
    private static final Color   DEF_EDITOR_FG = Color.GREEN;
    private static final Color   DEF_TERM_BG   = Color.BLACK;
    private static final Color   DEF_TERM_FG   = Color.GREEN;
    private static final boolean DEF_WORD_WRAP = true;

    private SettingsManager() {}

    /* ---------- Шрифт ---------- */
    public static Font getEditorFont() {
        String name = PREFS.get(KEY_FONT_NAME, DEF_FONT_NAME);
        int size = PREFS.getInt(KEY_FONT_SIZE, DEF_FONT_SIZE);
        return new Font(name, Font.PLAIN, size);
    }

    public static void setEditorFont(Font f) {
        if (f != null) {
            PREFS.put(KEY_FONT_NAME, f.getFamily());
            PREFS.putInt(KEY_FONT_SIZE, f.getSize());
        }
    }

    /* ---------- Цвета редактора ---------- */
    public static Color getEditorBackground() {
        return new Color(PREFS.getInt(KEY_EDITOR_BG, DEF_EDITOR_BG.getRGB()));
    }

    public static void setEditorBackground(Color c) {
        if (c != null) PREFS.putInt(KEY_EDITOR_BG, c.getRGB());
    }

    public static Color getEditorForeground() {
        return new Color(PREFS.getInt(KEY_EDITOR_FG, DEF_EDITOR_FG.getRGB()));
    }

    public static void setEditorForeground(Color c) {
        if (c != null) PREFS.putInt(KEY_EDITOR_FG, c.getRGB());
    }

    /* ---------- Цвета терминала ---------- */
    public static Color getTerminalBackground() {
        return new Color(PREFS.getInt(KEY_TERM_BG, DEF_TERM_BG.getRGB()));
    }

    public static void setTerminalBackground(Color c) {
        if (c != null) PREFS.putInt(KEY_TERM_BG, c.getRGB());
    }

    public static Color getTerminalForeground() {
        return new Color(PREFS.getInt(KEY_TERM_FG, DEF_TERM_FG.getRGB()));
    }

    public static void setTerminalForeground(Color c) {
        if (c != null) PREFS.putInt(KEY_TERM_FG, c.getRGB());
    }

    /* ---------- Word‑wrap ---------- */
    public static boolean isWordWrapEnabled() {
        return PREFS.getBoolean(KEY_WORD_WRAP, DEF_WORD_WRAP);
    }

    public static void setWordWrapEnabled(boolean enabled) {
        PREFS.putBoolean(KEY_WORD_WRAP, enabled);
    }

    /* ---------- UI‑ключи (произвольные) ---------- */
    /** Сохранить цвет для UIKey (или удалить, если color==null). */
    public static void setUIKeyColor(UIKey key, Color color) {
        String fullKey = UI_PREFIX + key.name();
        if (color != null) {
            PREFS.putInt(fullKey, color.getRGB());
        } else {
            PREFS.remove(fullKey);
        }
    }

    /** Прочитать цвет UIKey, либо вернуть fallback. */
    public static Color getUIKeyColor(UIKey key, Color fallback) {
        String fullKey = UI_PREFIX + key.name();
        try {
            // Если в Preferences нет такого ключа, будет возвращено значение‑заглушка.
            int rgb = PREFS.getInt(fullKey, Integer.MIN_VALUE);
            if (rgb != Integer.MIN_VALUE) {
                return new Color(rgb);
            }
        } catch (Exception ignored) { /* preferences могут бросать исключения, игнорируем */ }
        return fallback;
    }


    /** Очистить все сохранённые UI‑цвета (не удаляет редактор/терминал). */
    public static void clearAllUIColors() {
        try {
            for (String k : PREFS.keys()) {
                if (k.startsWith(UI_PREFIX)) {
                    PREFS.remove(k);
                }
            }
        } catch (Exception ignored) {}
    }
}
