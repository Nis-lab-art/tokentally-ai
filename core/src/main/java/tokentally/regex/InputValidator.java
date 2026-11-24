package tokentally.regex;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tokentally.model.ModelPrice;

// shrijana
// checks user input with regular expressions before we use it
// and checks crawled prices that look wrong
public class InputValidator {

    // 2M, 1.5m, 500K, 500k, 1,000,000 or 750000 with an optional "tokens"
    private static final Pattern TOKENS = Pattern.compile("^\\s*(\\d{1,3}(,\\d{3})+|\\d+(\\.\\d+)?)\\s*([kKmMbB])?\\s*(tokens?)?\\s*$");
    // $50, 50, 49.99, $1,200.50
    private static final Pattern MONEY = Pattern.compile("^\\s*\\$?\\s*(\\d{1,3}(,\\d{3})+|\\d+)(\\.\\d{1,2})?\\s*$");
    private static final Pattern DATE = Pattern.compile("^(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$");
    // gpt-6.1-sol, claude-opus-5-5, gemini-2.5-flash, glm-4.5, grok-4.20-0309-reasoning
    private static final Pattern MODEL = Pattern.compile("^[a-z][a-z0-9]*(?:[-.][a-z0-9]+)*$");
    private static final Pattern WORD = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._\\- ]{0,60}$");
    private static final Pattern PERCENT = Pattern.compile("^\\s*(100|\\d{1,2})(\\.\\d+)?\\s*%?\\s*$");

    private static final double MAX_TOKENS = 1e15;

    // token amount to a number, or -1 when the text is not a token amount
    public static long tokens(String text) {
        if (text == null) {
            return -1;
        }
        Matcher m = TOKENS.matcher(text);
        if (!m.matches()) {
            return -1;
        }
        double n = Double.parseDouble(m.group(1).replace(",", ""));
        String unit = m.group(4) == null ? "" : m.group(4).toLowerCase();
        if (unit.equals("k")) {
            n *= 1_000;
        } else if (unit.equals("m")) {
            n *= 1_000_000;
        } else if (unit.equals("b")) {
            n *= 1_000_000_000;
        }
        if (n > MAX_TOKENS) {
            return -1; // a number like 999999999999B is a typo, not a workload
        }
        return Math.round(n);
    }

    public static double money(String text) {
        if (text == null || !MONEY.matcher(text).matches()) {
            return -1;
        }
        return Double.parseDouble(text.replace("$", "").replace(",", "").trim());
    }

    // a real date in yyyy-mm-dd, so 2026-02-30 is rejected too
    public static boolean date(String text) {
        if (text == null || !DATE.matcher(text.trim()).matches()) {
            return false;
        }
        try {
            LocalDate.parse(text.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean modelName(String text) {
        return text != null && MODEL.matcher(text.trim().toLowerCase()).matches();
    }

    public static boolean searchWord(String text) {
        return text != null && WORD.matcher(text.trim()).matches();
    }

    // 0 to 100 percent as a fraction, -1 if wrong
    public static double percent(String text) {
        if (text == null || !PERCENT.matcher(text).matches()) {
            return -1;
        }
        return Double.parseDouble(text.replace("%", "").trim()) / 100.0;
    }

    // menu choice between 1 and max, -1 if wrong
    public static int choice(String text, int max) {
        if (text == null || !text.trim().matches("\\d{1,2}")) {
            return -1;
        }
        int n = Integer.parseInt(text.trim());
        return n >= 1 && n <= max ? n : -1;
    }

    // things in crawled prices that mean the parser read something wrong
    public static ArrayList<String> problems(ModelPrice p) {
        ArrayList<String> out = new ArrayList<>();
        if (!modelName(p.model)) {
            out.add("odd model name " + p.model);
        }
        if (p.input < 0 && p.output < 0) {
            out.add("no input or output price");
        }
        if (p.cachedInput > 0 && p.input >= 0 && p.cachedInput > p.input) {
            out.add("cached input is more than input");
        }
        if (p.longInput >= 0 && p.input >= 0 && p.longInput < p.input) {
            out.add("long context input is cheaper than short");
        }
        if (p.input > 1000 || p.output > 1000) {
            out.add("price over $1000 per million tokens");
        }
        return out;
    }
}
