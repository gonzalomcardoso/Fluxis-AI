import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StatusBadge } from '@/features/job-opportunities/components/StatusBadge'

describe('StatusBadge', () => {
  it('muestra la etiqueta legible del estado', () => {
    render(<StatusBadge status="PENDING" />)

    expect(screen.getByText('Pendiente')).toBeInTheDocument()
  })
})
