import { type FormEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { ApiError, register } from '../lib/api'
import { useAuth } from '../lib/auth'

export function RegisterPage() {
  const navigate = useNavigate()
  const { saveSession } = useAuth()
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    const formData = new FormData(event.currentTarget)

    try {
      const response = await register({
        name: String(formData.get('name')),
        email: String(formData.get('email')),
        password: String(formData.get('password')),
      })
      saveSession({ token: response.accessToken, user: response.user })
      navigate('/dashboard', { replace: true })
    } catch (caughtError) {
      setError(
        caughtError instanceof ApiError
          ? caughtError.message
          : 'Não foi possível criar a conta. Tente novamente.',
      )
    }
  }

  return (
    <main className="ledger-auth-layout"><section className="ledger-auth-card">
      <h1>Criar conta</h1><form className="ledger-auth-form" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="name">Nome</label>
          <input className="ledger-auth-control" id="name" name="name" required />
        </div>
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
          Criar conta
        </button>
      </form>
      <p>
        Já tem uma conta? <Link to="/login">Entrar</Link>
      </p>
      </section></main>
  )
}
