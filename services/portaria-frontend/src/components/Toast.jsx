import { useEffect } from 'react'

export default function Toast({ message, type = 'success', onClose }) {
  useEffect(() => {
    const t = setTimeout(onClose, 3500)
    return () => clearTimeout(t)
  }, [onClose])

  const icon = type === 'success' ? '✓' : '✕'
  return (
    <div className={`toast toast-${type}`}>
      <span style={{ color: type === 'success' ? 'var(--green)' : 'var(--red)' }}>{icon}</span>
      {message}
    </div>
  )
}
