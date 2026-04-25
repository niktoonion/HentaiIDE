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

/**
 * Ключи UIManager и «собственные» (цвета редактора/терминала).
 * Позволяют избавиться от «строк‑магических» и гарантировать отсутствие опечаток.
 */
public enum UIKey {
    // ---------- редактор и терминал ----------
    EDITOR_BACKGROUND   ("Editor.background"),
    EDITOR_FOREGROUND   ("Editor.foreground"),
    TERMINAL_BACKGROUND ("Terminal.background"),
    TERMINAL_FOREGROUND ("Terminal.foreground"),

    // ---------- UI‑компоненты ----------
    PANEL_BACKGROUND    ("Panel.background"),
    LABEL_FOREGROUND    ("Label.foreground"),

    BUTTON_BACKGROUND   ("Button.background"),
    BUTTON_FOREGROUND   ("Button.foreground"),

    MENU_BAR_BACKGROUND ("MenuBar.background"),
    MENU_BAR_FOREGROUND ("MenuBar.foreground"),

    MENU_BACKGROUND     ("Menu.background"),
    MENU_FOREGROUND     ("Menu.foreground"),

    MENU_ITEM_BACKGROUND("MenuItem.background"),
    MENU_ITEM_FOREGROUND("MenuItem.foreground"),

    TOOL_TIP_BACKGROUND ("ToolTip.background"),
    TOOL_TIP_FOREGROUND ("ToolTip.foreground"),

    TOOL_BAR_BACKGROUND ("ToolBar.background"),
    TOOL_BAR_FOREGROUND ("ToolBar.foreground"),

    // ---------- дополнительные ----------
    TABLE_BACKGROUND    ("Table.background"),
    TABLE_FOREGROUND    ("Table.foreground"),

    TABLE_HEADER_BACKGROUND ("TableHeader.background"),
    TABLE_HEADER_FOREGROUND ("TableHeader.foreground"),

    LIST_BACKGROUND     ("List.background"),
    LIST_FOREGROUND     ("List.foreground"),

    TREE_BACKGROUND     ("Tree.background"),
    TREE_FOREGROUND     ("Tree.foreground"),

    TABBED_PANE_BACKGROUND ("TabbedPane.background"),
    TABBED_PANE_FOREGROUND ("TabbedPane.foreground"),

    SPLIT_PANE_BACKGROUND ("SplitPane.background"),

    SCROLL_BAR_BACKGROUND ("ScrollBar.background"),
    SCROLL_BAR_FOREGROUND ("ScrollBar.foreground"),

    POPUP_MENU_BACKGROUND ("PopupMenu.background"),
    POPUP_MENU_FOREGROUND ("PopupMenu.foreground");

    private final String uiKey;
    UIKey(String uiKey) { this.uiKey = uiKey; }
    public String key() { return uiKey; }
}
