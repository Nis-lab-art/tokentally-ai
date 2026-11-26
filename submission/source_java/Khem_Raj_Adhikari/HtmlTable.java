package tokentally.parse;

import java.util.ArrayList;

// owner: Khem Raj Adhikari
// one html table
public class HtmlTable {

    public String heading = "";
    public String tab = "";
    public ArrayList<ArrayList<String>> rows = new ArrayList<>();

    // header column, -1 if none
    public int column(String name) {
        for (ArrayList<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (row.get(i).equalsIgnoreCase(name)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean hasHeader(String name) {
        return column(name) >= 0;
    }
}
