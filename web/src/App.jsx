import { useEffect, useState } from 'react'
import { get } from './api.js'
import Search from './Search.jsx'
import Models from './Models.jsx'
import Calculator from './Calculator.jsx'
import Compare from './Compare.jsx'
import Insights from './Insights.jsx'

const TABS = [
  ['calculator', 'Cost calculator'],
  ['search', 'Search'],
  ['models', 'Models'],
  ['compare', 'Compare'],
  ['insights', 'Insights'],
]

export default function App() {
  const [tab, setTab] = useState('calculator')
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    get('/stats').then(setStats).catch(() => setError('the api is not running, start it with ./run.sh in api'))
  }, [])

  return (
    <div className="page">
      <header>
        <div>
          <h1>TokenTally AI</h1>
          <p className="muted">AI model prices from OpenAI, Gemini, Claude, Z.ai and xAI in one place</p>
        </div>
        {stats && (
          <div className="stats">
            <span><b>{stats.prices}</b> prices</span>
            <span><b>{stats.pages}</b> pages</span>
            <span><b>{stats.words}</b> words</span>
          </div>
        )}
      </header>

      <nav>
        {TABS.map(([id, label]) => (
          <button key={id} className={tab === id ? 'active' : ''} onClick={() => setTab(id)}>
            {label}
          </button>
        ))}
      </nav>

      {error && <div className="error">{error}</div>}

      <main>
        {tab === 'calculator' && <Calculator />}
        {tab === 'search' && <Search />}
        {tab === 'models' && <Models />}
        {tab === 'compare' && <Compare />}
        {tab === 'insights' && <Insights />}
      </main>

      <footer className="muted">COMP 8547 Group 10 · prices come from the official pricing pages</footer>
    </div>
  )
}
