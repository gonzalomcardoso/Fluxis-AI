import type {
  JobOpportunity,
  PageResponse,
} from '@/features/job-opportunities/schemas/job-opportunity.schema'

export const jobOpportunityFixture: JobOpportunity = {
  id: 'b74a71f5-e7b4-4af3-bdae-2c7af17907c9',
  companyName: 'Acme',
  title: 'Backend Developer',
  url: 'https://jobs.example.test/acme',
  source: 'Synthetic',
  postedOn: '2026-11-01',
  description: 'Synthetic description',
  requirements: 'Synthetic requirements',
  technologies: ['Java'],
  status: 'INTERESTING',
  createdAt: '2026-11-04T10:00:00Z',
  updatedAt: '2026-11-04T10:00:00Z',
}

export function pageOf(content: JobOpportunity[]): PageResponse<JobOpportunity> {
  return {
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
  }
}
