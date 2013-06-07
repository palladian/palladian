/**
 * Created on: 12.03.2013 17:18:43
 */
package ws.palladian.processing.features.utils;

public class FeatureDescriptor {
    private final String qualifier;
    private final String identifier;

    public FeatureDescriptor(String qualifier, String identifier) {
        this.qualifier = qualifier;
        this.identifier = identifier;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(qualifier);
        builder.append(":");
        builder.append(identifier);
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureDescriptor other = (FeatureDescriptor)obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (qualifier == null) {
            if (other.qualifier != null)
                return false;
        } else if (!qualifier.equals(other.qualifier))
            return false;
        return true;
    }
}