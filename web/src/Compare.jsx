import { useEffect, useState } from 'react'
import { get } from './api.js'
import PriceTable from './PriceTable.jsx'

// side by side comparison of the standard tier of a few models
export default function Compare() {
  const [text, setText] = useState('gpt-6.2-astra, claude-opus-5-5, gemini-4-carbon, grok-4.7, glm-5.3')
  const [rows, setRows] = useState([])
  const [error, setError] = useState('')

  const run = () => {
    setError('')
    get('/compare?ids=' + encodeURIComponent(text)).then(setRows).catch((e) => setError(e.message))
  }

  useEffect(run, [])

  const cheapest = rows.length ? Math.min(...rows.map((r) => r.input + r.output)) : 0

  return (
    <section>
      <form className="searchbar" onSubmit={(e) => { e.preventDefault(); run() }}>
        <input className="grow" value={text} onChange={(e) => setText(e.target.value)} />
        <button>Compare</button>
      </form>
      {error && <div className="error">{error}</div>}
      {rows.length > 0 && (
        <p className="muted">
          cheapest for input plus output: <b>{rows.find((r) => r.input + r.output === cheapest).model}</b>
        </p>
      )}
      <PriceTable rows={rows} />
    </section>
  )
}
