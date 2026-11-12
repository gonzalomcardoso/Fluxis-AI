import { z } from 'zod'
import { apiRequest } from '@/lib/api-client'
import { applicationSchema, followUpSchema, interviewSchema, pageSchema, type CreateApplicationRequest, type FollowUpRequest, type InterviewRequest, type UpdateApplicationRequest } from './application.types'
const pageResponse=pageSchema
export async function getApplications(){return pageResponse.parse(await apiRequest('/job-applications?page=0&size=100&sort=createdAt,desc'))}
export async function createApplication(request:CreateApplicationRequest){return applicationSchema.parse(await apiRequest('/job-applications',{method:'POST',body:JSON.stringify(request)}))}
export async function updateApplication(id:string,request:UpdateApplicationRequest){return applicationSchema.parse(await apiRequest(`/job-applications/${id}`,{method:'PUT',body:JSON.stringify(request)}))}
export async function deleteApplication(id:string){await apiRequest(`/job-applications/${id}`,{method:'DELETE'})}
export async function getInterviews(applicationId:string){return z.array(interviewSchema).parse(await apiRequest(`/job-applications/${applicationId}/interviews`))}
export async function createInterview(applicationId:string,request:InterviewRequest){return interviewSchema.parse(await apiRequest(`/job-applications/${applicationId}/interviews`,{method:'POST',body:JSON.stringify(request)}))}
export async function getFollowUps(applicationId:string){return z.array(followUpSchema).parse(await apiRequest(`/job-applications/${applicationId}/follow-ups`))}
export async function createFollowUp(applicationId:string,request:FollowUpRequest){return followUpSchema.parse(await apiRequest(`/job-applications/${applicationId}/follow-ups`,{method:'POST',body:JSON.stringify(request)}))}
