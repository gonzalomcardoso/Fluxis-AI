import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ApiError } from '@/lib/api-error'
import {
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  type AuthenticatedUser,
} from '@/features/auth/api/auth.api'
import { es } from '@/i18n/es'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'error'

interface AuthContextValue {
  user: AuthenticatedUser | null
  status: AuthStatus
  error: string | null
  login: (email: string, password: string) => Promise<void>
  register: (displayName: string, email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setStatus('loading')
    setError(null)
    try {
      const currentUser = await getCurrentUser()
      setUser(currentUser)
      setStatus('authenticated')
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 401) {
        setUser(null)
        setStatus('unauthenticated')
        return
      }
      setError(requestError instanceof Error ? requestError.message : es.auth.sessionError)
      setStatus('error')
    }
  }, [])

  useEffect(() => {
    let active = true
    getCurrentUser()
      .then((currentUser) => {
        if (active) {
          setUser(currentUser)
          setStatus('authenticated')
        }
      })
      .catch((requestError: unknown) => {
        if (!active) {
          return
        }
        if (requestError instanceof ApiError && requestError.status === 401) {
          setUser(null)
          setStatus('unauthenticated')
          return
        }
        setError(requestError instanceof Error ? requestError.message : es.auth.sessionError)
        setStatus('error')
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null)
      setStatus('unauthenticated')
      queryClient.clear()
    }
    window.addEventListener('fluxis:unauthorized', handleUnauthorized)
    return () => window.removeEventListener('fluxis:unauthorized', handleUnauthorized)
  }, [queryClient])

  const login = useCallback(async (email: string, password: string) => {
    const authenticatedUser = await loginRequest(email, password)
    queryClient.clear()
    setUser(authenticatedUser)
    setStatus('authenticated')
    setError(null)
  }, [queryClient])

  const register = useCallback(async (
    displayName: string,
    email: string,
    password: string,
  ) => {
    await registerRequest(displayName, email, password)
    await login(email, password)
  }, [login])

  const logout = useCallback(async () => {
    await logoutRequest()
    queryClient.clear()
    setUser(null)
    setStatus('unauthenticated')
  }, [queryClient])

  const value = useMemo(
    () => ({ user, status, error, login, register, logout, refresh }),
    [user, status, error, login, register, logout, refresh],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
