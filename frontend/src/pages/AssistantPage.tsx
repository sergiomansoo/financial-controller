import { type FormEvent, useState } from 'react'

import { ApiError, askAssistant } from '../lib/api'
import type { AssistantChatRequest, AssistantHistoryMessage } from '../types/api'

const currentMonth = () => new Date().toISOString().slice(0, 7)
const fallbackError = 'Não foi possível enviar a pergunta. Tente novamente.'

export function AssistantPage() {
  const [month, setMonth] = useState(currentMonth)
  const [messages, setMessages] = useState<AssistantHistoryMessage[]>([])
  const [draft, setDraft] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastUnsentRequest, setLastUnsentRequest] = useState<AssistantChatRequest | null>(null)

  async function send(request: AssistantChatRequest) {
    setSubmitting(true)
    setError(null)

    try {
      const response = await askAssistant(request)
      const nextMessages: AssistantHistoryMessage[] = [
        ...request.history,
        { role: 'user', content: request.message },
        { role: 'assistant', content: response.message },
      ]
      setMessages(nextMessages.slice(-10))
      setDraft((currentDraft) => currentDraft.trim() === request.message ? '' : currentDraft)
      setLastUnsentRequest(null)
    } catch (caughtError) {
      setError(caughtError instanceof ApiError ? caughtError.message : fallbackError)
      setLastUnsentRequest(request)
    } finally {
      setSubmitting(false)
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const message = draft.trim()

    if (!message || submitting) {
      return
    }

    void send({ message, month, history: messages })
  }

  return (
    <main className="mx-auto max-w-3xl space-y-6 py-8">
      <div>
        <h1 className="text-3xl font-bold">Assistente financeiro</h1>
        <p className="mt-2 text-slate-600">
          Faça perguntas sobre os seus dados financeiros do mês selecionado.
        </p>
      </div>

      <div>
        <label className="font-medium" htmlFor="assistant-month">Mês de análise</label>
        <input
          className="mt-1 block rounded border border-slate-300 bg-white p-2"
          id="assistant-month"
          onChange={(event) => setMonth(event.target.value)}
          type="month"
          value={month}
        />
      </div>

      <section
        aria-label="Conversa com o assistente"
        aria-live="polite"
        className="min-h-48 space-y-3 rounded-lg border border-slate-200 bg-white p-4"
      >
        {messages.length === 0 && (
          <p className="text-slate-500">A conversa aparecerá aqui.</p>
        )}
        {messages.map((message, index) => (
          <div
            className={message.role === 'user' ? 'text-right' : 'text-left'}
            key={`${message.role}-${index}`}
          >
            <p className="text-sm font-semibold">
              {message.role === 'user' ? 'Você' : 'Assistente'}
            </p>
            <p className="whitespace-pre-wrap">{message.content}</p>
          </div>
        ))}
      </section>

      <form className="space-y-3" onSubmit={handleSubmit}>
        <div>
          <label className="font-medium" htmlFor="assistant-question">Pergunta</label>
          <textarea
            className="mt-1 block min-h-28 w-full rounded border border-slate-300 bg-white p-3"
            id="assistant-question"
            onChange={(event) => setDraft(event.target.value)}
            value={draft}
          />
        </div>
        <p className="text-sm text-slate-600">
          Ao enviar uma pergunta, os dados financeiros necessários para a análise são processados pelo provedor de IA Groq. Não envie senhas, chaves ou dados que não estejam no seu controle financeiro.
        </p>
        {submitting && <p role="status">Enviando...</p>}
        {error && <p role="alert">{error}</p>}
        <div className="flex gap-3">
          <button
            className="rounded bg-slate-900 px-4 py-2 text-white disabled:cursor-not-allowed disabled:opacity-50"
            disabled={!draft.trim() || submitting}
            type="submit"
          >
            Enviar
          </button>
          {lastUnsentRequest && (
            <button
              className="rounded border border-slate-400 px-4 py-2 disabled:opacity-50"
              disabled={submitting}
              onClick={() => void send(lastUnsentRequest)}
              type="button"
            >
              Tentar novamente
            </button>
          )}
        </div>
      </form>
    </main>
  )
}
