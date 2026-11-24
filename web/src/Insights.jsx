import { useEffect, useState } from 'react'
import { get, post } from './api.js'
import PriceTable from './PriceTable.jsx'

// free models, warnings, top searches, regex patterns and a fresh crawl
export default function Insights() {
  const [free, setFree] = useState([])
  const [warnings, setWarnings] = useState([])
  const [top, setTop] = useState([])
  const [pages, setPages] = useState([])
  const [url, setUrl] = useState('')
  const [patterns, setPatterns] = useState(null)
  const [problems, setProblems] = useState(null)
  const [crawlLog, setCrawlLog] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    get('/free').then(setFree)
    get('/warnings').then(setWarnings)
    get('/top-searches').then(setTop)
    get('/pages').then(setPages)
    get('/problems').then(setProblems)
  }, [])

  useEffect(() => {
    get('/patterns?url=' + encodeURIComponent(url)).then(setPatterns)
  }, [url])

  const crawl = () => {
    setBusy(true)
    post('/crawl').then(setCrawlLog).catch((e) => setCrawlLog([e.message])).finally(() => setBusy(false))
  }

  return (
    <section className="insights">
      <div className="panel">
        <h3>Top searches</h3>
        {top.length === 0 ? <p className="muted">nothing searched yet</p> : (
          <ol>{top.map((t) => <li key={t.word}><b>{t.count}</b> {t.word}</li>)}</ol>
        )}
      </div>

      <div className="panel">
        <h3>Data check</h3>
        {problems && Object.keys(problems).length === 0
          ? <p>every crawled price passed the regex and sanity checks</p>
          : problems && Object.entries(problems).map(([k, v]) => <p key={k}>{k}: {v.join(', ')}</p>)}
        <button onClick={crawl} disabled={busy}>{busy ? 'crawling…' : 'Crawl the live pages again'}</button>
        {crawlLog && <pre>{crawlLog.join('\n')}</pre>}
      </div>

      <div className="wide">
        <h3>Free models</h3>
        <PriceTable rows={free} />
        <h3>Price warnings</h3>
        <PriceTable rows={warnings} />
      </div>

      <div className="wide">
        <h3>Patterns found with regex</h3>
        <select value={url} onChange={(e) => setUrl(e.target.value)}>
          <option value="">all {pages.length} pages</option>
          {pages.map((p) => <option key={p.url} value={p.url}>{p.url.replace('https://', '')}</option>)}
        </select>
        <div className="patterns">
          {patterns && Object.entries(patterns).map(([kind, found]) => (
            <div key={kind} className="panel">
              <h4>{kind} <span className="muted">({Object.keys(found).length})</span></h4>
              {Object.entries(found).slice(0, 10).map(([v, n]) => <div key={v}><span className="muted">{n}×</span> {v}</div>)}
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
