import { z } from 'zod'

export const CLIENT_STATUSES=['POTENCIAL','ACTIVE','INACTIVE','LOST'] as const
export const PRICING_MODELS=['HOURLY','FIXED','RECURRING'] as const
export const SERVICE_STATUSES=['ACTIVE','INACTIVE'] as const
export const clientSchema=z.object({id:z.string().uuid(),companyId:z.string().uuid().nullable(),contactId:z.string().uuid().nullable(),displayName:z.string(),status:z.enum(CLIENT_STATUSES),origin:z.string().nullable(),notes:z.string().nullable(),createdAt:z.iso.datetime({offset:true}),updatedAt:z.iso.datetime({offset:true})})
export const serviceSchema=z.object({id:z.string().uuid(),name:z.string(),description:z.string().nullable(),category:z.string().nullable(),pricingModel:z.enum(PRICING_MODELS),rate:z.coerce.number(),currency:z.string(),available:z.boolean(),status:z.enum(SERVICE_STATUSES),notes:z.string().nullable(),createdAt:z.iso.datetime({offset:true}),updatedAt:z.iso.datetime({offset:true})})
export const pageSchema=<T extends z.ZodType>(item:T)=>z.object({content:z.array(item),page:z.number(),size:z.number(),totalElements:z.number(),totalPages:z.number()})
export type Client=z.infer<typeof clientSchema>; export type ProfessionalService=z.infer<typeof serviceSchema>
export type ClientRequest={companyId?:string;contactId?:string;displayName:string;status?:typeof CLIENT_STATUSES[number];origin?:string;notes?:string}
export type ServiceRequest={name:string;description?:string;category?:string;pricingModel:typeof PRICING_MODELS[number];rate:string;currency:string;available:boolean;status?:typeof SERVICE_STATUSES[number];notes?:string}
