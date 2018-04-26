package project.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ComboBox;
import project.App;

public class LoginController {

    public ComboBox<String> comboBox;

    public void initialize() {
        ObservableList<String> items = FXCollections.observableArrayList(App.getLibraries().keySet());
        comboBox.setItems(items);
        comboBox.setValue(items.get(0));
    }

    public void onBackClick(ActionEvent actionEvent) {
        App.getInstance().goToAccountsScreen();
    }

    public void onLoginClick(ActionEvent actionEvent) {
        App.getLibraries().get(comboBox.getValue()).login();
    }

}
