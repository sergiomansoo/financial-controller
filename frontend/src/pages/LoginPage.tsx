import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError, login } from '../lib/api'
import { useAuth } from '../lib/auth'

export function LoginPage() {
  const navigate = useNavigate()
  const { saveSession } = useAuth()
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    const formData = new FormData(event.currentTarget)

    try {
      const response = await login({
        email: String(formData.get('email')),
        password: String(formData.get('password')),
      })
      saveSession({ token: response.accessToken, user: response.user })
      navigate('/dashboard', { replace: true })
    } catch (caughtError) {
      setError(
        caughtError instanceof ApiError
          ? caughtError.message
          : 'Não foi possível entrar. Tente novamente.',
      )
    }
  }

  return (
    <main className="ledger-auth-layout"><section className="ledger-auth-card">
      <h1>Entrar</h1><form className="ledger-auth-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">Email</label>
          <input className="ledger-auth-control" id="email" name="email" required type="email" />
        </div>
        <div>
          <label htmlFor="password">Senha</label>
          <input className="ledger-auth-control" id="password" name="password" required type="password" />
        </div>
        {error && <p role="alert">{error}</p>}
        <button className="ledger-button" type="submit">
          Entrar
        </button>
      </form>
      <p>
        Não tem uma conta? <Link to="/register">Criar conta</Link>
      </p>
      </section></main>
  )
}
