package ui;

import javax.swing.text.JTextComponent;

/**
 * Фабрика, которая «вкалывает» нужный подсветчик в JTextComponent
 * в зависимости от переданного языка.
 */
public final class SyntaxHighlighterFactory {

    private SyntaxHighlighterFactory() {}

    public static void install(JTextComponent component, Language language) {
        switch (language) {
            case CPP:     CppSyntaxHighlighter.install(component);   break;
            case JAVA:    JavaSyntaxHighlighter.install(component);  break;
            case LUA:     LuaSyntaxHighlighter.install(component);   break;
            case PYTHON:  PythonSyntaxHighlighter.install(component);break;
            case RMS:     RmsSyntaxHighlighter.install(component);  break;
            case UNKNOWN: /* ничего не делаем – будет обычный чёрный текст */ break;
        }
    }
}
