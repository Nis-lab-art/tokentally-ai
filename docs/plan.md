# Build plan

1. Repo layout, docs, shared model, crawler interface, robots.txt check, html cleaner.
2. Five crawlers and five price parsers. Saved pages so the demo works offline.
3. Inverted index, frequency count, spell checking, word completion.
4. Search frequency, page ranking, regex validation and pattern finding.
5. Cost calculator, recommender and the command line menu that ties it together.
6. Tests with a small hand written test runner.
7. Spring boot api and react ui.
8. Scheduled email report with login (extra, not started).

## Data structures

| Where | Structure |
|---|---|
| crawler | queue for urls to visit, hash set for visited urls |
| inverted index | hash map of word to url to list of positions |
| frequency count | boyer moore and kmp string search |
| spell checking | edit distance dp table and a heap for the best suggestions |
| word completion | trie with a count on each word |
| search frequency | hash map saved to a file, sorted for the top list |
| page ranking | max heap |
| recommendation | min heap on monthly cost |
