import { type FormEvent, type KeyboardEvent, useEffect, useRef, useState } from 'react'
import { ArrowUp, Bot, RotateCcw } from 'lucide-react'

import { AssistantVisualCard } from '../components/AssistantVisualCard'
import { AssistantMessageContent } from '../components/AssistantMessageContent'
import { ApiError, askAssistant } from '../lib/api'
import type { AssistantChatRequest, AssistantHistoryMessage, AssistantVisualType } from '../types/api'
import './AssistantPage.css'

type ChatMessage = AssistantHistoryMessage & { visualType?: AssistantVisualType; visualData?: unknown }
const MAX_HISTORY_MESSAGE_LENGTH = 6000
const currentMonth = () => new Date().toISOString().slice(0, 7)

function historyForRequest(messages: ChatMessage[]): AssistantHistoryMessage[] {
  return messages.slice(-10).map(({ role, content }) => ({ role, content: content.slice(0, MAX_HISTORY_MESSAGE_LENGTH) }))
}
const fallbackError = 'Não foi possível enviar a pergunta. Tente novamente.'

export function AssistantPage() {
  const [month, setMonth] = useState(currentMonth)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [draft, setDraft] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastUnsentRequest, setLastUnsentRequest] = useState<AssistantChatRequest | null>(null)
  const transcriptRef = useRef<HTMLElement>(null)
  useEffect(() => {
    if (messages.length === 0) return
    const reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    const transcript = transcriptRef.current
    if (!transcript || typeof transcript.scrollTo !== 'function') return
    transcript.scrollTo({ top: transcript.scrollHeight, behavior: reducedMotion ? 'auto' : 'smooth' })
  }, [messages])
  async function send(request: AssistantChatRequest) { setSubmitting(true); setError(null); try { const response = await askAssistant(request); const next: ChatMessage[] = [...request.history, { role: 'user', content: request.message }, { role: 'assistant', content: response.message, visualType: response.visualType, visualData: response.visualData }]; setMessages(next.slice(-10)); setDraft((value) => value.trim() === request.message ? '' : value); setLastUnsentRequest(null) } catch (caught) { setError(caught instanceof ApiError ? caught.message : fallbackError); setLastUnsentRequest(request) } finally { setSubmitting(false) } }
  const submit = () => { const message = draft.trim(); if (message && !submitting) void send({ message, month, history: historyForRequest(messages) }) }
  const keyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); submit() } }
  return <section className="ledger-page monolith-assistant" aria-label="Assistente financeiro"><header className="page-title monolith-header"><div><p>ANÁLISE COM IA</p><h1>Assistente financeiro</h1></div><label className="monolith-month">Mês<input aria-label="Mês de análise" onChange={(event) => setMonth(event.target.value)} type="month" value={month} /></label></header><section aria-label="Conversa com o assistente" aria-live="polite" className="monolith-panel assistant-transcript" ref={transcriptRef} tabIndex={0}>{messages.length === 0 ? <div className="assistant-empty"><Bot size={26} /><p>A conversa aparecerá aqui.</p></div> : messages.map((message, index) => <article className={`assistant-message assistant-message--${message.role}`} key={`${message.role}-${index}`}><small>{message.role === 'user' ? 'VOCÊ' : 'ASSISTENTE'}</small><AssistantMessageContent content={message.content} />{message.role === 'assistant' && message.visualType && message.visualData ? <AssistantVisualCard visualData={message.visualData} visualType={message.visualType} /> : null}</article>)}</section><form className="monolith-panel assistant-form" onSubmit={(event: FormEvent<HTMLFormElement>) => { event.preventDefault(); submit() }}><label className="sr-only" htmlFor="assistant-question">Pergunta</label><div className="assistant-chat-input"><textarea id="assistant-question" onChange={(event) => setDraft(event.target.value)} onKeyDown={keyDown} placeholder="Pergunte sobre suas finanças..." rows={1} value={draft} /><button aria-label="Enviar pergunta" className="assistant-send" disabled={!draft.trim() || submitting} type="submit"><ArrowUp aria-hidden size={18} /></button></div><p className="assistant-hint">Enter envia · Shift + Enter quebra linha</p><p className="assistant-privacy">Ao enviar uma pergunta, os dados financeiros necessários para a análise são processados pelo provedor de IA Groq. Não envie senhas, chaves ou dados que não estejam no seu controle financeiro.</p>{submitting && <p role="status">Enviando...</p>}{error && <p className="ledger-alert" role="alert">{error}</p>}{lastUnsentRequest && <button className="ledger-button ledger-button--secondary" disabled={submitting} onClick={() => void send(lastUnsentRequest)} type="button"><RotateCcw size={16} /> Tentar novamente</button>}</form></section>
}
