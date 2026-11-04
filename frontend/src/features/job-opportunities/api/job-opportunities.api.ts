import { z } from 'zod'
import { apiRequest } from '@/lib/api-client'
import type {
  CreateJobOpportunityRequest,
  JobOpportunity,
  PageResponse,
} from '@/features/job-opportunities/schemas/job-opportunity.schema'
import { JOB_STATUSES } from '@/features/job-opportunities/schemas/job-opportunity.schema'

const jobOpportunityResponseSchema = z.object({
  id: z.string().uuid(),
  companyName: z.string(),
  title: z.string(),
  url: z.string().nullable(),
  source: z.string().nullable(),
  postedOn: z.iso.date().nullable(),
  description: z.string().nullable(),
  requirements: z.string().nullable(),
  technologies: z.array(z.string()),
  status: z.enum(JOB_STATUSES),
  createdAt: z.iso.datetime({ offset: true }),
  updatedAt: z.iso.datetime({ offset: true }),
})

const pageResponseSchema = z.object({
  content: z.array(jobOpportunityResponseSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
})

export async function getJobOpportunities(): Promise<PageResponse<JobOpportunity>> {
  const response = await apiRequest('/job-opportunities?page=0&size=100&sort=createdAt,desc')
  return pageResponseSchema.parse(response)
}

export async function createJobOpportunity(
  request: CreateJobOpportunityRequest,
): Promise<JobOpportunity> {
  const response = await apiRequest('/job-opportunities', {
    method: 'POST',
    body: JSON.stringify(request),
  })
  return jobOpportunityResponseSchema.parse(response)
}
