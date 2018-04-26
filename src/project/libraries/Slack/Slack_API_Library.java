package project.libraries.Slack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import project.API_Library;
import project.App;
import project.Chat;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Slack_API_Library implements API_Library {

    private static final String API_LIBRARY_NAME = "Slack";

    private static final String ACCOUNT_DATA_FILE = "Slack Account Data.tmp";

    private static final String clientID = "323630498438.322130661072";

    private static final String clientSecret = "3e4839a8e9cb7b4bee4509871dcc8b36";

    private static final String verificationToken = "JYqyL5BGGUe2CzWN0Z3zMakk";

    private static final String[] accessScopes = {
            "channels:history",
            "channels:read",
            "channels:write",
            "chat:write:bot",
            "chat:write:user",
            "files:read",
            "groups:history",
            "groups:read",
            "groups:write",
            "im:history",
            "im:read",
            "im:write",
            "users:read"
    };

    private static Slack_API_Library instance;

    static {
        instance = new Slack_API_Library();
    }

    public static Slack_API_Library getInstance() {
        return instance;
    }

    private JsonParser jsonParser;

    //private String code;

    private String accessToken;

    //private String team;

    //private String username;

    private String accountInfo;

    private String userId;

    //private String teamId;

    private boolean authorized;

    private LinkedList<Chat> chats;

    private Slack_API_Library() {
        jsonParser = new JsonParser();
        loadAccountData();
    }

    @Override
    public String getName() {
        return API_LIBRARY_NAME;
    }

    @Override
    public void login() {
        //String redirectUri = "https://slack.com";
        StringBuilder permissions = new StringBuilder();
        Arrays.stream(accessScopes).forEach(s -> permissions.append(s).append(","));
        String url = "https://slack.com/oauth/authorize?" +
                "client_id=" + clientID +
                "&scope=" + permissions + "users:read.email";
        //"&redirect_uri=" + redirectUri;

        Stage stage = new Stage();
        stage.setTitle("Authorization");
        stage.setMinWidth(650);
        stage.setMinHeight(600);
        stage.setWidth(650);
        stage.setHeight(600);

        WebView webView = new WebView();
        stage.setScene(new Scene(webView));

        stage.show();

        WebEngine webEngine = webView.getEngine();
        webEngine.load(url);

        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            Pattern pattern = Pattern.compile("https://slack\\.com/\\?code=([0-9a-z]+\\.[0-9a-z]+\\.[0-9a-z]+)&state=");

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldLocation, String newLocation) {
                Matcher matcher = pattern.matcher(newLocation);
                if (matcher.matches()) {
                    String code = matcher.group(1);

                    String response = makeHttpRequest(makeApiRequestUrl("oauth.access",
                            "client_id=" + clientID,
                            "client_secret=" + clientSecret,
                            "code=" + code));

                    JsonObject responseObject = getJsonObjectFromResponse(response);

                    accessToken = responseObject.get("access_token").getAsString();

                    authorized = true;

                    response = makeHttpRequest(makeApiRequestUrl("auth.test", "token=" + accessToken));

                    responseObject = getJsonObjectFromResponse(response);

                    userId = responseObject.get("user_id").getAsString();

                    saveAccountData();

                    stage.close();

                    App.getInstance().goToAccountsScreen();
                }
            }
        });
    }

    @Override
    public void logout() {
        authorized = false;
        saveAccountData();
    }

    @Override
    public boolean isAuthorized() {
        return authorized;
    }

    @Override
    public String getAccountInfo() {
        if (accountInfo == null) {
            String response = makeHttpRequest(makeApiRequestUrl("auth.test", "token=" + accessToken));

            JsonObject responseObject = getJsonObjectFromResponse(response);

            accountInfo = responseObject.get("user").getAsString();
        }
        return accountInfo;
    }

    @Override
    public LinkedList<Chat> getChats() {
        if (chats == null) {
            chats = new LinkedList<>();

            String response = makeHttpRequest(makeApiRequestUrl("channels.list", "token=" + accessToken));

            JsonObject responseObject = getJsonObjectFromResponse(response);

            JsonArray channelsArray = responseObject.getAsJsonArray("channels");

            for (int i = 0; i < channelsArray.size(); i++) {
                JsonObject channelObject = channelsArray.get(i).getAsJsonObject();

                String chatTitle = channelObject.get("name").getAsString();

                String chatId = channelObject.get("id").getAsString();

                Slack_Chat chat = new Slack_Chat(chatTitle, chatId);

                String channelResponse = makeHttpRequest(makeApiRequestUrl("channels.info", "token=" + accessToken, "channel=" + chatId));

                JsonObject channelResponseObject = getJsonObjectFromResponse(channelResponse);

                JsonObject latestMessageObject;

                latestMessageObject = channelResponseObject.get("channel").getAsJsonObject().get("latest").getAsJsonObject();

                Slack_Message message = createMessageFromJsonObject(latestMessageObject);

                chat.messages.addLast(message);

                chats.addLast(chat);
            }
        }
        return chats;
    }

    public boolean updateChat(Slack_Chat chat) {
        Slack_Message lastMessage = (Slack_Message) chat.getLastMessage();

        String response = makeHttpRequest(makeApiRequestUrl("channels.history",
                "token=" + accessToken,  "channel=" + chat.id, "count=1", "oldest=" + lastMessage.timestamp));

        JsonObject responseObject = getJsonObjectFromResponse(response);

        JsonArray messagessArray = responseObject.get("messages").getAsJsonArray();

        if (messagessArray.size() == 0) return false;

        JsonObject messageObject = messagessArray.get(messagessArray.size() - 1).getAsJsonObject();

        Slack_Message message = createMessageFromJsonObject(messageObject);

        chat.messages.addLast(message);

        return true;
    }

    public void loadChatMessages(Slack_Chat chat) {
        Slack_Message firstMessage = (Slack_Message) chat.getFirstMessage();

        String response = makeHttpRequest(makeApiRequestUrl("channels.history",
                "token=" + accessToken, "channel=" + chat.id, "count=100", "latest=" + firstMessage.timestamp));

        JsonObject responseObject = getJsonObjectFromResponse(response);

        JsonArray messagesArray = responseObject.get("messages").getAsJsonArray();

        for (int i = 0; i < messagesArray.size(); i++) {
            JsonObject messageObject = messagesArray.get(i).getAsJsonObject();

            Slack_Message message = createMessageFromJsonObject(messageObject);

            chat.messages.addFirst(message);
        }

        chat.allMessagesLoaded = !responseObject.get("has_more").getAsBoolean();
    }

    public void sendChatMessage(Slack_Chat chat, String text) {
        String encodedText = null;

        try {
            encodedText = URLEncoder.encode(text, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String response = makeHttpRequest(makeApiRequestUrl("chat.postMessage", "token=" + accessToken, "channel=" + chat.id, "text=" + encodedText, "as_user=true"));

        JsonObject responseObject = getJsonObjectFromResponse(response);

        JsonObject messageObject = responseObject.get("message").getAsJsonObject();

        Slack_Message message = createMessageFromJsonObject(messageObject);

        chat.messages.addLast(message);
    }

    /**
     * Метод разбирает строку с ответом от сервера и возвращает объект Json
     *
     * @param response строка с ответом от сервера в формате Json
     * @return объект Json
     */
    private JsonObject getJsonObjectFromResponse(String response) {
        return jsonParser.parse(response).getAsJsonObject();
    }

    /**
     * Метод создаёт объект сообщения на основе заданного объекта Json.
     *
     * @param messageObject объект Json, представляющий сообщение
     * @return созданный объект сообщения
     */
    private Slack_Message createMessageFromJsonObject(JsonObject messageObject) {
        String messageText = messageObject.get("text").getAsString();

        if (messageObject.has("attachments")) {
            String attachmentText = messageObject.get("attachments").getAsJsonObject().get("text").getAsString();
            messageText += "\n" + "Attachment: " + attachmentText;
        }

        String messageTimestamp = messageObject.get("ts").getAsString();
        String messageUsername = messageObject.get("user").getAsString();

        boolean messageOutgoing = Objects.equals(messageUsername, userId);

        return new Slack_Message(messageText, messageTimestamp, messageUsername, messageOutgoing);
    }

    /**
     * Метод формирует запрос к серверу для заданных метода и параметров и возвращает строку с адресом URL.
     *
     * @param methodName имя метода Slack API
     * @param params     параметры метода Slack API
     * @return строка с адресом URL
     */
    private String makeApiRequestUrl(String methodName, String... params) {
        StringBuilder url = new StringBuilder("https://slack.com/api/" + methodName + "?");

        if (params.length != 0) {
            for (int i = 0; i < params.length - 1; i++) {
                url.append(params[i]);
                url.append("&");
            }
            url.append(params[params.length - 1]);
        }

        return url.toString();
    }

    private String makeHttpRequest(String url) {
        HttpURLConnection connection = null;
        String response = null;
        //StringBuilder response = new StringBuilder();

        try {
            connection = (HttpURLConnection) (new URL(url)).openConnection();
            connection.setRequestMethod("GET");
            //connection.setConnectTimeout(10);
            //connection.setReadTimeout(10);

            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    /*String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }*/
                    response = in.readLine();
                    System.out.println(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Connection failed: " + connection.getResponseCode() + ", " + connection.getResponseMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) connection.disconnect();
        }

        return response;
    }

    /**
     * Метод сохраняет ключ доступа пользователя к Slack API, идентификатор пользователя и значение, показывающее, выполнен ли вход в аккаунт.
     */
    private void saveAccountData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ACCOUNT_DATA_FILE))) {
            out.writeObject(accessToken);
            out.writeObject(userId);
            out.writeBoolean(authorized);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод загружает ключ доступа пользователя к Slack API, идентификатор пользователя и значение, показывающее, выполнен ли вход в аккаунт.
     */
    private void loadAccountData() {
        File file = new File(ACCOUNT_DATA_FILE);

        if (file.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                accessToken = (String) in.readObject();
                userId = (String) in.readObject();
                authorized = in.readBoolean();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
