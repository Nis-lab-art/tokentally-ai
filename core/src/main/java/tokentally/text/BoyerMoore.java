package tokentally.text;

// abhinav
// boyer moore search that counts whole word matches
// compares from the right end and jumps with the bad character rule or the good suffix rule
public class BoyerMoore {

    private final String word;
    private final int[] lastSeen = new int[65536];
    private final int[] suffixJump;
    public long comparisons; // how many char checks the last count did

    public BoyerMoore(String word) {
        this.word = word.toLowerCase();
        int m = this.word.length();

        // bad character table: last place of each char in the word
        for (int i = 0; i < lastSeen.length; i++) {
            lastSeen[i] = -1;
        }
        for (int i = 0; i < m; i++) {
            lastSeen[this.word.charAt(i)] = i;
        }

        // good suffix table: jump for a mismatch at j, suffixJump[0] is for a full match
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

    // text should already be lowercase
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
