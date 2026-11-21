import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import * as api from './crm.api'; import type { CompanyRequest, ContactRequest } from './crm.types'
export const crmKeys={companies:['companies'] as const,contacts:['contacts'] as const}
export function useCompanies(){return useQuery({queryKey:crmKeys.companies,queryFn:api.getCompanies})}; export function useContacts(){return useQuery({queryKey:crmKeys.contacts,queryFn:api.getContacts})}
function useRefresh(){const q=useQueryClient();return async()=>{await Promise.all([q.invalidateQueries({queryKey:crmKeys.companies}),q.invalidateQueries({queryKey:crmKeys.contacts})])}}
export function useSaveCompany(){const refresh=useRefresh();return useMutation({mutationFn:(v:{id?:string;request:CompanyRequest})=>v.id?api.updateCompany(v.id,v.request):api.createCompany(v.request),onSuccess:refresh})}
export function useDeleteCompany(){const refresh=useRefresh();return useMutation({mutationFn:api.deleteCompany,onSuccess:refresh})}
export function useSaveContact(){const refresh=useRefresh();return useMutation({mutationFn:(v:{id?:string;request:ContactRequest})=>v.id?api.updateContact(v.id,v.request):api.createContact(v.request),onSuccess:refresh})}
export function useDeleteContact(){const refresh=useRefresh();return useMutation({mutationFn:api.deleteContact,onSuccess:refresh})}
