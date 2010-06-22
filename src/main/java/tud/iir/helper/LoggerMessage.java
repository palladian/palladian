package tud.iir.helper;

/**
 * The LoggerMessage is a message that is sent from the Logger to its observers.
 * 
 * @author David Urbansky
 */
public class LoggerMessage {
    private String loggerName = "";
    private String message = "";

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessage() {
        return getMessage(true);
    }

    public String getMessage(boolean addBreak) {
        if (addBreak)
            return message + "\n";
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
