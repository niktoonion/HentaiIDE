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
import java.awt.*;

/**
 * JTextPane с поддержкой word‑wrap.
 * По‑умолчанию режим включён.
 */
public class WrapableTextPane extends JTextPane {

    private boolean wordWrap = true;   // включено по‑умолчанию

    public WrapableTextPane() {
        super();
        setWordWrap(wordWrap);
    }

    /** Включить/выключить автоматический перенос строк. */
    public void setWordWrap(boolean wrap) {
        this.wordWrap = wrap;
        revalidate();          // заставляем JScrollPane пересчитать ширину
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // При включённом wrap‑е панель растягивается до ширины viewport,
        // иначе появляется горизонтальная полоса прокрутки.
        return wordWrap;
    }
}
