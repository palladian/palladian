package ws.palladian.retrieval;

public class SecureProxy {

    private String ip;
    private int port;
    private String username;
    private String password;

    /** The time when this proxy was blocked. If null, the proxy was / is not blocked. */
    private Long blockedTimestamp = null;

    /** The number of requests sent through this proxy. */
    private int requestsSent;

    public SecureProxy(String ip, int port, String username, String password) {
        super();
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public Long getBlockedTimestamp() {
        return blockedTimestamp;
    }

    public void setBlockedTimestamp(Long blockedTimestamp) {
        this.blockedTimestamp = blockedTimestamp;
    }

    public int getRequestsSent() {
        return requestsSent;
    }

    public void increaseRequestsSent() {
        this.requestsSent++;
    }

    @Override
    public String toString() {
        return "SecureProxy [ip=" + ip + ", port=" + port + ", username=" + username + ", password=" + password
                + ", blockedTimestamp=" + blockedTimestamp + ", requestsSend=" + requestsSent + "]";
    }


}
