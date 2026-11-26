package tokentally.text;

import java.util.ArrayList;

import tokentally.model.Page;

// owner: Abhinav Pandey
// word count on a page
public class FrequencyCounter {

    public static class Result {
        public String url;
        public int boyerMoore;
        public int kmp;
        public long bmComparisons;
        public long kmpComparisons;
    }

    public static Result count(String word, Page page) {
        if (word == null || word.isBlank()) {
            throw new IllegalArgumentException("type a word to count");
        }
        if (page == null) {
            throw new IllegalArgumentException("no page to count in");
        }
        String text = page.text == null ? "" : page.text.toLowerCase();
        BoyerMoore bm = new BoyerMoore(word);
        KMPMatcher kmp = new KMPMatcher(word);
        Result r = new Result();
        r.url = page.url;
        r.boyerMoore = bm.count(text);
        r.bmComparisons = bm.comparisons;
        r.kmp = kmp.count(text);
        r.kmpComparisons = kmp.comparisons;
        return r;
    }

    // pages with the word
    public static ArrayList<Result> countAll(String word, ArrayList<Page> pages) {
        ArrayList<Result> list = new ArrayList<>();
        for (Page p : pages) {
            Result r = count(word, p);
            if (r.boyerMoore > 0) {
                list.add(r);
            }
        }
        return list;
    }
}
