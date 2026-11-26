package tokentally.text;

// owner: Nishan Giri
// edit distance, dp table
public class EditDistance {

    public static int between(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int replace = d[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                int delete = d[i - 1][j] + 1;
                int insert = d[i][j - 1] + 1;
                d[i][j] = Math.min(replace, Math.min(delete, insert));
            }
        }
        return d[a.length()][b.length()];
    }
}
