package tokentally.cost;

import tokentally.model.ModelPrice;

// owner: Nishan Giri
// monthly cost
public class CostCalculator {

    public static class Breakdown {
        public double input;
        public double cached;
        public double output;
        public double total;
        public boolean longPrices;
    }

    public static Breakdown monthly(ModelPrice p, Workload w) {
        if (!p.hasTokenPrice()) {
            throw new IllegalArgumentException(p.model + " has no token price");
        }
        if (w.inputPerDay < 0 || w.outputPerDay < 0 || w.cachedShare < 0 || w.cachedShare > 1) {
            throw new IllegalArgumentException("token amounts must be positive and the cached share between 0 and 1");
        }

        // long context prices
        boolean useLong = p.longThreshold > 0 && w.promptSize >= p.longThreshold && p.longInput >= 0;
        double inPrice = useLong ? p.longInput : p.input;
        double outPrice = useLong && p.longOutput >= 0 ? p.longOutput : p.output;
        double cachedPrice = useLong ? p.longCachedInput : p.cachedInput;
        if (cachedPrice < 0) {
            cachedPrice = inPrice; // no cache price, use input
        }

        double cachedTokens = w.inputPerDay * w.cachedShare;
        double uncachedTokens = w.inputPerDay - cachedTokens;

        Breakdown b = new Breakdown();
        b.longPrices = useLong;
        b.input = w.days * uncachedTokens * inPrice / 1_000_000.0;
        b.cached = w.days * cachedTokens * cachedPrice / 1_000_000.0;
        b.output = w.days * w.outputPerDay * outPrice / 1_000_000.0;
        b.total = b.input + b.cached + b.output;
        return b;
    }
}
