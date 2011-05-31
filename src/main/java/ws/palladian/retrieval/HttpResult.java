package ws.palladian.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResult {
    
    private String url;
    private byte[] content;
    private Map<String, List<String>> headers;
    private int statusCode;
    private long transferedBytes;
    
    public HttpResult() {
        headers = new HashMap<String, List<String>>();
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * @return the headers
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * @param headers the headers to set
     */
    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }
    
    public void putHeader(String name, String value) {
        List<String> list = headers.get(name);
        if (list == null) {
            list = new ArrayList<String>();
            headers.put(name, list);
        }
        list.add(value);
    }

    /**
     * @return the statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @return the transferedBytes
     */
    public long getTransferedBytes() {
        return transferedBytes;
    }

    /**
     * @param transferedBytes the transferedBytes to set
     */
    public void setTransferedBytes(long transferedBytes) {
        this.transferedBytes = transferedBytes;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HttpResult [url=");
        builder.append(url);
        builder.append(", content[bytes]=");
        builder.append(content.length);
        builder.append(", headers=");
        builder.append(headers);
        builder.append(", statusCode=");
        builder.append(statusCode);
        builder.append(", transferedBytes=");
        builder.append(transferedBytes);
        builder.append("]");
        return builder.toString();
    }
    
    

}
