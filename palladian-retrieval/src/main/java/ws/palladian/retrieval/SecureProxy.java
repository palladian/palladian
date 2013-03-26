package ws.palladian.retrieval;

import java.util.Date;

import ws.palladian.helper.nlp.StringHelper;

public class SecureProxy implements Proxy {

    private String address;
    private int port;
    private String username = "";
    private String password = "";

    /** The response time in milliseconds. */
    private long responseTime;

    /** The time when this proxy was blocked. If null, the proxy was / is not blocked. */
    private Date lastBlocked = null;

    /** Number of times we found that this proxy is blocked. */
    private int blockCount = 0;

    /** Number of times we found that this proxy is dead (connection timeouts etc.). */
    private int deadCount = 0;

    /** The number of times this proxy has been used. */
    private int useCount = 0;

    /** The last time the proxy was used. */
    private Date lastUsed = null;

    /**
     * <p>
     * Initialize a proxy with a string.
     * </p>
     * 
     * @param proxyString Must be in the form of ip:port.
     */
    public SecureProxy(String proxyString) {
        String host = StringHelper.clean(proxyString.substring(0, proxyString.indexOf(":")));
        String port = proxyString.substring(proxyString.indexOf(":") + 1);
        port = StringHelper.clean(port);
        this.address = host;
        this.port = Integer.valueOf(port);
    }

    public SecureProxy(String address, int port, String username, String password) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getLastBlocked() {
        return lastBlocked;
    }

    public void setLastBlocked(Date blockedTimestamp) {
        this.lastBlocked = blockedTimestamp;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    public void increaseUseCount() {
        this.useCount++;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsedTimestamp) {
        this.lastUsed = lastUsedTimestamp;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public void increaseBlockCount() {
        this.blockCount++;
    }

    public int getDeadCount() {
        return deadCount;
    }

    public void setDeadCount(int deadCount) {
        this.deadCount = deadCount;
    }

    public void increaseDeadCount() {
        this.deadCount++;
    }

    @Override
    public String toString() {
        return "SecureProxy [address=" + address + ", port=" + port + ", username=" + username + ", password="
                + password
                + ", responseTime=" + responseTime + ", blockedTimestamp=" + lastBlocked + ", requestsSent="
                + useCount + "]";
    }

}
