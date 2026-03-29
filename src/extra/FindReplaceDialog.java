package extra;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Диалог «Найти / Заменить». Принимает любой {@link JTextComponent},
 * в том числе {@link WrapableTextPane} с подсветкой.
 */
public class FindReplaceDialog extends JDialog {

    private final JTextComponent targetArea;

    private final JTextField tfFind    = new JTextField(20);
    private final JTextField tfReplace = new JTextField(20);
    private final JCheckBox cbCase    = new JCheckBox("Учитывать регистр");

    private final JButton btnFind       = new JButton("Найти следующий");
    private final JButton btnReplace    = new JButton("Заменить");
    private final JButton btnReplaceAll = new JButton("Заменить всё");

    public FindReplaceDialog(Frame owner, JTextComponent target) {
        super(owner, "Найти и заменить", false);
        this.targetArea = target;
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new BorderLayout(5,5));

        // --- поиск ---
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Найти:"));
        top.add(tfFind);
        top.add(cbCase);
        top.add(btnFind);
        add(top, BorderLayout.NORTH);

        // --- замена ---
        JPanel middle = new JPanel(new FlowLayout(FlowLayout.LEFT));
        middle.add(new JLabel("Заменить на:"));
        middle.add(tfReplace);
        middle.add(btnReplace);
        middle.add(btnReplaceAll);
        add(middle, BorderLayout.CENTER);

        // --- кнопка «Закрыть» ---
        JButton btnClose = new JButton("Закрыть");
        btnClose.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnClose);
        add(bottom, BorderLayout.SOUTH);

        // --------------- действия -----------------
        btnFind.addActionListener(e -> findNext());
        tfFind.addActionListener(e -> findNext());

        btnReplace.addActionListener(e -> replaceCurrent());
        btnReplaceAll.addActionListener(e -> replaceAll());
    }

    /** Поиск следующего вхождения. */
    private void findNext() {
        String query = tfFind.getText();
        if (query.isEmpty()) return;

        boolean caseSensitive = cbCase.isSelected();
        String text = targetArea.getText();
        int start = targetArea.getCaretPosition();

        if (!caseSensitive) {
            query = query.toLowerCase();
            text = text.toLowerCase();
        }

        int idx = text.indexOf(query, start);
        if (idx == -1 && start > 0) {
            idx = text.indexOf(query, 0); // «завернуть» поиск
        }

        if (idx >= 0) {
            targetArea.setCaretPosition(idx);
            targetArea.moveCaretPosition(idx + query.length());
        } else {
            JOptionPane.showMessageDialog(this,
                    "Текст не найден.", "Поиск", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Заменить текущее найденное вхождение. */
    private void replaceCurrent() {
        String selected = targetArea.getSelectedText();
        String query = tfFind.getText();
        String repl  = tfReplace.getText();
        boolean cs = cbCase.isSelected();

        if (selected != null) {
            boolean match = cs ? selected.equals(query) : selected.equalsIgnoreCase(query);
            if (match) {
                targetArea.replaceSelection(repl);
                findNext();   // перейти к следующему
                return;
            }
        }
        // если текущий выбор не совпадает – ищем дальше
        findNext();
    }

    /** Заменить **все** вхождения. */
    private void replaceAll() {
        String query = tfFind.getText();
        String repl  = tfReplace.getText();
        if (query.isEmpty()) return;

        boolean cs = cbCase.isSelected();
        String text = targetArea.getText();

        Pattern p = cs
                ? Pattern.compile(Pattern.quote(query))
                : Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        String newText = m.replaceAll(Matcher.quoteReplacement(repl));

        targetArea.setText(newText);
        targetArea.setCaretPosition(0);
        JOptionPane.showMessageDialog(this,
                "Все вхождения заменены.", "Замена", JOptionPane.INFORMATION_MESSAGE);
    }
}
