package ui;

import javax.swing.text.JTextComponent;

/**
 * Фабрика «приклеивает» нужный подсветчик в зависимости от языка.
 */
public final class SyntaxHighlighterFactory {

    private SyntaxHighlighterFactory() {}

    public static void install(JTextComponent comp, Language language) {
        switch (language) {
            case CPP:    CppSyntaxHighlighter.install(comp);   break;
            case JAVA:   JavaSyntaxHighlighter.install(comp);  break;
            case LUA:    LuaSyntaxHighlighter.install(comp);   break;
            case PYTHON: PythonSyntaxHighlighter.install(comp);break;
            case RMS:    RmsSyntaxHighlighter.install(comp);  break;
            case UNKNOWN: /* без подсветки */                break;
        }
    }
}
