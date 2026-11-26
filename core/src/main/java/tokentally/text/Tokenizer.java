package tokentally.text;

import java.util.ArrayList;

// owner: Abhinav Pandey
// splits text into words
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
                // drop ending . - _
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

    // has a letter
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
