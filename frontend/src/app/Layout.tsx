import { NavLink, Outlet } from 'react-router'
import { useState } from 'react'
import { useAuth } from '@/features/auth/auth-context'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { es } from '@/i18n/es'
import { ApiError } from '@/lib/api-error'

export function Layout() {
  const { user, logout } = useAuth()
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  async function handleLogout() {
    setLogoutError(null)
    setIsLoggingOut(true)
    try {
      await logout()
    } catch (error) {
      setLogoutError(error instanceof ApiError ? error.message : es.auth.logoutFailed)
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4 sm:px-6">
          <NavLink
            to="/oportunidades"
            className="text-lg font-bold tracking-tight text-slate-950"
          >
            Fluxis <span className="text-indigo-600">AI</span>
          </NavLink>
          <nav aria-label={es.navigation.main} className="flex items-center gap-2 sm:gap-5">
            <NavLink
              to="/oportunidades"
              end
              className={({ isActive }) =>
                `rounded-lg px-3 py-2 text-sm font-medium ${
                  isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:text-slate-950'
                }`
              }
            >
              {es.navigation.opportunities}
            </NavLink>
            <NavLink
              to="/oportunidades/nueva"
              className="rounded-lg bg-indigo-600 px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-indigo-700"
            >
              {es.navigation.newOpportunity}
            </NavLink>
            <span className="hidden text-sm text-slate-600 md:inline">{user?.displayName}</span>
            <Button
              className="px-3"
              disabled={isLoggingOut}
              onClick={() => void handleLogout()}
              variant="quiet"
            >
              {es.auth.logout}
            </Button>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-12">
        {logoutError && <Alert className="mb-6" variant="error">{logoutError}</Alert>}
        <Outlet />
      </main>
      <footer className="mx-auto max-w-6xl px-4 pb-8 text-xs text-slate-500 sm:px-6">
        {es.footer}
      </footer>
    </div>
  )
}
