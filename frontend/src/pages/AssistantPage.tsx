import { type FormEvent, type KeyboardEvent, useState } from 'react'
import { ArrowUp, Bot, RotateCcw } from 'lucide-react'

import { ApiError, askAssistant } from '../lib/api'
import type { AssistantChatRequest, AssistantHistoryMessage } from '../types/api'
import './AssistantPage.css'

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
    setSubmitting(true); setError(null)
    try {
      const response = await askAssistant(request)
      const nextMessages: AssistantHistoryMessage[] = [...request.history, { role: 'user', content: request.message }, { role: 'assistant', content: response.message }]
      setMessages(nextMessages.slice(-10)); setDraft((value) => value.trim() === request.message ? '' : value); setLastUnsentRequest(null)
    } catch (caughtError) { setError(caughtError instanceof ApiError ? caughtError.message : fallbackError); setLastUnsentRequest(request) } finally { setSubmitting(false) }
  }

  function submitDraft() { const message = draft.trim(); if (message && !submitting) void send({ message, month, history: messages }) }
  function handleSubmit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); submitDraft() }
  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); submitDraft() } }

  return <section className="ledger-page monolith-assistant" aria-label="Assistente financeiro">
    <header className="page-title monolith-header"><div><p>ANÁLISE COM IA</p><h1>Assistente financeiro</h1></div><label className="monolith-month">Mês<input aria-label="Mês de análise" onChange={(event) => setMonth(event.target.value)} type="month" value={month} /></label></header>
    <section aria-label="Conversa com o assistente" aria-live="polite" className="monolith-panel assistant-transcript">{messages.length === 0 ? <div className="assistant-empty"><Bot size={26} /><p>A conversa aparecerá aqui.</p></div> : messages.map((message, index) => <article className={`assistant-message assistant-message--${message.role}`} key={`${message.role}-${index}`}><small>{message.role === 'user' ? 'VOCÊ' : 'ASSISTENTE'}</small><p>{message.content}</p></article>)}</section>
    <form className="monolith-panel assistant-form" onSubmit={handleSubmit}>
      <label className="sr-only" htmlFor="assistant-question">Pergunta</label>
      <div className="assistant-chat-input"><textarea id="assistant-question" onChange={(event) => setDraft(event.target.value)} onKeyDown={handleKeyDown} placeholder="Pergunte sobre suas finanças..." rows={1} value={draft} /><button aria-label="Enviar pergunta" className="assistant-send" disabled={!draft.trim() || submitting} type="submit"><ArrowUp aria-hidden size={18} /></button></div>
      <p className="assistant-hint">Enter envia · Shift + Enter quebra linha</p>
      <p className="assistant-privacy">Ao enviar uma pergunta, os dados financeiros necessários para a análise são processados pelo provedor de IA Groq. Não envie senhas, chaves ou dados que não estejam no seu controle financeiro.</p>
      {submitting && <p role="status">Enviando...</p>}{error && <p className="ledger-alert" role="alert">{error}</p>}
      {lastUnsentRequest && <div className="assistant-actions"><button className="ledger-button ledger-button--secondary" disabled={submitting} onClick={() => void send(lastUnsentRequest)} type="button"><RotateCcw size={16} /> Tentar novamente</button></div>}
    </form>
  </section>
}
