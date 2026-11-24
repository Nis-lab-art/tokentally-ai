import { money, size } from './api.js'

// one table layout for model prices used on every tab
export default function PriceTable({ rows, onPick }) {
  return (
    <div className="scroll">
      <table>
        <thead>
          <tr>
            <th>model</th><th>provider</th><th>tier</th><th className="num">input</th><th className="num">cached</th>
            <th className="num">output</th><th className="num">long input</th><th className="num">context</th><th>notes</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((p) => (
            <tr key={p.provider + p.model + p.tier}>
              <td>{onPick ? <button className="link" onClick={() => onPick(p.model)}>{p.model}</button> : p.model}</td>
              <td><span className={'tag ' + p.provider}>{p.provider}</span></td>
              <td>{p.tier}</td>
              <td className="num">{money(p.input)}</td>
              <td className="num">{money(p.cachedInput)}</td>
              <td className="num">{money(p.output)}</td>
              <td className="num">{money(p.longInput)}</td>
              <td className="num">{size(p.contextWindow)}</td>
              <td className="muted small-text">{p.notes}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
