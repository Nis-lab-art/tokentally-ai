# honest critique and likely questions

Read this before the presentation. Every member should be able to answer the questions in their own area. Say the weak points yourself before the marker finds them.

## part 1 faults in the idea and design

| # | fault | why it matters | what we say or do |
|---|---|---|---|
| 1 | we scrape html, not an api | the parsers depend on the page layout, a redesign breaks them | we keep saved pages for the demo, a sanity check flags odd prices (insights tab), robots.txt is respected. next step would be a diff of old and new prices |
| 2 | "cheapest" is only about price | ignores quality, speed, rate limits, tool fees, reasoning tokens, image and audio prices | say it is a cost estimate for text tokens, not a quality ranking |
| 3 | free models always win the list | glm flash models show $0.00 at the top | fixed: the calculator has a "hide free models" box |
| 4 | missing context windows | some Z.ai, old Gemini and Claude models show a dash because the pages were not crawled or do not list it. the recommender drops them when a minimum context is set | we show a dash instead of guessing a number |
| 5 | page ranking is keyword count, not link based | people may expect PageRank. ours scores by keyword count (tie = more distinct keywords) using a max heap | say this clearly. a link graph rank is possible future work |
| 6 | small data set | 81 pages and 4713 words is a tiny search engine | the goal is the algorithms on real data, not scale. say the complexity so it is clear it would scale |
| 7 | price history is new and has little data | the log starts on the day of the first run, so no changes show until a later crawl | explain the design (append only log, only changes are written) and crawl again before the demo if possible |
| 8 | crawl button is open to anyone | the api has no login, any visitor can start a 40 second crawl | fine for a class demo, would need auth and a rate limit for real use |
| 9 | thread safety not checked | search history and crawl are shared state in one spring app. we did not test many users at once | single user demo, say so |
| 10 | tests are mostly on saved data | 123 checks on saved pages, none on a live crawl or the page in a browser | say what the tests cover. the ui was only checked by hand |
| 11 | tailwind is a 400 KB script file | it is the play version, fine for a demo, a real build would compile only the used css | say so if asked |
| 12 | email report dropped | we chose price history instead because it needs no passwords or mail server | say that choice clearly |
| 13 | cost formula is simple | days x (uncached x in + cached x cached + out x out) / 1M, long prices above a size threshold, batch = discount tier | know it by heart (Nishan) |

## part 2 process risks (the marker may ask)

- All code so far was written with an assistant. Marking is per student, so each person must be able to explain their own files line by line and change something small live. Practice this. If you cannot explain a file, read it again or ask for a walkthrough.
- The git history is mostly one author. Teamwork is graded, so each member should make real commits from their own account (review fixes, tests, comments, report parts) and keep the real dates.
- Do not say a member wrote something they did not. Say "we built it with help and I studied and tested my part".

## part 3 questions per member with short answers

### Nishan (spell checking, cost calculator, Claude crawler)
- Why edit distance and not just a dictionary lookup? It finds the closest known word to a typo like gemnii. Dp table, O(a x b) per word.
- Why a heap for suggestions? We only need the best few, a heap keeps the top k without sorting everything.
- What does the cost calculator ignore? See faults 2 and 13.
- How does batch change the price? It is its own tier, about half, only used when batch is ok.

### Abhinav (OpenAI crawler, inverted index, frequency count)
- What is in the inverted index? hash map word to url to list of positions, so phrase search is possible.
- Boyer-Moore vs KMP? Both count a word on a page. KMP is O(n+m). Boyer-Moore skips ahead and usually checks fewer chars (the page shows 24818 vs 110129 on one page).
- Why not just use String.indexOf or regex? The course wants the algorithms, and the comparison counts show the difference.

### Shrijana (Gemini crawler, page ranking, data validation)
- How are pages ranked? score = keyword count, tie = more distinct keywords, max heap, O(n log n). Not link based (fault 5).
- What does validation check? regex on the search input and on every crawled price, plus sanity checks, bad ones show in the data check box.
- Why is the Gemini parser odd? The page is markdoc text inside the html, so we parse headings, tabs and star lines.

### Aakriti (Z.ai crawler, word completion, pattern finding)
- Why a trie? Prefix lookup costs the prefix length, not the number of words. Counts on each word, heap for the top suggestions.
- What patterns are found with regex? prices, urls, model names and similar, shown on the insights tab.
- Why are some Z.ai contexts missing? Fault 4.

### Khem Raj (xAI crawler, search frequency, html cleaning)
- What does html cleaning remove? scripts, styles, tags and extra spaces, one pass O(n). Everything else depends on clean text.
- What is search frequency used for? hash map saved to a file, tree map for the top list, shown as top searches. It also decides nothing else, it is only statistics.
- How do you handle bad pages? the crawler skips and logs them, the crawl keeps going.

### everyone
- Why Java and a spring boot page? The course is Java. The page is only a front, the core is plain jdk.
- How does the crawler avoid loops? queue plus a hash set of visited urls, urls are normalized first.
- Is crawling allowed? robots.txt is checked, there is a 400 ms delay, depth 2, at most 20 pages per provider.
- What would you do next? price history, link based ranking, email report, a hide free filter.

## part 4 decisions made

1. email report is dropped, price history is our extra feature (built, tested, pushed)
2. free models can be hidden in the calculator
3. tailwind is saved in the project so the demo works without wifi
4. windows scripts (`run.bat`, `test.bat`) and eclipse files are included, but only a teammate on windows can prove they work

## part 5 still open

1. who presents which part and who runs the live demo
2. crawl once on a later day before the demo so the history tab shows a real change
