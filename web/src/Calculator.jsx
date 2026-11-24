import { useState } from 'react'
import { post, size, PROVIDERS } from './api.js'

const START = { inputPerDay: '2M', outputPerDay: '500K', cachedPercent: '0', batchOk: false, promptSize: '', minContext: '', budget: '', provider: '' }

// workload cost calculator: the cheapest models for the user's token volume
export default function Calculator() {
  const [form, setForm] = useState(START)
  const [picks, setPicks] = useState(null)
  const [error, setError] = useState('')

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value })

  const submit = (e) => {
    e.preventDefault()
    setError('')
    post('/recommend', form).then(setPicks).catch((err) => { setError(err.message); setPicks(null) })
  }

  const max = picks && picks.length ? picks[picks.length - 1].cost.total || 1 : 1

  return (
    <section className="two">
      <form className="panel" onSubmit={submit}>
        <h3>Your workload</h3>
        <div className="fields">
          <label>Input tokens / day<input value={form.inputPerDay} onChange={set('inputPerDay')} placeholder="2M" /></label>
          <label>Output tokens / day<input value={form.outputPerDay} onChange={set('outputPerDay')} placeholder="500K" /></label>
        </div>
        <label>Provider
          <select value={form.provider} onChange={set('provider')}>
            <option value="">all</option>
            {PROVIDERS.map((p) => <option key={p}>{p}</option>)}
          </select>
        </label>
        <label className="check"><input type="checkbox" checked={form.batchOk} onChange={set('batchOk')} /> batch or flex is fine</label>
        <details>
          <summary>More options</summary>
          <label>Input that hits the cache (%)<input value={form.cachedPercent} onChange={set('cachedPercent')} /></label>
          <label>Tokens in one request<input value={form.promptSize} onChange={set('promptSize')} placeholder="like 250K" /></label>
          <label>Smallest context window<input value={form.minContext} onChange={set('minContext')} placeholder="like 200K" /></label>
          <label>Monthly budget ($)<input value={form.budget} onChange={set('budget')} placeholder="no limit" /></label>
        </details>
        <button>Find the cheapest</button>
        {error && <div className="error">{error}</div>}
      </form>

      <div>
        <h3>Cheapest per month</h3>
        {!picks && <div className="empty muted">fill in your workload and press the button</div>}
        {picks && picks.length === 0 && <p className="muted">no model fits, try a bigger budget or a smaller context</p>}
        {picks && picks.map((p, i) => (
          <div className="pick" key={p.price.provider + p.price.model}>
            <div className="rank">{i + 1}</div>
            <div className="grow">
              <div className="row">
                <b>{p.price.model}</b>
                <span className={'tag ' + p.price.provider}>{p.price.provider}</span>
                <span className="muted">{p.why}{p.cost.longPrices ? ', long context prices' : ''}</span>
                <span className="cost">${p.cost.total.toFixed(2)}</span>
              </div>
              <div className="bar"><div style={{ width: Math.max(2, (p.cost.total / max) * 100) + '%' }} /></div>
              <div className="muted small-text">
                input ${p.cost.input.toFixed(2)} · cached ${p.cost.cached.toFixed(2)} · output ${p.cost.output.toFixed(2)}
                {p.price.contextWindow > 0 && <> · context {size(p.price.contextWindow)}</>}
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
