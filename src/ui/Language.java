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

package ui;

import java.util.HashSet;
import java.util.Set;

/**
 * Язык файла – определяется по расширению.
 */
public enum Language {
    CPP("cpp","c","h","hpp"),
    JAVA("java"),
    LUA("lua"),
    PYTHON("py"),
    RMS("rms"),
    UNKNOWN;                 // без подсветки

    private final Set<String> extensions = new HashSet<>();

    Language(String... exts) {
        for (String e : exts) extensions.add(e.toLowerCase());
    }

    /** Возвращает язык по имени файла (по расширению). */
    public static Language detect(String fileName) {
        if (fileName == null) return UNKNOWN;
        int i = fileName.lastIndexOf('.');
        if (i < 0) return UNKNOWN;
        String ext = fileName.substring(i + 1).toLowerCase();
        for (Language l : values()) {
            if (l != UNKNOWN && l.extensions.contains(ext)) return l;
        }
        return UNKNOWN;
    }
}
