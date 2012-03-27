package de.philippkatz.dependencies;

public class DemoClass {

    private final String username;
    private final String password;

    public DemoClass(
            @ConfigurationParameter(key = "demo.username") String username,
            @ConfigurationParameter(key = "demo.password") String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DemoClass [username=");
        builder.append(username);
        builder.append(", password=");
        builder.append(password);
        builder.append("]");
        return builder.toString();
    }

}
