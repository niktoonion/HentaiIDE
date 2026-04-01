
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Небольшой диалог поиска (Ctrl+F).
 * Находит последовательные вхождения, поддерживает регистрозависимый режим.
 */
public class FindDialog extends JDialog {

    private final JTextField tfSearch = new JTextField(20);
    private final JCheckBox cbCase = new JCheckBox("Учитывать регистр");
    private final JButton btnFind = new JButton("Найти следующий");
    private final JTextArea targetArea;

    public FindDialog(Frame owner, JTextArea target) {
        super(owner, "Найти", false);
        this.targetArea = target;
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Текст:"));
        top.add(tfSearch);
        top.add(cbCase);
        top.add(btnFind);
        add(top, BorderLayout.CENTER);

        btnFind.addActionListener(e -> findNext());
        tfSearch.addActionListener(e -> findNext());

        // Кнопка «Закрыть»
        JButton btnClose = new JButton("Закрыть");
        btnClose.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
    }

    private void findNext() {
        String query = tfSearch.getText();
        if (query.isEmpty()) return;

        String text = targetArea.getText();
        int start = targetArea.getCaretPosition();

        if (!cbCase.isSelected()) {
            query = query.toLowerCase();
            text = text.toLowerCase();
        }

        int idx = text.indexOf(query, start);
        if (idx == -1 && start > 0) {        // «завернуть» поиск
            idx = text.indexOf(query, 0);
        }

        if (idx >= 0) {
            targetArea.setCaretPosition(idx);
            targetArea.moveCaretPosition(idx + query.length());
            targetArea.getCaret().setSelectionVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Текст не найден.", "Поиск", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
