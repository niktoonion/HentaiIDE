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
