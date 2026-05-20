import { useState, useEffect } from 'react'

const SERVICES = [
  { name: 'API Gateway',       proxy: '/api/gateway' },
  { name: 'Ingestion Service', proxy: '/api/ingestion' },
  { name: 'Analytics Service', proxy: '/api/analytics' },
  { name: 'Fraud Detection',   proxy: '/api/fraud' },
  { name: 'AI Insight',        proxy: '/api/insight' },
]

interface Health  { name: string; proxy: string; status: 'UP' | 'DOWN' | 'LOADING' }
interface Metrics { totalEvents: number; totalFraudAlerts: number; fraudRatePct: string }

export default function Dashboard() {
  const [healths, setHealths] = useState<Health[]>(SERVICES.map(s => ({ ...s, status: 'LOADING' })))
  const [metrics, setMetrics] = useState<Metrics | null>(null)
  const [log,     setLog]     = useState<string[]>([])
  const [sending, setSending] = useState(false)

  const addLog = (msg: string) =>
    setLog(prev => [`[${new Date().toLocaleTimeString()}] ${msg}`, ...prev.slice(0, 49)])

  useEffect(() => {
    const check = async () => {
      const updated = await Promise.all(
        SERVICES.map(async s => {
          try {
            const r = await fetch(`${s.proxy}/actuator/health`)
            const j = await r.json()
            return { ...s, status: j.status === 'UP' ? 'UP' : 'DOWN' } as Health
          } catch {
            return { ...s, status: 'DOWN' } as Health
          }
        })
      )
      setHealths(updated)
    }
    check()
    const id = setInterval(check, 10000)
    return () => clearInterval(id)
  }, [])

  useEffect(() => {
    const poll = async () => {
      try {
        const r = await fetch('/api/analytics/api/v1/analytics/summary')
        const j = await r.json()
        setMetrics(j)
      } catch {}
    }
    poll()
    const id = setInterval(poll, 5000)
    return () => clearInterval(id)
  }, [])

  const sendPayment = async (type: 'legit' | 'fraud') => {
    setSending(true)
    const payload = type === 'legit'
      ? { transactionId: `TXN-${Date.now()}`, amount: 1500,  currency: 'INR', merchantId: 'amazon-india',  cardHash: 'hash-001', tenantId: '00000000-0000-0000-0000-000000000001', countryCode: 'IN', eventTime: new Date().toISOString() }
      : { transactionId: `TXN-${Date.now()}`, amount: 95000, currency: 'INR', merchantId: 'crypto-nigeria', cardHash: 'hash-002', tenantId: '00000000-0000-0000-0000-000000000001', countryCode: 'NG', eventTime: new Date().toISOString() }
    try {
      const r = await fetch('/api/gateway/api/v1/payments', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const j = await r.json()
      addLog(`${type.toUpperCase()} → ${JSON.stringify(j)}`)
    } catch (e) {
      addLog(`ERROR: ${e}`)
    }
    setSending(false)
  }

  const statusColor = (s: string) =>
    s === 'UP' ? '#22c55e' : s === 'DOWN' ? '#ef4444' : '#f59e0b'

  return (
    <div style={{ fontFamily: 'Arial, sans-serif', background: '#0f172a', minHeight: '100vh', color: '#e2e8f0', padding: '24px' }}>

      <div style={{ marginBottom: 32 }}>
        <h1 style={{ margin: 0, fontSize: 28, color: '#38bdf8' }}>🛡️ SentinelFlow</h1>
        <p style={{ margin: '4px 0 0', color: '#64748b', fontSize: 14 }}>AI-Powered Payment Fraud Detection Platform</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 32 }}>
        {[
          { label: 'Total Transactions', value: metrics?.totalEvents      ?? '—', color: '#38bdf8' },
          { label: 'Fraud Alerts',       value: metrics?.totalFraudAlerts ?? '—', color: '#f87171' },
          { label: 'Fraud Rate',         value: metrics?.fraudRatePct     ?? '—', color: '#fb923c' },
        ].map(m => (
          <div key={m.label} style={{ background: '#1e293b', borderRadius: 12, padding: '20px 24px', border: '1px solid #334155' }}>
            <div style={{ fontSize: 13, color: '#64748b', marginBottom: 8 }}>{m.label}</div>
            <div style={{ fontSize: 32, fontWeight: 700, color: m.color }}>{String(m.value)}</div>
          </div>
        ))}
      </div>

      <div style={{ background: '#1e293b', borderRadius: 12, padding: 24, marginBottom: 32, border: '1px solid #334155' }}>
        <h2 style={{ margin: '0 0 16px', fontSize: 16, color: '#94a3b8' }}>SERVICE HEALTH</h2>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12 }}>
          {healths.map(h => (
            <div key={h.name} style={{ background: '#0f172a', borderRadius: 8, padding: '12px 16px', border: `1px solid ${statusColor(h.status)}44` }}>
              <div style={{ fontSize: 12, color: '#64748b', marginBottom: 6 }}>{h.name}</div>
              <div style={{ fontSize: 13, fontWeight: 600, color: statusColor(h.status) }}>● {h.status}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ background: '#1e293b', borderRadius: 12, padding: 24, marginBottom: 32, border: '1px solid #334155' }}>
        <h2 style={{ margin: '0 0 16px', fontSize: 16, color: '#94a3b8' }}>TEST TRANSACTIONS</h2>
        <div style={{ display: 'flex', gap: 16 }}>
          <button onClick={() => sendPayment('legit')} disabled={sending}
            style={{ background: '#166534', color: '#86efac', border: 'none', borderRadius: 8, padding: '12px 24px', cursor: 'pointer', fontSize: 14, fontWeight: 600 }}>
            ✅ Send Legitimate (₹1,500 Amazon)
          </button>
          <button onClick={() => sendPayment('fraud')} disabled={sending}
            style={{ background: '#7f1d1d', color: '#fca5a5', border: 'none', borderRadius: 8, padding: '12px 24px', cursor: 'pointer', fontSize: 14, fontWeight: 600 }}>
            🚨 Send Fraud (₹95,000 Crypto Nigeria)
          </button>
        </div>
      </div>

      <div style={{ background: '#1e293b', borderRadius: 12, padding: 24, border: '1px solid #334155' }}>
        <h2 style={{ margin: '0 0 16px', fontSize: 16, color: '#94a3b8' }}>EVENT LOG</h2>
        <div style={{ background: '#0f172a', borderRadius: 8, padding: 16, minHeight: 120, maxHeight: 240, overflowY: 'auto', fontFamily: 'monospace', fontSize: 13 }}>
          {log.length === 0
            ? <span style={{ color: '#475569' }}>No events yet. Send a test transaction above.</span>
            : log.map((l, i) => (
                <div key={i} style={{ color: l.includes('FRAUD') || l.includes('ERROR') ? '#f87171' : '#86efac', marginBottom: 4 }}>{l}</div>
              ))
          }
        </div>
      </div>
    </div>
  )
}
