package ws.palladian.retrieval.email;

/**
 * A simple email contact.
 *
 * @author David Urbansky
 */
public class EmailContact {
    /**
     * The contact's name.
     */
    private String name;

    /**
     * The contact's email address.
     */
    private String email;

    /**
     * The id of the list the contact should be subscribed to.
     */
    private String list;

    /**
     * The two-letter country code of the contact (ISO 639-1)
     */
    private String countryCode;

    /**
     * The contact's IP address.
     */
    private String ipAddress;

    /**
     * The URL where the user signed up from.
     */
    private String referrer;

    /**
     * If you're signing up EU users in a GDPR compliant manner, set this to "true".
     */
    private boolean gdpr = false;

    /**
     * Set to "true" if your list is 'Double opt-in' but you want to bypass that and signup the user to the list as 'Single Opt-in instead'.
     */
    private boolean silent = true;

    /**
     * Include this 'honeypot' field to prevent spambots from signing up via this API call. When spambots fills in this field, this API call will exit, preventing them from signing up fake addresses to your form.
     */
    private boolean honeyPot = false;

    public EmailContact(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public boolean isGdpr() {
        return gdpr;
    }

    public void setGdpr(boolean gdpr) {
        this.gdpr = gdpr;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean isHoneyPot() {
        return honeyPot;
    }

    public void setHoneyPot(boolean honeyPot) {
        this.honeyPot = honeyPot;
    }
}
