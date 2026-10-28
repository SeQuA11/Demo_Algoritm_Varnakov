package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Deque;

public class NameListApp extends Application {

    private static final KeyCombination UNDO = new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN);

    /** Одно изменение списка, которое можно отменить: добавление или удаление имени на позиции index. */
    private record Change(String name, int index, boolean added) {
    }

    private final ListView<String> namesList = new ListView<>();
    private final TextField nameField = new TextField();
    private final Deque<Change> history = new ArrayDeque<>();

    @Override
    public void start(Stage stage) {
        namesList.getItems().addAll("Алексей", "Андрей", "Варвара");
        namesList.setStyle("-fx-selection-bar: #ffcc80; -fx-selection-bar-non-focused: #ffe0b2;");
        namesList.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                deleteSelected();
            }
        });

        nameField.setPromptText("например, Марфа");
        Button addButton = new Button("Добавить в список");
        addButton.setDefaultButton(true);
        addButton.setOnAction(e -> addName());

        Button checkButton = new Button("Проверить на символы");
        checkButton.setOnAction(e -> checkSymbols());

        ImageView deleteIcon = new ImageView(new Image(NameListApp.class.getResourceAsStream("/icons/delete.png")));
        Button deleteButton = new Button("Удалить", deleteIcon);
        deleteButton.setTooltip(new Tooltip("Удалить выбранное имя (Delete). Отмена — Ctrl+Z"));
        deleteButton.setOnAction(e -> deleteSelected());

        Label inputLabel = new Label("Введите имя:");
        inputLabel.setLabelFor(nameField);
        HBox inputRow = new HBox(10, inputLabel, nameField, addButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        HBox actionsRow = new HBox(10, checkButton, deleteButton);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(10, new Label("Список имён:"), namesList, inputRow, actionsRow);
        root.setPadding(new Insets(15));
        VBox.setVgrow(namesList, Priority.ALWAYS);

        Scene scene = new Scene(root, 460, 420);
        // Фильтр, а не обработчик: иначе Ctrl+Z перехватит текстовое поле и отменит ввод текста, а не изменение списка.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (UNDO.match(e)) {
                undo();
                e.consume();
            }
        });

        stage.setTitle("Редактирование списка");
        stage.setScene(scene);
        stage.setMinWidth(440);
        stage.setMinHeight(320);
        stage.show();
        nameField.requestFocus();
    }

    private void addName() {
        String input = nameField.getText();
        if (input.isBlank()) {
            show(Alert.AlertType.WARNING, "Пустое имя", "Введите имя в поле «Введите имя» и нажмите «Добавить в список».");
            return;
        }
        if (NameRules.hasBadChars(input)) {
            show(Alert.AlertType.WARNING, "Недопустимые символы",
                    "Имя может содержать только буквы и дефис. Нажмите «Проверить на символы», чтобы убрать цифры, знаки препинания и другие лишние символы.");
            return;
        }
        String name = NameRules.normalize(input);
        if (!name.matches(".*\\p{L}.*")) {
            show(Alert.AlertType.WARNING, "Нет букв", "Имя должно содержать хотя бы одну букву.");
            return;
        }
        if (namesList.getItems().stream().anyMatch(name::equalsIgnoreCase)) {
            show(Alert.AlertType.INFORMATION, "Повтор", "Уже есть в списке");
            return;
        }
        namesList.getItems().add(name);
        int index = namesList.getItems().size() - 1;
        history.push(new Change(name, index, true));
        namesList.getSelectionModel().select(index);
        namesList.scrollTo(index);
        nameField.clear();
        nameField.requestFocus();
    }

    private void deleteSelected() {
        int index = namesList.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            show(Alert.AlertType.WARNING, "Ничего не выбрано", "Выберите имя в списке, затем нажмите «Удалить».");
            return;
        }
        String name = namesList.getItems().remove(index);
        history.push(new Change(name, index, false));
    }

    private void undo() {
        if (history.isEmpty()) {
            show(Alert.AlertType.INFORMATION, "Отмена", "Нет действий для отмены.");
            return;
        }
        Change change = history.pop();
        if (change.added()) {
            namesList.getItems().remove(change.index());
        } else {
            namesList.getItems().add(change.index(), change.name());
            namesList.getSelectionModel().select(change.index());
        }
    }

    private void checkSymbols() {
        String input = nameField.getText();
        if (input.isBlank()) {
            show(Alert.AlertType.INFORMATION, "Проверка символов", "Поле «Введите имя» пустое, проверять нечего.");
            return;
        }
        if (!NameRules.hasBadChars(input)) {
            show(Alert.AlertType.INFORMATION, "Проверка символов", "Лишних символов нет.");
            return;
        }
        ButtonType clear = new ButtonType("Очистить", ButtonBar.ButtonData.OK_DONE);
        ButtonType keep = new ButtonType("Оставить", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.WARNING, "Есть не текстовые символы, очистить?", clear, keep);
        alert.setTitle("Внимание");
        alert.setHeaderText(null);
        if (alert.showAndWait().orElse(keep) == clear) {
            nameField.setText(NameRules.removeBadChars(input));
        }
    }

    private void show(Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type, text);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
