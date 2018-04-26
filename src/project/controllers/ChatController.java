package project.controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;
import project.App;
import project.Chat;
import project.Message;
import project.selection_models.NoSelectionModel;

import java.text.SimpleDateFormat;
import java.util.concurrent.Callable;

public class ChatController {

    private static final Color MESSAGE_WRAPPER_COLOR = Color.web("ACE0EE");

    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("HH:mm E dd.MM");

    private static final int UPDATE_TIME = 5;

    private static final int MAX_NUMBER_OF_LINES = 6;

    private static ChatController instance;

    public static ChatController getInstance() {
        return instance;
    }

    public Label titleLabel;

    public Label lastActivityLabel;

    public Label accountLabel;

    public ListView<Message> listView;

    public TextArea textArea;

    private Chat chat;

    private ScheduledService<Boolean> updateService = new ScheduledService<Boolean>() {
        @Override
        protected Task<Boolean> createTask() {
            return new Task<Boolean>() {
                @Override
                protected Boolean call() {
                    if (isCancelled()) return false;

                    boolean updated = chat.update();

                    if (updated) Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            listView.getItems().add(chat.getLastMessage());

                            listView.scrollTo(chat.getLastMessage());
                        }
                    });

                    return updated;
                }
            };
        }
    };

    public ChatController() {
        instance = this;
    }

    public void initialize() {
        listView.setSelectionModel(new NoSelectionModel<Message>());

        listView.setCellFactory(new Callback<ListView<Message>, ListCell<Message>>() {
            @Override
            public ListCell<Message> call(ListView<Message> param) {
                return new MessageCell();
            }
        });

        updateService.setDelay(Duration.seconds(UPDATE_TIME));
        updateService.setPeriod(Duration.seconds(UPDATE_TIME));
    }

    public void updateScreen(Chat chat) {
        this.chat = chat;

        titleLabel.setText(chat.getTitle());

        lastActivityLabel.setText(SHORT_DATE_FORMAT.format(chat.getLastMessage().getDate()));

        accountLabel.setText(chat.getLibraryName());

        chat.loadMessages();

        ObservableList<Message> items = FXCollections.observableArrayList();

        items.addAll(chat.getMessages());

        if (items.isEmpty()) items.add(Message.EMPTY);

        listView.setItems(items);

        listView.scrollTo(chat.getLastMessage());

        updateService.restart();
    }

    public void enableTextAreaAutoResize() {
        Text text = (Text) textArea.lookup(".text");
        textArea.prefHeightProperty().bind(Bindings.createDoubleBinding(new Callable<Double>() {

            private double maxHeight = text.getBoundsInLocal().getHeight() * MAX_NUMBER_OF_LINES;

            @Override
            public Double call() {
                if (text.getBoundsInLocal().getHeight() < maxHeight) return text.getBoundsInLocal().getHeight();
                else return maxHeight;
            }

        }, text.boundsInLocalProperty()).add(10));
        textArea.setPromptText("Write a message...");
    }

    public void onBackClick(ActionEvent actionEvent) {
        updateService.cancel();
        chat.unloadMessages();
        App.getInstance().goToMainScreen();
    }

    public void onSendClick(ActionEvent actionEvent) {
        String text = textArea.getText();

        if (text.length() != 0) {
            chat.sendMessage(text);

            textArea.clear();

            listView.getItems().add(chat.getLastMessage());

            listView.scrollTo(chat.getLastMessage());
        }
    }

    static class MessageCell extends ListCell<Message> {

        private static final Insets INSETS = new Insets(10);

        private static final Background MESSAGE_BACKGROUND = new Background(new BackgroundFill(MESSAGE_WRAPPER_COLOR, new CornerRadii(4), new Insets(-10)));

        ChangeListener<Number> scrollListener = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Chat chat = ChatController.instance.chat;

                if (newValue.intValue() == 0 && !chat.areAllMessagesLoaded()) {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                Message currentMessage = chat.getFirstMessage();

                                chat.loadMessages();

                                ObservableList<Message> items = FXCollections.observableArrayList();

                                items.addAll(chat.getMessages());

                                getListView().setItems(items);

                                getListView().scrollTo(currentMessage);
                            }
                        });
                }
            }
        };

        @Override
        protected void updateItem(Message item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) setGraphic(null);
            else {
                if (item == Message.EMPTY) {
                    Label label = new Label("Messages will be shown here.");

                    setGraphic(label);
                }
                else {
                    Label messageLabel = new Label(item.getText());

                    messageLabel.setWrapText(true);
                    messageLabel.setBackground(MESSAGE_BACKGROUND);

                    Label dateLabel = new Label(SHORT_DATE_FORMAT.format(item.getDate()));
                    dateLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent event) {
                            System.out.println("Date label width = " + dateLabel.getWidth());
                        }
                    });

                    dateLabel.setMinWidth(78);

                    HBox hBox;

                    if (item.isOutgoing()) {
                        hBox = new HBox(dateLabel, messageLabel);
                        hBox.setAlignment(Pos.BOTTOM_RIGHT);
                    }
                    else {
                        hBox = new HBox(messageLabel, dateLabel);
                        hBox.setAlignment(Pos.BOTTOM_LEFT);
                    }

                    hBox.setPadding(INSETS);
                    hBox.setSpacing(20);

                    setGraphic(hBox);

                    prefWidthProperty().bind(getListView().prefWidthProperty().subtract(2));

                    layoutYProperty().removeListener(scrollListener);

                    if (item == ChatController.instance.chat.getFirstMessage()) layoutYProperty().addListener(scrollListener);
                }
            }
        }

    }

}
