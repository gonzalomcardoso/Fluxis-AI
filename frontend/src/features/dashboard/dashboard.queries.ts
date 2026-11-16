import { useQuery } from '@tanstack/react-query'
import { getDashboard } from './dashboard.api'

export const useDashboard = () => useQuery({ queryKey: ['dashboard'], queryFn: getDashboard })
