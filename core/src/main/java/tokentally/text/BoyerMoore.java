package tokentally.text;

// owner: Abhinav Pandey
// boyer moore search
public class BoyerMoore {

    private final String word;
    private final int[] lastSeen = new int[65536];
    private final int[] suffixJump;
    public long comparisons; // chars compared

    public BoyerMoore(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("the word to search is empty");
        }
        this.word = word.toLowerCase();
        int m = this.word.length();

        // bad character table
        for (int i = 0; i < lastSeen.length; i++) {
            lastSeen[i] = -1;
        }
        for (int i = 0; i < m; i++) {
            lastSeen[this.word.charAt(i)] = i;
        }

        // good suffix table
        suffixJump = new int[m + 1];
        for (int j = -1; j < m; j++) {
            for (int jump = 1; jump <= m; jump++) {
                boolean fits = true;
                for (int i = j + 1; i < m && fits; i++) {
                    if (i - jump >= 0 && this.word.charAt(i - jump) != this.word.charAt(i)) {
                        fits = false;
                    }
                }
                if (fits && j >= 0 && j - jump >= 0 && this.word.charAt(j - jump) == this.word.charAt(j)) {
                    fits = false;
                }
                if (fits) {
                    suffixJump[j + 1] = jump;
                    break;
                }
            }
        }
    }

    // lowercase text
    public int count(String text) {
        int m = word.length();
        int n = text.length();
        int found = 0;
        comparisons = 0;
        if (m == 0) {
            return 0;
        }
        int s = 0;
        while (s <= n - m) {
            int j = m - 1;
            while (j >= 0) {
                comparisons++;
                if (word.charAt(j) != text.charAt(s + j)) {
                    break;
                }
                j--;
            }
            if (j < 0) {
                if (WholeWord.at(text, s, m)) {
                    found++;
                }
                s += suffixJump[0];
            } else {
                int bad = j - lastSeen[text.charAt(s + j)];
                s += Math.max(bad, suffixJump[j + 1]);
            }
        }
        return found;
    }
}
