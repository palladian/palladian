package ws.palladian.retrieval;

public class ImmutableCookie implements Cookie {

    private final String name;

    private final String value;

    private final String domain;

    private final String path;

    public ImmutableCookie(String name, String value, String domain, String path) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ImmutableCookie [name=");
        builder.append(name);
        builder.append(", value=");
        builder.append(value);
        builder.append(", domain=");
        builder.append(domain);
        builder.append(", path=");
        builder.append(path);
        builder.append("]");
        return builder.toString();
    }

}
