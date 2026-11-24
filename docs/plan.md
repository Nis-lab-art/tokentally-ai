# Build plan and status

| Step | Status |
|---|---|
| 1 repo layout, docs, shared model, crawler interface, robots.txt check, html cleaner | done |
| 2 five crawlers and five price parsers, saved pages for an offline demo | done |
| 3 inverted index, frequency count, spell checking, word completion | done |
| 4 search frequency, page ranking, regex validation and pattern finding | done |
| 5 cost calculator, recommender, command line menu | done |
| 6 tests with our own small test runner (123 checks) | done |
| 7 spring boot api and react ui | done |
| 8 scheduled email report with email login (extra) | not started |
| 9 final report and presentation | not started |

## Data structures

| Where | Structure |
|---|---|
| crawler | queue for urls to visit, hash set for visited urls |
| inverted index | hash map of word to url to list of positions |
| frequency count | boyer moore and kmp string search |
| spell checking | edit distance dp table and a heap for the best suggestions |
| word completion | trie with a count on each word, heap for the top words |
| search frequency | hash map saved to a file, tree map for the top list |
| page ranking | max heap |
| recommendation | min heap on monthly cost |
