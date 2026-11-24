package tokentally.model;

// one model on one tier with its prices in usd per 1 million tokens
// a price of -1 means the page did not list it
public class ModelPrice {

    public String provider;
    public String model;
    public String tier;
    public double input = -1;
    public double cachedInput = -1;
    public double output = -1;
    public double longInput = -1;
    public double longCachedInput = -1;
    public double longOutput = -1;
    public long longThreshold = -1; // prompt size where long prices start
    public long contextWindow = -1;
    public long maxOutput = -1;
    public String notes = "";
    public String sourceUrl = "";

    public ModelPrice(String provider, String model, String tier) {
        this.provider = provider;
        this.model = model;
        this.tier = tier;
    }

    public boolean hasTokenPrice() {
        return input >= 0 && output >= 0;
    }

    public boolean isFree() {
        return input == 0 && output == 0;
    }

    public String key() {
        return provider + "|" + model + "|" + tier;
    }

    public String toString() {
        return provider + " " + model + " " + tier + " in " + money(input) + " out " + money(output);
    }

    public static String money(double v) {
        if (v < 0) {
            return "-";
        }
        if (v == 0) {
            return "free";
        }
        String s = String.valueOf(Math.round(v * 10000) / 10000.0);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return "$" + s;
    }
}
