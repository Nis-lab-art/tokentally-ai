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

- `core` plain java back end. All the features live here. Only the jdk is used.
- `api` small spring boot layer that calls `core` (optional, for the web ui).
- `web` small react ui (optional, gui does not count as a feature).
- `docs` our proposal, the requirements in our own words and the plan.

## Run the back end

```
cd core
./run.sh
```

`run.sh` compiles with `javac` and starts the menu. The first time it uses the saved pages in `core/data`. Pick "crawl again" in the menu to fetch the live pages.

## Run the tests

```
cd core
./test.sh
```
