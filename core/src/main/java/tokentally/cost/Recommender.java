package tokentally.cost;

import java.util.ArrayList;
import java.util.HashMap;

import tokentally.model.ModelPrice;
import tokentally.util.Heap;

// owner: Nishan Giri
// cheapest models, min heap
public class Recommender {

    public static class Pick {
        public ModelPrice price;
        public CostCalculator.Breakdown cost;
        public String why;
    }

    public static ArrayList<Pick> recommend(ArrayList<ModelPrice> prices, Workload w, int howMany) {
        // cheapest tier per model
        HashMap<String, Pick> bestPerModel = new HashMap<>();
        for (ModelPrice p : prices) {
            if (!p.hasTokenPrice() || !tierAllowed(p.tier, w.batchOk)) {
                continue;
            }
            if (w.skipFree && p.isFree()) {
                continue;
            }
            if (!w.provider.isEmpty() && !p.provider.equals(w.provider)) {
                continue;
            }
            // context must fit
            if (w.minContext > 0 && (p.contextWindow < 0 || p.contextWindow < w.minContext)) {
                continue;
            }
            if (w.minContext > 0 && p.contextWindow > 0 && w.promptSize > p.contextWindow) {
                continue;
            }
            Pick pick = new Pick();
            pick.price = p;
            pick.cost = CostCalculator.monthly(p, w);
            if (w.budget >= 0 && pick.cost.total > w.budget) {
                continue;
            }
            pick.why = reason(p, w);
            String key = p.provider + "|" + p.model;
            Pick old = bestPerModel.get(key);
            if (old == null || pick.cost.total < old.cost.total) {
                bestPerModel.put(key, pick);
            }
        }

        Heap<Pick> minHeap = new Heap<>((a, b) -> {
            int c = Double.compare(a.cost.total, b.cost.total);
            return c != 0 ? c : a.price.model.compareTo(b.price.model);
        });
        for (Pick p : bestPerModel.values()) {
            minHeap.push(p);
        }
        ArrayList<Pick> out = new ArrayList<>();
        while (!minHeap.isEmpty() && out.size() < howMany) {
            out.add(minHeap.pop());
        }
        return out;
    }

    // batch and flex if allowed
    static boolean tierAllowed(String tier, boolean batchOk) {
        if (tier.equals("standard")) {
            return true;
        }
        return batchOk && (tier.equals("batch") || tier.equals("flex"));
    }

    private static String reason(ModelPrice p, Workload w) {
        StringBuilder r = new StringBuilder(p.tier);
        if (p.isFree()) {
            r.append(", free");
        }
        if (p.contextWindow > 0) {
            r.append(", context ").append(p.contextWindow / 1000).append("k");
        } else if (w.minContext > 0) {
            r.append(", context not listed");
        }
        if (w.cachedShare > 0 && p.cachedInput < 0) {
            r.append(", no cache price");
        }
        return r.toString();
    }
}
