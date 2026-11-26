package tokentally.cost;

// owner: Nishan Giri
// user workload
public class Workload {

    public long inputPerDay;          // tokens per day
    public long outputPerDay;         // tokens per day
    public double cachedShare = 0;    // 0 to 1
    public boolean batchOk = false;   // batch or flex ok
    public long promptSize = 0;       // tokens per request
    public long minContext = 0;       // min context
    public double budget = -1;        // per month, -1 none
    public String provider = "";      // empty = all
    public boolean skipFree = false;  // hide free
    public int days = 30;             // per month
}
