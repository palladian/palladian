package ws.palladian.helper.conversion;

import java.util.Optional;

public class AmountUnit {
    private float amount = -1;
    private String unit;

    public Double getAmount() {
        return amount < 0 ? null : (double) amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setAmount(Double amount) {
        this.amount = Optional.ofNullable(amount).orElse(-1d).floatValue();
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
        if (amount > -1) {
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
