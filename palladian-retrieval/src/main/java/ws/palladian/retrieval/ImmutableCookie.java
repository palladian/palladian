package ws.palladian.retrieval;

import org.apache.commons.lang3.Validate;

public final class ImmutableCookie implements Cookie {

    private final String name;

    private final String value;

    private final String domain;

    private final String path;

    public ImmutableCookie(String name, String value, String domain, String path) {
        Validate.notNull(name, "name must not be null");
        Validate.notNull(value, "value must not be null");
        Validate.notNull(domain, "domain must not be null");
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + domain.hashCode();
        result = prime * result + name.hashCode();
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ImmutableCookie other = (ImmutableCookie)obj;
        if (!domain.equals(other.domain))
            return false;
        if (!name.equals(other.name))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        return true;
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
