package ws.palladian.helper.collection;

public enum ComparisonType {
    LESS("<"), LESS_EQUALS("<="), MORE(">"), MORE_EQUALS(">="), EQUALS("=");

    private String operator;

    ComparisonType(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public static ComparisonType getByOperator(String operator) {
        for (ComparisonType comparisonType : values()) {
            if (comparisonType.getOperator().equals(operator)) {
                return comparisonType;
            }
        }

        return null;
    }
}
