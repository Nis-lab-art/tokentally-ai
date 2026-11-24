package tokentally.cost;

// nishan
// what the user expects to send to a model
public class Workload {

    public long inputPerDay;          // input tokens per day
    public long outputPerDay;         // output tokens per day
    public double cachedShare = 0;    // part of the input that hits the cache, 0 to 1
    public boolean batchOk = false;   // fine to wait for batch or flex results
    public long promptSize = 0;       // tokens in one request, for long context prices
    public long minContext = 0;       // smallest context window the user accepts
    public double budget = -1;        // most the user wants to pay per month, -1 for no limit
    public String provider = "";      // only this provider, empty for all
    public boolean skipFree = false;  // leave out free models
    public int days = 30;             // days in a month
}
