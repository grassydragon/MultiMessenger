package project.controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javafx.util.Duration;
import project.API_Library;
import project.App;
import project.Chat;
import project.selection_models.NoSelectionModel;

import java.text.SimpleDateFormat;
import java.util.Collections;

public class MainController {

    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("HH:mm E dd.MM");

    private static MainController instance;

    public static MainController getInstance() {
        return instance;
    }

    public ListView<Chat> listView;

    public MainController() {
        instance = this;
    }

    public void initialize() {
        listView.setSelectionModel(new NoSelectionModel<Chat>());

        listView.setCellFactory(new Callback<ListView<Chat>, ListCell<Chat>>() {
            @Override
            public ListCell<Chat> call(ListView<Chat> param) {
                return new ChatCell();
            }
        });
    }

    public void updateScreen() {
        ObservableList<Chat> items = FXCollections.observableArrayList();

        for (API_Library library : App.getLibraries().values())
            if (library.isAuthorized()) items.addAll(library.getChats());

        if (items.isEmpty()) items.add(Chat.EMPTY);
        else Collections.sort(items);

        listView.setItems(items);

        listView.refresh();
    }

    public void onAccountsClick(ActionEvent actionEvent) {
        App.getInstance().goToAccountsScreen();
    }

    public void onSettingsClick(ActionEvent actionEvent) {
        App.getInstance().goToSettingsScreen();
    }

    public void onHelpClick(ActionEvent actionEvent) {
        App.getInstance().goToHelpScreen();
    }

    static class ChatCell extends ListCell<Chat> {

        private static final Insets INSETS = new Insets(10);

        @Override
        protected void updateItem(Chat item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) setGraphic(null);
            else {
                if (item == Chat.EMPTY) {
                    Label label = new Label("Chats will be shown here.");

                    StackPane stackPane = new StackPane(label);

                    stackPane.setPadding(INSETS);

                    StackPane.setAlignment(label, Pos.CENTER_LEFT);

                    setGraphic(stackPane);
                }
                else {
                    Label titleLabel = new Label(item.getTitle());

                    titleLabel.setMinWidth(150);
                    titleLabel.setMaxWidth(150);
                    titleLabel.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));

                    String messageText = item.getLastMessage().getText();

                    if (messageText.contains("\n")) messageText = messageText.substring(0, messageText.indexOf("\n")) + "...";

                    Label messageLabel = new Label(messageText);

                    HBox hBox = new HBox(titleLabel, messageLabel);

                    hBox.setSpacing(10);

                    Label accountLabel = new Label(item.getLibraryName());

                    Label dateLabel = new Label(SHORT_DATE_FORMAT.format(item.getLastMessage().getDate()));

                    StackPane stackPane = new StackPane(accountLabel, dateLabel);

                    StackPane.setAlignment(accountLabel, Pos.CENTER_LEFT);
                    StackPane.setAlignment(dateLabel, Pos.CENTER_RIGHT);

                    VBox vBox = new VBox(hBox, stackPane);

                    vBox.setPadding(INSETS);

                    setOnMouseClicked(new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent event) {
                            App.getInstance().goToChatScreen(item);
                        }
                    });

                    setGraphic(vBox);

                    prefWidthProperty().bind(getListView().prefWidthProperty().subtract(2));
                }
            }
        }

    }

}
