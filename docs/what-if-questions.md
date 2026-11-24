# what if questions (group and individual)

The presentation is 10 minutes. After that the group gets questions, then every member is asked about their own part. Many questions will be "if we do this, what happens here and there". Every answer below was run on the real code. You can show each one live.

Show it live in the menu (works without spring boot):

```
cd core
./run.sh data nishan      # only Nishan's options, same for abhinav, shrijana, aakriti, khem
```

Option 18 runs the bad input cases, option 19 runs the measured experiments. In the web page open the tabs Error handling and Experiments.

## group questions

| question | answer | where to show it |
|---|---|---|
| What if the internet is down? | The saved crawl in `core/data` is used, everything except a new crawl works. A crawl that cannot connect tries twice, logs "no connection" and moves on. | menu option 18, case "server that is not there" |
| What if a provider changes its page? | The parser may find no prices. The crawl then keeps the old saved data for that provider and says so in the log. The data check shows prices that look wrong. | `CrawlAll`, menu option 11 |
| What if prices.csv is damaged? | Damaged rows are skipped and counted, the good rows still load. | option 18, case "damaged prices.csv" |
| What if there is no saved data and no internet? | The program stops with one clear message that says to check the connection and the data folder. | `Engine.load` |
| What if two people use the page at the same time? | Search counts are saved with a lock, 200 searches from 20 threads give exactly 200. The crawl button is also one at a time. | option 18, case "two searches at the same time" |
| What if someone sends something strange to the api? | A wrong value gives a 400 with a plain sentence (and the field name so the page can mark it). Anything unexpected gives a short 500 message and the details stay in the server log. No stack trace reaches the user. | page: Error handling tab, "Try to break it yourself" |
| What if a price is missing on a page? | It is stored as -1 and shown as a dash. The cost calculator never guesses a context window. | Models tab |
| What if the data grows 100 times? | The index lookup and trie stay fast (hash map and tree walk). Sorting and the heap are O(n log n). See the experiments for the measured numbers. | Experiments tab |
| Do your algorithms really matter? | Index against reading all pages is thousands of times faster, trie against a scan is about 30 times faster, Boyer-Moore checks about 4.6 times fewer characters than KMP. For a heap against a sort the difference is small at our size and we say so. | Experiments tab |

## Nishan Giri, spell checking and cost calculator

| if we do this | what happens |
|---|---|
| spell check an empty word or null | no suggestions, no crash |
| spell check a nonsense word (qzxqzxqzx) | nothing is within 2 edits, the page says nothing close was found |
| spell check a 1000 letter word | answered at once, words with a very different length are skipped before the dp table |
| search gemnii | suggests gemini, genie, genai and searches again with gemini |
| enter "2 million" as tokens | rejected, the message says to use 2M, 500K or 1,000,000 |
| enter 999999999999B | rejected as too big to be a real workload (limit 1000 trillion) |
| cached share 150 percent | rejected, must be 0 to 100 |
| budget of $0.01 a month | no model fits, the page says to raise the budget |
| minimum context 100M tokens | no model fits, models with an unknown context are not guessed |
| 0 input and 0 output | rejected, a workload of zero costs nothing |
| Why edit distance? | It finds the closest known word to a typo, dp table O(a x b), the heap keeps the best few |

## Abhinav Pandey, inverted index and frequency count

| if we do this | what happens |
|---|---|
| look up a word that is on no page | empty result |
| index a page with empty text, a null page or null text | skipped, no crash |
| search a phrase that is on no page | no page has the phrase |
| frequency count of an empty word | rejected, "type a word to count" |
| frequency count of @@@ | rejected by the validator |
| frequency count on a page we never crawled | rejected, "no crawled page with that url" |
| frequency of a word that is not on the page | 0 from both algorithms |
| do Boyer-Moore and KMP always agree | yes, a test compares them with a slow count and the demo checks 3 words |
| which is faster | on the biggest page the times are close. Boyer-Moore checks about 4.6 times fewer characters in total because it jumps ahead |

## Shrijana Karki, page ranking and data validation

| if we do this | what happens |
|---|---|
| rank a blank search or null | no ranking |
| the same keyword 200 times | counted once, the score stays the same |
| ask for zero results | empty list |
| search text with symbols like script tags | rejected, letters digits spaces dots and dashes only |
| search text over 60 characters | rejected |
| date 2026-02-30 | rejected, 30 February is not a real date |
| money $12.345 | rejected, budget should look like 50 or $49.99 |
| a crawled price that looks wrong | flagged in the data check, for example cached price above the input price or a price over $1000 |
| how are ties ranked | the page with more different keywords wins, then the url |

## Aakriti Mainali, word completion and finding patterns

| if we do this | what happens |
|---|---|
| empty or null prefix | no suggestions until the user types |
| prefix nothing starts with | empty list |
| capital letters (CLAUDE-OP) | still completes, case is ignored |
| store null, empty or count 0 in the trie | ignored |
| patterns on empty or null text | all six groups are empty, no crash |
| patterns on a url that does not exist | nothing found |
| trie against a plain scan | the trie is about 30 times faster on our words because it only walks the prefix |

## Khem Raj Adhikari, search frequency and html cleaning

| if we do this | what happens |
|---|---|
| the history file has broken lines | those lines are skipped, the good ones load |
| blank or null search | not counted, nothing saved |
| 20 threads search at once | the count is exact, nothing is lost (the methods are synchronized) |
| html that is never closed | the text is still kept |
| a page that is only scripts | nothing is left, no crash |
| null or blank html | empty text |
| a 5 MB page | cleaned in about half a second, one pass O(n) |
| why save the file after every search | the history survives a crash. It costs about 100 microseconds per search |

## questions we should ask ourselves

1. What is the weakest part of our project? (scraping depends on the page layout, page ranking only counts keywords, price history needs more crawls over time)
2. What would we change with more time? (link based ranking, more providers, a price alert)
3. Which of our choices was a trade off? (saving the history file on every search, standard tier only in the calculator)
