import { apiRequest } from '@/lib/api-client'
import { applicationSchema, followUpSchema, interviewSchema, pageSchema, type CreateApplicationRequest, type FollowUpRequest, type InterviewRequest, type UpdateApplicationRequest } from './application.types'
export async function getApplications(){return apiRequest('/job-applications?page=0&size=100&sort=createdAt,desc', undefined, pageSchema)}
export async function createApplication(request:CreateApplicationRequest){return apiRequest('/job-applications',{method:'POST',body:JSON.stringify(request)}, applicationSchema)}
export async function updateApplication(id:string,request:UpdateApplicationRequest){return apiRequest(`/job-applications/${id}`,{method:'PUT',body:JSON.stringify(request)}, applicationSchema)}
export async function deleteApplication(id:string){await apiRequest(`/job-applications/${id}`,{method:'DELETE'})}
export async function getInterviews(applicationId:string){return apiRequest(`/job-applications/${applicationId}/interviews`, undefined, interviewSchema.array())}
export async function createInterview(applicationId:string,request:InterviewRequest){return apiRequest(`/job-applications/${applicationId}/interviews`,{method:'POST',body:JSON.stringify(request)}, interviewSchema)}
export async function getFollowUps(applicationId:string){return apiRequest(`/job-applications/${applicationId}/follow-ups`, undefined, followUpSchema.array())}
export async function createFollowUp(applicationId:string,request:FollowUpRequest){return apiRequest(`/job-applications/${applicationId}/follow-ups`,{method:'POST',body:JSON.stringify(request)}, followUpSchema)}
