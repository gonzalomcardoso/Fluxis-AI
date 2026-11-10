import { z } from 'zod'
export const companySchema = z.object({ id: z.string().uuid(), name: z.string(), website: z.string().nullable(), industry: z.string().nullable(), notes: z.string().nullable(), createdAt: z.iso.datetime({ offset: true }), updatedAt: z.iso.datetime({ offset: true }) })
export const contactSchema = z.object({ id: z.string().uuid(), companyId: z.string().uuid().nullable(), firstName: z.string(), lastName: z.string(), email: z.string().nullable(), jobTitle: z.string().nullable(), origin: z.string().nullable(), notes: z.string().nullable(), createdAt: z.iso.datetime({ offset: true }), updatedAt: z.iso.datetime({ offset: true }) })
export const pageSchema = <T extends z.ZodType>(item: T) => z.object({ content: z.array(item), page: z.number(), size: z.number(), totalElements: z.number(), totalPages: z.number() })
export type Company = z.infer<typeof companySchema>; export type Contact = z.infer<typeof contactSchema>
export type CompanyRequest = { name: string; website?: string; industry?: string; notes?: string }
export type ContactRequest = { companyId?: string; firstName: string; lastName: string; email?: string; jobTitle?: string; origin?: string; notes?: string }
