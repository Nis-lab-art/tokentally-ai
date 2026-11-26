package tokentally.text;

// owner: Abhinav Pandey
// whole word check
public class WholeWord {

    public static boolean at(String text, int start, int length) {
        if (start > 0 && Tokenizer.isLetterOrDigit(text.charAt(start - 1))) {
            return false;
        }
        int end = start + length;
        return end >= text.length() || !Tokenizer.isLetterOrDigit(text.charAt(end));
    }
}
