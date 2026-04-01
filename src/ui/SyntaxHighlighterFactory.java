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

import javax.swing.text.JTextComponent;

/**
 * Фабрика «приклеивает» нужный подсветчик в зависимости от языка.
 */
public final class SyntaxHighlighterFactory {

    private SyntaxHighlighterFactory() {}

    public static void install(JTextComponent comp, Language language) {
        switch (language) {
            case CPP:    CppSyntaxHighlighter.install(comp);   break;
            case JAVA:   JavaSyntaxHighlighter.install(comp);  break;
            case LUA:    LuaSyntaxHighlighter.install(comp);   break;
            case PYTHON: PythonSyntaxHighlighter.install(comp);break;
            case RMS:    RmsSyntaxHighlighter.install(comp);  break;
            case UNKNOWN: /* без подсветки */                break;
        }
    }
}
