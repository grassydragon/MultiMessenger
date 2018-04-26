package project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import project.controllers.AccountsController;
import project.controllers.ChatController;
import project.controllers.MainController;
import project.libraries.Slack.Slack_API_Library;
import project.libraries.VK.VK_API_Library;
import project.libraries.VK.VK_Message;

import java.io.IOException;
import java.util.HashMap;

public class App extends Application {

    private static final int MIN_WINDOW_WIDTH = 600;

    private static final int MIN_WINDOW_HEIGHT = 600;

    private static App instance;

    public static App getInstance() {
        return instance;
    }

    public static HashMap<String, API_Library> getLibraries() {
        return instance.libraries;
    }

    private Stage stage;

    private Parent mainRoot;

    private Parent chatRoot;

    private Parent accountsRoot;

    private Parent loginRoot;

    private Parent settingsRoot;

    private Parent helpRoot;

    private HashMap<String, API_Library> libraries;

    public App() {
        instance = this;
        libraries = new HashMap<>();
        libraries.put(VK_API_Library.getInstance().getName(), VK_API_Library.getInstance());
        libraries.put(Slack_API_Library.getInstance().getName(), Slack_API_Library.getInstance());
    }

    @Override
    public void start(Stage stage) throws Exception {
        mainRoot = FXMLLoader.load(App.class.getResource("layouts/main.fxml"));

        mainRoot.getStylesheets().addAll("project/styles/project_style.css", "project/styles/list_cell_borders_style.css");

        this.stage = stage;

        stage.setTitle("MultiMessenger");

        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(MIN_WINDOW_HEIGHT);

        stage.setWidth(MIN_WINDOW_WIDTH);
        stage.setHeight(MIN_WINDOW_HEIGHT);

        stage.setScene(new Scene(mainRoot));

        stage.show();

        MainController.getInstance().updateScreen();
    }

    public void goToMainScreen() {
        stage.getScene().setRoot(mainRoot);
        MainController.getInstance().updateScreen();
    }

    public void goToChatScreen(Chat chat) {
        if (chatRoot == null) {
            try {
                chatRoot = FXMLLoader.load(App.class.getResource("layouts/chat.fxml"));
                chatRoot.getStylesheets().add("project/styles/project_style.css");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    ChatController.getInstance().enableTextAreaAutoResize();
                }
            });
        }
        stage.getScene().setRoot(chatRoot);
        ChatController.getInstance().updateScreen(chat);
    }

    public void goToAccountsScreen() {
        if (accountsRoot == null) {
            try {
                accountsRoot = FXMLLoader.load(App.class.getResource("layouts/accounts.fxml"));
                accountsRoot.getStylesheets().add("project/styles/project_style.css");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stage.getScene().setRoot(accountsRoot);
        AccountsController.getInstance().updateScreen();
    }

    public void goToLoginScreen() {
        if (loginRoot == null) {
            try {
                loginRoot = FXMLLoader.load(App.class.getResource("layouts/login.fxml"));
                loginRoot.getStylesheets().add("project/styles/project_style.css");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stage.getScene().setRoot(loginRoot);
    }

    public void goToSettingsScreen() {
        if (settingsRoot == null) {
            try {
                settingsRoot = FXMLLoader.load(App.class.getResource("layouts/settings.fxml"));
                settingsRoot.getStylesheets().add("project/styles/project_style.css");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stage.getScene().setRoot(settingsRoot);
    }

    public void goToHelpScreen() {
        if (helpRoot == null) {
            try {
                helpRoot = FXMLLoader.load(App.class.getResource("layouts/help.fxml"));
                helpRoot.getStylesheets().add("project/styles/project_style.css");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        stage.getScene().setRoot(helpRoot);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
