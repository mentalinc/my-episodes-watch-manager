package eu.vranckaert.episodeWatcher.domain;

public class User {
	public static final String USERNAME = "USERNAME";
	public static final String PASSWORD = "PASSWORD";
	
    private String username;
    private String password;

    public User(String username, String password) {
        setUsername(username);
        setPassword(password);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
