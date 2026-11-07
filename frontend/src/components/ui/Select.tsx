import type { ComponentProps } from 'react'

export function Select({ className = '', ...props }: ComponentProps<'select'>) {
  return (
    <select
      className={`min-h-11 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 shadow-sm focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 ${className}`}
      {...props}
    />
  )
}
