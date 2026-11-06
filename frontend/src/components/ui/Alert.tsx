import type { ReactNode } from 'react'

interface AlertProps {
  children: ReactNode
  variant?: 'error' | 'success' | 'info'
  className?: string
}

const variants: Record<NonNullable<AlertProps['variant']>, string> = {
  error: 'border-rose-200 bg-rose-50 text-rose-900',
  success: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  info: 'border-indigo-200 bg-indigo-50 text-indigo-900',
}

export function Alert({ children, variant = 'info', className = '' }: AlertProps) {
  return (
    <div
      className={`rounded-xl border px-4 py-3 text-sm ${variants[variant]} ${className}`}
      role={variant === 'error' ? 'alert' : 'status'}
    >
      {children}
    </div>
  )
}
