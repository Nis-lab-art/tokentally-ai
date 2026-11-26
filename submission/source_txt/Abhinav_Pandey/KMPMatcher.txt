package tokentally.text;

// owner: Abhinav Pandey
// kmp search
public class KMPMatcher {

    private final String word;
    private final int[] lps;
    public long comparisons;

    public KMPMatcher(String word) {
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("the word to search is empty");
        }
        this.word = word.toLowerCase();
        lps = new int[this.word.length()];
        int len = 0;
        int i = 1;
        while (i < this.word.length()) {
            if (this.word.charAt(i) == this.word.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
    }

    // lowercase text
    public int count(String text) {
        int m = word.length();
        int found = 0;
        comparisons = 0;
        if (m == 0) {
            return 0;
        }
        int j = 0;
        for (int i = 0; i < text.length(); i++) {
            comparisons++;
            while (j > 0 && text.charAt(i) != word.charAt(j)) {
                j = lps[j - 1];
                comparisons++;
            }
            if (text.charAt(i) == word.charAt(j)) {
                j++;
            }
            if (j == m) {
                if (WholeWord.at(text, i - m + 1, m)) {
                    found++;
                }
                j = lps[j - 1];
            }
        }
        return found;
    }
}
