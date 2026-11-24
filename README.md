# TokenTally AI

COMP 8547 Advanced Computing Concepts, Fall 2026, Group 10.

TokenTally AI crawls the official pricing pages of five AI providers (OpenAI, Google Gemini, Anthropic Claude, Z.ai and xAI Grok), puts every model price in one format, and lets you search, compare and work out what a workload will cost per month.

## Team

| Member | Provider | Features |
|---|---|---|
| Nishan Giri | Anthropic Claude | spell checking, workload cost calculator |
| Abhinav Pandey | OpenAI | inverted indexing, frequency count |
| Shrijana Karki | Google Gemini | page ranking, data validation |
| Aakriti Mainali | Z.ai | word completion, finding patterns |
| Khem Raj Adhikari | xAI Grok | search frequency, html cleaning |

## Folders

- `core` plain java back end. All the features live here. Only the jdk is used. Eclipse project files are included.
- `api` spring boot app. It compiles `core` together with itself and serves the web page from `api/src/main/resources/static/index.html` (one page, plain javascript, tailwind from a cdn link). No node needed.
- `docs` our proposal, the requirements in our own words, the plan and the report draft.

You need java 17 or newer. Maven is only needed for `api` (Eclipse has it built in).

## Run it in Eclipse

Use "Eclipse IDE for Enterprise Java and Web Developers" (it has Maven built in).

Menu version (`core`):
1. File > Import > General > Existing Projects into Workspace, pick the `core` folder.
2. Open `src/main/java/tokentally/app/TokenTallyApp.java`, right click > Run As > Java Application.
3. The menu shows in the console. It uses the saved pages in `core/data`. Pick option 12 to crawl the live pages again.

Web version (`api`):
1. File > Import > Maven > Existing Maven Projects, pick the `api` folder.
2. If Eclipse asks to install a connector for build-helper-maven-plugin, accept it. If a red error stays, right click the project > Maven > Update Project.
3. Open `tokentally/api/ApiApplication.java`, right click > Run As > Java Application.
4. Open http://localhost:8080 in the browser.

Tests: open `src/test/java/tokentally/TestRunner.java` in `core` and Run As > Java Application. It prints how many checks passed.

## Run it from the terminal

```
cd core && ./run.sh        # menu
cd core && ./test.sh       # 123 checks
cd api && ./run.sh         # web page on http://localhost:8080
```

If port 8080 is busy start with `SERVER_PORT=8090 java -jar api/target/tokentally-api-1.0.jar` after `./run.sh` built it.

The web page loads tailwind from a cdn, so it needs internet for the styling. The back end works offline with the saved data.

## Where each feature lives

| Feature | Files |
|---|---|
| web crawler | `crawl/WebCrawler`, `crawl/*Crawler`, `crawl/RobotsTxtChecker`, `crawl/Fetcher` |
| html parser | `parse/HtmlCleaner`, `parse/*Parser` |
| spell checking | `text/SpellChecker`, `text/EditDistance` |
| word completion | `text/Trie`, `text/WordCompleter` |
| frequency count | `text/FrequencyCounter`, `text/BoyerMoore`, `text/KMPMatcher` |
| search frequency | `search/SearchFrequency` |
| page ranking | `rank/PageRanker`, `util/Heap` |
| inverted indexing | `text/InvertedIndex` |
| data validation | `regex/InputValidator` |
| finding patterns | `regex/PatternExtractor` |
| cost calculator | `cost/Workload`, `cost/Recommender`, `cost/CostCalculator` |
| menu | `app/TokenTallyApp`, `app/Engine` |
