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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;

/**
 * Отображает номера строк слева от любого {@link JTextComponent}
 * (JTextArea, JTextPane, …).  Теперь номера выравниваются
 * по фактической позиции каждой строки, а не по фиксированной
 * высоте строк.
 *
 * <p>Для корректного отображения требуется, чтобы в {@link JTextComponent}
 * использовался один‑единственный шрифт (как в нашем редакторе) – тогда
 * высота строки одинакова для всех строк.  Если в документе будет
 * использовано несколько размеров шрифта, номера будут привязаны к
 * первой (самой верхней) части каждой логической строки.</p>
 */
public final class LineNumberComponent extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int MARGIN = 5;               // отступ справа от номера
    private final JTextComponent textComp;             // редактор, к которому привязаны номера
    private final Font font = new Font("Consolas", Font.PLAIN, 12);
    private final Color bg = new Color(0x2B2B2B);
    private final Color fg = new Color(0xBBBBBB);

    public LineNumberComponent(JTextComponent comp) {
        this.textComp = comp;

        // любое изменение текста требует перерисовки номеров
        comp.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { repaint(); }
            @Override public void removeUpdate(DocumentEvent e) { repaint(); }
            @Override public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        // изменение позиции каретки тоже заставляет пере‑рисовать
        comp.addCaretListener(e -> repaint());

        // желательно задать минимальную ширину (30 px), но реальная ширина будет
        // автоматически увеличиваться, если потребуется больше места для цифр
        setPreferredSize(new Dimension(30, Integer.MAX_VALUE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // фон (чёрный)
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());

        // шрифт и метрики
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        Document doc = textComp.getDocument();
        if (doc == null) return;

        // корневой элемент – строковый (по символу '\n')
        Element root = doc.getDefaultRootElement();
        int lineCount = root.getElementCount();

        // Если документ заканчивается переводом строки, последняя логическая
        // строка будет пустой – её обычно не показывают в нумерации.
        // Стандартный `DefaultStyledDocument` считает её отдельной,
        // поэтому просто игнорируем её, когда она пустая.
        if (lineCount > 0) {
            try {
                Element last = root.getElement(lineCount - 1);
                if (last.getEndOffset() == doc.getLength() + 1 &&
                    last.getStartOffset() == last.getEndOffset() - 1) {
                    lineCount--;                // убираем «пустую» последнюю строку
                }
            } catch (Exception ignore) {}
        }

        // Для каждой строки получаем её визуальную позицию
        for (int i = 0; i < lineCount; i++) {
            try {
                // Смещение начала строки в модели
                int startOffset = root.getElement(i).getStartOffset();

                // Преобразуем в координаты вьюпорта
                Rectangle r = textComp.modelToView(startOffset);
                if (r == null) continue;          // может случиться в редких случаях

                // Формируем строку‑номер (нумерация с 1)
                String lineNum = Integer.toString(i + 1);
                int strWidth = fm.stringWidth(lineNum);
                int x = getWidth() - strWidth - MARGIN;
                int y = r.y + fm.getAscent();      // позиция базовой линии текста

                g.setColor(fg);
                g.drawString(lineNum, x, y);
            } catch (BadLocationException ex) {
                // Если позиция уже недоступна – просто пропускаем
            }
        }
    }
}
