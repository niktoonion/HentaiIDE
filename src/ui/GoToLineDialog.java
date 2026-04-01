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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

/**
 * Диалог «Перейти к строке…».
 * Открывается по Ctrl G.
 */
public final class GoToLineDialog extends JDialog {

    private final JTextField tfLine = new JTextField(6);
    private final EditorPanel editor;

    public GoToLineDialog(Frame owner, EditorPanel editor) {
        super(owner, "Перейти к строке", false);
        this.editor = editor;
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Номер строки:"));
        panel.add(tfLine);
        JButton btnGo = new JButton("Перейти");
        panel.add(btnGo);
        add(panel, BorderLayout.CENTER);

        btnGo.addActionListener(e -> {
			try {
				go();
			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
        tfLine.addActionListener(e -> {
			try {
				go();
			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

        JButton btnClose = new JButton("Закрыть");
        btnClose.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);
    }

    private void go() throws BadLocationException {
        String txt = tfLine.getText().trim();
        if (txt.isEmpty()) return;
        try {
            int lineNum = Integer.parseInt(txt);
            if (lineNum < 1) throw new NumberFormatException();

            JTextComponent tc = editor.getTextArea();
            Document doc = tc.getDocument();
            Element root = doc.getDefaultRootElement();
            int total = root.getElementCount();
            if (lineNum > total) lineNum = total;

            Element line = root.getElement(lineNum - 1);
            int pos = line.getStartOffset();
            tc.setCaretPosition(pos);
            tc.requestFocusInWindow();
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Введите корректный номер строки.",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
