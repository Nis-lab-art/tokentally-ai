package tokentally.parse;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// small helpers to read prices and token sizes out of table cells
public class Prices {

    private static final Pattern DOLLAR = Pattern.compile("\\$\\s?([0-9][0-9,]*(?:\\.[0-9]+)?)");
    private static final Pattern SIZE = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*([kKmM])?");

    // first dollar amount in the cell, 0 for free and -1 when there is none
    public static double first(String cell) {
        if (cell == null) {
            return -1;
        }
        String c = cell.trim().toLowerCase();
        if (c.startsWith("free")) {
            return 0;
        }
        Matcher m = DOLLAR.matcher(cell);
        return m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : -1;
    }

    // every dollar amount in the cell in order
    public static ArrayList<Double> all(String cell) {
        ArrayList<Double> list = new ArrayList<>();
        Matcher m = DOLLAR.matcher(cell);
        while (m.find()) {
            list.add(Double.parseDouble(m.group(1).replace(",", "")));
        }
        return list;
    }

    // 500k -> 500000, 1M -> 1000000, 1,050,000 -> 1050000, -1 if no number
    public static long tokens(String text) {
        if (text == null) {
            return -1;
        }
        Matcher m = SIZE.matcher(text.trim());
        if (!m.find()) {
            return -1;
        }
        double n = Double.parseDouble(m.group(1).replace(",", ""));
        String unit = m.group(2) == null ? "" : m.group(2).toLowerCase();
        if (unit.equals("k")) {
            n = n * 1000;
        } else if (unit.equals("m")) {
            n = n * 1000000;
        }
        return Math.round(n);
    }

    // first word of a cell like "grok-4.7 Long context" -> grok-4.7
    public static String firstWord(String cell) {
        String t = cell.trim();
        int sp = t.indexOf(' ');
        return (sp < 0 ? t : t.substring(0, sp)).toLowerCase();
    }
}
