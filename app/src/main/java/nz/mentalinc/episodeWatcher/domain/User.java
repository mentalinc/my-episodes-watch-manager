package nz.mentalinc.episodeWatcher.domain;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1161467681474966905L;
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";

    private String usernameString;
    private String passwordString;

    public User(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    public String getUsername() {
        return usernameString;
    }

    private void setUsername(String username) {
        this.usernameString = username;
    }

    public String getPassword() {
        return passwordString;
    }

    private void setPassword(String password) {
        this.passwordString = password;
    }
}
