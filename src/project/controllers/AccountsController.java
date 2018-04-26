package project.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import project.API_Library;
import project.App;
import project.selection_models.NoSelectionModel;

import java.util.Map;

public class AccountsController {

    private static AccountsController instance;

    public static AccountsController getInstance() {
        return instance;
    }

    public ListView<String> listView;

    public AccountsController() {
        instance = this;
    }

    public void initialize() {
        listView.setSelectionModel(new NoSelectionModel<String>());

        listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new AccountCell();
            }
        });
    }

    public void updateScreen() {
        ObservableList<String> items = FXCollections.observableArrayList();

        for (API_Library library : App.getLibraries().values())
            if (library.isAuthorized()) items.add(library.getName());

        if (items.isEmpty()) items.add("Accounts will be shown here.");

        listView.setItems(items);
    }

    public void onBackClick(ActionEvent actionEvent) {
        App.getInstance().goToMainScreen();
    }

    public void onAddClick(ActionEvent actionEvent) {
        App.getInstance().goToLoginScreen();
    }

    static class AccountCell extends ListCell<String> {

        private static final Insets INSETS = new Insets(10);

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) setGraphic(null);
            else {
                API_Library library = App.getLibraries().get(item);

                if (library == null) {
                    Label label = new Label(item);

                    StackPane stackPane = new StackPane(label);

                    stackPane.setPadding(INSETS);

                    StackPane.setAlignment(label, Pos.CENTER_LEFT);

                    setGraphic(stackPane);
                }
                else  {
                    Label label = new Label(library.getAccountInfo() + " " + item);

                    Button button = new Button("Logout");

                    button.setOnAction(new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            library.logout();
                            AccountsController.getInstance().updateScreen();
                        }
                    });

                    StackPane stackPane = new StackPane(label, button);

                    stackPane.setPadding(INSETS);

                    StackPane.setAlignment(label, Pos.CENTER_LEFT);
                    StackPane.setAlignment(button, Pos.CENTER_RIGHT);

                    setGraphic(stackPane);
                }
            }
        }

    }

}
