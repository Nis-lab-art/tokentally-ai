package tokentally.parse;

import java.util.ArrayList;

import tokentally.model.ModelPrice;

// owner: Shrijana Karki
// parser interface
public interface PriceParser {

    ArrayList<ModelPrice> parse(String html, String url);
}
