import { z } from 'zod'
import { es } from '@/i18n/es'

export const JOB_STATUSES = [
  'PENDING',
  'INTERESTING',
  'PREPARING_APPLICATION',
  'APPLICATION_SENT',
  'INTERVIEW',
  'OFFER_RECEIVED',
  'REJECTED',
  'DISCARDED',
] as const

export const createJobOpportunitySchema = z.object({
  companyName: z
    .string()
    .max(200, es.form.companyMax)
    .refine((value) => value.trim().length > 0, es.form.companyRequired),
  title: z
    .string()
    .max(200, es.form.titleMax)
    .refine((value) => value.trim().length > 0, es.form.titleRequired),
  url: z
    .string()
    .max(2048, es.form.urlMax)
    .refine(
      (value) => value.trim().length === 0 || /^https?:\/\/\S+$/.test(value),
      es.form.urlPattern,
    ),
  source: z.string().max(100, es.form.sourceMax),
  postedOn: z
    .string()
    .refine(
      (value) => value.length === 0 || z.iso.date().safeParse(value).success,
      es.form.dateInvalid,
    ),
  description: z.string().max(20000, es.form.descriptionMax),
  requirements: z.string().max(20000, es.form.requirementsMax),
  technologies: z
    .array(
      z
        .string()
        .max(50, es.form.technologyMax)
        .refine((value) => value.trim().length > 0, es.form.technologyBlank),
    )
    .max(30, es.form.technologiesMax),
  status: z.enum(JOB_STATUSES),
})

export type JobOpportunityFormValues = z.infer<typeof createJobOpportunitySchema>

export interface CreateJobOpportunityRequest {
  companyName: string
  title: string
  url?: string
  source?: string
  postedOn?: string
  description?: string
  requirements?: string
  technologies?: string[]
  status?: (typeof JOB_STATUSES)[number]
}

export interface JobOpportunity {
  id: string
  companyName: string
  title: string
  url: string | null
  source: string | null
  postedOn: string | null
  description: string | null
  requirements: string | null
  technologies: string[]
  status: (typeof JOB_STATUSES)[number]
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
