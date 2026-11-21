import { NavLink, Outlet, useLocation } from 'react-router'
import { useEffect, useRef, useState } from 'react'
import { useAuth } from '@/features/auth/auth-context'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { es } from '@/i18n/es'
import { ApiError } from '@/lib/api-error'

interface NavigationItem {
  label: string
  to: string
}

interface NavigationGroup {
  key: 'career' | 'business'
  label: string
  items: NavigationItem[]
}

const navigationGroups: NavigationGroup[] = [
  {
    key: 'career',
    label: 'Carrera',
    items: [
      { label: 'Oportunidades', to: '/oportunidades' },
      { label: 'Candidaturas', to: '/candidaturas' },
      { label: 'Contactos', to: '/crm' },
    ],
  },
  {
    key: 'business',
    label: 'Comercial',
    items: [
      { label: 'Clientes y servicios', to: '/clientes-servicios' },
      { label: 'Propuestas', to: '/propuestas' },
      { label: 'Suscripciones', to: '/suscripciones' },
    ],
  },
]

type OpenMenu = NavigationGroup['key'] | 'user' | null

function isPathActive(pathname: string, path: string): boolean {
  return pathname === path || pathname.startsWith(`${path}/`)
}

export function Layout() {
  const location = useLocation()
  const { user, logout } = useAuth()
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [openMenu, setOpenMenu] = useState<OpenMenu>(null)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const headerRef = useRef<HTMLDivElement>(null)

  function closeNavigation() {
    setOpenMenu(null)
    setIsMobileMenuOpen(false)
  }

  useEffect(() => {
    if (!openMenu) {
      return
    }

    const handlePointerDown = (event: PointerEvent) => {
      if (!headerRef.current?.contains(event.target as Node)) {
        setOpenMenu(null)
      }
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpenMenu(null)
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [openMenu])

  async function handleLogout() {
    setLogoutError(null)
    setIsLoggingOut(true)
    setOpenMenu(null)
    try {
      await logout()
    } catch (error) {
      setLogoutError(error instanceof ApiError ? error.message : es.auth.logoutFailed)
    } finally {
      setIsLoggingOut(false)
    }
  }

  const userName = user?.displayName ?? 'Cuenta'

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div ref={headerRef} className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="flex items-center justify-between gap-4 py-4">
            <NavLink
              to="/dashboard"
              aria-label="Ir al dashboard"
              className="shrink-0 text-lg font-bold tracking-tight text-slate-950"
              onClick={closeNavigation}
            >
              Fluxis <span className="text-indigo-600">AI</span>
            </NavLink>

            <div className="hidden min-w-0 flex-1 items-center justify-center md:flex">
              <nav aria-label={es.navigation.main} className="flex items-center gap-1">
                {navigationGroups.map((group) => {
                  const isActive = group.items.some((item) => isPathActive(location.pathname, item.to))
                  const isOpen = openMenu === group.key
                  return (
                    <div className="relative" key={group.key}>
                      <button
                        type="button"
                        className={`inline-flex min-h-10 items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                          isActive
                            ? 'bg-indigo-50 text-indigo-700'
                            : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950'
                        }`}
                        aria-expanded={isOpen}
                        aria-haspopup="true"
                        aria-controls={`${group.key}-navigation`}
                        onClick={() => setOpenMenu(isOpen ? null : group.key)}
                      >
                        {group.label}
                        <span aria-hidden="true" className={`text-xs transition-transform ${isOpen ? 'rotate-180' : ''}`}>
                          ▾
                        </span>
                      </button>
                      {isOpen && (
                        <div
                          id={`${group.key}-navigation`}
                          className="absolute left-0 top-full z-30 mt-2 w-56 rounded-xl border border-slate-200 bg-white p-2 shadow-lg"
                        >
                          {group.items.map((item) => (
                            <NavLink
                              key={item.to}
                              to={item.to}
                              onClick={closeNavigation}
                              className={({ isActive: itemIsActive }) =>
                                `block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                                  itemIsActive
                                    ? 'bg-indigo-50 text-indigo-700'
                                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950'
                                }`
                              }
                            >
                              {item.label}
                            </NavLink>
                          ))}
                        </div>
                      )}
                    </div>
                  )
                })}

                <NavLink
                  to="/tareas"
                  onClick={closeNavigation}
                  className={({ isActive }) =>
                    `inline-flex min-h-10 items-center rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-indigo-50 text-indigo-700'
                        : 'text-slate-600 hover:bg-slate-50 hover:text-slate-950'
                    }`
                  }
                >
                  Agenda
                </NavLink>
              </nav>
            </div>

            <div className="flex shrink-0 items-center gap-2">
              <div className="relative">
                <button
                  type="button"
                  className="inline-flex min-h-10 items-center gap-2 rounded-lg px-2 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
                  aria-expanded={openMenu === 'user'}
                  aria-haspopup="true"
                  aria-controls="user-navigation"
                  aria-label={`Menú de usuario: ${userName}`}
                  onClick={() => setOpenMenu(openMenu === 'user' ? null : 'user')}
                >
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-indigo-100 text-xs font-bold text-indigo-700">
                    {userName.slice(0, 1).toUpperCase()}
                  </span>
                  <span className="hidden max-w-40 truncate sm:inline">{userName}</span>
                  <span aria-hidden="true" className="text-xs">▾</span>
                </button>
                {openMenu === 'user' && (
                  <div
                    id="user-navigation"
                    className="absolute right-0 top-full z-30 mt-2 w-64 rounded-xl border border-slate-200 bg-white p-2 shadow-lg"
                  >
                    <div className="border-b border-slate-100 px-3 pb-3 pt-1">
                      <p className="truncate text-sm font-semibold text-slate-900">{userName}</p>
                      {user?.email && <p className="truncate text-xs text-slate-500">{user.email}</p>}
                    </div>
                    <Button
                      className="mt-2 w-full justify-start"
                      disabled={isLoggingOut}
                      onClick={() => void handleLogout()}
                      type="button"
                      variant="quiet"
                    >
                      {isLoggingOut ? 'Cerrando sesión…' : es.auth.logout}
                    </Button>
                  </div>
                )}
              </div>

              <button
                type="button"
                className="inline-flex h-10 w-10 items-center justify-center rounded-lg text-slate-700 hover:bg-slate-50 md:hidden"
                aria-expanded={isMobileMenuOpen}
                aria-controls="mobile-navigation"
                aria-label={isMobileMenuOpen ? 'Cerrar navegación' : 'Abrir navegación'}
                onClick={() => setIsMobileMenuOpen((isOpen) => !isOpen)}
              >
                <span aria-hidden="true" className="text-xl">
                  {isMobileMenuOpen ? '×' : '☰'}
                </span>
              </button>
            </div>
          </div>

          {isMobileMenuOpen && (
            <nav id="mobile-navigation" aria-label="Navegación móvil" className="space-y-5 border-t border-slate-100 py-4 md:hidden">
              {navigationGroups.map((group) => (
                <div key={group.key}>
                  <p className="px-3 text-xs font-semibold uppercase tracking-wide text-slate-500">{group.label}</p>
                  <div className="mt-1 space-y-1">
                    {group.items.map((item) => (
                      <NavLink
                        key={item.to}
                        to={item.to}
                        onClick={closeNavigation}
                        className={({ isActive }) =>
                          `block rounded-lg px-3 py-2 text-sm font-medium ${
                            isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-50'
                          }`
                        }
                      >
                        {item.label}
                      </NavLink>
                    ))}
                  </div>
                </div>
              ))}
              <NavLink
                to="/tareas"
                onClick={closeNavigation}
                className={({ isActive }) =>
                  `block rounded-lg px-3 py-2 text-sm font-medium ${
                    isActive ? 'bg-indigo-50 text-indigo-700' : 'text-slate-600 hover:bg-slate-50'
                  }`
                }
              >
                Agenda
              </NavLink>
            </nav>
          )}
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