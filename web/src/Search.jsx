import { useEffect, useState } from 'react'
import { get } from './api.js'
import PriceTable from './PriceTable.jsx'

// search box with word completion, spell suggestions, ranked pages and frequency count
export default function Search() {
  const [q, setQ] = useState('')
  const [hints, setHints] = useState([])
  const [showHints, setShowHints] = useState(false)
  const [result, setResult] = useState(null)
  const [freq, setFreq] = useState(null)
  const [error, setError] = useState('')

  // word completion on the last word typed
  useEffect(() => {
    const last = q.split(' ').pop()
    if (last.length < 2) return setHints([])
    const t = setTimeout(() => {
      get('/complete?prefix=' + encodeURIComponent(last)).then(setHints).catch(() => setHints([]))
    }, 150)
    return () => clearTimeout(t)
  }, [q])

  const run = (text) => {
    const query = (text ?? q).trim()
    if (!query) return
    setError('')
    setShowHints(false)
    setFreq(null)
    get('/search?q=' + encodeURIComponent(query)).then(setResult).catch((e) => setError(e.message))
  }

  const pickHint = (h) => {
    const words = q.split(' ')
    words[words.length - 1] = h
    const next = words.join(' ')
    setQ(next)
    run(next)
  }

  const countOn = (url) => {
    const word = (result.searchedInstead || result.query).split(' ')[0]
    get('/frequency?word=' + encodeURIComponent(word) + '&url=' + encodeURIComponent(url))
      .then((r) => setFreq({ url, word, ...(r[0] || { boyerMoore: 0, kmp: 0 }) }))
      .catch((e) => setError(e.message))
  }

  return (
    <section>
      <form className="searchbar" onSubmit={(e) => { e.preventDefault(); run() }}>
        <div className="grow">
          <input value={q} onChange={(e) => { setQ(e.target.value); setShowHints(true) }}
            onBlur={() => setShowHints(false)} placeholder="try gemnii, batch api or claude-opus" />
          {showHints && hints.length > 0 && (
            <ul className="hints">
              {hints.map((h) => <li key={h} onMouseDown={() => pickHint(h)}>{h}</li>)}
            </ul>
          )}
        </div>
        <button>Search</button>
      </form>
      {error && <div className="error">{error}</div>}

      {result && (
        <>
          <p className="muted">searched {result.timesSearched} time(s)</p>
          {result.unknownWords.map((w) => (
            <div key={w} className="note">
              "{w}" is not in the pages.{' '}
              {result.suggestions[w].length > 0 ? (
                <>did you mean{' '}
                  {result.suggestions[w].map((s) => (
                    <button key={s.word} className="link" onClick={() => { setQ(s.word); run(s.word) }}>{s.word}</button>
                  ))}
                </>
              ) : 'nothing close was found.'}
            </div>
          ))}
          {result.searchedInstead && <p>showing results for <b>{result.searchedInstead}</b></p>}

          {result.models.length > 0 && (<><h3>Models</h3><PriceTable rows={result.models} /></>)}

          <h3>Pages ranked by keyword count</h3>
          {result.pages.length === 0 ? <p className="muted">no page has these words</p> : (
            <table>
              <thead><tr><th>#</th><th>score</th><th>keywords</th><th>page</th><th></th></tr></thead>
              <tbody>
                {result.pages.map((p, i) => (
                  <tr key={p.url}>
                    <td>{i + 1}</td><td>{p.score}</td><td>{p.keywordsFound}</td>
                    <td><a href={p.url} target="_blank" rel="noreferrer">{p.url.replace('https://', '')}</a></td>
                    <td><button className="small" onClick={() => countOn(p.url)}>count</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {freq && (
            <div className="note">
              "{freq.word}" shows up <b>{freq.boyerMoore}</b> time(s) on {freq.url.replace('https://', '')}.
              Boyer-Moore checked {freq.bmComparisons ?? 0} chars, KMP checked {freq.kmpComparisons ?? 0}.
            </div>
          )}
        </>
      )}
    </section>
  )
}
