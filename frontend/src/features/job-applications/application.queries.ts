import { useMutation,useQuery,useQueryClient } from '@tanstack/react-query'
import * as api from './application.api'; import type { CreateApplicationRequest,FollowUpRequest,InterviewRequest,UpdateApplicationRequest } from './application.types'
export const applicationKeys={all:['job-applications'] as const,interviews:(id:string)=>['job-applications',id,'interviews'] as const,followUps:(id:string)=>['job-applications',id,'follow-ups'] as const}
export function useApplications(){return useQuery({queryKey:applicationKeys.all,queryFn:api.getApplications})}
function refresh(){const query=useQueryClient();return ()=>query.invalidateQueries({queryKey:applicationKeys.all})}
export function useCreateApplication(){const r=refresh();return useMutation({mutationFn:(v:CreateApplicationRequest)=>api.createApplication(v),onSuccess:r})}
export function useUpdateApplication(){const r=refresh();return useMutation({mutationFn:(v:{id:string;request:UpdateApplicationRequest})=>api.updateApplication(v.id,v.request),onSuccess:r})}
export function useDeleteApplication(){const r=refresh();return useMutation({mutationFn:api.deleteApplication,onSuccess:r})}
export function useInterviews(id:string){return useQuery({queryKey:applicationKeys.interviews(id),queryFn:()=>api.getInterviews(id)})}
export function useCreateInterview(id:string){const q=useQueryClient();return useMutation({mutationFn:(r:InterviewRequest)=>api.createInterview(id,r),onSuccess:()=>q.invalidateQueries({queryKey:applicationKeys.interviews(id)})})}
export function useFollowUps(id:string){return useQuery({queryKey:applicationKeys.followUps(id),queryFn:()=>api.getFollowUps(id)})}
export function useCreateFollowUp(id:string){const q=useQueryClient();return useMutation({mutationFn:(r:FollowUpRequest)=>api.createFollowUp(id,r),onSuccess:()=>q.invalidateQueries({queryKey:applicationKeys.followUps(id)})})}
