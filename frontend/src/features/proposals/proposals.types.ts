import { z } from 'zod'
import { uuidSchema } from '@/lib/uuid.schema'
export const proposalStatuses=['DRAFT','PENDING_REVIEW','SENT','ACCEPTED','REJECTED','EXPIRED'] as const
export const proposalItemSchema=z.object({id:uuidSchema,serviceId:uuidSchema,description:z.string(),quantity:z.coerce.number(),unitPrice:z.coerce.number(),amount:z.coerce.number()})
export const proposalSchema=z.object({id:uuidSchema,clientId:uuidSchema,title:z.string(),description:z.string().nullable(),currency:z.string(),validUntil:z.string().nullable(),status:z.enum(proposalStatuses),subtotal:z.coerce.number(),total:z.coerce.number(),items:z.array(proposalItemSchema),createdAt:z.iso.datetime({offset:true}),updatedAt:z.iso.datetime({offset:true})})
export const proposalPageSchema=z.object({content:z.array(proposalSchema),page:z.number(),size:z.number(),totalElements:z.number(),totalPages:z.number()})
export type Proposal=z.infer<typeof proposalSchema>; export type ProposalItemRequest={serviceId:string;description:string;quantity:number;unitPrice:number}; export type ProposalRequest={clientId:string;title:string;description?:string;currency:string;validUntil?:string;status?:typeof proposalStatuses[number];items:ProposalItemRequest[]}
