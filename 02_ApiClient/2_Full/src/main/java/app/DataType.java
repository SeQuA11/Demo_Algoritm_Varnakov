package app;

/**
 * Методы API из api_info и формат, которому должно соответствовать значение.
 * Названия методов чувствительны к регистру.
 */
public enum DataType {
    FULL_NAME("ФИО", "fullName", "[\\p{L} \\-]+"),
    SNILS("СНИЛС", "snils", "\\d{3}-\\d{3}-\\d{3} \\d{2}"),
    INN("ИНН", "inn", "\\d{10}|\\d{12}"),
    EMAIL("Email", "email", "[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"),
    IDENTITY_CARD("Карта-пропуск", "identityCard", "\\d{2} \\d{2} \\d{6}");

    final String title;
    final String method;
    final String pattern;

    DataType(String title, String method, String pattern) {
        this.title = title;
        this.method = method;
        this.pattern = pattern;
    }

    public String check(String value) {
        if (value.isBlank()) {
            return "Нет данных: сначала нажмите «Получить данные»";
        }
        if (value.matches(pattern)) {
            return title + ": данные корректны";
        }
        return title + ": содержит запрещенные символы";
    }

    @Override
    public String toString() {
        return title;
    }
}
