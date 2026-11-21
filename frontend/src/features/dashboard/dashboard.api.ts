import { apiRequest } from '@/lib/api-client'
import { dashboardSchema } from './dashboard.types'

export async function getDashboard() {
  return dashboardSchema.parse(await apiRequest('/dashboard'))
}
