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
          : 'Unable to sign in. Please try again.',
      )
    }
  }

  return (
    <main className="mx-auto max-w-md p-6">
      <h1 className="text-3xl font-bold">Sign in</h1>
      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="email">Email</label>
          <input className="block w-full border p-2" id="email" name="email" required type="email" />
        </div>
        <div>
          <label htmlFor="password">Password</label>
          <input className="block w-full border p-2" id="password" name="password" required type="password" />
        </div>
        {error && <p role="alert">{error}</p>}
        <button className="rounded bg-slate-900 px-4 py-2 text-white" type="submit">
          Sign in
        </button>
      </form>
      <p className="mt-4">
        Need an account? <Link to="/register">Register</Link>
      </p>
    </main>
  )
}
