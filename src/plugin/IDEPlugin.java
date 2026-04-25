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

import java.util.*;
import javax.swing.*;

/**
 * Базовый интерфейс любого плагина IDE.
 *
 * Плагин получает ссылки на главную форму и на меню.
 * Он может добавить свои пункты в меню, панели, регистрировать
 * новые шорт‑каты и т.д.
 */
public interface IDEPlugin {

    /** Вызывается один раз после загрузки плагина. */
    void init(JFrame owner, JMenuBar menuBar);

    /** Возвращает имя плагина (для отображения в диалоге). */
    String getName();

    /** Краткое описание (показывается в UI). */
    String getDescription();
}
