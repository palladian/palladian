package ws.palladian.helper.conversion;

public class AmountUnit {

    private Double amount;
    private String unit;

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCombined() {
        return amount + " " + unit;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AmountUnit [");
        if (amount != null) {
            builder.append("amount=");
            builder.append(amount);
            builder.append(", ");
        }
        if (unit != null) {
            builder.append("unit=");
            builder.append(unit);
        }
        builder.append("]");
        return builder.toString();
    }

}
