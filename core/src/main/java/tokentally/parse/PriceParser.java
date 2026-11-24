package tokentally.parse;

import java.util.ArrayList;

import tokentally.model.ModelPrice;

// every provider parser turns its pricing page html into model prices
public interface PriceParser {

    ArrayList<ModelPrice> parse(String html, String url);
}
