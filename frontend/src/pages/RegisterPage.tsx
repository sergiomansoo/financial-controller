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
          : 'Unable to register. Please try again.',
      )
    }
  }

  return (
    <main className="mx-auto max-w-md p-6">
      <h1 className="text-3xl font-bold">Create account</h1>
      <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="name">Name</label>
          <input className="block w-full border p-2" id="name" name="name" required />
        </div>
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
          Register
        </button>
      </form>
      <p className="mt-4">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </main>
  )
}
