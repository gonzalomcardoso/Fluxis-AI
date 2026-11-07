import { useState, type FormEvent } from 'react'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { es } from '@/i18n/es'
import { ApiError } from '@/lib/api-error'
import { useAuth } from '@/features/auth/auth-context'

export function AuthPage() {
  const { login, register } = useAuth()
  const registrationEnabled = import.meta.env.MODE !== 'prod'
  const [isRegistering, setIsRegistering] = useState(false)
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      if (isRegistering) {
        await register(displayName, email, password)
      } else {
        await login(email, password)
      }
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : es.auth.requestFailed)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-12">
      <section className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-7 shadow-sm sm:p-9">
        <p className="text-sm font-bold tracking-tight text-indigo-700">Fluxis AI</p>
        <h1 className="mt-3 text-2xl font-bold tracking-tight text-slate-950">
          {isRegistering ? es.auth.registerTitle : es.auth.loginTitle}
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          {isRegistering ? es.auth.registerDescription : es.auth.loginDescription}
        </p>

        {error && <Alert className="mt-5" variant="error">{error}</Alert>}

        <form className="mt-6 space-y-4" onSubmit={(event) => void handleSubmit(event)}>
          {isRegistering && (
            <label className="block space-y-1.5">
              <span className="text-sm font-medium text-slate-700">{es.auth.displayName}</span>
              <input
                autoComplete="name"
                className="min-h-11 w-full rounded-lg border border-slate-300 px-3 text-slate-900"
                maxLength={200}
                onChange={(event) => setDisplayName(event.target.value)}
                required
                value={displayName}
              />
            </label>
          )}
          <label className="block space-y-1.5">
            <span className="text-sm font-medium text-slate-700">{es.auth.email}</span>
            <input
              autoComplete="email"
              className="min-h-11 w-full rounded-lg border border-slate-300 px-3 text-slate-900"
              maxLength={320}
              onChange={(event) => setEmail(event.target.value)}
              required
              type="email"
              value={email}
            />
          </label>
          <div className="space-y-1.5">
            <label className="block text-sm font-medium text-slate-700" htmlFor="auth-password">
              {es.auth.password}
            </label>
            <input
              autoComplete={isRegistering ? 'new-password' : 'current-password'}
              className="min-h-11 w-full rounded-lg border border-slate-300 px-3 text-slate-900"
              id="auth-password"
              maxLength={72}
              minLength={isRegistering ? 12 : undefined}
              onChange={(event) => setPassword(event.target.value)}
              required
              type="password"
              value={password}
              aria-describedby={isRegistering ? 'auth-password-hint' : undefined}
            />
            {isRegistering && (
              <p className="text-xs text-slate-500" id="auth-password-hint">{es.auth.passwordHint}</p>
            )}
          </div>
          <Button className="w-full" disabled={isSubmitting} type="submit">
            {isSubmitting
              ? es.auth.submitting
              : isRegistering ? es.auth.registerAction : es.auth.loginAction}
          </Button>
        </form>

        {registrationEnabled && (
          <p className="mt-6 text-center text-sm text-slate-600">
            {isRegistering ? es.auth.hasAccount : es.auth.noAccount}{' '}
            <button
              className="font-semibold text-indigo-700 hover:text-indigo-900"
              onClick={() => {
                setIsRegistering(!isRegistering)
                setError(null)
              }}
              type="button"
            >
              {isRegistering ? es.auth.loginAction : es.auth.registerAction}
            </button>
          </p>
        )}
      </section>
    </main>
  )
}
