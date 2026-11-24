// small helpers to call the spring boot api
async function handle(res) {
  const body = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(body.error || 'request failed')
  return body
}

export const get = (path) => fetch('/api' + path).then(handle)

export const post = (path, data) =>
  fetch('/api' + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data || {}),
  }).then(handle)

export const money = (v) => {
  if (v === undefined || v === null || v < 0) return '–'
  if (v === 0) return 'free'
  return '$' + (Math.round(v * 10000) / 10000)
}

export const size = (n) => {
  if (!n || n < 0) return '–'
  if (n >= 1000000) return Math.round(n / 10000) / 100 + 'M'
  return Math.round(n / 1000) + 'K'
}

export const PROVIDERS = ['openai', 'gemini', 'anthropic', 'zai', 'xai']
