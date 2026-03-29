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
