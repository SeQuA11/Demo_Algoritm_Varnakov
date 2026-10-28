package app;

import java.util.regex.Pattern;

/** Правила для имени: нормализация и поиск «лишних» символов. */
public final class NameRules {

    /**
     * Всё, кроме букв, дефиса и пробелов: цифры, знаки препинания, скобки, арифметика.
     * Пробелы не считаются лишними — их убирает normalize.
     */
    private static final Pattern BAD_CHARS = Pattern.compile("[^\\p{L}\\s-]");

    private NameRules() {
    }

    /**
     * Убирает пробелы (в том числе внутри слова) и приводит регистр: ИнноКЕнТиЙ -> Иннокентий.
     * Части двойного имени через дефис тоже начинаются с прописной: анна-мария -> Анна-Мария.
     */
    public static String normalize(String name) {
        String compact = name.replaceAll("\\s+", "").toLowerCase();
        StringBuilder result = new StringBuilder(compact.length());
        boolean startOfWord = true;
        for (char c : compact.toCharArray()) {
            result.append(startOfWord ? Character.toUpperCase(c) : c);
            startOfWord = c == '-';
        }
        return result.toString();
    }

    public static boolean hasBadChars(String name) {
        return BAD_CHARS.matcher(name).find();
    }

    public static String removeBadChars(String name) {
        return BAD_CHARS.matcher(name).replaceAll("").trim();
    }
}
