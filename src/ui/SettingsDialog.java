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

import extra.SettingsManager;
import main.Main;

import javax.swing.*;
import java.awt.*;

/**
 * Диалог «Настройки IDE». Позволяет менять шрифт и цвета
 * редактора и терминала, а также включать/выключать word‑wrap.
 */
public final class SettingsDialog extends JDialog {

    private final JComboBox<String> fontCombo;
    private final JSpinner fontSizeSpinner;
    private final JButton editorBgBtn;
    private final JButton editorFgBtn;
    private final JButton termBgBtn;
    private final JButton termFgBtn;
    private final JCheckBox wordWrapCheck;

    public SettingsDialog(Frame owner) {
        super(owner, "Настройки IDE", true);
        setLayout(new BorderLayout(5, 5));

        // Шрифты
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        fontCombo = new JComboBox<>(fonts);
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(14, 8, 48, 1));

        Font cur = SettingsManager.getEditorFont();
        fontCombo.setSelectedItem(cur.getFamily());
        fontSizeSpinner.setValue(cur.getSize());

        // Цвета
        editorBgBtn = new JButton("Фон редактора");
        editorFgBtn = new JButton("Текст редактора");
        termBgBtn    = new JButton("Фон терминала");
        termFgBtn    = new JButton("Текст терминала");

        editorBgBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Выбор фона редактора",
                    SettingsManager.getEditorBackground());
            if (c != null) SettingsManager.setEditorBackground(c);
        });
        editorFgBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Выбор цвета текста редактора",
                    SettingsManager.getEditorForeground());
            if (c != null) SettingsManager.setEditorForeground(c);
        });
        termBgBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Выбор фона терминала",
                    SettingsManager.getTerminalBackground());
            if (c != null) SettingsManager.setTerminalBackground(c);
        });
        termFgBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Выбор цвета текста терминала",
                    SettingsManager.getTerminalForeground());
            if (c != null) SettingsManager.setTerminalForeground(c);
        });

        // Word‑wrap
        wordWrapCheck = new JCheckBox("Перенос строк в редакторе",
                SettingsManager.isWordWrapEnabled());

        // Формируем панель настроек
        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; center.add(new JLabel("Шрифт:"), gbc);
        gbc.gridx = 1; center.add(fontCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1; center.add(new JLabel("Размер:"), gbc);
        gbc.gridx = 1; center.add(fontSizeSpinner, gbc);
        gbc.gridx = 0; gbc.gridy = 2; center.add(editorBgBtn, gbc);
        gbc.gridx = 1; center.add(editorFgBtn, gbc);
        gbc.gridx = 0; gbc.gridy = 3; center.add(termBgBtn, gbc);
        gbc.gridx = 1; center.add(termFgBtn, gbc);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; center.add(wordWrapCheck, gbc);

        add(center, BorderLayout.CENTER);

        // Кнопки OK / Cancel
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Отмена");
        okBtn.addActionListener(e -> applyAndClose());
        cancelBtn.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(okBtn);
        bottom.add(cancelBtn);
        add(bottom, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void applyAndClose() {
        // Шрифт редактора
        String fName = (String) fontCombo.getSelectedItem();
        int fSize = (Integer) fontSizeSpinner.getValue();
        SettingsManager.setEditorFont(new Font(fName, Font.PLAIN, fSize));

        // Word‑wrap
        SettingsManager.setWordWrapEnabled(wordWrapCheck.isSelected());

        // Применить сразу (главный фрейм знает, как это сделать)
        if (getOwner() instanceof Main) {
            ((Main) getOwner()).applyAllSettings();
        }
        dispose();
    }
}
