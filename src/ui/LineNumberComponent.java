package ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;

/**
 * Отображает номера строк слева от любого {@link JTextComponent}
 * (JTextArea, JTextPane, …). Теперь работает с {@link WrapableTextPane}.
 */
public class LineNumberComponent extends JComponent {

    private static final long serialVersionUID = 1L;
    private static final int MARGIN = 5;
    private final JTextComponent textComp;
    private final Font font = new Font("Consolas", Font.PLAIN, 12);
    private final Color bg = new Color(0x2B2B2B);
    private final Color fg = new Color(0xBBBBBB);

    public LineNumberComponent(JTextComponent comp) {
        this.textComp = comp;

        // Любое изменение текста → перерисовка
        comp.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { repaint(); }
            @Override public void removeUpdate(DocumentEvent e) { repaint(); }
            @Override public void changedUpdate(DocumentEvent e) { repaint(); }
        });
        comp.addCaretListener(e -> repaint());

        setPreferredSize(new Dimension(30, Integer.MAX_VALUE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();

        // Вычисляем количество строк через корневой элемент документа
        int lineCount = 0;
        Document doc = textComp.getDocument();
        if (doc != null) {
            Element root = doc.getDefaultRootElement();
            lineCount = root.getElementCount();
        }

        int y = fm.getAscent() + 2;
        for (int i = 1; i <= lineCount; i++) {
            String num = Integer.toString(i);
            int strWidth = fm.stringWidth(num);
            int x = getWidth() - strWidth - MARGIN;

            g.setColor(fg);
            g.drawString(num, x, y);
            y += lineHeight;
        }
    }
}
