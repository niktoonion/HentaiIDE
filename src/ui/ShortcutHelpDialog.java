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
import javax.swing.table.*;
import java.awt.*;

/**
 * Диалог, показывающий список всех горячих клавиш и их описание.
 * Вызывается из главного окна через пункт меню «Справка → Список горячих клавиш…».
 */
public final class ShortcutHelpDialog extends JDialog {

    public ShortcutHelpDialog(Frame owner) {
        super(owner, "Список горячих клавиш", false);
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        String[] columnNames = {"Действие", "Сочетание", "Описание"};
        Object[][] data = {
                {"Новый файл", "Ctrl+N", "Создать пустую вкладку"},
                {"Открыть файл", "Ctrl+O", "Открыть диалог выбора файла"},
                {"Сохранить", "Ctrl+S", "Сохранить текущий файл"},
                {"Сохранить как…", "Ctrl+Shift+S", "Сохранить под новым именем"},
                {"Закрыть вкладку", "Ctrl+W", "Закрыть текущую вкладку"},
                {"Сохранить всё", "Ctrl+Alt+S", "Сохранить все открытые файлы"},
                {"Выход", "Alt+F4", "Завершить работу программы"},
                {"Отменить", "Ctrl+Z", "Отменить последнее действие"},
                {"Повторить", "Ctrl+Y / Ctrl+Shift+Z", "Повторить отменённое действие"},
                {"Вырезать", "Ctrl+X", "Вырезать выделенный текст"},
                {"Копировать", "Ctrl+C", "Копировать выделенный текст"},
                {"Вставить", "Ctrl+V", "Вставить текст из буфера обмена"},
                {"Найти", "Ctrl+F", "Открыть диалог поиска"},
                {"Найти и заменить", "Ctrl+H", "Открыть диалог поиска/замены"},
                {"Перейти к строке", "Ctrl+G", "Открыть диалог «Перейти к строке…»"},
                {"Комментировать/раскомментировать", "Ctrl+/", "Добавить/убрать //, --, #"},
                {"Автодополнение", "Ctrl+Space", "Вызвать автодополнение кода"},
                {"Переключить перенос строк", "Ctrl+Shift+W", "Вкл/выкл word‑wrap"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);

        // Ширина колонок
        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(180);
        colModel.getColumn(1).setPreferredWidth(120);
        colModel.getColumn(2).setPreferredWidth(300);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        setLayout(new BorderLayout(5,5));
        add(scroll, BorderLayout.CENTER);

        // Кнопка Закрыть
        JButton btnClose = new JButton("Закрыть");
        btnClose.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
    }
}
