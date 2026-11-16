import { apiRequest } from '@/lib/api-client'
import { companySchema, contactSchema, pageSchema, type CompanyRequest, type ContactRequest } from './crm.types'
const companiesPage = pageSchema(companySchema); const contactsPage = pageSchema(contactSchema)
const list = (path:string) => apiRequest(`${path}?page=0&size=100&sort=createdAt,desc`)
export async function getCompanies(){return companiesPage.parse(await list('/companies'))}
export async function createCompany(request:CompanyRequest){return companySchema.parse(await apiRequest('/companies',{method:'POST',body:JSON.stringify(request)}))}
export async function updateCompany(id:string,request:CompanyRequest){return companySchema.parse(await apiRequest(`/companies/${id}`,{method:'PUT',body:JSON.stringify(request)}))}
export async function deleteCompany(id:string){await apiRequest(`/companies/${id}`,{method:'DELETE'})}
export async function getContacts(){return contactsPage.parse(await list('/contacts'))}
export async function createContact(request:ContactRequest){return contactSchema.parse(await apiRequest('/contacts',{method:'POST',body:JSON.stringify(request)}))}
export async function updateContact(id:string,request:ContactRequest){return contactSchema.parse(await apiRequest(`/contacts/${id}`,{method:'PUT',body:JSON.stringify(request)}))}
export async function deleteContact(id:string){await apiRequest(`/contacts/${id}`,{method:'DELETE'})}
