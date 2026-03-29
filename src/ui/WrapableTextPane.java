package ui;

import javax.swing.*;
import java.awt.*;

/**
 * JTextPane с поддержкой word‑wrap.
 * По‑умолчанию режим включён.
 */
public class WrapableTextPane extends JTextPane {

    private boolean wordWrap = true;   // включено по‑умолчанию

    public WrapableTextPane() {
        super();
        setWordWrap(wordWrap);
    }

    /** Включить/выключить автоматический перенос строк. */
    public void setWordWrap(boolean wrap) {
        this.wordWrap = wrap;
        revalidate();          // заставляем JScrollPane пересчитать ширину
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // При включённом wrap‑е панель растягивается до ширины viewport,
        // иначе появляется горизонтальная полоса прокрутки.
        return wordWrap;
    }
}
