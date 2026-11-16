import { describe, expect, it } from 'vitest'
import { proposalSchema } from './proposals.types'

describe('proposalSchema', () => {
  it('accepts decimal totals and proposal items', () => {
    const result = proposalSchema.parse({
      id: '11111111-1111-4111-8111-111111111111', clientId: '22222222-2222-4222-8222-222222222222',
      title: 'Web corporativa', description: null, currency: 'EUR', validUntil: null, status: 'DRAFT',
      subtotal: '1250.5000', total: '1250.5000',
      items: [{ id: '33333333-3333-4333-8333-333333333333', serviceId: '44444444-4444-4444-8444-444444444444', description: 'Diseño', quantity: '1', unitPrice: '1250.5000', amount: '1250.5000' }],
      createdAt: '2026-11-13T10:00:00Z', updatedAt: '2026-11-13T10:00:00Z',
    })
    expect(result.total).toBe(1250.5)
    expect(result.items.at(0)?.amount).toBe(1250.5)
  })

  it('rejects an unknown proposal status', () => {
    expect(() => proposalSchema.parse({ status: 'UNKNOWN' })).toThrow()
  })
})
