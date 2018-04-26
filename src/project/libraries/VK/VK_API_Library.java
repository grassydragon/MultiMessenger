package project.libraries.VK;

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
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VK_API_Library implements API_Library {

    private static final String API_LIBRARY_NAME = "VK";

    private static final String API_VERSION = "5.71";

    private static final String ACCOUNT_DATA_FILE = "VK Account Data.tmp";

    private static VK_API_Library instance;

    static {
        instance = new VK_API_Library();
    }

    public static VK_API_Library getInstance() {
        return instance;
    }

    private JsonParser jsonParser;

    private Random random;

    private String accessToken;

    private String accountInfo;

    private int userId;

    private boolean authorized;

    private LinkedList<Chat> chats;

    private VK_API_Library() {
        jsonParser = new JsonParser();
        random = new Random();
        loadAccountData();
    }

    @Override
    public String getName() {
        return API_LIBRARY_NAME;
    }

    @Override
    public void login() {
        String redirectUri = "https://oauth.vk.com/blank.html";

        String url = "https://oauth.vk.com/authorize?" +
                "client_id=6338916" +
                "&display=page" +
                "&redirect_uri=" + redirectUri +
                "&scope=offline,messages" +
                "&response_type=token" +
                "&v=" + API_VERSION;

        Stage stage = new Stage();

        stage.setTitle("Authorization");

        stage.setMinWidth(672);
        stage.setMinHeight(388);

        stage.setWidth(672);
        stage.setHeight(388);

        WebView webView = new WebView();

        stage.setScene(new Scene(webView));

        stage.show();

        WebEngine webEngine = webView.getEngine();

        webEngine.load(url);

        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldLocation, String newLocation) {
                if (newLocation.contains(redirectUri)) {
                    Pattern pattern = Pattern.compile("https://oauth.vk.com/blank.html#access_token=([0-9a-z]+)&expires_in=0&user_id=([0-9]+)");

                    Matcher matcher = pattern.matcher(newLocation);

                    if (matcher.matches()) {
                        accessToken = matcher.group(1);
                        userId = Integer.parseInt(matcher.group(2));
                        authorized = true;
                        saveAccountData();
                    }

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
            String response = makeHttpRequest(makeApiRequestUrl("account.getProfileInfo", accessToken, API_VERSION));

            JsonObject responseObject = getJsonObjectFromResponse(response);

            accountInfo = responseObject.get("first_name").getAsString() + " " +  responseObject.get("last_name").getAsString() + " " + responseObject.get("phone").getAsString();
        }
        return accountInfo;
    }

    @Override
    public LinkedList<Chat> getChats() {
        if (chats == null) {
            chats = new LinkedList<>();

            StringBuilder userIds = new StringBuilder();

            String response = makeHttpRequest(makeApiRequestUrl("messages.getDialogs", accessToken, API_VERSION, "count=200"));

            JsonObject responseObject = getJsonObjectFromResponse(response);

            JsonArray itemsArray = responseObject.get("items").getAsJsonArray();

            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject chatObject = itemsArray.get(i).getAsJsonObject();

                JsonObject messageObject = chatObject.get("message").getAsJsonObject();

                //Пропуск сообщений из сообществ

                if (messageObject.get("user_id").getAsInt() < 0) continue;

                String chatTitle = messageObject.get("title").getAsString();

                int chatId = 0;

                if (messageObject.has("chat_id")) chatId = messageObject.get("chat_id").getAsInt();
                else {
                    if (userIds.length() != 0) userIds.append(",");
                    userIds.append(messageObject.get("user_id").getAsString());
                }

                VK_Chat chat = new VK_Chat(chatTitle, chatId);

                VK_Message message = createMessageFromJsonObject(messageObject);

                chat.messages.addLast(message);

                chats.addLast(chat);
            }

            if (userIds.length() != 0) {
                response = makeHttpRequest(makeApiRequestUrl("users.get", accessToken, API_VERSION, "user_ids=" + userIds.toString()));

                JsonArray responseArray = getJsonArrayFromResponse(response);

                int i = 0;

                for (Chat chat : chats) {
                    if (chat.getTitle().isEmpty()) {
                        JsonObject userObject = responseArray.get(i).getAsJsonObject();

                        String firstName = userObject.get("first_name").getAsString();
                        String lastName = userObject.get("last_name").getAsString();

                        ((VK_Chat) chat).title = firstName + " " + lastName;

                        i++;
                    }
                }
            }
        }

        return chats;
    }

    public boolean updateChat(VK_Chat chat) {
        VK_Message lastMessage = (VK_Message) chat.getLastMessage();

        int peerId;

        if (chat.id != 0) peerId = 2000000000 + chat.id;
        else peerId = lastMessage.userId;

        String response = makeHttpRequest(makeApiRequestUrl("messages.getHistory", accessToken, API_VERSION,
                "offset=-1", "count=1", "peer_id=" + peerId, "start_message_id=" + lastMessage.id));

        JsonObject responseObject = getJsonObjectFromResponse(response);

        JsonArray itemsArray = responseObject.get("items").getAsJsonArray();

        if (itemsArray.size() == 0) return false;

        JsonObject messageObject = itemsArray.get(itemsArray.size() - 1).getAsJsonObject();

        VK_Message message = createMessageFromJsonObject(messageObject);

        chat.messages.addLast(message);

        return true;
    }

    public void loadChatMessages(VK_Chat chat) {
        VK_Message firstMessage = (VK_Message) chat.messages.getFirst();

        int peerId;

        if (chat.id != 0) peerId = 2000000000 + chat.id;
        else peerId = firstMessage.userId;

        String response = makeHttpRequest(makeApiRequestUrl("messages.getHistory", accessToken, API_VERSION,
                "offset=1", "count=100", "peer_id=" + peerId, "start_message_id=" + firstMessage.id));

        JsonObject responseObject = getJsonObjectFromResponse(response);

        JsonArray itemsArray = responseObject.get("items").getAsJsonArray();

        for (int i = 0; i < itemsArray.size(); i++) {
            JsonObject messageObject = itemsArray.get(i).getAsJsonObject();

            VK_Message message = createMessageFromJsonObject(messageObject);

            chat.messages.addFirst(message);
        }

        if (itemsArray.size() == 0) chat.allMessagesLoaded = true;

        System.out.println("Number of messages loaded = " + chat.messages.size());
    }

    public void sendChatMessage(VK_Chat chat, String text) {
        int peerId;

        if (chat.id != 0) peerId = 2000000000 + chat.id;
        else peerId = ((VK_Message) chat.getLastMessage()).userId;

        String encodedText = null;

        try {
            encodedText = URLEncoder.encode(text, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String response = makeHttpRequest(makeApiRequestUrl("messages.send", accessToken, API_VERSION,
                "random_id=" + random.nextInt(Integer.MAX_VALUE), "peer_id=" + peerId, "message=" + encodedText));

        int messageId = getIntFromResponse(response);

        response = makeHttpRequest(makeApiRequestUrl("messages.getById", accessToken, API_VERSION,"message_ids=" + messageId));

        JsonObject responseObject = getJsonObjectFromResponse(response);

        JsonArray itemsArray = responseObject.get("items").getAsJsonArray();

        JsonObject messageObject = itemsArray.get(0).getAsJsonObject();

        VK_Message message = createMessageFromJsonObject(messageObject);

        chat.messages.addLast(message);
    }

    /**
     * Метод создаёт объект сообщения на основе заданного объекта Json.
     * @param messageObject объект Json, представляющий сообщение
     * @return созданный объект сообщения
     */
    private VK_Message createMessageFromJsonObject(JsonObject messageObject) {
        String messageText = messageObject.get("body").getAsString();

        if (messageText.isEmpty()) {
            if (messageObject.has("attachments")) messageText = "[Attachment]";
            else if (messageObject.has("fwd_messages")) messageText = "[Forwarded message]";
            else if (messageObject.has("action")) messageText = "[Action]";
        }

        Date messageDate = new Date(messageObject.get("date").getAsLong() * 1000);

        int messageId = messageObject.get("id").getAsInt();

        int messageUserId = messageObject.get("user_id").getAsInt();

        boolean messageOutgoing = (messageObject.get("out").getAsInt() == 1);

        return new VK_Message(messageText, messageDate, messageId, messageUserId, messageOutgoing);
    }

    /**
     * Метод разбирает строку с ответом от сервера и возвращает объект Json с именем "response".
     * @param response строка с ответом от сервера в формате Json
     * @return объект Json с именем "response"
     */
    private JsonObject getJsonObjectFromResponse(String response) {
        return jsonParser.parse(response).getAsJsonObject().get("response").getAsJsonObject();
    }

    /**
     * Метод разбирает строку с ответом от сервера и возвращает массив Json с именем "response".
     * @param response строка с ответом от сервера в формате Json
     * @return массив Json с именем "response"
     */
    private JsonArray getJsonArrayFromResponse(String response) {
        return jsonParser.parse(response).getAsJsonObject().get("response").getAsJsonArray();
    }

    /**
     * Метод разбирает строку с ответом от сервера и возвращает целое число с именем "response".
     * @param response строка с ответом от сервера в формате Json
     * @return целое число с именем "response"
     */
    private int getIntFromResponse(String response) {
        return jsonParser.parse(response).getAsJsonObject().get("response").getAsInt();
    }

    /**
     * Метод выполняет запрос к серверу по заданному адресу URL и возвращает строку с ответом от сервера.
     * @param url адрес URL
     * @return строка с ответом от сервера
     */
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
            }
            else {
                System.out.println("Connection failed: " + connection.getResponseCode() + ", " + connection.getResponseMessage());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (connection != null) connection.disconnect();
        }

        return response;
    }

    /**
     * Метод формирует запрос к серверу для заданных метода и параметров и возвращает строку с адресом URL.
     * @param methodName  имя метода VK API
     * @param accessToken ключ доступа пользователя к VK API (null, если не требуется)
     * @param version     версия VK API (null, если не требуется)
     * @param params      параметры метода VK API
     * @return строка с адресом URL
     */
    private String makeApiRequestUrl(String methodName, String accessToken, String version, String... params) {
        StringBuilder url = new StringBuilder("https://api.vk.com/method/" + methodName + "?");

        if (params.length != 0) {
            for (int i = 0; i < params.length - 1; i++) {
                url.append(params[i]);
                url.append("&");
            }
            url.append(params[params.length - 1]);
        }

        if (accessToken != null) {
            url.append("&access_token=");
            url.append(accessToken);
        }

        if (version != null) {
            url.append("&v=");
            url.append(version);
        }

        return url.toString();
    }

    /**
     * Метод загружает ключ доступа пользователя к VK API, идентификатор пользователя и значение, показывающее, выполнен ли вход в аккаунт.
     */
    private void loadAccountData() {
        File file = new File(ACCOUNT_DATA_FILE);

        if (file.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                accessToken = (String) in.readObject();
                userId = in.readInt();
                authorized = in.readBoolean();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Метод сохраняет ключ доступа пользователя к VK API, идентификатор пользователя и значение, показывающее, выполнен ли вход в аккаунт.
     */
    private void saveAccountData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ACCOUNT_DATA_FILE))) {
            out.writeObject(accessToken);
            out.writeInt(userId);
            out.writeBoolean(authorized);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
