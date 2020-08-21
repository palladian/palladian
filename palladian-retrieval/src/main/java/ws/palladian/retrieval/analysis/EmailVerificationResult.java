package ws.palladian.retrieval.analysis;

import ws.palladian.retrieval.parser.json.JsonObject;

public class EmailVerificationResult {
    private enum VerfificationResult {
        VALID, INVALID, UNKNOWN
    }

    private VerfificationResult result;
    private String reason;
    private boolean disposable;
    private boolean acceptAll;
    private boolean free;
    private String user;
    private String domain;
    private boolean safeToSend;
    private String message;

    public EmailVerificationResult(JsonObject response) {
        setResult(VerfificationResult.valueOf(response.tryGetString("result").toUpperCase()));
        setReason(response.tryGetString("reason"));
        setDisposable(response.tryGetBoolean("disposable"));
        setFree(response.tryGetBoolean("free"));
        setSafeToSend(response.tryGetBoolean("safe_to_send"));
        setUser(response.tryGetString("user"));
        setDomain(response.tryGetString("domain"));
        setMessage(response.tryGetString("message"));
    }

    public VerfificationResult getResult() {
        return result;
    }

    public void setResult(VerfificationResult result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isDisposable() {
        return disposable;
    }

    public void setDisposable(boolean disposable) {
        this.disposable = disposable;
    }

    public boolean isAcceptAll() {
        return acceptAll;
    }

    public void setAcceptAll(boolean acceptAll) {
        this.acceptAll = acceptAll;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public boolean isSafeToSend() {
        return safeToSend;
    }

    public void setSafeToSend(boolean safeToSend) {
        this.safeToSend = safeToSend;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "EmailVerificationResult{" +
                "result=" + result +
                ", reason='" + reason + '\'' +
                ", disposable=" + disposable +
                ", acceptAll=" + acceptAll +
                ", free=" + free +
                ", user='" + user + '\'' +
                ", domain='" + domain + '\'' +
                ", safeToSend=" + safeToSend +
                ", message='" + message + '\'' +
                '}';
    }
}
