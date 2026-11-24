package tokentally.text;

import java.util.ArrayList;

// splits text into lowercase words one char at a time
// dot dash and underscore stay inside a word so gpt-6.1-sol stays one word
public class Tokenizer {

    public static ArrayList<String> words(String text) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder w = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            char c = i < text.length() ? Character.toLowerCase(text.charAt(i)) : ' ';
            boolean letter = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            boolean joiner = c == '-' || c == '.' || c == '_';
            if (letter || (joiner && w.length() > 0)) {
                w.append(c);
            } else if (w.length() > 0) {
                // drop joiners at the end like "tokens." -> tokens
                int end = w.length();
                while (end > 0 && !isLetterOrDigit(w.charAt(end - 1))) {
                    end--;
                }
                if (end > 0) {
                    out.add(w.substring(0, end));
                }
                w.setLength(0);
            }
        }
        return out;
    }

    public static boolean isLetterOrDigit(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    // true if the word has a letter, so prices like 2.50 are not words
    public static boolean hasLetter(String w) {
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            if (c >= 'a' && c <= 'z') {
                return true;
            }
        }
        return false;
    }
}
