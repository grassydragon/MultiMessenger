package project.controllers;

import javafx.event.ActionEvent;
import project.App;

public class SettingsController {

    public void onBackClick(ActionEvent actionEvent) {
        App.getInstance().goToMainScreen();
    }

}
