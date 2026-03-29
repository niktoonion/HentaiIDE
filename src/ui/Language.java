package ui;

import java.util.HashSet;
import java.util.Set;

/**
 * Язык файла – определяется по расширению.
 */
public enum Language {
    CPP("cpp","c","h","hpp"),
    JAVA("java"),
    LUA("lua"),
    PYTHON("py"),
    RMS("rms"),
    UNKNOWN;                 // без подсветки

    private final Set<String> extensions = new HashSet<>();

    Language(String... exts) {
        for (String e : exts) extensions.add(e.toLowerCase());
    }

    /** Возвращает язык по имени файла (по расширению). */
    public static Language detect(String fileName) {
        if (fileName == null) return UNKNOWN;
        int i = fileName.lastIndexOf('.');
        if (i < 0) return UNKNOWN;
        String ext = fileName.substring(i + 1).toLowerCase();
        for (Language l : values()) {
            if (l != UNKNOWN && l.extensions.contains(ext)) return l;
        }
        return UNKNOWN;
    }
}
