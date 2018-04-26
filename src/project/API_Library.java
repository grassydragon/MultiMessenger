package project;

import java.util.LinkedList;

public interface API_Library {

    String getName();

    void login();

    void logout();

    boolean isAuthorized();

    String getAccountInfo();

    LinkedList<Chat> getChats();

}
