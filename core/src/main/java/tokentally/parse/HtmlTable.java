package tokentally.parse;

import java.util.ArrayList;

// one table from a page with the heading above it and the tab it sits in
public class HtmlTable {

    public String heading = "";
    public String tab = "";
    public ArrayList<ArrayList<String>> rows = new ArrayList<>();

    // index of the first header cell that has this text or -1
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
