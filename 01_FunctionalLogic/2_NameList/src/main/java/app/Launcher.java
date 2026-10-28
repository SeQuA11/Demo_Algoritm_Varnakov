package app;

import javafx.application.Application;

/**
 * Запускать нужно этот класс: если main находится в наследнике Application,
 * без module-path появляется ошибка "JavaFX runtime components are missing".
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(NameListApp.class, args);
    }
}
