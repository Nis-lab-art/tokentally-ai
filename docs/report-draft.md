# TokenTally AI final report (draft)

COMP 8547 Advanced Computing Concepts, Fall 2026, Group 10

## 1 team

| member | provider crawled | features | main files |
|---|---|---|---|
| Nishan Giri | Anthropic Claude | spell checking, cost calculator | AnthropicCrawler, AnthropicParser, SpellChecker, EditDistance, Workload, CostCalculator, Recommender |
| Abhinav Pandey | OpenAI | inverted index, frequency count | OpenAICrawler, OpenAIParser, InvertedIndex, FrequencyCounter, BoyerMoore, KMPMatcher |
| Shrijana Karki | Google Gemini | page ranking, data validation | GeminiCrawler, GeminiParser, PageRanker, InputValidator |
| Aakriti Mainali | Z.ai | word completion, pattern finding | ZaiCrawler, ZaiParser, Trie, WordCompleter, PatternExtractor |
| Khem Raj Adhikari | xAI Grok | search frequency, html cleaning | XaiCrawler, XaiParser, HtmlCleaner, SearchFrequency |

## 2 what the program does

TokenTally AI crawls the official pricing pages of five ai providers and turns every model price into one format. The user can search the pages, compare models and work out the monthly cost of a workload. Last crawl found 81 pages, 194 prices and 4713 index words.

## 3 how it is built

crawler (queue + hash set) -> html cleaner -> parsers -> data store -> features (index, search, rank, cost) -> menu or api and web ui.
The core uses only the jdk. The api (spring boot) and ui (react) are only a front for the same core.

Figure 1: architecture diagram (todo)

## 4 features, one section each

For every feature write: owner, data structure, how it works, time complexity, error handling, figure.

| feature | structure | complexity to explain | figure |
|---|---|---|---|
| crawler | queue, hash set | each page visited once, O(pages + links) | 2 crawl log |
| inverted index | hash map word to url to positions | build O(words), lookup O(1) average | 3 |
| frequency count | boyer moore and kmp | kmp O(n+m), boyer moore best case O(n/m) | 4 |
| spell checking | edit distance dp table, heap | O(a*b) per word | 5 |
| word completion | trie with counts, heap | O(prefix + subtree) | 6 |
| page ranking | max heap | O(n log n) | 7 |
| search frequency | hash map saved to file, tree map | O(1) update | 8 |
| data validation and patterns | regex | linear scan of the text | 9 |
| cost calculator | min heap on monthly cost | O(n log n) | 10 |
| html cleaning | single pass over the page | O(n) | 11 |

## 5 testing

Own small test runner, 123 checks, all pass (`core/test.sh`). Add figure 12 with the test output.

## 6 extra feature

Scheduled email report (not built yet).

## 7 team work

Table of who wrote and who explained which files. Say clearly that each member reviewed their own files.

## 8 references

- official pricing pages of OpenAI, Google Gemini, Anthropic, Z.ai and xAI (access dates)
- course notes for edit distance, kmp, boyer moore, tries and heaps

## screenshot checklist (number every figure)

menu, crawl log, search with spell suggestion, ranked pages, frequency count, word completion, regex patterns, validation error, calculator result, compare, insights, tests passing.
