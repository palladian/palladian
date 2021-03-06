package ws.palladian.retrieval.facebook;

import java.util.Date;

public class Value {
    private final Object value;
    private final Date endTime;

    Value(Object value, Date endTime) {
        this.value = value;
        this.endTime = endTime;
    }

    public Object getValue() {
        return value;
    }

    /**
     * @return The end time, or <code>null</code> if not specified.
     */
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return "Value [value=" + value + ", endTime=" + endTime + "]";
    }
}