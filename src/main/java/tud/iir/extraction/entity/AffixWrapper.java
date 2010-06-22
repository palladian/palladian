package tud.iir.extraction.entity;

/**
 * A wrapper candidate is one prefix and one suffix with information about the maximum length.
 * 
 * @author David Urbansky
 */
class AffixWrapper {
    String prefix = "";
    String suffix = "";
    int prefixLength = 0;
    int suffixLength = 0;

    public AffixWrapper(String prefix, String suffix) {
        super();
        this.prefix = prefix;
        this.suffix = suffix;
        this.prefixLength = this.prefix.length();
        this.suffixLength = this.suffix.length();
    }

    public String getPrefix() {
        return getPrefix(true);
    }

    public String getPrefix(boolean trimmedToLength) {
        if (trimmedToLength)
            return prefix.substring(prefix.length() - prefixLength);
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return getSuffix(true);
    }

    public String getSuffix(boolean trimmedToLength) {
        if (trimmedToLength)
            return suffix.substring(0, suffixLength);
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public void setPrefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
    }

    public int getSuffixLength() {
        return suffixLength;
    }

    public void setSuffixLength(int suffixLength) {
        this.suffixLength = suffixLength;
    }

    public boolean isEmpty() {
        if (getPrefixLength() == 0 && getSuffixLength() == 0)
            return true;
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return ((AffixWrapper) obj).getPrefix().equals(this.getPrefix()) && ((AffixWrapper) obj).getSuffix().equals(this.getSuffix());
    }
}