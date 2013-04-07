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
}
