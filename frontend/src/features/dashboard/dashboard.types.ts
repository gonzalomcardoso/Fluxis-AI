import { z } from 'zod'

const money = z.object({ currency: z.string(), amount: z.coerce.number() })
export const dashboardSchema = z.object({
  generatedAt: z.iso.datetime({ offset: true }),
  clients: z.object({ total: z.number(), potential: z.number(), active: z.number() }),
  services: z.object({ total: z.number(), active: z.number() }),
  proposals: z.object({ total: z.number(), byStatus: z.record(z.string(), z.number()), acceptedByCurrency: z.array(money) }),
  subscriptions: z.object({ active: z.number(), monthlyByCurrency: z.array(money), annualByCurrency: z.array(money), upcomingRenewals: z.number() }),
  tasks: z.object({ total: z.number(), pending: z.number(), inProgress: z.number(), done: z.number(), overdue: z.number() }),
  margins: z.array(z.object({ currency: z.string(), acceptedRevenue: z.coerce.number(), monthlySubscriptionCost: z.coerce.number(), margin: z.coerce.number() })),
})
export type Dashboard = z.infer<typeof dashboardSchema>
