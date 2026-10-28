package app;

import com.google.gson.JsonParser;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * Файл users.db создаётся рядом с проектом при первом запуске,
 * в пустой базе заводится администратор admin / admin.
 */
public class App extends Application {

    /** Сервер API в лаборатории. Вне техникума: http://prb.sylas.ru/TransferSimulator/ */
    static final String API = "http://192.168.1.200:4444/TransferSimulator/";

    /** Собранный пазл: фрагменты 0 1 / 2 3 слева направо, сверху вниз. */
    static final List<Integer> SOLVED = List.of(0, 1, 2, 3);

    /** order.get(позиция) = номер фрагмента, который сейчас лежит на этой позиции. */
    static final List<Integer> order = new ArrayList<>(SOLVED);

    static final Image[] pieces = new Image[4];

    /** Позиция фрагмента после первого клика, -1 — ничего не выбрано. */
    static int picked = -1;

    static Stage stage;

    @Override
    public void start(Stage s) throws Exception {
        stage = s;
        for (int i = 0; i < 4; i++) {
            pieces[i] = new Image(App.class.getResourceAsStream("/captcha/" + (i + 1) + ".png"), 110, 110, false, true);
        }
        sql("CREATE TABLE IF NOT EXISTS users (login TEXT PRIMARY KEY, password TEXT, role TEXT, "
                + "blocked INTEGER DEFAULT 0, fails INTEGER DEFAULT 0)");
        if (sql("SELECT 1 FROM users").isEmpty()) {
            sql("INSERT INTO users (login, password, role) VALUES ('admin', ?, 'admin')", hash("admin"));
        }
        showLogin();
    }

    /** Для INSERT/UPDATE/CREATE возвращает пустой список. Соединение открывается на каждый запрос. */
    static List<String[]> sql(String query, Object... args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:users.db");
             PreparedStatement p = c.prepareStatement(query)) {
            for (int i = 0; i < args.length; i++) {
                p.setObject(i + 1, args[i]);
            }
            List<String[]> rows = new ArrayList<>();
            if (p.execute()) {
                ResultSet r = p.getResultSet();
                int n = r.getMetaData().getColumnCount();
                while (r.next()) {
                    String[] row = new String[n];
                    for (int i = 0; i < n; i++) {
                        row[i] = r.getString(i + 1);
                    }
                    rows.add(row);
                }
            }
            return rows;
        }
    }

    /** В базе хранится SHA-256 от пароля, а не сам пароль. */
    static String hash(String password) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(password.getBytes()));
    }

    static void showLogin() {
        TextField login = new TextField();
        login.setPromptText("Логин");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Пароль");
        Button enter = new Button("Войти");
        Button reg = new Button("Регистрация");
        enter.setOnAction(e -> {
            if (login.getText().isBlank() || pass.getText().isEmpty()) {
                err("Заполните поля «Логин» и «Пароль»");
                return;
            }
            try {
                tryLogin(login.getText().trim(), pass.getText());
            } catch (Exception ex) {
                err("Ошибка: " + ex.getMessage());
            }
        });
        reg.setOnAction(e -> addDialog(false));
        show("Авторизация", 360, 560, login, pass, captcha(), enter, reg);
    }

    /**
     * Неверный пароль и неверно собранный пазл считаются одинаково: три ошибки подряд блокируют.
     * После неудачи экран входа строится заново, поэтому пазл перемешивается.
     */
    static void tryLogin(String login, String password) throws Exception {
        String bad = "Вы ввели неверный логин или пароль. Пожалуйста проверьте ещё раз введенные данные";
        String blocked = "Вы заблокированы. Обратитесь к администратору";
        List<String[]> rows = sql("SELECT password, role, blocked, fails FROM users WHERE login = ?", login);
        if (rows.isEmpty()) {
            err(bad);
            return;
        }
        String[] u = rows.get(0);   // u[0]=хэш пароля, u[1]=роль, u[2]=блокировка, u[3]=число ошибок
        if (u[2].equals("1")) {
            err(blocked);
            return;
        }
        boolean passwordOk = u[0].equals(hash(password));
        if (passwordOk && order.equals(SOLVED)) {
            sql("UPDATE users SET fails = 0 WHERE login = ?", login);
            msg("Вы успешно авторизовались");
            if (u[1].equals("admin")) {
                showAdmin();
            } else {
                showUser(login);
            }
            return;
        }
        int fails = Integer.parseInt(u[3]) + 1;
        sql("UPDATE users SET fails = ?, blocked = ? WHERE login = ?", fails, fails >= 3 ? 1 : 0, login);
        err(fails >= 3 ? blocked : passwordOk ? "Пазл собран неверно. Попробуйте ещё раз" : bad);
        showLogin();
    }

    /** Пазл 2x2: клик по двум фрагментам меняет их местами. Перемешивание не может дать уже собранный пазл. */
    static GridPane captcha() {
        do {
            Collections.shuffle(order);
        } while (order.equals(SOLVED));
        picked = -1;
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        ImageView[] views = new ImageView[4];
        for (int i = 0; i < 4; i++) {
            int position = i;
            views[i] = new ImageView();
            views[i].setOnMouseClicked(e -> {
                if (picked < 0) {
                    picked = position;
                } else {
                    Collections.swap(order, picked, position);
                    picked = -1;
                }
                draw(views);
            });
            grid.add(views[i], i % 2, i / 2);
        }
        draw(views);
        return grid;
    }

    /** Выбранный первым кликом фрагмент делается полупрозрачным. */
    static void draw(ImageView[] views) {
        for (int i = 0; i < 4; i++) {
            views[i].setImage(pieces[order.get(i)]);
            views[i].setOpacity(i == picked ? 0.5 : 1);
        }
    }

    static void showAdmin() {
        ListView<String> list = new ListView<>();
        Runnable reload = () -> {
            try {
                list.getItems().clear();
                for (String[] u : sql("SELECT login, role, blocked FROM users")) {
                    list.getItems().add(u[0] + " | " + u[1] + " | " + (u[2].equals("1") ? "заблокирован" : "активен"));
                }
            } catch (Exception ex) {
                err("Ошибка: " + ex.getMessage());
            }
        };
        reload.run();

        Button add = new Button("Добавить");
        add.setOnAction(e -> {
            addDialog(true);
            reload.run();
        });
        Button unblock = new Button("Снять блокировку");
        unblock.setOnAction(e -> {
            String selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) {
                err("Выберите пользователя в списке");
                return;
            }
            try {
                sql("UPDATE users SET blocked = 0, fails = 0 WHERE login = ?", selected.split(" \\| ")[0]);
            } catch (Exception ex) {
                err("Ошибка: " + ex.getMessage());
            }
            reload.run();
        });
        Button api = new Button("Проверка API");
        api.setOnAction(e -> showApi());
        Button out = new Button("Выйти");
        out.setOnAction(e -> showLogin());
        show("Администратор", 700, 450, list, new HBox(10, add, unblock, api, out));
    }

    static void showUser(String login) {
        Button api = new Button("Проверка API");
        api.setOnAction(e -> showApi());
        Button out = new Button("Выйти");
        out.setOnAction(e -> showLogin());
        show("Рабочий стол", 360, 200, new Label("Добро пожаловать, " + login + "!"), api, out);
    }

    /** admin=false — самостоятельная регистрация с ролью user, admin=true — администратор сам выбирает роль. */
    static void addDialog(boolean admin) {
        TextField login = new TextField();
        PasswordField pass = new PasswordField();
        PasswordField repeat = new PasswordField();
        ChoiceBox<String> role = new ChoiceBox<>(FXCollections.observableArrayList("user", "admin"));
        role.setValue("user");
        VBox box = new VBox(8, new Label("Логин"), login, new Label("Пароль"), pass, new Label("Повтор пароля"), repeat);
        if (admin) {
            box.getChildren().add(role);
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(admin ? "Добавление пользователя" : "Регистрация");
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            String name = login.getText().trim();
            if (name.isEmpty() || pass.getText().isEmpty()) {
                throw new Exception("Заполните логин и пароль");
            }
            if (!pass.getText().equals(repeat.getText())) {
                throw new Exception("Пароли не совпадают");
            }
            if (!sql("SELECT 1 FROM users WHERE login = ?", name).isEmpty()) {
                throw new Exception("Пользователь с таким логином уже существует");
            }
            sql("INSERT INTO users (login, password, role) VALUES (?, ?, ?)", name, hash(pass.getText()), role.getValue());
            msg("Пользователь добавлен");
        } catch (Exception ex) {
            err(ex.getMessage());
        }
    }

    static void showApi() {
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
                err("Не удалось получить данные от API (запущен ли эмулятор?): " + ex.getMessage());
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
        Stage window = new Stage();
        window.setTitle("Валидация данных");
        window.setScene(new Scene(grid, 560, 200));
        window.setMinWidth(480);
        window.setMinHeight(220);
        window.show();
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

    /** Все экраны показываются в одном главном окне, новый экран заменяет предыдущий. */
    static void show(String title, int width, int height, Node... items) {
        VBox box = new VBox(10, items);
        box.setPadding(new Insets(15));
        stage.setTitle(title);
        stage.setScene(new Scene(box, width, height));
        stage.sizeToScene();
        stage.show();
    }

    static void err(String text) {
        alert(Alert.AlertType.ERROR, "Ошибка", text);
    }

    static void msg(String text) {
        alert(Alert.AlertType.INFORMATION, "Информация", text);
    }

    /** У JavaFX нет русской локализации, без явного заголовка окно подписано "Error" / "Information". */
    static void alert(Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type, text);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
