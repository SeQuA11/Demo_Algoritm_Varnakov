package app;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculatorApp extends Application {

    @Override
    public void start(Stage stage) {
        TextField firstArgument = new TextField();
        firstArgument.setPromptText("например, 5");
        TextField secondArgument = new TextField();
        secondArgument.setPromptText("например, 6");
        Button calculate = new Button("Вычислить");
        calculate.setDefaultButton(true);
        TextArea result = new TextArea();
        result.setEditable(false);
        result.setFocusTraversable(false);

        calculate.setOnAction(e -> {
            try {
                BigDecimal a = parse(firstArgument.getText(), "Аргумент 1");
                BigDecimal b = parse(secondArgument.getText(), "Аргумент 2");
                result.setText(calculateAll(a, b));
            } catch (IllegalArgumentException ex) {
                Alert alert = new Alert(Alert.AlertType.WARNING, ex.getMessage());
                alert.setTitle("Ошибка ввода");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);
        grid.add(new Label("Аргумент 1:"), 0, 0);
        grid.add(firstArgument, 1, 0);
        grid.add(new Label("Аргумент 2:"), 0, 1);
        grid.add(secondArgument, 1, 1);
        grid.add(calculate, 1, 2);
        grid.add(new Label("Результат:"), 0, 3);
        grid.add(result, 0, 4, 2, 1);
        GridPane.setVgrow(result, Priority.ALWAYS);

        stage.setTitle("Отработка ввода/вывода");
        stage.setScene(new Scene(grid, 380, 360));
        stage.setMinWidth(320);
        stage.setMinHeight(320);
        stage.show();
    }

    /** Запятая допускается наравне с точкой, т.к. в русской раскладке дробные числа пишут через запятую. */
    static BigDecimal parse(String text, String fieldName) {
        String value = text.trim().replace(',', '.');
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Поле «" + fieldName + "» не заполнено. Введите число.");
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("В поле «" + fieldName + "» должно быть число, например 5 или 2,5. Сейчас там: «" + text.trim() + "».");
        }
    }

    static String calculateAll(BigDecimal a, BigDecimal b) {
        String first = format(a);
        String second = format(b);
        String division = b.signum() == 0
                ? first + " / " + second + " — делить на ноль нельзя"
                : first + " / " + second + " = " + format(a.divide(b, 4, RoundingMode.HALF_UP));
        return first + " + " + second + " = " + format(a.add(b)) + "\n"
                + first + " - " + second + " = " + format(a.subtract(b)) + "\n"
                + first + " * " + second + " = " + format(a.multiply(b)) + "\n"
                + division;
    }

    /** Убирает лишние нули: 30.00 -> 30, 0.8300 -> 0.83. */
    static String format(BigDecimal number) {
        return number.stripTrailingZeros().toPlainString();
    }
}
