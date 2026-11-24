#!/bin/sh
# builds TokenTally_AI_Report.docx from report.md and the figures folder (needs pandoc)
cd "$(dirname "$0")"
python3 - <<'PY'
s = open('report.md').read()
for key, f in [('CRAWL', 'crawl'), ('SEARCH', 'search'), ('BAD', 'bad'), ('COMPLETION', 'completion'),
               ('CALC', 'calc'), ('HISTORY', 'history'), ('TESTS', 'tests'),
               ('ERRNISHAN', 'errors-nishan'), ('EXPSHORT', 'experiments-short')]:
    text = open('figures/console-%s.txt' % f).read().rstrip('\n')
    s = s.replace('FIGURE_' + key, text)
open('build_tmp.md', 'w').write(s)
PY
pandoc build_tmp.md -o TokenTally_AI_Report.docx --resource-path=. --toc --toc-depth=2 --reference-doc=reference.docx
rm -f build_tmp.md
