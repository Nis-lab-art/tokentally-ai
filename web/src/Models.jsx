import { useEffect, useState } from 'react'
import { get, size, PROVIDERS } from './api.js'
import PriceTable from './PriceTable.jsx'

// every model price with filters, and a card for one model
export default function Models() {
  const [provider, setProvider] = useState('')
  const [tier, setTier] = useState('standard')
  const [sort, setSort] = useState('input')
  const [rows, setRows] = useState([])
  const [card, setCard] = useState(null)

  useEffect(() => {
    get(`/models?provider=${provider}&tier=${tier}`).then(setRows)
  }, [provider, tier])

  const sorted = [...rows].sort((a, b) => {
    if (sort === 'model') return a.model.localeCompare(b.model)
    const x = a[sort] < 0 ? Infinity : a[sort]
    const y = b[sort] < 0 ? Infinity : b[sort]
    return sort === 'contextWindow' ? y - x : x - y
  })

  const open = (id) => get('/models/' + id).then(setCard)

  return (
    <section>
      <div className="filters">
        <select value={provider} onChange={(e) => setProvider(e.target.value)}>
          <option value="">all providers</option>
          {PROVIDERS.map((p) => <option key={p}>{p}</option>)}
        </select>
        <select value={tier} onChange={(e) => setTier(e.target.value)}>
          <option value="">all tiers</option>
          {['standard', 'batch', 'flex', 'priority', 'fast'].map((t) => <option key={t}>{t}</option>)}
        </select>
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="input">cheapest input</option>
          <option value="output">cheapest output</option>
          <option value="contextWindow">biggest context</option>
          <option value="model">name</option>
        </select>
        <span className="muted">{rows.length} rows</span>
      </div>

      {card && card.length > 0 && (
        <div className="card">
          <button className="close" onClick={() => setCard(null)}>×</button>
          <h3>{card[0].model} <span className={'tag ' + card[0].provider}>{card[0].provider}</span></h3>
          <p>context window <b>{size(card[0].contextWindow)}</b> · max output <b>{size(card[0].maxOutput)}</b> ·{' '}
            <a href={card[0].sourceUrl} target="_blank" rel="noreferrer">source</a></p>
          <PriceTable rows={card} />
        </div>
      )}

      <PriceTable rows={sorted} onPick={open} />
    </section>
  )
}
