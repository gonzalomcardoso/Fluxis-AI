import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Textarea } from '@/components/ui/Textarea'
import { es } from '@/i18n/es'
import { ApiError } from '@/lib/api-error'
import { FieldError } from '@/features/job-opportunities/components/FieldError'
import { useCreateJobOpportunity } from '@/features/job-opportunities/api/job-opportunities.queries'
import {
  createJobOpportunitySchema,
  JOB_STATUSES,
} from '@/features/job-opportunities/schemas/job-opportunity.schema'
import type {
  CreateJobOpportunityRequest,
  JobOpportunityFormValues,
} from '@/features/job-opportunities/schemas/job-opportunity.schema'

const formFields = [
  'companyName',
  'title',
  'url',
  'source',
  'postedOn',
  'description',
  'requirements',
  'status',
] as const

type FormField = (typeof formFields)[number] | `technologies.${number}`
const knownFormFields = new Set<string>(formFields)

function isFormField(field: string): field is FormField {
  return knownFormFields.has(field) || /^technologies\.\d+$/.test(field)
}

function normalizeOptional(value: string): string | undefined {
  const normalized = value.trim()
  return normalized.length > 0 ? normalized : undefined
}

function toRequest(values: JobOpportunityFormValues): CreateJobOpportunityRequest {
  return {
    companyName: values.companyName.trim(),
    title: values.title.trim(),
    url: normalizeOptional(values.url),
    source: normalizeOptional(values.source),
    postedOn: normalizeOptional(values.postedOn),
    description: normalizeOptional(values.description),
    requirements: normalizeOptional(values.requirements),
    technologies: values.technologies.map((technology) => technology.trim()),
    status: values.status,
  }
}

function toFormField(field: string): FormField | undefined {
  const normalized = field.replace(/^technologies\[(\d+)\]$/, 'technologies.$1')
  return isFormField(normalized) ? normalized : undefined
}

export function JobOpportunityForm() {
  const navigate = useNavigate()
  const createMutation = useCreateJobOpportunity()
  const {
    control,
    register,
    handleSubmit,
    setError,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<JobOpportunityFormValues>({
    resolver: zodResolver(createJobOpportunitySchema),
    defaultValues: {
      companyName: '',
      title: '',
      url: '',
      source: '',
      postedOn: '',
      description: '',
      requirements: '',
      technologies: [],
      status: 'PENDING',
    },
  })
  const technologies = useWatch({ control, name: 'technologies' })

  async function onSubmit(values: JobOpportunityFormValues) {
    try {
      await createMutation.mutateAsync(toRequest(values))
      navigate('/oportunidades', {
        state: { notice: es.list.createdNotice },
      })
    } catch (error: unknown) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
        let mappedFieldError = false
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          const formField = toFormField(field)
          if (formField) {
            setError(formField, { type: 'server', message })
            mappedFieldError = true
          }
        }
        if (mappedFieldError) {
          return
        }
      }

      setError('root.server', {
        type: 'server',
        message:
          error instanceof ApiError ? error.message : es.form.requestFailed,
      })
    }
  }

  const submissionError = errors.root?.server?.message
  const isPending = isSubmitting || createMutation.isPending

  return (
    <section className="mx-auto max-w-3xl">
      <div className="mb-7">
        <p className="text-sm font-semibold uppercase tracking-wide text-indigo-700">
          {es.form.eyebrow}
        </p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">
          {es.form.title}
        </h1>
        <p className="mt-2 text-slate-600">{es.form.description}</p>
      </div>

      <form
        className="space-y-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-8"
        noValidate
        onSubmit={handleSubmit(onSubmit)}
      >
        <p className="text-sm text-slate-500">{es.form.requiredHint}</p>
        {submissionError && <Alert variant="error">{submissionError}</Alert>}

        <div className="grid gap-5 sm:grid-cols-2">
          <div>
            <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="companyName">
              {es.form.companyName} *
            </label>
            <Input
              autoComplete="organization"
              id="companyName"
              maxLength={200}
              aria-invalid={Boolean(errors.companyName)}
              aria-describedby={errors.companyName ? 'companyName-error' : undefined}
              {...register('companyName')}
              placeholder={es.form.companyNamePlaceholder}
            />
            <FieldError id="companyName-error" message={errors.companyName?.message} />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="title">
              {es.form.titleLabel} *
            </label>
            <Input
              id="title"
              maxLength={200}
              aria-invalid={Boolean(errors.title)}
              aria-describedby={errors.title ? 'title-error' : undefined}
              {...register('title')}
              placeholder={es.form.titlePlaceholder}
            />
            <FieldError id="title-error" message={errors.title?.message} />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="url">
              {es.form.url}
            </label>
            <Input
              id="url"
              type="url"
              maxLength={2048}
              aria-invalid={Boolean(errors.url)}
              aria-describedby={errors.url ? 'url-error' : undefined}
              {...register('url')}
              placeholder={es.form.urlPlaceholder}
            />
            <FieldError id="url-error" message={errors.url?.message} />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="source">
              {es.form.source}
            </label>
            <Input
              id="source"
              maxLength={100}
              aria-invalid={Boolean(errors.source)}
              aria-describedby={errors.source ? 'source-error' : undefined}
              {...register('source')}
              placeholder={es.form.sourcePlaceholder}
            />
            <FieldError id="source-error" message={errors.source?.message} />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="postedOn">
              {es.form.postedOn}
            </label>
            <Input
              id="postedOn"
              type="date"
              aria-invalid={Boolean(errors.postedOn)}
              aria-describedby={errors.postedOn ? 'postedOn-error' : undefined}
              {...register('postedOn')}
            />
            <FieldError id="postedOn-error" message={errors.postedOn?.message} />
          </div>

          <div>
            <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="status">
              {es.form.status}
            </label>
            <Select id="status" {...register('status')}>
              {JOB_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {es.statuses[status]}
                </option>
              ))}
            </Select>
          </div>
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="description">
            {es.form.descriptionLabel}
          </label>
          <Textarea
            id="description"
            maxLength={20000}
            aria-invalid={Boolean(errors.description)}
            aria-describedby={errors.description ? 'description-error' : undefined}
            {...register('description')}
            placeholder={es.form.descriptionPlaceholder}
          />
          <FieldError id="description-error" message={errors.description?.message} />
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-semibold text-slate-800" htmlFor="requirements">
            {es.form.requirements}
          </label>
          <Textarea
            id="requirements"
            maxLength={20000}
            aria-invalid={Boolean(errors.requirements)}
            aria-describedby={errors.requirements ? 'requirements-error' : undefined}
            {...register('requirements')}
            placeholder={es.form.requirementsPlaceholder}
          />
          <FieldError id="requirements-error" message={errors.requirements?.message} />
        </div>

        <fieldset className="space-y-3">
          <legend className="text-sm font-semibold text-slate-800">{es.form.technologies}</legend>
          {technologies.map((technology, index) => {
            const fieldName = `technologies.${index}` as const
            const errorId = `technology-${index}-error`
            return (
              <div className="flex items-start gap-2" key={fieldName}>
                <div className="min-w-0 flex-1">
                  <label className="sr-only" htmlFor={`technology-${index}`}>
                    {`${es.form.technologies} ${index + 1}`}
                  </label>
                  <Input
                    id={`technology-${index}`}
                    maxLength={50}
                    value={technology}
                    aria-invalid={Boolean(errors.technologies?.[index])}
                    aria-describedby={errors.technologies?.[index] ? errorId : undefined}
                    onChange={(event) => {
                      const updated = [...technologies]
                      updated[index] = event.target.value
                      setValue('technologies', updated, { shouldDirty: true })
                    }}
                    placeholder={es.form.technologyPlaceholder}
                  />
                  <FieldError
                    id={errorId}
                    message={errors.technologies?.[index]?.message}
                  />
                </div>
                <Button
                  className="shrink-0"
                  disabled={isPending}
                  onClick={() =>
                    setValue(
                      'technologies',
                      technologies.filter((_, technologyIndex) => technologyIndex !== index),
                      { shouldDirty: true },
                    )
                  }
                  type="button"
                  variant="secondary"
                  aria-label={es.form.removeTechnology(technology || `${index + 1}`)}
                >
                  ×
                </Button>
              </div>
            )
          })}
          {typeof errors.technologies?.message === 'string' && (
            <FieldError id="technologies-error" message={errors.technologies.message} />
          )}
          <p className="text-xs text-slate-500">{es.form.technologyLimit}</p>
          <Button
            disabled={technologies.length >= 30 || isPending}
            onClick={() =>
              setValue('technologies', [...technologies, ''], { shouldDirty: true })
            }
            type="button"
            variant="secondary"
          >
            {es.form.addTechnology}
          </Button>
        </fieldset>

        <div className="flex flex-col-reverse gap-3 border-t border-slate-100 pt-5 sm:flex-row sm:justify-end">
          <Link
            className="inline-flex min-h-11 items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100"
            to="/oportunidades"
          >
            {es.form.cancel}
          </Link>
          <Button disabled={isPending} type="submit">
            {isPending ? es.form.submitting : es.form.submit}
          </Button>
        </div>
      </form>
    </section>
  )
}
