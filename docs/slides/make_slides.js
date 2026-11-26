// builds the presentation: npm install pptxgenjs, then node docs/slides/make_slides.js
const path = require('path')
const fs = require('fs')
const pptxgen = require('pptxgenjs')
const p = new pptxgen()
p.layout = 'LAYOUT_WIDE' // 13.333 x 7.5
const INK = '18181B', MUTED = '5B5B66', ACC = '4F46E5', SOFT = 'EEF2FF', WHITE = 'FFFFFF'
const FIG = path.join(__dirname, '../report/figures')
p.defineSlideMaster({ title: 'PLAIN', background: { color: WHITE }, slideNumber: { x: 12.4, y: 7.0, color: MUTED, fontSize: 10 },
  objects: [{ placeholder: { options: { name: 'title', type: 'title', x: 0.6, y: 0.35, w: 12.1, h: 0.8, fontSize: 32, bold: true, color: INK, fontFace: 'Calibri', align: 'left', margin: 0 }, text: '' } }] })
p.defineSlideMaster({ title: 'DARK', background: { color: '17143F' }, objects: [] })

function slide(title, notes) { const s = p.addSlide({ masterName: 'PLAIN' }); s.addText(title, { placeholder: 'title' }); s.addNotes(notes); return s }
function text(s, t, x, y, w, h, o = {}) { s.addText(t, { x, y, w, h, fontSize: 16, color: INK, fontFace: 'Calibri', valign: 'top', margin: 0, isTextBox: true, ...o }) }
function bullets(s, items, x, y, w, h, o = {}) {
  text(s, items.map((t, i) => ({ text: t, options: { bullet: true, breakLine: i < items.length - 1 } })), x, y, w, h, { paraSpaceAfter: 8, ...o })
}
// picture scaled to fit inside the box, never stretched
function picture(s, file, x, y, w, h, alt) {
  const b = fs.readFileSync(path.join(FIG, file))
  const iw = b.readUInt32BE(16), ih = b.readUInt32BE(20)
  const k = Math.min(w / iw, h / ih)
  s.addImage({ path: path.join(FIG, file), x: x + (w - iw * k) / 2, y, w: iw * k, h: ih * k, altText: alt, shadow: { type: 'outer', color: '000000', opacity: 0.15, blur: 8, offset: 2, angle: 90 } })
}
function table(s, rows, x, y, w, colW, size = 14, rowH = 0.45) {
  s.addTable(rows.map((r, i) => r.map((c) => ({ text: c, options: i === 0 ? { bold: true, color: WHITE, fill: { color: ACC } } : { fill: { color: i % 2 ? SOFT : WHITE } } }))),
    { x, y, w, colW, fontSize: size, fontFace: 'Calibri', color: INK, rowH, valign: 'middle', border: { type: 'solid', color: 'DADAE8', pt: 0.75 } })
}

// 1 title
let s = p.addSlide({ masterName: 'DARK' })
text(s, 'TokenTally AI', 0.9, 1.5, 11.5, 1.1, { fontSize: 60, bold: true, color: WHITE })
text(s, 'AI Model Pricing Comparison Engine', 0.9, 2.65, 11.5, 0.6, { fontSize: 28, color: 'C7C9FF' })
text(s, 'Group 10', 0.9, 4.2, 11.5, 0.5, { fontSize: 22, bold: true, color: WHITE })
text(s, 'Abhinav Pandey,  Nishan Giri,  Shrijana Karki,  Aakriti Mainali,  Khem Raj Adhikari', 0.9, 4.75, 11.5, 0.5, { fontSize: 18, color: WHITE })
text(s, 'COMP 8547 Advanced Computing Concepts, Fall 2026, University of Windsor', 0.9, 6.4, 11.5, 0.4, { fontSize: 14, color: 'C7C9FF' })
s.addNotes('0:15. group 10, project name, five members.')

// 2 task
s = slide('Our task', '0:35. read the problem in one sentence, then the main feature.')
text(s, 'TokenTally AI is a Java back-end application. It crawls the official pricing and model documentation pages of five AI providers: OpenAI, Google Gemini, Anthropic Claude, Z.ai, and xAI Grok. It pulls the prices and specs for each model out of those pages and stores them in one common format. Users can then search and compare models from a command-line menu.', 0.6, 1.5, 12.1, 1.7, { fontSize: 20 })
text(s, 'The main feature is a workload cost calculator. The user types in their expected usage, for example, "2 million input tokens and 500,000 output tokens per day, batch processing is fine". The program then lists the matching models from cheapest to most expensive by estimated monthly cost, counting batch discounts, cached input prices, long-context prices, and free models.', 0.6, 3.4, 12.1, 1.7, { fontSize: 20 })
;[['5', 'providers crawled'], ['81', 'pages'], ['194', 'model prices'], ['147', 'checks pass']].forEach(([n, l], i) => {
  const x = 0.6 + i * 3.1
  s.addShape(p.ShapeType.roundRect, { x, y: 5.45, w: 2.8, h: 1.35, fill: { color: SOFT }, rectRadius: 0.1 })
  text(s, n, x, 5.55, 2.8, 0.7, { fontSize: 36, bold: true, color: ACC, align: 'center' })
  text(s, l, x, 6.3, 2.8, 0.35, { fontSize: 14, color: MUTED, align: 'center' })
})

// 3 how it is built
s = slide('How we completed the task', '0:35. same path for every provider. core is plain java, spring boot only serves the page.')
;[['Crawler', 'queue + hash set'], ['HTML cleaner', 'one pass'], ['5 parsers', 'one per site'], ['Data store', 'csv + text files'], ['Engine', 'builds features'], ['Menu / web', 'Java CLI, Spring Boot']].forEach(([a, b], i) => {
  const x = 0.6 + i * 2.05
  s.addShape(p.ShapeType.roundRect, { x, y: 1.8, w: 1.75, h: 1.4, fill: { color: i === 5 ? ACC : SOFT }, rectRadius: 0.1 })
  text(s, a, x, 2.05, 1.75, 0.4, { fontSize: 16, bold: true, align: 'center', color: i === 5 ? WHITE : INK })
  text(s, b, x + 0.08, 2.55, 1.59, 0.5, { fontSize: 12, align: 'center', color: i === 5 ? WHITE : MUTED })
  if (i < 5) text(s, '›', x + 1.75, 2.15, 0.3, 0.6, { fontSize: 28, color: ACC, align: 'center' })
})
bullets(s, ['core: plain Java, only the JDK, own heap, trie, index and string search', 'crawl is breadth first, depth 2, at most 20 pages per provider, robots.txt checked', 'api: Spring Boot with one web page, not a required feature', 'saved crawl lets everything run without internet', 'tested with our own test runner'], 0.6, 3.9, 12, 2.8, { fontSize: 20 })

// 4 team
s = slide('Who did what', '0:30. every java file starts with an owner line.')
table(s, [['Member', 'Features', 'Own files', 'Shared files'],
  ['Abhinav Pandey', 'OpenAI crawler, inverted index, frequency count, web api and page', 'OpenAICrawler, OpenAIParser, InvertedIndex, FrequencyCounter, BoyerMoore, KMPMatcher', 'Engine, DataStore, Tokenizer, WholeWord'],
  ['Nishan Giri', 'Claude crawler, spell checking, cost calculator, web api and page', 'AnthropicCrawler, AnthropicParser, SpellChecker, EditDistance, Workload, CostCalculator, Recommender', 'ModelPrice, Prices, PriceHistory, Heap'],
  ['Shrijana Karki', 'Gemini crawler, page ranking, data validation', 'GeminiCrawler, GeminiParser, PageRanker, InputValidator', 'Page, PriceParser, ErrorDemo, TestRunner'],
  ['Aakriti Mainali', 'Z.ai crawler, word completion, finding patterns', 'ZaiCrawler, ZaiParser, Trie, WordCompleter, PatternExtractor', 'Crawler, CrawlAll, TokenTallyApp, Experiments'],
  ['Khem Raj Adhikari', 'xAI crawler, search frequency, html cleaning', 'XaiCrawler, XaiParser, HtmlCleaner, SearchFrequency', 'HtmlTable, WebCrawler, Fetcher, RobotsTxtChecker']],
  0.6, 1.4, 12.1, [2.0, 3.2, 4.2, 2.7], 13, 0.85)
text(s, 'Web api by Abhinav and Nishan: ApiApplication, TokenTallyController, index.html', 0.6, 6.7, 11, 0.35, { fontSize: 14, color: MUTED })

// 5-9 members
function member(name, provider, rows, files, shot, alt, notes) {
  const m = slide(name, notes)
  text(m, provider, 0.6, 1.2, 12, 0.4, { fontSize: 18, color: ACC, bold: true })
  let y = 1.85
  rows.forEach(([f, d]) => {
    text(m, f, 0.6, y, 4.9, 0.35, { fontSize: 18, bold: true })
    text(m, d, 0.6, y + 0.4, 4.9, 0.95, { fontSize: 15, color: MUTED })
    y += 1.5
  })
  text(m, files, 0.6, 6.35, 4.9, 0.65, { fontSize: 12, color: MUTED })
  picture(m, shot, 5.9, 1.4, 6.8, 5.45, alt)
}
member('Abhinav Pandey', 'OpenAI crawler and parser', [['Inverted index', 'hash map: word → page → positions. lookup O(1) average, gives phrase search'], ['Frequency count', 'Boyer-Moore and KMP, KMP is O(n+m), Boyer-Moore skips and checks fewer characters'], ['Error handling', 'empty word, symbols and unknown page are refused with a message']],
  'OpenAICrawler, OpenAIParser, InvertedIndex, FrequencyCounter, BoyerMoore, KMPMatcher', 'web-frequency.png', 'search page with a frequency count of a word', '1:00. show index lookup and the count with both algorithms.')
member('Nishan Giri', 'Anthropic Claude crawler and parser', [['Spell checking', 'edit distance dp table O(a×b), a heap keeps the best words'], ['Cost calculator', 'monthly cost from tokens, cache, batch and long context, min heap for the cheapest'], ['Error handling', 'wrong amounts mark the field and say the right format']],
  'AnthropicCrawler, AnthropicParser, SpellChecker, EditDistance, Workload, CostCalculator, Recommender', 'web-calculator.png', 'cost calculator results', '1:00. typo gemnii, then one workload.')
member('Shrijana Karki', 'Google Gemini crawler and parser', [['Page ranking', 'score is keyword count, ties by more keywords, max heap O(n log n)'], ['Data validation', 'regular expressions for tokens, money, percent, date and search text'], ['Error handling', 'script tags, 30 February and over long text are rejected']],
  'GeminiCrawler, GeminiParser, PageRanker, InputValidator', 'wi-calculator-reject.png', 'a wrong token amount rejected', '1:00. ranked pages, then a bad input.')
member('Aakriti Mainali', 'Z.ai crawler and parser', [['Word completion', 'trie with counts, a heap returns the most used words, cost depends on the prefix'], ['Finding patterns', 'regex finds prices, context sizes, dates, model ids and free offers'], ['Error handling', 'empty prefix and empty text give an empty result, no crash']],
  'ZaiCrawler, ZaiParser, Trie, WordCompleter, PatternExtractor', 'web-completion.png', 'word completion in the search box', '1:00. type claude-op, then the patterns.')
member('Khem Raj Adhikari', 'xAI Grok crawler and parser', [['HTML cleaning', 'removes scripts, styles and tags in one pass, parsers and index use the clean text'], ['Search frequency', 'hash map saved to a file, tree map for the top searches'], ['Error handling', 'broken history lines are skipped, 20 threads count correctly']],
  'XaiCrawler, XaiParser, HtmlCleaner, SearchFrequency', 'web-insights.png', 'top searches on the insights page', '1:00. top searches and one broken html case.')

// 10 additional features
s = slide('Additional features', '0:30. say the example in price history is made up.')
bullets(s, ['workload cost calculator and recommendation (Nishan)', 'compare up to 10 models, model card (shared)', 'free models and price warnings (shared)', 'web page and api (Abhinav, Nishan)', 'price history: a dated log of changes (Nishan)', 'error handling demo (Shrijana)', 'measured experiments (Aakriti)'], 0.6, 1.6, 4.9, 5, { fontSize: 18 })
picture(s, 'web-compare.png', 5.9, 1.4, 6.8, 3.0, 'compare page with the model picker')
picture(s, 'web-history.png', 5.9, 4.55, 6.8, 2.4, 'price history page with example changes, not real prices')

// 11 errors and experiments
s = slide('Error handling and experiments', '0:30. 54 cases, none crash. heap equals sort at our size, say it.')
table(s, [['Experiment on our data', 'Result'], ['inverted index vs reading every page', 'thousands of times faster'], ['trie vs scanning every word', 'about 30 times faster'], ['Boyer-Moore vs KMP characters checked', 'about 4.6 times fewer'], ['heap vs full sort (small data)', 'about the same']], 0.6, 1.6, 6.2, [4.0, 2.2], 15, 0.6)
text(s, '54 bad and extreme inputs, all handled with a message or a safe result', 0.6, 5.1, 6.2, 1.0, { fontSize: 22, bold: true, color: ACC })
picture(s, 'ui-errors-report.png', 7.2, 1.4, 5.5, 5.5, 'error handling page, 54 cases handled')

// 12 demo
s = slide('Live demo', '1:15. keep the saved data, do not crawl. if it fails use the screenshots on the member slides.')
table(s, [['Step', 'Do this', 'Shows'], ['1', 'cost calculator, 2M in and 500K out', 'cheapest models'], ['2', 'What if I… type "2 million"', 'field error'], ['3', 'search gemnii', 'spell check and ranking'], ['4', 'press count on a page', 'Boyer-Moore and KMP'], ['5', 'type claude-op', 'word completion'], ['6', 'Insights tab', 'top searches and patterns'], ['7', './run.sh data nishan, option 20', 'same in the command line']], 0.6, 1.5, 12.1, [1.0, 6.4, 4.7], 18, 0.6)

// 13 limits
s = slide('Limits and next steps', '0:15.')
bullets(s, ['pages are read as html, a redesign can break a parser', 'cost compares token prices only, not quality or speed', 'page ranking counts keywords, it does not use links', 'some models do not list a context window', 'the proposal named jsoup, we wrote our own parser instead', 'next: more price history, more model details'], 0.6, 1.6, 12, 4.5, { fontSize: 22 })
text(s, 'Some code was written with the help of an AI assistant.', 0.6, 6.5, 12, 0.4, { fontSize: 14, color: MUTED })

// 14 references
s = slide('References', '0:05. thank you, questions.')
bullets(s, ['OpenAI pricing, developers.openai.com/api/docs/pricing', 'Google Gemini pricing, ai.google.dev/gemini-api/docs/pricing', 'Anthropic Claude pricing, platform.claude.com/docs/en/about-claude/pricing', 'Z.ai pricing, docs.z.ai/guides/overview/pricing', 'xAI pricing, docs.x.ai/developers/pricing', 'course notes: tries, heaps, edit distance, KMP, Boyer-Moore', 'Spring Boot, spring.io/projects/spring-boot', 'Tailwind CSS, tailwindcss.com'], 0.6, 1.5, 12, 4.8, { fontSize: 20 })
text(s, 'github.com/Nis-lab-art/tokentally-ai', 0.6, 6.5, 12, 0.4, { fontSize: 16, color: ACC, bold: true })

p.writeFile({ fileName: path.join(__dirname, 'TokenTally_AI_Presentation.pptx') }).then((f) => console.log(f))
