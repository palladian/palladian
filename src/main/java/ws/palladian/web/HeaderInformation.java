package ws.palladian.web;

import java.util.Date;

public class HeaderInformation {

    private Date lastModifiedSince = null;

    private String eTag = "";

    public HeaderInformation(Date lastModifiedSince, String eTag) {
        this.lastModifiedSince = lastModifiedSince;
        this.eTag = eTag;
    }

    public void setLastModifiedSince(Date lastModifiedSince) {
        this.lastModifiedSince = lastModifiedSince;
    }

    public Date getLastModifiedSince() {
        return lastModifiedSince;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getETag() {
        return eTag;
    }

}
