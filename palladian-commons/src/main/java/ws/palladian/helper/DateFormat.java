package ws.palladian.helper;

public final class DateFormat {
    
    private final String regExp;
    private final String format;
    
    DateFormat(String regExp, String format) {
        this.regExp = regExp;
        this.format = format;
    }

    /**
     * @return the regExp
     */
    public String getRegExp() {
        return regExp;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

}
