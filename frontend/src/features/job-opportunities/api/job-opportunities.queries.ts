import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createJobOpportunity,
  getJobOpportunities,
} from '@/features/job-opportunities/api/job-opportunities.api'
import type { CreateJobOpportunityRequest } from '@/features/job-opportunities/schemas/job-opportunity.schema'

export const jobOpportunityKeys = {
  all: ['job-opportunities'] as const,
}

export function useJobOpportunities() {
  return useQuery({
    queryKey: jobOpportunityKeys.all,
    queryFn: getJobOpportunities,
  })
}

export function useCreateJobOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: CreateJobOpportunityRequest) => createJobOpportunity(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: jobOpportunityKeys.all })
    },
  })
}
