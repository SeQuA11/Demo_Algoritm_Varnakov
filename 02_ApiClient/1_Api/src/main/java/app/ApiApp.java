package app;

import com.google.gson.JsonParser;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiApp extends Application {

    /** Сервер API в лаборатории. Вне техникума: http://prb.sylas.ru/TransferSimulator/ */
    static final String API = "http://192.168.1.200:4444/TransferSimulator/";

    @Override
    public void start(Stage stage) {
        ComboBox<DataType> type = new ComboBox<>();
        type.getItems().addAll(DataType.values());
        type.setValue(DataType.FULL_NAME);
        Label data = new Label();
        Label result = new Label();
        Button get = new Button("Получить данные");
        Button send = new Button("Отправить результат теста");

        type.setOnAction(e -> {
            data.setText("");
            result.setText("");
        });

        get.setOnAction(e -> {
            try {
                data.setText(apiGet(type.getValue().method));
                result.setText("");
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Не удалось получить данные от API (запущен ли эмулятор?): " + ex.getMessage());
                alert.setTitle("Ошибка");
                alert.setHeaderText(null);
                alert.showAndWait();
            }
        });

        send.setOnAction(e -> result.setText(type.getValue().check(data.getText())));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Тип данных"), 0, 0);
        grid.add(type, 1, 0);
        grid.add(get, 0, 1);
        grid.add(data, 1, 1);
        grid.add(send, 0, 2);
        grid.add(result, 1, 2);
        stage.setTitle("Валидация данных");
        stage.setScene(new Scene(grid, 560, 200));
        stage.setMinWidth(480);
        stage.setMinHeight(220);
        stage.show();
    }

    /** API отвечает JSON вида {"value": "..."}, возвращаем содержимое value. */
    static String apiGet(String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + method)).timeout(Duration.ofSeconds(5)).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new Exception("код ответа " + response.statusCode() + (response.statusCode() == 500 ? ", обратитесь к главному эксперту" : ""));
        }
        return JsonParser.parseString(response.body()).getAsJsonObject().get("value").getAsString();
    }
}
