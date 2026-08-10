import { useCallback, useEffect, useRef, useState } from 'react'
import './App.css'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'
const TOKEN_KEY = 'cartalaap_token'

function Icon({ name, size = 20 }) {
  const paths = {
    home: <><path d="M3 11.5 12 4l9 7.5"/><path d="M5.5 10v10h13V10M9 20v-6h6v6"/></>,
    compass: <><circle cx="12" cy="12" r="9"/><path d="m15.5 8.5-2.2 4.8-4.8 2.2 2.2-4.8 4.8-2.2Z"/></>,
    market: <><path d="M4 9h16l-1-5H5L4 9Z"/><path d="M6 9v11h12V9M9 20v-6h6v6"/><path d="M4 9c0 2 3 2 4 0 1 2 3 2 4 0 1 2 3 2 4 0 1 2 4 2 4 0"/></>,
    users: <><path d="M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M16 4.2a4 4 0 0 1 0 7.6M22 20v-2a4 4 0 0 0-3-3.7"/></>,
    hash: <><path d="M10 3 8 21M16 3l-2 18M4 9h16M3 15h16"/></>,
    poll: <><path d="M4 20V10M10 20V4M16 20v-7M22 20V7"/></>,
    message: <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v8Z"/>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></>,
    search: <><circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/></>,
    plus: <path d="M12 5v14M5 12h14"/>,
    image: <><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9" r="1.5"/><path d="m21 15-5-5L5 20"/></>,
    send: <path d="m22 2-7 20-4-9-9-4 20-7ZM22 2 11 13"/>,
    heart: <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.7-7.5a5.5 5.5 0 0 0 1.1-8.9Z"/>,
    arrowUp: <><path d="m6 10 6-6 6 6"/><path d="M12 4v16"/></>,
    arrowDown: <><path d="m6 14 6 6 6-6"/><path d="M12 20V4"/></>,
    comment: <path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4v8Z"/>,
    share: <><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.6 10.5 6.8-4M8.6 13.5l6.8 4"/></>,
    more: <><circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/></>,
    wrench: <path d="M14.7 6.3a4 4 0 0 0-5-5L12 3.6 9.6 6 7.3 3.7a4 4 0 0 0 5 5L20 16.3a2.6 2.6 0 1 1-3.7 3.7L8.7 12.3a4 4 0 0 0-5-5"/>,
    chevron: <path d="m9 18 6-6-6-6"/>,
    x: <path d="M18 6 6 18M6 6l12 12"/>,
  }
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>
}

async function api(path, options = {}) {
  const token = sessionStorage.getItem(TOKEN_KEY)
  const isFormData = options.body instanceof FormData
  let response
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: { ...(!isFormData ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
    })
  } catch {
    throw new Error('Cannot reach the CartaLaap API. Make sure the backend is running and then refresh this page.')
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}))
    throw new Error(problem.detail || 'Something went wrong. Please try again.')
  }
  return response.status === 204 ? null : response.json()
}

function useRealtime(currentUser) {
  const username = currentUser?.username
  const socketRef = useRef(null)
  const reconnectRef = useRef(null)
  const [status, setStatus] = useState('offline')
  const [onlineUsers, setOnlineUsers] = useState(() => new Set())
  const [lastEvent, setLastEvent] = useState(null)

  useEffect(() => {
    const token = sessionStorage.getItem(TOKEN_KEY)
    if (!username || !token) { setStatus('offline'); setOnlineUsers(new Set()); return }
    let stopped = false
    let retryDelay = 1000
    const connect = () => {
      if (stopped) return
      setStatus('connecting')
      const base = new URL(API_URL, window.location.origin)
      base.protocol = base.protocol === 'https:' ? 'wss:' : 'ws:'
      base.pathname = `${base.pathname.replace(/\/api\/?$/, '')}/ws/messages`
      base.search = ''
      const socket = new WebSocket(base, ['cartalaap', token])
      socketRef.current = socket
      socket.onopen = () => { retryDelay = 1000; setStatus('online') }
      socket.onmessage = (message) => {
        try {
          const event = JSON.parse(message.data)
          if (event.type === 'presence_snapshot') setOnlineUsers(new Set(event.onlineUsers || []))
          else if (event.type === 'presence') setOnlineUsers((users) => { const next = new Set(users); if (event.online) next.add(event.username); else next.delete(event.username); return next })
          setLastEvent({ ...event, receivedAt: `${Date.now()}-${Math.random()}` })
        } catch { /* Ignore malformed real-time frames. */ }
      }
      socket.onclose = () => {
        if (socketRef.current === socket) socketRef.current = null
        if (stopped) return
        setStatus('connecting')
        reconnectRef.current = window.setTimeout(connect, retryDelay)
        retryDelay = Math.min(retryDelay * 2, 10000)
      }
      socket.onerror = () => socket.close()
    }
    connect()
    return () => {
      stopped = true
      window.clearTimeout(reconnectRef.current)
      const socket = socketRef.current
      socketRef.current = null
      if (socket) socket.close()
    }
  }, [username])

  const send = useCallback((event) => {
    if (socketRef.current?.readyState === WebSocket.OPEN) socketRef.current.send(JSON.stringify(event))
  }, [])
  return { status, onlineUsers, lastEvent, send }
}

function Avatar({ user, className = '' }) {
  const initials = (user?.displayName || user?.username || 'C').split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()
  return user?.avatarUrl ? <img className={`avatar ${className}`} src={user.avatarUrl} alt="" /> : <div className={`avatar avatar-fallback ${className}`}>{initials}</div>
}

const roadThoughts = [
  "It’s not just a machine.\nIt’s the story you build\none kilometre at a time.",
  "Every careful turn of a bolt\nbrings the dream a little\ncloser to the road.",
  "A garage is where patience\nmeets possibility and old\nmachines find new stories.",
  "The perfect drive begins\nlong before the ignition\nand stays after the road ends.",
  "We do not count the hours.\nWe remember the moment\nthe engine came alive.",
  "Two wheels or four, every\ngreat journey starts with\nthe courage to set out.",
  "Machines bring us together.\nThe stories keep us talking\nlong after parking.",
  "Every scratch has a memory.\nEvery upgrade has a reason.\nEvery ride has a story.",
  "Built with patient hands.\nDriven with an open heart.\nRemembered for a lifetime.",
  "Not every destination is\nmarked on a map. Some are\nfound behind the wheel.",
]

function RoadThoughtCard() {
  const [index, setIndex] = useState(0)
  const [text, setText] = useState('')
  const [erasing, setErasing] = useState(false)
  const [reducedMotion] = useState(() => window.matchMedia?.('(prefers-reduced-motion: reduce)').matches || false)
  const thought = roadThoughts[index]

  useEffect(() => {
    if (reducedMotion) { setText(thought); return }
    let delay = erasing ? 17 : 34
    if (!erasing && text === thought) delay = 2600
    const timer = window.setTimeout(() => {
      if (!erasing && text.length < thought.length) setText(thought.slice(0, text.length + 1))
      else if (!erasing) setErasing(true)
      else if (text.length > 0) setText(text.slice(0, -1))
      else { setErasing(false); setIndex((current) => (current + 1) % roadThoughts.length) }
    }, delay)
    return () => window.clearTimeout(timer)
  }, [thought, text, erasing, reducedMotion])

  return <section className="quote-card live-quote-card" aria-label={thought.replaceAll('\n', ' ')}><span aria-hidden="true">“</span><p aria-hidden="true">{text}<i className="typing-cursor" /></p><div className="quote-progress" aria-hidden="true">{roadThoughts.map((_, itemIndex) => <b className={itemIndex === index ? 'active' : ''} key={itemIndex} />)}</div></section>
}

function AuthModal({ initialMode, onClose, onAuthenticated }) {
  const [mode, setMode] = useState(initialMode)
  const [form, setForm] = useState({ username: '', email: '', displayName: '', login: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const isRegister = mode === 'register'
  const submit = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const body = isRegister ? { username: form.username, email: form.email, displayName: form.displayName, password: form.password } : { login: form.login, password: form.password }
      const result = await api(`/auth/${isRegister ? 'register' : 'login'}`, { method: 'POST', body: JSON.stringify(body) })
      sessionStorage.setItem(TOKEN_KEY, result.accessToken); onAuthenticated(result.user)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value })
  return <div className="modal-backdrop" onMouseDown={onClose}><div className="auth-modal" onMouseDown={(event) => event.stopPropagation()}>
    <button className="icon-button close-button" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
    <div className="modal-brand"><span className="brand-mark">CL</span></div><p className="eyebrow">WELCOME TO THE GARAGE</p>
    <h2>{isRegister ? 'Create your account' : 'Good to see you again'}</h2><p className="modal-copy">Join the conversation with people who live and breathe automobiles.</p>
    <form onSubmit={submit}>{isRegister && <><label>Display name<input name="displayName" value={form.displayName} onChange={update} required minLength="2" placeholder="Sayan Dey" /></label><label>Username<input name="username" value={form.username} onChange={update} required minLength="3" pattern="[A-Za-z0-9_]+" placeholder="sayan_drives" /></label><label>Email<input name="email" value={form.email} onChange={update} required type="email" placeholder="you@example.com" /></label></>}{!isRegister && <label>Username or email<input name="login" value={form.login} onChange={update} required autoFocus placeholder="sayan_drives" /></label>}<label>Password<input name="password" value={form.password} onChange={update} required type="password" minLength={isRegister ? 8 : undefined} placeholder="••••••••" /></label>{error && <p className="form-error">{error}</p>}<button className="primary-button modal-submit" disabled={busy}>{busy ? 'Please wait…' : isRegister ? 'Join CartaLaap' : 'Log in'}</button></form>
    <p className="switch-mode">{isRegister ? 'Already a member?' : 'New to CartaLaap?'} <button onClick={() => { setMode(isRegister ? 'login' : 'register'); setError('') }}>{isRegister ? 'Log in' : 'Create an account'}</button></p>
  </div></div>
}

function MomentUploadModal({ onClose, onCreated }) {
  const inputRef = useRef(null)
  const [file, setFile] = useState(null)
  const [preview, setPreview] = useState('')
  const [caption, setCaption] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview) }, [preview])
  const choose = (event) => {
    const next = event.target.files?.[0]; event.target.value = ''; if (!next) return
    if (!['image/jpeg','image/png','image/gif'].includes(next.type)) { setError('Choose a JPEG, PNG, or GIF image.'); return }
    if (next.size > 10 * 1024 * 1024) { setError('Moments must be 10 MB or smaller.'); return }
    setFile(next); setPreview(URL.createObjectURL(next)); setError('')
  }
  const publish = async (event) => {
    event.preventDefault(); if (!file) { inputRef.current?.click(); return }
    setBusy(true); setError('')
    try { const data = new FormData(); data.append('image', file); const upload = await api('/media/images', { method:'POST', body:data }); const moment = await api('/moments', { method:'POST', body:JSON.stringify({ imageUrl:upload.url, caption }) }); onCreated(moment) }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="moment-upload-modal" onMouseDown={(event) => event.stopPropagation()}><button className="icon-button close-button" onClick={onClose}><Icon name="x" /></button><p className="eyebrow">SHARE FOR 24 HOURS</p><h2>Create a Moment</h2><form onSubmit={publish}><input ref={inputRef} className="file-input" type="file" accept="image/jpeg,image/png,image/gif" onChange={choose} />{preview ? <button type="button" className="moment-upload-preview" onClick={() => inputRef.current?.click()}><img src={preview} alt="Moment preview" /><span>Change photo</span></button> : <button type="button" className="moment-upload-drop" onClick={() => inputRef.current?.click()}><Icon name="image" size={29} /><strong>Choose a photo</strong><span>JPEG, PNG, or GIF up to 10 MB</span></button>}<textarea value={caption} onChange={(event) => setCaption(event.target.value)} maxLength="300" placeholder="Add a caption (optional)…" />{error && <p className="form-error">{error}</p>}<button className="primary-button" disabled={busy || !file}>{busy ? 'Sharing…' : 'Share Moment'}</button></form></section></div>
}

function MomentViewer({ moments, initialIndex, currentUser, onClose, onUpdated, onDeleted }) {
  const [index, setIndex] = useState(initialIndex)
  const moment = moments[index]
  useEffect(() => {
    if (!moment || !currentUser || moment.ownedByCurrentUser || moment.viewedByCurrentUser) return
    api(`/moments/${moment.id}/view`, { method:'POST' }).then(onUpdated).catch(() => {})
  }, [moment, currentUser, onUpdated])
  if (!moment) return null
  const previous = () => setIndex((current) => current > 0 ? current - 1 : moments.length - 1)
  const next = () => setIndex((current) => current < moments.length - 1 ? current + 1 : 0)
  const remove = async () => {
    if (!window.confirm('Delete this Moment?')) return
    try { await api(`/moments/${moment.id}`, { method:'DELETE' }); onDeleted(moment.id); if (moments.length <= 1) onClose(); else setIndex(Math.min(index, moments.length - 2)) } catch {}
  }
  const hoursLeft = Math.max(1, Math.ceil((new Date(moment.expiresAt) - Date.now()) / 3600000))
  return <div className="moment-viewer-backdrop" onMouseDown={onClose}><section className="moment-viewer" onMouseDown={(event) => event.stopPropagation()}><div className="moment-progress">{moments.map((item, itemIndex) => <span className={itemIndex <= index ? 'active' : ''} key={item.id} />)}</div><header><Avatar user={moment.author} /><div><strong>{moment.author.displayName}</strong><span>@{moment.author.username} · {hoursLeft}h left</span></div>{moment.ownedByCurrentUser && <button className="moment-delete" onClick={remove}>Delete</button>}<button className="icon-button" onClick={onClose}><Icon name="x" /></button></header><img className="moment-view-image" src={moment.imageUrl} alt="Shared Moment" />{moment.caption && <p className="moment-caption">{moment.caption}</p>}{moment.ownedByCurrentUser && <div className="moment-views"><Icon name="users" size={15} /> {moment.viewCount} {moment.viewCount === 1 ? 'viewer' : 'viewers'}</div>}{moments.length > 1 && <><button className="moment-nav previous" onClick={previous} aria-label="Previous Moment"><Icon name="chevron" /></button><button className="moment-nav next" onClick={next} aria-label="Next Moment"><Icon name="chevron" /></button></>}</section></div>
}

function MessagingModal({ currentUser, initialUsername, onClose, onOpenProfile, realtime, onInboxChanged }) {
  const [conversations, setConversations] = useState([])
  const [active, setActive] = useState(null)
  const [messages, setMessages] = useState([])
  const [body, setBody] = useState('')
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [error, setError] = useState('')
  const [otherTyping, setOtherTyping] = useState(false)
  const typingStopTimer = useRef(null)
  const otherTypingTimer = useRef(null)
  const lastTypingSent = useRef(0)

  const loadInbox = useCallback(() => api('/conversations').then((data) => { setConversations(data); onInboxChanged(data) }).catch((err) => setError(err.message)), [onInboxChanged])
  const loadThread = useCallback((conversation) => api(`/conversations/${conversation.id}/messages`).then((data) => { setMessages(data); loadInbox() }).catch((err) => setError(err.message)), [loadInbox])
  const openConversation = useCallback((conversation) => { setActive(conversation); loadThread(conversation) }, [loadThread])
  const start = useCallback(async (username) => {
    try { const conversation = await api(`/conversations/with/${encodeURIComponent(username)}`, { method: 'POST' }); setQuery(''); setResults([]); openConversation(conversation) }
    catch (err) { setError(err.message) }
  }, [openConversation])

  useEffect(() => { loadInbox(); if (initialUsername) start(initialUsername) }, [initialUsername, loadInbox, start])
  useEffect(() => {
    const event = realtime.lastEvent
    if (!event) return
    if (event.type === 'message') {
      loadInbox()
      if (active?.id === event.conversationId) {
        setMessages((current) => current.some((message) => message.id === event.message.id) ? current : [...current, event.message])
        if (!event.message.ownedByCurrentUser) loadThread(active)
      }
    } else if (event.type === 'read_receipt' && active?.id === event.conversationId) {
      setMessages((current) => current.map((message) => message.ownedByCurrentUser && !message.readAt ? { ...message, readAt: event.readAt } : message))
    } else if (event.type === 'message_deleted') {
      loadInbox()
      if (active?.id === event.conversationId) setMessages((current) => current.filter((message) => message.id !== event.messageId))
    } else if (event.type === 'typing' && active?.id === event.conversationId) {
      setOtherTyping(event.typing)
      window.clearTimeout(otherTypingTimer.current)
      if (event.typing) otherTypingTimer.current = window.setTimeout(() => setOtherTyping(false), 3000)
    }
  }, [realtime.lastEvent, active, loadInbox, loadThread])
  useEffect(() => {
    setOtherTyping(false)
    return () => { window.clearTimeout(typingStopTimer.current); window.clearTimeout(otherTypingTimer.current) }
  }, [active?.id])
  useEffect(() => {
    if (!query.trim()) { setResults([]); return }
    const timer = window.setTimeout(() => api(`/users/search?q=${encodeURIComponent(query)}&size=8`).then((data) => setResults(data.content.filter((person) => person.username !== currentUser.username && !person.blockedByCurrentUser && !person.blocksCurrentUser))).catch(() => setResults([])), 250)
    return () => window.clearTimeout(timer)
  }, [query, currentUser.username])

  const send = async (event) => {
    event.preventDefault(); if (!body.trim() || !active) return
    try { const sent = await api(`/conversations/${active.id}/messages`, { method: 'POST', body: JSON.stringify({ body }) }); setMessages((current) => current.some((message) => message.id === sent.id) ? current : [...current, sent]); setBody(''); realtime.send({ type:'typing', conversationId:active.id, typing:false }); loadInbox() }
    catch (err) { setError(err.message) }
  }
  const compose = (event) => {
    const value = event.target.value
    setBody(value)
    if (!active) return
    const now = Date.now()
    if (value.trim() && now - lastTypingSent.current > 1200) {
      realtime.send({ type:'typing', conversationId:active.id, typing:true })
      lastTypingSent.current = now
    }
    window.clearTimeout(typingStopTimer.current)
    typingStopTimer.current = window.setTimeout(() => realtime.send({ type:'typing', conversationId:active.id, typing:false }), 1400)
  }
  const remove = async (id) => {
    if (!window.confirm('Delete this message?')) return
    try { await api(`/messages/${id}`, { method: 'DELETE' }); setMessages((current) => current.filter((message) => message.id !== id)); loadInbox() }
    catch (err) { setError(err.message) }
  }

  return <div className="modal-backdrop" onMouseDown={onClose}><section className={`messages-modal ${active?'thread-open':'inbox-open'}`} onMouseDown={(event) => event.stopPropagation()}>
    <button className="icon-button close-button" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
    <aside className="inbox-panel"><div className="inbox-heading"><p className="eyebrow">PRIVATE GARAGE</p><h2>Messages</h2><span className="signed-in-as">Signed in as @{currentUser.username}</span></div><div className="message-search"><Icon name="search" size={16} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Start a new chat…" /></div>{results.length > 0 && <div className="message-search-results">{results.map((person) => <button key={person.id} onClick={() => start(person.username)}><Avatar user={person} /><span><strong>{person.displayName}</strong><small>@{person.username}</small></span></button>)}</div>}<div className="conversation-list">{conversations.length === 0 && <p>No conversations yet.</p>}{conversations.map((conversation) => <button className={active?.id === conversation.id ? 'active' : ''} key={conversation.id} onClick={() => openConversation(conversation)}><Avatar user={conversation.participant} /><span><strong>{conversation.participant.displayName}</strong><small>{conversation.lastMessage || 'Start the conversation'}</small></span>{conversation.unreadCount > 0 && <b>{conversation.unreadCount}</b>}</button>)}</div></aside>
    <main className="chat-panel">{active ? <><header><button className="mobile-panel-back" onClick={()=>{setActive(null);setMessages([])}} aria-label="Back to conversations"><Icon name="chevron"/></button><button className="avatar-button" onClick={() => onOpenProfile(active.participant.username)}><Avatar user={active.participant} /></button><div><strong>{active.participant.displayName}</strong><span>@{active.participant.username} · <i className={realtime.onlineUsers.has(active.participant.username) ? 'presence-online' : ''}>{realtime.onlineUsers.has(active.participant.username) ? 'Online' : 'Offline'}</i></span></div><b className={`realtime-state ${realtime.status}`}>{realtime.status === 'online' ? 'Live' : 'Reconnecting…'}</b></header><div className="message-thread">{messages.length === 0 && <p className="chat-empty">Say hello to start the conversation.</p>}{messages.map((message) => <div className={message.ownedByCurrentUser ? 'message-bubble mine' : 'message-bubble'} key={message.id}><p>{message.body}</p><span>{new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}{message.ownedByCurrentUser && (message.readAt ? ' · Read' : ' · Sent')}</span>{message.ownedByCurrentUser && <button onClick={() => remove(message.id)}>Delete</button>}</div>)}{otherTyping && <div className="typing-indicator"><span/><span/><span/></div>}</div><form className="message-composer" onSubmit={send}><textarea value={body} onChange={compose} maxLength="2000" placeholder={otherTyping ? `${active.participant.displayName} is typing…` : 'Write a private message…'} /><button disabled={!body.trim()}><Icon name="send" size={17} /></button></form></> : <div className="chat-placeholder"><Icon name="message" size={35} /><h3>Your private garage</h3><p>Select a conversation or search for an enthusiast to start chatting.</p></div>}{error && <p className="form-error chat-error">{error}</p>}</main>
  </section></div>
}

function InlineArticleText({ text }) {
  const parts = text.split(/(\[[^\]]+\]\([^)]+\)|\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`)/g).filter(Boolean)
  return parts.map((part, index) => {
    const link = part.match(/^\[([^\]]+)\]\(([^)]+)\)$/)
    if (link) return <a key={index} href={safeArticleUrl(link[2])} target="_blank" rel="noreferrer">{link[1]}</a>
    if (part.startsWith('**') && part.endsWith('**')) return <strong key={index}>{part.slice(2, -2)}</strong>
    if (part.startsWith('*') && part.endsWith('*')) return <em key={index}>{part.slice(1, -1)}</em>
    if (part.startsWith('`') && part.endsWith('`')) return <code key={index}>{part.slice(1, -1)}</code>
    return <span key={index}>{part}</span>
  })
}

function safeArticleUrl(url) {
  const trimmed = url?.trim() || ''
  return /^(https?:\/\/|\/)/i.test(trimmed) ? trimmed : '#'
}

function ArticleBody({ body }) {
  return <div className="article-content">{body.split('\n').map((line, index) => {
    const image = line.trim().match(/^!\[([^\]]*)\]\(([^)]+)\)$/)
    if (image) return <figure className="article-inline-image" key={index}><img src={safeArticleUrl(image[2])} alt={image[1]} />{image[1] && <figcaption>{image[1]}</figcaption>}</figure>
    if (line.trim() === '---') return <hr key={index} />
    if (line.startsWith('### ')) return <h4 key={index}><InlineArticleText text={line.slice(4)} /></h4>
    if (line.startsWith('## ')) return <h3 key={index}><InlineArticleText text={line.slice(3)} /></h3>
    if (line.startsWith('# ')) return <h2 key={index}><InlineArticleText text={line.slice(2)} /></h2>
    if (line.startsWith('> ')) return <blockquote key={index}><InlineArticleText text={line.slice(2)} /></blockquote>
    if (line.startsWith('- ')) return <div className="article-list-item" key={index}><span>•</span><p><InlineArticleText text={line.slice(2)} /></p></div>
    const numbered = line.match(/^(\d+)\.\s(.+)$/)
    if (numbered) return <div className="article-list-item numbered" key={index}><span>{numbered[1]}.</span><p><InlineArticleText text={numbered[2]} /></p></div>
    return line ? <p key={index}><InlineArticleText text={line} /></p> : <br key={index} />
  })}</div>
}

function ArticleEditor({ article, onClose, onSaved }) {
  const coverInput = useRef(null)
  const inlineInput = useRef(null)
  const bodyRef = useRef(null)
  const [form, setForm] = useState({ title: article?.title || '', body: article?.body || '', coverImageUrl: article?.coverImageUrl || '', topicSlug: article?.topicSlug || '' })
  const [topics, setTopics] = useState([])
  const [creatingTopic, setCreatingTopic] = useState(false)
  const [newTopic, setNewTopic] = useState({ name:'', description:'' })
  const [coverFile, setCoverFile] = useState(null)
  const [preview, setPreview] = useState(article?.coverImageUrl || '')
  const [previewMode, setPreviewMode] = useState(false)
  const [uploadingInline, setUploadingInline] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => { api('/topics').then(setTopics).catch((err) => setError(err.message)) }, [])
  const insert = (before, after = '') => {
    const input = bodyRef.current; if (!input) return
    const start = input.selectionStart; const end = input.selectionEnd
    const selected = form.body.slice(start, end) || 'text'
    const next = form.body.slice(0, start) + before + selected + after + form.body.slice(end)
    setForm({ ...form, body: next }); window.setTimeout(() => { input.focus(); input.setSelectionRange(start + before.length, start + before.length + selected.length) }, 0)
  }
  const insertRaw = (value) => {
    const input = bodyRef.current
    const start = input?.selectionStart ?? form.body.length
    const end = input?.selectionEnd ?? start
    setForm((current) => ({ ...current, body: current.body.slice(0, start) + value + current.body.slice(end) }))
    window.setTimeout(() => { bodyRef.current?.focus(); bodyRef.current?.setSelectionRange(start + value.length, start + value.length) }, 0)
  }
  const insertLink = () => {
    const url = window.prompt('Paste an http:// or https:// link')
    if (url) insert('[Link text](', `${url})`)
  }
  const chooseInlineImages = async (event) => {
    const files = Array.from(event.target.files || []); event.target.value = ''; if (!files.length) return
    if (files.some((file) => !['image/jpeg','image/png','image/gif'].includes(file.type) || file.size > 10*1024*1024)) { setError('Use JPEG, PNG, or GIF images up to 10 MB.'); return }
    setUploadingInline(true); setError('')
    try {
      const markdown = []
      for (const file of files) {
        const data = new FormData(); data.append('image', file)
        const uploaded = await api('/media/images', { method:'POST', body:data })
        const alt = file.name.replace(/[\[\]()`]/g, ' ').replace(/\.[^.]+$/, '').trim() || 'Article image'
        markdown.push(`![${alt}](${uploaded.url})`)
      }
      insertRaw(`\n\n${markdown.join('\n\n')}\n\n`)
    } catch (err) { setError(err.message) } finally { setUploadingInline(false) }
  }
  const chooseCover = (event) => { const file = event.target.files?.[0]; event.target.value=''; if (!file) return; if (!['image/jpeg','image/png','image/gif'].includes(file.type) || file.size > 10*1024*1024) { setError('Choose a JPEG, PNG, or GIF up to 10 MB.'); return } setCoverFile(file); setPreview(URL.createObjectURL(file)) }
  const save = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try { let topicSlug=form.topicSlug; if(creatingTopic){const created=await api('/topics',{method:'POST',body:JSON.stringify(newTopic)});topicSlug=created.slug} let coverImageUrl = form.coverImageUrl || null; if (coverFile) { const data = new FormData(); data.append('image', coverFile); coverImageUrl = (await api('/media/images', { method:'POST', body:data })).url } const saved = await api(article ? `/articles/${article.id}` : '/articles', { method: article ? 'PATCH' : 'POST', body: JSON.stringify({ ...form, topicSlug, coverImageUrl }) }); onSaved(saved) }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="article-editor" onMouseDown={(event) => event.stopPropagation()}><button className="icon-button close-button" onClick={onClose}><Icon name="x" /></button><p className="eyebrow">LONG-FORM GARAGE STORIES</p><h2>{article ? 'Edit article' : 'Write an article'}</h2><form onSubmit={save}><div className="article-editor-meta"><input className="article-title-input" value={form.title} onChange={(event) => setForm({ ...form, title:event.target.value })} required maxLength="160" placeholder="Give your story a strong title" /><label>Topic<select value={creatingTopic?'__new__':form.topicSlug} onChange={(event) => {const create=event.target.value==='__new__';setCreatingTopic(create);if(!create)setForm({...form,topicSlug:event.target.value})}} required><option value="">Choose an existing topic</option>{topics.map((topic) => <option key={topic.slug} value={topic.slug}>{topic.name}</option>)}<option value="__new__">＋ Create a new topic</option></select></label></div>{creatingTopic&&<div className="new-topic-fields"><label>New topic name<input value={newTopic.name} onChange={(event)=>setNewTopic({...newTopic,name:event.target.value})} required minLength="3" maxLength="80" placeholder="e.g. Indian road trips"/></label><label>Short description<input value={newTopic.description} onChange={(event)=>setNewTopic({...newTopic,description:event.target.value})} maxLength="240" placeholder="What kind of articles belong here?"/></label></div>}<div className="editor-toolbar"><button type="button" disabled={previewMode} onClick={() => insert('# ')}>H1</button><button type="button" disabled={previewMode} onClick={() => insert('## ')}>H2</button><button type="button" disabled={previewMode} onClick={() => insert('### ')}>H3</button><button type="button" disabled={previewMode} onClick={() => insert('**','**')}><strong>Bold</strong></button><button type="button" disabled={previewMode} onClick={() => insert('*','*')}><em>Italic</em></button><button type="button" disabled={previewMode} onClick={() => insert('> ')}>Quote</button><button type="button" disabled={previewMode} onClick={() => insert('- ')}>Bullets</button><button type="button" disabled={previewMode} onClick={() => insert('1. ')}>Numbered</button><button type="button" disabled={previewMode} onClick={() => insert('`','`')}>Code</button><button type="button" disabled={previewMode} onClick={insertLink}>Link</button><button type="button" disabled={previewMode} onClick={() => insertRaw('\n\n---\n\n')}>Divider</button><button type="button" disabled={previewMode || uploadingInline} onClick={() => inlineInput.current?.click()}><Icon name="image" size={15} /> {uploadingInline ? 'Uploading…' : 'Inline images'}</button><button type="button" onClick={() => coverInput.current?.click()}><Icon name="image" size={15} /> Cover</button><button type="button" className="preview-toggle" onClick={() => setPreviewMode(!previewMode)}>{previewMode ? 'Edit' : 'Preview'}</button></div>{previewMode ? <div className="article-editor-preview">{form.body ? <ArticleBody body={form.body} /> : <p>Your article preview will appear here.</p>}</div> : <textarea ref={bodyRef} className="article-body-input" value={form.body} onChange={(event) => setForm({ ...form, body:event.target.value })} required maxLength="50000" placeholder={'# Start with a heading\n\nTell the full story of your build, journey, review, or guide…'} />}<input ref={coverInput} className="file-input" type="file" accept="image/jpeg,image/png,image/gif" onChange={chooseCover} /><input ref={inlineInput} className="file-input" type="file" multiple accept="image/jpeg,image/png,image/gif" onChange={chooseInlineImages} />{preview && <div className="article-cover-preview"><img src={preview} alt="Article cover preview" /><button type="button" onClick={() => { setPreview(''); setCoverFile(null); setForm({ ...form, coverImageUrl:'' }) }}><Icon name="x" size={15} /></button></div>}{error && <p className="form-error">{error}</p>}<div className="article-editor-actions"><span>{form.body.length.toLocaleString()} / 50,000</span><button className="primary-button" disabled={busy || uploadingInline}>{busy ? 'Publishing…' : article ? 'Save changes' : 'Publish article'}</button></div></form></section></div>
}

function ArticleCard({ article, onOpenProfile, onEdit, onDelete }) {
  const [expanded, setExpanded] = useState(false)
  return <article className="article-card">{article.coverImageUrl && <img className="article-cover" src={article.coverImageUrl} alt="" />}<div className="article-card-main"><div className="article-byline"><button className="avatar-button" onClick={() => onOpenProfile(article.author.username)}><Avatar user={article.author} /></button><div><strong>{article.author.displayName}</strong><span>@{article.author.username} · {new Date(article.createdAt).toLocaleDateString()}</span></div>{article.ownedByCurrentUser && <div className="owner-actions"><button onClick={() => onEdit(article)}>Edit</button><button onClick={() => onDelete(article.id)}>Delete</button></div>}</div>{article.topicName && <span className="topic-chip">{article.topicName}</span>}<h2>{article.title}</h2>{expanded ? <ArticleBody body={article.body} /> : <p className="article-excerpt">{article.excerpt}</p>}<button className="read-article" onClick={() => setExpanded(!expanded)}>{expanded ? 'Show less' : 'Read full article'} <Icon name="chevron" size={15} /></button></div></article>
}

const marketplaceCategories = ['CAR', 'MOTORCYCLE', 'PART', 'ACCESSORY']

function MarketplaceEditor({ listing, onClose, onSaved }) {
  const imageInput = useRef(null)
  const [form, setForm] = useState({ category:listing?.category||'CAR',title:listing?.title||'',description:listing?.description||'',price:listing?.price||'',condition:listing?.condition||'USED',location:listing?.location||'',brand:listing?.brand||'',model:listing?.model||'',year:listing?.year||'',mileage:listing?.mileage||'' })
  const [existingImages, setExistingImages] = useState(listing?.imageUrls || [])
  const [files, setFiles] = useState([])
  const [previews, setPreviews] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => () => previews.forEach(URL.revokeObjectURL), [previews])
  const choose = (event) => { const chosen=Array.from(event.target.files||[]);event.target.value='';const valid=chosen.filter((file)=>['image/jpeg','image/png','image/gif'].includes(file.type)&&file.size<=10*1024*1024);if(valid.length!==chosen.length)setError('Use JPEG, PNG, or GIF images up to 10 MB.');const room=Math.max(0,8-existingImages.length-files.length);const accepted=valid.slice(0,room);setFiles([...files,...accepted]);setPreviews([...previews,...accepted.map(URL.createObjectURL)]) }
  const submit=async(event)=>{event.preventDefault();if(existingImages.length+files.length===0){setError('Add at least one image.');return}setBusy(true);setError('');try{const uploaded=[];for(const file of files){const data=new FormData();data.append('image',file);uploaded.push((await api('/media/images',{method:'POST',body:data})).url)}const payload={...form,price:Number(form.price),year:form.year?Number(form.year):null,mileage:form.mileage?Number(form.mileage):null,imageUrls:[...existingImages,...uploaded]};const saved=await api(listing?`/marketplace/listings/${listing.id}`:'/marketplace/listings',{method:listing?'PATCH':'POST',body:JSON.stringify(payload)});onSaved(saved)}catch(err){setError(err.message)}finally{setBusy(false)}}
  const update=(event)=>setForm({...form,[event.target.name]:event.target.value})
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="market-editor" onMouseDown={(event)=>event.stopPropagation()}><button className="icon-button close-button" onClick={onClose}><Icon name="x" /></button><p className="eyebrow">CARTALAAP MARKETPLACE</p><h2>{listing?'Edit listing':'Create a listing'}</h2><form onSubmit={submit}><div className="market-form-grid"><label>Category<select name="category" value={form.category} onChange={update}>{marketplaceCategories.map((category)=><option key={category}>{category}</option>)}</select></label><label>Condition<select name="condition" value={form.condition} onChange={update}><option>NEW</option><option>USED</option><option>REFURBISHED</option></select></label><label className="wide">Title<input name="title" value={form.title} onChange={update} required maxLength="160" placeholder="2019 Honda City ZX" /></label><label>Price (₹)<input name="price" value={form.price} onChange={update} required type="number" min="0" step="0.01" /></label><label>Location<input name="location" value={form.location} onChange={update} required maxLength="120" /></label><label>Brand<input name="brand" value={form.brand} onChange={update} maxLength="80" /></label><label>Model<input name="model" value={form.model} onChange={update} maxLength="80" /></label><label>Year<input name="year" value={form.year} onChange={update} type="number" min="1886" max="2100" /></label><label>Mileage (km)<input name="mileage" value={form.mileage} onChange={update} type="number" min="0" /></label><label className="wide">Description<textarea name="description" value={form.description} onChange={update} required maxLength="5000" placeholder="Condition, service history, upgrades, and anything buyers should know…" /></label></div><input ref={imageInput} className="file-input" type="file" multiple accept="image/jpeg,image/png,image/gif" onChange={choose}/><div className="market-images">{existingImages.map((url,index)=><div key={url}><img src={url} alt=""/><button type="button" onClick={()=>setExistingImages(existingImages.filter((_,i)=>i!==index))}><Icon name="x" size={13}/></button></div>)}{previews.map((url,index)=><div key={url}><img src={url} alt=""/><button type="button" onClick={()=>{setFiles(files.filter((_,i)=>i!==index));setPreviews(previews.filter((_,i)=>i!==index))}}><Icon name="x" size={13}/></button></div>)}{existingImages.length+files.length<8&&<button type="button" className="add-market-image" onClick={()=>imageInput.current?.click()}><Icon name="image"/><span>Add photos</span></button>}</div>{error&&<p className="form-error">{error}</p>}<div className="market-editor-actions"><span>{existingImages.length+files.length}/8 photos</span><button className="primary-button" disabled={busy}>{busy?'Saving…':listing?'Save changes':'Publish listing'}</button></div></form></section></div>
}

function MarketplaceCard({ listing, currentUser, onOpen, onAuth, onUpdated }) {
  const favorite=async(event)=>{event.stopPropagation();if(!currentUser){onAuth();return}try{const updated=await api(`/marketplace/listings/${listing.id}/favorite`,{method:listing.favoritedByCurrentUser?'DELETE':'PUT'});onUpdated(updated)}catch{}}
  return <article className={`market-card ${listing.status==='SOLD'?'sold':''}`} onClick={()=>onOpen(listing)}><div className="market-card-image"><img src={listing.imageUrls[0]} alt=""/>{listing.status==='SOLD'&&<b>SOLD</b>}<button onClick={favorite} className={listing.favoritedByCurrentUser?'favorite active':'favorite'}><Icon name="heart" size={17}/></button><span>{listing.imageUrls.length} photo{listing.imageUrls.length===1?'':'s'}</span></div><div className="market-card-info"><small>{listing.category} · {listing.condition}</small><h3>{listing.title}</h3><strong>₹{Number(listing.price).toLocaleString('en-IN')}</strong><p>{[listing.year,listing.brand,listing.model].filter(Boolean).join(' · ')}</p><span>📍 {listing.location}</span></div></article>
}

function MarketplaceDetail({ listing, currentUser, onClose, onUpdated, onDeleted, onEdit, onMessage, onAuth }) {
  const [imageIndex,setImageIndex]=useState(0);const [error,setError]=useState('')
  const favorite=async()=>{if(!currentUser){onAuth();return}try{onUpdated(await api(`/marketplace/listings/${listing.id}/favorite`,{method:listing.favoritedByCurrentUser?'DELETE':'PUT'}))}catch(err){setError(err.message)}}
  const setStatus=async()=>{try{onUpdated(await api(`/marketplace/listings/${listing.id}/status`,{method:'PATCH',body:JSON.stringify({status:listing.status==='SOLD'?'ACTIVE':'SOLD'})}))}catch(err){setError(err.message)}}
  const remove=async()=>{if(!window.confirm('Delete this marketplace listing?'))return;try{await api(`/marketplace/listings/${listing.id}`,{method:'DELETE'});onDeleted(listing.id);onClose()}catch(err){setError(err.message)}}
  const report=async()=>{if(!currentUser){onAuth();return}const reason=window.prompt('Why are you reporting this listing?');if(!reason)return;try{await api(`/marketplace/listings/${listing.id}/reports`,{method:'POST',body:JSON.stringify({reason})});window.alert('Report submitted. Thank you.')}catch(err){setError(err.message)}}
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="market-detail" onMouseDown={(event)=>event.stopPropagation()}><button className="icon-button close-button" onClick={onClose}><Icon name="x"/></button><div className="market-gallery"><img src={listing.imageUrls[imageIndex]} alt=""/><div>{listing.imageUrls.map((url,index)=><button className={index===imageIndex?'active':''} key={url} onClick={()=>setImageIndex(index)}><img src={url} alt=""/></button>)}</div></div><div className="market-detail-main"><div className="market-title-row"><div><p className="eyebrow">{listing.category} · {listing.condition}</p><h2>{listing.title}</h2></div><strong>₹{Number(listing.price).toLocaleString('en-IN')}</strong></div>{listing.status==='SOLD'&&<div className="sold-banner">This listing has been marked as sold.</div>}<div className="market-specs">{listing.year&&<span><b>{listing.year}</b>Year</span>}{listing.mileage!==null&&<span><b>{Number(listing.mileage).toLocaleString('en-IN')} km</b>Mileage</span>}{listing.brand&&<span><b>{listing.brand}</b>Brand</span>}{listing.model&&<span><b>{listing.model}</b>Model</span>}<span><b>{listing.location}</b>Location</span></div><h3>About this listing</h3><p className="market-description">{listing.description}</p><div className="seller-box"><Avatar user={listing.seller}/><div><strong>{listing.seller.displayName}</strong><span>@{listing.seller.username}</span></div>{!listing.ownedByCurrentUser&&<button className="primary-button" onClick={()=>currentUser?onMessage(listing.seller.username):onAuth()}><Icon name="message" size={15}/> Message seller</button>}</div>{listing.ownedByCurrentUser?<div className="market-owner-actions"><button onClick={()=>onEdit(listing)}>Edit</button><button onClick={setStatus}>{listing.status==='SOLD'?'Mark active':'Mark sold'}</button><button className="danger" onClick={remove}>Delete</button></div>:<div className="market-buyer-actions"><button onClick={favorite}><Icon name="heart" size={16}/>{listing.favoritedByCurrentUser?'Saved':'Save listing'}</button><button onClick={report}>Report</button></div>}<div className="market-safety"><strong>Meet safely</strong><p>Inspect the vehicle or part in person. Never send advance payment to an unknown seller.</p></div>{error&&<p className="form-error">{error}</p>}</div></section></div>
}

function GarageEditor({ vehicle, onClose, onSaved }) {
  const inputRef=useRef(null);const [form,setForm]=useState({vehicleType:vehicle?.vehicleType||'CAR',status:vehicle?.status||'CURRENT',brand:vehicle?.brand||'',model:vehicle?.model||'',year:vehicle?.year||'',variant:vehicle?.variant||'',fuelType:vehicle?.fuelType||'',modifications:vehicle?.modifications||'',ownershipStory:vehicle?.ownershipStory||''});const [existing,setExisting]=useState(vehicle?.imageUrls||[]);const [files,setFiles]=useState([]);const [previews,setPreviews]=useState([]);const [busy,setBusy]=useState(false);const [error,setError]=useState('')
  useEffect(()=>()=>previews.forEach(URL.revokeObjectURL),[previews]);const update=(event)=>setForm({...form,[event.target.name]:event.target.value});const choose=(event)=>{const chosen=Array.from(event.target.files||[]);event.target.value='';const valid=chosen.filter((f)=>['image/jpeg','image/png','image/gif'].includes(f.type)&&f.size<=10*1024*1024).slice(0,8-existing.length-files.length);setFiles([...files,...valid]);setPreviews([...previews,...valid.map(URL.createObjectURL)]) }
  const save=async(event)=>{event.preventDefault();if(existing.length+files.length===0){setError('Add at least one vehicle photo.');return}setBusy(true);try{const uploaded=[];for(const file of files){const data=new FormData();data.append('image',file);uploaded.push((await api('/media/images',{method:'POST',body:data})).url)}const payload={...form,year:form.year?Number(form.year):null,imageUrls:[...existing,...uploaded]};onSaved(await api(vehicle?`/garage/${vehicle.id}`:'/garage',{method:vehicle?'PATCH':'POST',body:JSON.stringify(payload)}))}catch(err){setError(err.message)}finally{setBusy(false)}}
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="garage-editor" onMouseDown={(event)=>event.stopPropagation()}><button className="icon-button close-button" onClick={onClose}><Icon name="x"/></button><p className="eyebrow">PERSONAL GARAGE</p><h2>{vehicle?'Edit vehicle':'Add a vehicle'}</h2><form onSubmit={save}><div className="market-form-grid"><label>Type<select name="vehicleType" value={form.vehicleType} onChange={update}><option>CAR</option><option>MOTORCYCLE</option></select></label><label>Status<select name="status" value={form.status} onChange={update}><option>CURRENT</option><option>SOLD</option><option>DREAM</option></select></label><label>Brand<input name="brand" value={form.brand} onChange={update} required/></label><label>Model<input name="model" value={form.model} onChange={update} required/></label><label>Year<input name="year" type="number" min="1886" max="2100" value={form.year} onChange={update}/></label><label>Variant<input name="variant" value={form.variant} onChange={update}/></label><label className="wide">Fuel type<input name="fuelType" value={form.fuelType} onChange={update} placeholder="Petrol, diesel, electric…"/></label><label className="wide">Modifications<textarea name="modifications" value={form.modifications} onChange={update} maxLength="3000"/></label><label className="wide">Ownership story<textarea name="ownershipStory" value={form.ownershipStory} onChange={update} maxLength="5000"/></label></div><input ref={inputRef} className="file-input" multiple type="file" accept="image/jpeg,image/png,image/gif" onChange={choose}/><div className="market-images">{existing.map((url,index)=><div key={url}><img src={url} alt=""/><button type="button" onClick={()=>setExisting(existing.filter((_,i)=>i!==index))}><Icon name="x" size={13}/></button></div>)}{previews.map((url,index)=><div key={url}><img src={url} alt=""/><button type="button" onClick={()=>{setFiles(files.filter((_,i)=>i!==index));setPreviews(previews.filter((_,i)=>i!==index))}}><Icon name="x" size={13}/></button></div>)}{existing.length+files.length<8&&<button type="button" className="add-market-image" onClick={()=>inputRef.current?.click()}><Icon name="image"/><span>Add photos</span></button>}</div>{error&&<p className="form-error">{error}</p>}<button className="primary-button" disabled={busy}>{busy?'Saving…':'Save vehicle'}</button></form></section></div>
}

function TopicsModal({ initialSlug, onClose, onOpenProfile, onEditArticle, onTopicsChanged }) {
  const [activeSlug, setActiveSlug] = useState(initialSlug)
  const [topics, setTopics] = useState([])
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true); setError(''); setDetail(null)
    api(activeSlug ? `/topics/${encodeURIComponent(activeSlug)}` : '/topics')
      .then((data) => activeSlug ? setDetail(data) : setTopics(data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [activeSlug])

  const deleteArticle = async (id) => {
    if (!window.confirm('Delete this article?')) return
    try { await api(`/articles/${id}`, { method:'DELETE' }); setDetail((current) => ({ ...current, articles:current.articles.filter((article) => article.id !== id), topic:{ ...current.topic, conversations:Math.max(0, current.topic.conversations - 1), articles:Math.max(0, current.topic.articles - 1) } })); onTopicsChanged() }
    catch (err) { setError(err.message) }
  }

  return <div className="modal-backdrop" onMouseDown={onClose}><section className="topics-modal" onMouseDown={(event) => event.stopPropagation()}>
    <button className="icon-button close-button" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
    <header>{activeSlug && <button className="topic-back" onClick={() => setActiveSlug(null)}>← All topics</button>}<p className="eyebrow">ARTICLE TOPICS</p><h2>{detail?.topic.name || 'Explore all topics'}</h2><p>{detail?.topic.description || 'Browse article categories created by CartaLaap authors.'}</p>{detail && <div className="topic-stats"><span><b>{detail.topic.articles}</b> {detail.topic.articles === 1 ? 'Article' : 'Articles'}</span></div>}</header>
    <main>{loading && <div className="state-card"><span className="spinner" />Loading topics…</div>}{error && <p className="form-error">{error}</p>}{!loading && !activeSlug && <div className="topic-grid">{topics.map((topic) => <button key={topic.slug} onClick={() => setActiveSlug(topic.slug)}><span>{String(topic.articles).padStart(2, '0')}</span><div><strong>{topic.name}</strong><p>{topic.description}</p><small>{topic.articles} {topic.articles === 1 ? 'article' : 'articles'}</small></div><Icon name="chevron" size={17} /></button>)}</div>}{!loading && detail && <>{detail.articles.length === 0 && <div className="topic-empty"><Icon name="compass" size={30} /><h3>No articles here yet</h3><p>The next article assigned to {detail.topic.name} will appear here.</p></div>}{detail.articles.length > 0 && <section className="topic-results"><h3>Articles</h3>{detail.articles.map((article) => <ArticleCard key={article.id} article={article} onOpenProfile={onOpenProfile} onEdit={(item) => { onClose(); onEditArticle(item) }} onDelete={deleteArticle} />)}</section>}</>}</main>
  </section></div>
}

function ConnectionsModal({ currentUser, onClose, onOpenProfile }) {
  const [tab, setTab] = useState('followers')
  const [people, setPeople] = useState([])
  const [query, setQuery] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async (nextTab, search = '') => {
    setBusy(true); setError('')
    try {
      let result
      if (nextTab === 'blocked') result = await api('/users/me/blocked')
      else if (nextTab === 'discover') result = (await api(`/users/search?q=${encodeURIComponent(search)}&size=30`)).content
      else result = await api(`/users/${encodeURIComponent(currentUser.username)}/${nextTab}`)
      setPeople(result.filter((person) => person.username !== currentUser.username))
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }, [currentUser.username])

  useEffect(() => { load(tab) }, [tab, load])
  useEffect(() => {
    if (tab !== 'discover') return
    const timer = window.setTimeout(() => load('discover', query), 250)
    return () => window.clearTimeout(timer)
  }, [query, tab, load])

  const toggleFollow = async (person) => {
    setError('')
    try {
      const updated = await api(`/users/${encodeURIComponent(person.username)}/follow`, { method: person.followedByCurrentUser ? 'DELETE' : 'POST' })
      if (tab === 'following' && person.followedByCurrentUser) setPeople((current) => current.filter((item) => item.id !== person.id))
      else setPeople((current) => current.map((item) => item.id === updated.id ? updated : item))
    } catch (err) { setError(err.message) }
  }

  const toggleBlock = async (person) => {
    if (!person.blockedByCurrentUser && !window.confirm(`Block @${person.username}? You will unfollow each other and private messages will be disabled.`)) return
    setError('')
    try {
      const updated = await api(`/users/${encodeURIComponent(person.username)}/block`, { method: person.blockedByCurrentUser ? 'DELETE' : 'POST' })
      if (tab === 'blocked' || tab === 'followers' || tab === 'following') setPeople((current) => current.filter((item) => item.id !== person.id))
      else setPeople((current) => current.map((item) => item.id === updated.id ? updated : item))
    } catch (err) { setError(err.message) }
  }

  return <div className="modal-backdrop" onMouseDown={onClose}><section className="connections-modal" onMouseDown={(event) => event.stopPropagation()}>
    <button className="icon-button close-button" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
    <header><p className="eyebrow">YOUR CREW</p><h2>Connections</h2><p>Find enthusiasts, manage your crew, and control who can contact you.</p></header>
    <nav>{['followers', 'following', 'discover', 'blocked'].map((item) => <button className={tab === item ? 'active' : ''} key={item} onClick={() => setTab(item)}>{item[0].toUpperCase() + item.slice(1)}</button>)}</nav>
    {tab === 'discover' && <div className="connections-search"><Icon name="search" size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by name or username…" autoFocus /></div>}
    {error && <p className="form-error">{error}</p>}
    <div className="connections-list">{busy && <div className="state-card"><span className="spinner" />Loading connections…</div>}{!busy && people.length === 0 && <div className="connections-empty"><Icon name="users" size={30} /><strong>{tab === 'blocked' ? 'No blocked users' : tab === 'discover' ? 'No enthusiasts found' : `No ${tab} yet`}</strong><span>{tab === 'discover' ? 'Try a different search.' : 'Your connections will appear here.'}</span></div>}{!busy && people.map((person) => <article key={person.id}>
      <button className="connection-person" onClick={() => onOpenProfile(person.username)}><Avatar user={person} /><span><strong>{person.displayName}</strong><small>@{person.username}</small>{person.bio && <p>{person.bio}</p>}</span></button>
      <div className="connection-actions">{!person.blockedByCurrentUser && !person.blocksCurrentUser && <button className={person.followedByCurrentUser ? 'secondary-button' : 'primary-button'} onClick={() => toggleFollow(person)}>{person.followedByCurrentUser ? 'Following' : 'Follow'}</button>}<button className="connection-block" onClick={() => toggleBlock(person)} disabled={person.blocksCurrentUser}>{person.blockedByCurrentUser ? 'Unblock' : person.blocksCurrentUser ? 'Unavailable' : 'Block'}</button></div>
    </article>)}</div>
  </section></div>
}

function ProfileModal({ username, currentUser, onClose, onAuthRequired, onCurrentUserUpdated, onMessage, onGarageEdit, focusGarage = false }) {
  const avatarInput = useRef(null)
  const garageSection = useRef(null)
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(null)
  const [avatarFile, setAvatarFile] = useState(null)
  const [avatarPreview, setAvatarPreview] = useState(null)
  const [listMode, setListMode] = useState(null)
  const [people, setPeople] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [garage, setGarage] = useState([])

  useEffect(() => {
    setProfile(null); setForm(null); setError(''); setListMode(null)
    api(`/users/${encodeURIComponent(username)}`).then((data) => {
      setProfile(data)
      setForm({ displayName: data.displayName, bio: data.bio || '', avatarUrl: data.avatarUrl || '', location: data.location || '', vehicleInterests: data.vehicleInterests || '' })
    }).catch((err) => setError(err.message))
    api(`/garage/users/${encodeURIComponent(username)}`).then(setGarage).catch(() => setGarage([]))
  }, [username])

  useEffect(() => () => { if (avatarPreview) URL.revokeObjectURL(avatarPreview) }, [avatarPreview])
  useEffect(() => { if (profile && focusGarage) window.setTimeout(() => garageSection.current?.scrollIntoView({ behavior:'smooth', block:'center' }), 80) }, [profile, focusGarage])

  const chooseAvatar = (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) { setError('Choose a JPEG, PNG, or GIF image.'); return }
    if (file.size > 10 * 1024 * 1024) { setError('Profile pictures must be 10 MB or smaller.'); return }
    setError(''); setAvatarFile(file); setAvatarPreview(URL.createObjectURL(file))
  }

  const save = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try {
      let avatarUrl = form.avatarUrl || null
      if (avatarFile) {
        const data = new FormData(); data.append('image', avatarFile)
        avatarUrl = (await api('/media/images', { method: 'POST', body: data })).url
      }
      const updated = await api('/users/me', { method: 'PATCH', body: JSON.stringify({ ...form, avatarUrl }) })
      onCurrentUserUpdated(updated)
      setProfile({ ...profile, ...updated }); setForm({ ...form, avatarUrl }); setAvatarFile(null); setAvatarPreview(null)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const toggleFollow = async () => {
    if (!currentUser) { onAuthRequired(); return }
    setBusy(true); setError('')
    try {
      const updated = await api(`/users/${encodeURIComponent(profile.username)}/follow`, { method: profile.followedByCurrentUser ? 'DELETE' : 'POST' })
      setProfile(updated)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const toggleBlock = async () => {
    if (!currentUser) { onAuthRequired(); return }
    if (!profile.blockedByCurrentUser && !window.confirm(`Block @${profile.username}? You will unfollow each other and private messages will be disabled.`)) return
    setBusy(true); setError('')
    try {
      const updated = await api(`/users/${encodeURIComponent(profile.username)}/block`, { method: profile.blockedByCurrentUser ? 'DELETE' : 'POST' })
      setProfile(updated)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const showPeople = async (mode) => {
    setListMode(mode); setBusy(true); setError('')
    try { setPeople(await api(`/users/${encodeURIComponent(profile.username)}/${mode}`)) }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const update = (event) => setForm({ ...form, [event.target.name]: event.target.value })
  const removeVehicle = async (id) => { if (!window.confirm('Remove this vehicle from your garage?')) return; try { await api(`/garage/${id}`, { method:'DELETE' }); setGarage((current)=>current.filter((vehicle)=>vehicle.id!==id)) } catch (err) { setError(err.message) } }
  return <div className="modal-backdrop" onMouseDown={onClose}><section className="profile-modal" onMouseDown={(event) => event.stopPropagation()}>
    <button className="icon-button close-button" onClick={onClose} aria-label="Close"><Icon name="x" /></button>
    {!profile && !error && <div className="state-card"><span className="spinner" />Loading profile…</div>}
    {error && !profile && <p className="form-error">{error}</p>}
    {profile && <>
      <div className="profile-hero">
        <div className="profile-avatar-wrap"><Avatar user={{ ...profile, avatarUrl: avatarPreview || profile.avatarUrl }} className="profile-avatar" />{profile.ownedByCurrentUser && <button onClick={() => avatarInput.current?.click()} aria-label="Change profile picture"><Icon name="image" size={17} /></button>}</div>
        <div className="profile-identity"><p className="eyebrow">CARTALAAP MEMBER</p><h2>{profile.displayName}</h2><span>@{profile.username}</span></div>
        {!profile.ownedByCurrentUser && <div className="profile-hero-actions">{!profile.blockedByCurrentUser && !profile.blocksCurrentUser && <><button className="secondary-button" onClick={() => onMessage(profile.username)}><Icon name="message" size={16} /> Message</button><button className={profile.followedByCurrentUser ? 'secondary-button' : 'primary-button'} onClick={toggleFollow} disabled={busy}>{profile.followedByCurrentUser ? 'Following' : 'Follow'}</button></>}<button className="profile-block-button" onClick={toggleBlock} disabled={busy || profile.blocksCurrentUser}>{profile.blockedByCurrentUser ? 'Unblock' : profile.blocksCurrentUser ? 'Unavailable' : 'Block'}</button></div>}
      </div>
      <div className="profile-counts"><button onClick={() => showPeople('followers')}><strong>{profile.followers}</strong><span>Followers</span></button><button onClick={() => showPeople('following')}><strong>{profile.following}</strong><span>Following</span></button><div><strong>{new Date(profile.joinedAt).getFullYear()}</strong><span>Member since</span></div></div>
      {listMode ? <div className="people-list"><div className="people-list-head"><h3>{listMode === 'followers' ? 'Followers' : 'Following'}</h3><button onClick={() => setListMode(null)}>Back to profile</button></div>{busy && <span className="spinner" />}{!busy && people.length === 0 && <p>No users here yet.</p>}{people.map((person) => <div className="profile-person" key={person.id}><Avatar user={person} /><div><strong>{person.displayName}</strong><span>@{person.username}</span></div></div>)}</div> : profile.ownedByCurrentUser ? <form className="profile-form" onSubmit={save}>
        <input ref={avatarInput} className="file-input" type="file" accept="image/jpeg,image/png,image/gif" onChange={chooseAvatar} />
        <label>Display name<input name="displayName" required minLength="2" maxLength="80" value={form.displayName} onChange={update} /></label>
        <label>Bio<textarea name="bio" maxLength="300" value={form.bio} onChange={update} placeholder="Tell the community about yourself and your garage." /></label>
        <div className="profile-form-row"><label>Location<input name="location" maxLength="100" value={form.location} onChange={update} placeholder="Kolkata, India" /></label><label>Vehicle interests<input name="vehicleInterests" maxLength="300" value={form.vehicleInterests} onChange={update} placeholder="JDM, motorcycles, EVs" /></label></div>
        {error && <p className="form-error">{error}</p>}<button className="primary-button" disabled={busy}>{busy ? 'Saving…' : 'Save profile'}</button>
      </form> : <div className="profile-about"><h3>About</h3><p>{profile.bio || 'This enthusiast has not added a bio yet.'}</p><div>{profile.location && <span>📍 {profile.location}</span>}{profile.vehicleInterests && <span>🏁 {profile.vehicleInterests}</span>}</div>{error && <p className="form-error">{error}</p>}</div>}
      {!listMode && <section ref={garageSection} className={`profile-garage ${focusGarage?'garage-focused':''}`}><div className="profile-garage-head"><div><p className="eyebrow">PERSONAL GARAGE</p><h3>Vehicles</h3></div>{profile.ownedByCurrentUser&&<button onClick={()=>onGarageEdit(null)}><Icon name="plus" size={15}/> Add vehicle</button>}</div>{garage.length===0?<p className="garage-empty">No vehicles added yet.</p>:<div className="garage-grid">{garage.map((vehicle)=><article key={vehicle.id}><img src={vehicle.imageUrls[0]} alt=""/><div><span>{vehicle.status} · {vehicle.vehicleType}</span><h4>{vehicle.year?`${vehicle.year} `:''}{vehicle.brand} {vehicle.model}</h4><p>{vehicle.variant||vehicle.fuelType||'Garage vehicle'}</p>{vehicle.modifications&&<small>{vehicle.modifications}</small>}{vehicle.ownedByCurrentUser&&<div><button onClick={()=>onGarageEdit(vehicle)}>Edit</button><button onClick={()=>removeVehicle(vehicle.id)}>Remove</button></div>}</div></article>)}</div>}</section>}
    </>}
  </section></div>
}

function CommentItem({ comment, currentUser, onUpdated, onDeleted }) {
  const [editing, setEditing] = useState(false)
  const [body, setBody] = useState(comment.body || '')
  const [busy, setBusy] = useState(false)
  const owned = currentUser?.username === comment.author.username

  const save = async () => {
    if (!body.trim() && !comment.imageUrl) return
    setBusy(true)
    try {
      const updated = await api(`/comments/${comment.id}`, { method: 'PATCH', body: JSON.stringify({ body, imageUrl: comment.imageUrl }) })
      onUpdated(updated)
      setEditing(false)
    } finally { setBusy(false) }
  }

  const remove = async () => {
    if (!window.confirm('Delete this comment?')) return
    setBusy(true)
    try { await api(`/comments/${comment.id}`, { method: 'DELETE' }); onDeleted(comment.id) } finally { setBusy(false) }
  }

  return <div className="comment-item">
    <Avatar user={comment.author} />
    <div className="comment-content">
      <div className="comment-meta"><strong>{comment.author.displayName}</strong><span>@{comment.author.username}</span>{owned && <div className="owner-actions"><button onClick={() => setEditing(!editing)}>Edit</button><button onClick={remove} disabled={busy}>Delete</button></div>}</div>
      {editing ? <div className="inline-editor"><textarea value={body} onChange={(event) => setBody(event.target.value)} maxLength="2000" /><div><button onClick={() => { setEditing(false); setBody(comment.body || '') }}>Cancel</button><button className="save-action" onClick={save} disabled={busy || (!body.trim() && !comment.imageUrl)}>Save</button></div></div> : comment.body ? <p>{comment.body}</p> : null}
      {comment.imageUrl && <img className="comment-image" src={comment.imageUrl} alt="Shared in this comment" />}
    </div>
  </div>
}

function PostCard({ post, currentUser, onAuthRequired, onUpdated, onDeleted, onOpenProfile }) {
  const [commentsOpen, setCommentsOpen] = useState(false)
  const [comments, setComments] = useState([])
  const [commentsLoaded, setCommentsLoaded] = useState(false)
  const [commentBody, setCommentBody] = useState('')
  const commentImageInput = useRef(null)
  const [commentImageFile, setCommentImageFile] = useState(null)
  const [commentImagePreview, setCommentImagePreview] = useState(null)
  const [commentBusy, setCommentBusy] = useState(false)
  const [editing, setEditing] = useState(false)
  const [editBody, setEditBody] = useState(post.body)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const days = Math.max(-30, Math.ceil((new Date(post.createdAt) - Date.now()) / 86400000))
  const owned = post.ownedByCurrentUser || currentUser?.username === post.author.username

  const vote = async (value) => {
    if (!currentUser) { onAuthRequired(); return }
    setError('')
    try {
      const target = post.currentUserVote === value ? 0 : value
      const summary = await api(`/posts/${post.id}/vote`, { method: 'PUT', body: JSON.stringify({ value: target }) })
      onUpdated({ ...post, ...summary })
    } catch (err) { setError(err.message) }
  }

  const toggleComments = async () => {
    const nextOpen = !commentsOpen
    setCommentsOpen(nextOpen)
    if (nextOpen && !commentsLoaded) {
      try { setComments(await api(`/posts/${post.id}/comments`)); setCommentsLoaded(true) } catch (err) { setError(err.message) }
    }
  }

  const addComment = async (event) => {
    event.preventDefault()
    if (!currentUser) { onAuthRequired(); return }
    setCommentBusy(true); setError('')
    try {
      let imageUrl = null
      if (commentImageFile) {
        const form = new FormData()
        form.append('image', commentImageFile)
        imageUrl = (await api('/media/images', { method: 'POST', body: form })).url
      }
      const created = await api(`/posts/${post.id}/comments`, { method: 'POST', body: JSON.stringify({ body: commentBody, imageUrl }) })
      setComments((current) => [...current, created]); setCommentBody(''); setCommentImageFile(null); setCommentImagePreview(null)
      onUpdated({ ...post, commentCount: (post.commentCount || 0) + 1 })
    } catch (err) { setError(err.message) } finally { setCommentBusy(false) }
  }

  const chooseCommentImage = (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) { setError('Choose a JPEG, PNG, or GIF image.'); return }
    if (file.size > 10 * 1024 * 1024) { setError('Images must be 10 MB or smaller.'); return }
    setError(''); setCommentImageFile(file); setCommentImagePreview(URL.createObjectURL(file))
  }

  useEffect(() => () => { if (commentImagePreview) URL.revokeObjectURL(commentImagePreview) }, [commentImagePreview])

  const savePost = async () => {
    if (!editBody.trim()) return
    setBusy(true); setError('')
    try {
      const updated = await api(`/posts/${post.id}`, { method: 'PATCH', body: JSON.stringify({ body: editBody, imageUrl: post.imageUrl }) })
      onUpdated(updated); setEditing(false)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const deletePost = async () => {
    if (!window.confirm('Delete this post and all of its comments?')) return
    setBusy(true); setError('')
    try { await api(`/posts/${post.id}`, { method: 'DELETE' }); onDeleted(post.id) } catch (err) { setError(err.message); setBusy(false) }
  }

  const updateComment = (updated) => setComments(comments.map((comment) => comment.id === updated.id ? updated : comment))
  const deleteComment = (commentId) => {
    setComments(comments.filter((comment) => comment.id !== commentId))
    onUpdated({ ...post, commentCount: Math.max(0, (post.commentCount || 0) - 1) })
  }

  return <article className="post-card">
    <div className="post-head"><button className="avatar-button" onClick={() => onOpenProfile(post.author.username)}><Avatar user={post.author} /></button><button className="post-author author-button" onClick={() => onOpenProfile(post.author.username)}><strong>{post.author.displayName}</strong><span>@{post.author.username} · {new Intl.RelativeTimeFormat('en', { numeric: 'auto' }).format(days, 'day')}</span></button>{owned ? <div className="owner-actions"><button onClick={() => setEditing(!editing)}>Edit</button><button onClick={deletePost} disabled={busy}>Delete</button></div> : <button className="icon-button"><Icon name="more" /></button>}</div>
    {editing ? <div className="inline-editor post-editor"><textarea value={editBody} onChange={(event) => setEditBody(event.target.value)} maxLength="5000" /><div><button onClick={() => { setEditing(false); setEditBody(post.body) }}>Cancel</button><button className="save-action" onClick={savePost} disabled={busy || !editBody.trim()}>Save post</button></div></div> : <p className="post-body">{post.body}</p>}
    {post.imageUrl && <img className="post-image" src={post.imageUrl} alt="Shared by the post author" />}
    <div className="post-actions">
      <button className={post.currentUserVote === 1 ? 'active' : ''} onClick={() => vote(1)}><Icon name="arrowUp" /><span>{post.upvotes || 0}</span></button>
      <strong className={(post.score || 0) < 0 ? 'negative-score' : ''}>{post.score || 0}</strong>
      <button className={post.currentUserVote === -1 ? 'active downvote' : 'downvote'} onClick={() => vote(-1)}><Icon name="arrowDown" /><span>{post.downvotes || 0}</span></button>
      <button onClick={toggleComments}><Icon name="comment" /><span>{post.commentCount || 0} Comments</span></button>
      <button><Icon name="share" /><span>Share</span></button>
    </div>
    {error && <p className="form-error post-error">{error}</p>}
    {commentsOpen && <section className="comments-section">
      <h4>Discussion</h4>
      {commentsLoaded && comments.length === 0 && <p className="no-comments">No comments yet. Start the conversation.</p>}
      {comments.map((comment) => <CommentItem key={comment.id} comment={comment} currentUser={currentUser} onUpdated={updateComment} onDeleted={deleteComment} />)}
      <form className="comment-form" onSubmit={addComment}><Avatar user={currentUser} /><div className="comment-compose-main">{commentImagePreview&&<div className="comment-image-preview"><img src={commentImagePreview} alt="Selected comment attachment"/><button type="button" onClick={()=>{setCommentImageFile(null);setCommentImagePreview(null)}} aria-label="Remove image"><Icon name="x" size={14}/></button></div>}<div><textarea value={commentBody} onChange={(event) => setCommentBody(event.target.value)} maxLength="2000" placeholder={currentUser ? 'Write a comment…' : 'Log in to join the discussion…'} /><input ref={commentImageInput} className="file-input" type="file" accept="image/jpeg,image/png,image/gif" onChange={chooseCommentImage}/><button type="button" className="comment-attach" onClick={()=>currentUser?commentImageInput.current?.click():onAuthRequired()} aria-label="Attach an image"><Icon name="image" size={16}/></button><button className="comment-send" disabled={commentBusy || (!commentBody.trim()&&!commentImageFile)}><Icon name="send" size={16} /></button></div></div></form>
    </section>}
  </article>
}

function CommunityPollCard({ message, busy, onVote }) {
  const poll=message.poll
  return <section className="community-poll"><div className="community-poll-heading"><Icon name="poll" size={18}/><div><span>COMMUNITY POLL</span><h4>{poll.question}</h4></div></div><div className="community-poll-options">{poll.options.map((option)=>{const percent=poll.totalVotes?Math.round(option.votes*100/poll.totalVotes):0;const selected=poll.selectedOptionId===option.id;return <button className={selected?'selected':''} disabled={busy} onClick={()=>onVote(message.id,option.id)} key={option.id}><i style={{width:`${percent}%`}}/><span><strong>{option.text}</strong><small>{option.votes} · {percent}%</small></span>{selected&&<b>✓</b>}</button>})}</div><footer>{poll.totalVotes} {poll.totalVotes===1?'vote':'votes'}{poll.selectedOptionId&&<span>You can change your vote</span>}</footer></section>
}

function CommunitiesModal({ currentUser, realtime, onClose, onOpenProfile }) {
  const [discover, setDiscover] = useState([])
  const [mine, setMine] = useState([])
  const [invites, setInvites] = useState([])
  const [active, setActive] = useState(null)
  const [messages, setMessages] = useState([])
  const [members, setMembers] = useState([])
  const [view, setView] = useState('mine')
  const [query, setQuery] = useState('')
  const [creating, setCreating] = useState(false)
  const [communityForm, setCommunityForm] = useState({ name:'', description:'' })
  const [inviteUsername, setInviteUsername] = useState('')
  const [body, setBody] = useState('')
  const [replyTo, setReplyTo] = useState(null)
  const [imageFile, setImageFile] = useState(null)
  const [imagePreview, setImagePreview] = useState(null)
  const [pollOpen, setPollOpen] = useState(false)
  const [pollQuestion, setPollQuestion] = useState('')
  const [pollOptions, setPollOptions] = useState(['',''])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const fileInput = useRef(null)
  const threadPane = useRef(null)

  const refreshRooms = useCallback(async () => {
    const [allRooms, myRooms, pending] = await Promise.all([api('/communities'), api('/communities/mine'), api('/communities/invites')])
    setDiscover(allRooms); setMine(myRooms); setInvites(pending)
  }, [])

  useEffect(() => { refreshRooms().catch((err) => setError(err.message)) }, [refreshRooms])
  useEffect(() => () => { if (imagePreview) URL.revokeObjectURL(imagePreview) }, [imagePreview])
  const scrollThreadToBottom = useCallback((behavior='smooth') => {
    window.requestAnimationFrame(() => {
      const pane = threadPane.current
      if (pane) pane.scrollTo({ top:pane.scrollHeight, behavior })
    })
  }, [])
  useEffect(() => { scrollThreadToBottom(messages.length ? 'smooth' : 'auto') }, [messages, scrollThreadToBottom])
  useEffect(() => {
    const event = realtime.lastEvent
    if (!event || event.communitySlug !== active?.slug) return
    if (event.type === 'community_message') setMessages((current) => current.some((message) => message.id === event.message.id) ? current : [...current, event.message])
    if (event.type === 'community_message_updated') setMessages((current) => current.map((message) => message.id===event.message.id?event.message:message))
    if (event.type === 'community_message_deleted') setMessages((current) => current.filter((message) => message.id !== event.messageId))
  }, [realtime.lastEvent, active?.slug])

  const openRoom = async (room) => {
    if (!room.joinedByCurrentUser) return
    setError(''); setActive(room); setMessages([]); setMembers([])
    try {
      const [roomMessages, roomMembers] = await Promise.all([api(`/communities/${room.slug}/messages`), api(`/communities/${room.slug}/members`)])
      setMessages(roomMessages); setMembers(roomMembers)
    } catch (err) { setError(err.message) }
  }

  const joinRoom = async (room) => {
    setBusy(true); setError('')
    try { const joined = await api(`/communities/${room.slug}/join`, { method:'POST' }); await refreshRooms(); await openRoom(joined) }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const createRoom = async (event) => {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const created = await api('/communities', { method:'POST', body:JSON.stringify(communityForm) })
      setCommunityForm({ name:'', description:'' }); setCreating(false); await refreshRooms(); await openRoom(created)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const answerInvite = async (invite, accept) => {
    setBusy(true); setError('')
    try {
      const joined = await api(`/communities/invites/${invite.id}/${accept?'accept':'decline'}`, { method:'POST' })
      await refreshRooms(); if (accept) await openRoom(joined)
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const sendInvite = async (event) => {
    event.preventDefault(); if (!inviteUsername.trim()) return
    setBusy(true); setError('')
    try { await api(`/communities/${active.slug}/invites/${encodeURIComponent(inviteUsername.replace(/^@/,''))}`, { method:'POST' }); setInviteUsername('') }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const leaveRoom = async () => {
    if (!window.confirm(`Leave ${active.name}?`)) return
    setBusy(true); setError('')
    try { await api(`/communities/${active.slug}/leave`, { method:'DELETE' }); setActive(null); setMessages([]); await refreshRooms() }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const chooseImage = (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!['image/jpeg','image/png','image/gif'].includes(file.type)) { setError('Choose a JPEG, PNG, or GIF image.'); return }
    if (file.size > 10*1024*1024) { setError('Images must be 10 MB or smaller.'); return }
    setError(''); setImageFile(file); setImagePreview(URL.createObjectURL(file))
  }

  const clearAttachment = () => { setImageFile(null); setImagePreview(null) }
  const resetPoll = () => { setPollOpen(false); setPollQuestion(''); setPollOptions(['','']) }
  const pollReady = pollOpen&&pollQuestion.trim()&&pollOptions.filter((option)=>option.trim()).length>=2
  const sendMessage = async (event) => {
    event.preventDefault(); if (!body.trim()&&!imageFile&&!pollReady) return
    setBusy(true); setError('')
    try {
      let imageUrl = null
      if (imageFile) { const form=new FormData();form.append('image',imageFile);imageUrl=(await api('/media/images',{method:'POST',body:form})).url }
      const poll=pollReady?{question:pollQuestion,options:pollOptions.filter((option)=>option.trim())}:null
      const sent = await api(`/communities/${active.slug}/messages`, { method:'POST', body:JSON.stringify({ body, imageUrl, replyToId:replyTo?.id, poll }) })
      setMessages((current)=>current.some((message)=>message.id===sent.id)?current:[...current,sent]);setBody('');setReplyTo(null);clearAttachment();resetPoll()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const deleteMessage = async (id) => {
    if (!window.confirm('Delete this message?')) return
    try { await api(`/communities/messages/${id}`,{method:'DELETE'});setMessages((current)=>current.filter((message)=>message.id!==id)) }
    catch (err) { setError(err.message) }
  }

  const votePoll = async (messageId,optionId) => {
    setBusy(true);setError('')
    try { const updated=await api(`/communities/messages/${messageId}/poll/vote`,{method:'POST',body:JSON.stringify({optionId})});setMessages((current)=>current.map((message)=>message.id===updated.id?updated:message)) }
    catch(err){setError(err.message)}finally{setBusy(false)}
  }

  const filtered = (view==='mine'?mine:discover).filter((room)=>!query.trim()||room.name.toLowerCase().includes(query.trim().toLowerCase()))
  return <div className="modal-backdrop" onMouseDown={onClose}><section className={`communities-modal ${active?'room-open':'room-list-open'}`} onMouseDown={(event)=>event.stopPropagation()}>
    <button className="icon-button close-button" onClick={onClose} aria-label="Close communities"><Icon name="x"/></button>
    <aside className="community-rail">
      <div className="community-title"><p className="eyebrow">CLUBHOUSE</p><h2>Communities</h2><p>Casual rooms for every kind of enthusiast.</p></div>
      <div className="community-tabs"><button className={view==='mine'?'active':''} onClick={()=>setView('mine')}>My rooms</button><button className={view==='discover'?'active':''} onClick={()=>setView('discover')}>Discover</button></div>
      <label className="community-search"><Icon name="search" size={15}/><input value={query} onChange={(event)=>setQuery(event.target.value)} placeholder="Find a community"/></label>
      {invites.length>0&&<section className="community-invites"><strong>Invitations</strong>{invites.map((invite)=><article key={invite.id}><div><b>{invite.community.name}</b><small>from @{invite.inviter.username}</small></div><button onClick={()=>answerInvite(invite,true)}>Join</button><button onClick={()=>answerInvite(invite,false)}>No</button></article>)}</section>}
      <div className="community-list">{filtered.length===0&&<p>{view==='mine'?'You have not joined a room yet.':'No communities found.'}</p>}{filtered.map((room)=><article className={active?.id===room.id?'active':''} key={room.id}><button className="community-room-main" onClick={()=>room.joinedByCurrentUser?openRoom(room):null}><span>#</span><div><strong>{room.name}</strong><small>{room.memberCount} {room.memberCount===1?'member':'members'}</small></div></button>{!room.joinedByCurrentUser&&<button className="community-join" disabled={busy} onClick={()=>joinRoom(room)}>Join</button>}</article>)}</div>
      <button className="create-community-button" onClick={()=>setCreating(!creating)}><Icon name="plus" size={15}/>{creating?'Cancel':'Create community'}</button>
      {creating&&<form className="community-create" onSubmit={createRoom}><label><span>#</span><input required minLength="3" maxLength="50" value={communityForm.name} onChange={(event)=>setCommunityForm({...communityForm,name:event.target.value.replace(/^#/,'')})} placeholder="tiago"/></label><textarea maxLength="300" value={communityForm.description} onChange={(event)=>setCommunityForm({...communityForm,description:event.target.value})} placeholder="What is this community about?"/><button disabled={busy}>Create room</button></form>}
    </aside>
    <main className="community-chat">
      {active?<><header><button className="mobile-panel-back" onClick={()=>{setActive(null);setMessages([]);resetPoll()}} aria-label="Back to communities"><Icon name="chevron"/></button><div className="community-hash">#</div><div className="community-chat-title"><strong>{active.name}</strong><span>{active.description||'A CartaLaap community hangout'} · {members.length} {members.length===1?'member':'members'}</span></div>{!active.ownedByCurrentUser&&<button className="community-leave" onClick={leaveRoom} disabled={busy}>Leave</button>}</header>
        <div className="community-tools"><div className="community-members">{members.slice(0,5).map((member)=><button key={member.id} title={`@${member.username}`} onClick={()=>onOpenProfile(member.username)}><Avatar user={member}/></button>)}{members.length>5&&<span>+{members.length-5}</span>}</div><form onSubmit={sendInvite}><input value={inviteUsername} onChange={(event)=>setInviteUsername(event.target.value)} placeholder="Invite by username"/><button disabled={busy||!inviteUsername.trim()}>Invite</button></form></div>
        <div className="community-thread" ref={threadPane}>{messages.length===0&&<div className="community-welcome"><span>#</span><h3>Welcome to {active.name}</h3><p>This is the start of the room. Say hello, share a photo, or ask the crew a question.</p></div>}{messages.map((message)=><article className={`community-message ${message.ownedByCurrentUser?'mine':''}`} key={message.id}><button className="avatar-button" onClick={()=>onOpenProfile(message.sender.username)}><Avatar user={message.sender}/></button><div><div className="community-message-meta"><strong>{message.sender.displayName}</strong><span>@{message.sender.username} · {new Date(message.createdAt).toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'})}</span></div>{message.replyTo&&<button className="community-reply-preview" onClick={()=>document.getElementById(`community-message-${message.replyTo.id}`)?.scrollIntoView({behavior:'smooth',block:'center'})}><strong>{message.replyTo.senderName}</strong><span>{message.replyTo.body||message.replyTo.pollQuestion||'Image'}</span>{message.replyTo.imageUrl&&<Icon name="image" size={13}/>}</button>}<div id={`community-message-${message.id}`} className="community-message-body">{message.body&&<p>{message.body}</p>}{message.imageUrl&&<img src={message.imageUrl} alt="Shared in this community" onLoad={()=>scrollThreadToBottom('auto')}/>}</div>{message.poll&&<CommunityPollCard message={message} busy={busy} onVote={votePoll}/>}<div className="community-message-actions"><button onClick={()=>setReplyTo(message)}>Reply</button>{message.ownedByCurrentUser&&<button onClick={()=>deleteMessage(message.id)}>Delete</button>}</div></div></article>)}</div>
        <form className="community-composer" onSubmit={sendMessage}>{replyTo&&<div className="community-composer-reply"><span>Replying to <strong>{replyTo.sender.displayName}</strong>: {replyTo.body||replyTo.poll?.question||'Image'}</span><button type="button" onClick={()=>setReplyTo(null)}><Icon name="x" size={14}/></button></div>}{imagePreview&&<div className="community-image-preview"><img src={imagePreview} alt="Selected attachment"/><button type="button" onClick={clearAttachment}><Icon name="x" size={14}/></button></div>}{pollOpen&&<section className="community-poll-editor"><header><div><Icon name="poll" size={17}/><strong>Create a poll</strong></div><button type="button" onClick={resetPoll} aria-label="Close poll editor"><Icon name="x" size={15}/></button></header><input required maxLength="300" value={pollQuestion} onChange={(event)=>setPollQuestion(event.target.value)} placeholder="Ask the community a question"/>{pollOptions.map((option,index)=><label key={index}><span>{index+1}</span><input required maxLength="120" value={option} onChange={(event)=>setPollOptions((current)=>current.map((item,itemIndex)=>itemIndex===index?event.target.value:item))} placeholder={`Option ${index+1}`}/>{pollOptions.length>2&&<button type="button" onClick={()=>setPollOptions((current)=>current.filter((_,itemIndex)=>itemIndex!==index))} aria-label={`Remove option ${index+1}`}><Icon name="x" size={13}/></button>}</label>)}{pollOptions.length<6&&<button type="button" className="add-poll-option" onClick={()=>setPollOptions((current)=>[...current,''])}><Icon name="plus" size={13}/> Add option</button>}</section>}<div><input ref={fileInput} className="file-input" type="file" accept="image/jpeg,image/png,image/gif" onChange={chooseImage}/><button type="button" className="community-attach" onClick={()=>fileInput.current?.click()} aria-label="Attach image"><Icon name="image"/></button><button type="button" className={`community-poll-toggle ${pollOpen?'active':''}`} onClick={()=>pollOpen?resetPoll():setPollOpen(true)} aria-label="Create poll"><Icon name="poll"/></button><textarea value={body} onChange={(event)=>setBody(event.target.value)} maxLength="2000" placeholder={`Message ${active.name}`}/><button className="community-send" disabled={busy||(!body.trim()&&!imageFile&&!pollReady)}><Icon name="send"/></button></div></form>
      </>:<div className="community-placeholder"><div>#</div><h2>Your community garage</h2><p>Choose one of your rooms, discover a new crew, or create a unique space for the vehicles and conversations you love.</p><button onClick={()=>setView('discover')}>Explore communities</button></div>}
      {error&&<p className="form-error community-error">{error}</p>}
    </main>
  </section></div>
}

function AboutPage({ currentUser, onExplore, onJoin }) {
  return <main className="about-page"><section className="about-hero"><div><p className="eyebrow">ABOUT CARTALAAP</p><h1>Built for people who live for the road.</h1><p>CartaLaap is a community home for car and motorcycle enthusiasts—somewhere to share builds, solve problems, publish stories, meet fellow owners, and find the next machine or part.</p><div className="about-actions"><button className="primary-button" onClick={onExplore}>Explore the community</button>{!currentUser&&<button className="about-secondary" onClick={onJoin}>Join the crew</button>}</div></div><div className="about-mark"><span>CL</span><p>Every vehicle has a story.<br/>CartaLaap gives it a garage.</p></div></section><section className="about-values"><article><span>01</span><h2>Share the journey</h2><p>Post quick updates, publish detailed articles, and preserve every stage of a build or ownership story.</p></article><article><span>02</span><h2>Learn together</h2><p>Ask technical questions, exchange experience, and discover ideas from enthusiasts who have been there before.</p></article><article><span>03</span><h2>Build your crew</h2><p>Follow people, message privately, share Moments, and connect through the vehicles and roads you care about.</p></article></section><section className="about-story"><p className="eyebrow">THE IDEA</p><h2>More than a social feed.</h2><p>CartaLaap brings conversations, long-form knowledge, personal garages, and a community marketplace into one focused platform. It is being built for enthusiasts first: useful, welcoming, and always centred on automobiles.</p></section></main>
}

function App() {
  const fileInputRef = useRef(null)
  const [authMode, setAuthMode] = useState(null)
  const [user, setUser] = useState(null)
  const [posts, setPosts] = useState([])
  const [feedState, setFeedState] = useState('loading')
  const [composer, setComposer] = useState({ body: '' })
  const [imageFile, setImageFile] = useState(null)
  const [imagePreview, setImagePreview] = useState(null)
  const [publishing, setPublishing] = useState(false)
  const [notice, setNotice] = useState('')
  const [profileUsername, setProfileUsername] = useState(null)
  const [profileGarageFocus, setProfileGarageFocus] = useState(false)
  const [showAbout, setShowAbout] = useState(() => window.location.hash === '#about')
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [suggestedUsers, setSuggestedUsers] = useState([])
  const [feedMode, setFeedMode] = useState('all')
  const [articles, setArticles] = useState([])
  const [articleEditor, setArticleEditor] = useState(null)
  const [messagesOpen, setMessagesOpen] = useState(false)
  const [communitiesOpen, setCommunitiesOpen] = useState(false)
  const [messageTarget, setMessageTarget] = useState(null)
  const [moments, setMoments] = useState([])
  const [momentUploadOpen, setMomentUploadOpen] = useState(false)
  const [momentIndex, setMomentIndex] = useState(null)
  const [listings, setListings] = useState([])
  const [marketEditor, setMarketEditor] = useState(null)
  const [marketDetail, setMarketDetail] = useState(null)
  const [marketView, setMarketView] = useState('all')
  const [marketFilters, setMarketFilters] = useState({ q:'', category:'', minPrice:'', maxPrice:'', sort:'newest' })
  const [notifications, setNotifications] = useState([])
  const [unreadNotifications, setUnreadNotifications] = useState(0)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [garageEditor, setGarageEditor] = useState(null)
  const [connectionsOpen, setConnectionsOpen] = useState(false)
  const [trendingTopics, setTrendingTopics] = useState([])
  const [topicModal, setTopicModal] = useState(false)
  const [unreadMessages, setUnreadMessages] = useState(0)
  const realtime = useRealtime(user)
  const updateUnreadMessages = useCallback((inbox) => {
    setUnreadMessages(inbox.reduce((total, conversation) => total + conversation.unreadCount, 0))
  }, [])

  const loadFeed = (mode = feedMode) => {
    if (mode === 'following' && !sessionStorage.getItem(TOKEN_KEY)) { setAuthMode('login'); return }
    setFeedState('loading')
    api(mode === 'marketplace' ? '/marketplace/listings' : mode === 'articles' ? '/articles' : mode === 'following' ? '/posts/following' : '/posts')
      .then((data) => { if (mode === 'marketplace') setListings(data.content); else if (mode === 'articles') setArticles(data.content); else setPosts(data.content); setFeedState('ready'); setFeedMode(mode) })
      .catch(() => setFeedState('offline'))
  }

  useEffect(() => {
    localStorage.removeItem(TOKEN_KEY)
    loadFeed('all')
    if (sessionStorage.getItem(TOKEN_KEY)) api('/users/me').then(setUser).catch(() => sessionStorage.removeItem(TOKEN_KEY))
    api('/users/suggestions?size=4').then(setSuggestedUsers).catch(() => {})
    api('/moments').then(setMoments).catch(() => {})
    api('/topics/trending?limit=4').then(setTrendingTopics).catch(() => setTrendingTopics([]))
  }, [])
  useEffect(() => { const syncPage=()=>setShowAbout(window.location.hash==='#about'); window.addEventListener('hashchange',syncPage); return()=>window.removeEventListener('hashchange',syncPage) }, [])
  useEffect(() => { if(!user)return;if(window.location.hash==='#communities')setCommunitiesOpen(true);if(window.location.hash==='#messages')setMessagesOpen(true) }, [user])
  useEffect(() => { if (showAbout) window.scrollTo({ top:0, behavior:'auto' }) }, [showAbout])
  useEffect(() => { api('/users/suggestions?size=4').then(setSuggestedUsers).catch(() => {}) }, [user])
  useEffect(() => {
    if (!searchQuery.trim()) { setSearchResults([]); return }
    const timer = window.setTimeout(() => {
      api(`/users/search?q=${encodeURIComponent(searchQuery.trim())}&size=8`).then((data) => setSearchResults(data.content)).catch(() => setSearchResults([]))
    }, 250)
    return () => window.clearTimeout(timer)
  }, [searchQuery])
  useEffect(() => {
    if (!user) { setNotifications([]); setUnreadNotifications(0); return }
    const load = () => { api('/notifications').then(setNotifications).catch(()=>{}); api('/notifications/unread-count').then((data)=>setUnreadNotifications(data.unreadCount)).catch(()=>{}) }
    load(); const timer=window.setInterval(load,15000); return()=>window.clearInterval(timer)
  }, [user])
  useEffect(() => {
    if (!user) { setUnreadMessages(0); return }
    api('/conversations').then(updateUnreadMessages).catch(() => {})
  }, [user, updateUnreadMessages])
  useEffect(() => {
    if (!user || !['message', 'read_receipt', 'message_deleted'].includes(realtime.lastEvent?.type)) return
    api('/conversations').then(updateUnreadMessages).catch(() => {})
  }, [realtime.lastEvent, user, updateUnreadMessages])
  useEffect(() => () => {
    if (imagePreview) URL.revokeObjectURL(imagePreview)
  }, [imagePreview])

  const chooseImage = (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/gif'].includes(file.type)) {
      setNotice('Choose a JPEG, PNG, or GIF image.')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      setNotice('Images must be 10 MB or smaller.')
      return
    }
    setNotice('')
    setImageFile(file)
    setImagePreview(URL.createObjectURL(file))
  }

  const removeImage = () => {
    setImageFile(null)
    setImagePreview(null)
  }

  const createPost = async (event) => {
    event.preventDefault(); if (!user) { setAuthMode('login'); return } setPublishing(true); setNotice('')
    try {
      let imageUrl = null
      if (imageFile) {
        const imageForm = new FormData()
        imageForm.append('image', imageFile)
        const uploaded = await api('/media/images', { method: 'POST', body: imageForm })
        imageUrl = uploaded.url
      }
      const post = await api('/posts', { method: 'POST', body: JSON.stringify({ body: composer.body, imageUrl }) })
      setPosts([post, ...posts])
      setComposer({ body: '' })
      removeImage()
    } catch (err) { setNotice(err.message) } finally { setPublishing(false) }
  }
  const followSuggested = async (person) => {
    if (!user) { setAuthMode('login'); return }
    try {
      const updated = await api(`/users/${encodeURIComponent(person.username)}/follow`, { method: 'POST' })
      setSuggestedUsers((current) => current.filter((item) => item.id !== updated.id))
      setSearchResults((current) => current.map((item) => item.id === updated.id ? updated : item))
    } catch (err) { setNotice(err.message) }
  }
  const openMessages = (username = null) => {
    if (!user) { setAuthMode('login'); return }
    setProfileUsername(null); setMessageTarget(username); setMessagesOpen(true)
  }
  const openConnections = () => {
    if (!user) { setAuthMode('login'); return }
    setProfileUsername(null); setConnectionsOpen(true)
  }
  const openCommunities = () => {
    if (!user) { setAuthMode('login'); return }
    setProfileUsername(null); setCommunitiesOpen(true)
  }
  const saveArticle = (saved) => {
    setArticles((current) => current.some((article) => article.id === saved.id) ? current.map((article) => article.id === saved.id ? saved : article) : [saved, ...current])
    setArticleEditor(null); setFeedMode('articles'); setFeedState('ready')
    api('/topics/trending?limit=4').then(setTrendingTopics).catch(() => {})
  }
  const deleteArticle = async (id) => {
    if (!window.confirm('Delete this article?')) return
    try { await api(`/articles/${id}`, { method:'DELETE' }); setArticles((current) => current.filter((article) => article.id !== id)); api('/topics/trending?limit=4').then(setTrendingTopics).catch(() => {}) }
    catch (err) { setNotice(err.message) }
  }
  const refreshMoments = () => api('/moments').then(setMoments).catch(() => {})
  const momentGroups = Object.values(moments.reduce((groups, moment) => {
    const key = moment.author.username
    if (!groups[key]) groups[key] = { author: moment.author, moments: [] }
    groups[key].moments.push(moment)
    return groups
  }, {}))
  const openMomentGroup = (group) => {
    const firstUnseen = group.moments.find((moment) => !moment.viewedByCurrentUser) || group.moments[0]
    setMomentIndex(moments.findIndex((moment) => moment.id === firstUnseen.id))
  }
  const loadMarketplace = async (view = marketView, filters = marketFilters) => {
    if (view !== 'all' && !user) { setAuthMode('login'); return }
    setFeedState('loading')
    try {
      let data
      if (view === 'mine') data = await api('/marketplace/mine')
      else if (view === 'saved') data = await api('/marketplace/saved')
      else { const params=new URLSearchParams();if(filters.q)params.set('q',filters.q);if(filters.category)params.set('category',filters.category);if(filters.minPrice)params.set('minPrice',filters.minPrice);if(filters.maxPrice)params.set('maxPrice',filters.maxPrice);params.set('sort',filters.sort);data=(await api(`/marketplace/listings?${params}`)).content }
      setListings(data);setMarketView(view);setFeedMode('marketplace');setFeedState('ready')
    } catch { setFeedState('offline') }
  }
  const updateListing = (updated) => { setListings((current)=>current.map((listing)=>listing.id===updated.id?updated:listing));setMarketDetail((current)=>current?.id===updated.id?updated:current) }
  const saveListing = (saved) => { setListings((current)=>current.some((listing)=>listing.id===saved.id)?current.map((listing)=>listing.id===saved.id?saved:listing):[saved,...current]);setMarketEditor(null);setMarketDetail(saved);setFeedMode('marketplace');setFeedState('ready') }
  const deleteListing = (id) => setListings((current)=>current.filter((listing)=>listing.id!==id))
  const readNotification = async (notification) => { if (!notification.read) { try { await api(`/notifications/${notification.id}/read`,{method:'POST'});setNotifications((current)=>current.map((item)=>item.id===notification.id?{...item,read:true}:item));setUnreadNotifications((count)=>Math.max(0,count-1)) } catch {} } if(notification.actor?.username){setNotificationsOpen(false);setProfileUsername(notification.actor.username)} }
  const readAllNotifications = async () => { try { await api('/notifications/read-all',{method:'POST'});setNotifications((current)=>current.map((item)=>({...item,read:true})));setUnreadNotifications(0) } catch {} }
  const openGarageEditor = (vehicle) => { setProfileUsername(null); setGarageEditor(vehicle || 'new') }
  const saveGarageVehicle = () => { setGarageEditor(null); if(user)setProfileUsername(user.username) }
  const logout = () => { sessionStorage.removeItem(TOKEN_KEY); setUser(null); setFeedMode('all'); loadFeed('all'); window.setTimeout(refreshMoments, 0) }
  return <div className="app-shell">
    <header className="topbar"><a className="brand" href="#top" onClick={()=>setShowAbout(false)}><span className="brand-mark">CL</span><span>Carta<span>Laap</span></span></a><div className="search-wrap"><div className="search"><Icon name="search" /><input aria-label="Search users" value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Search people by name or username…" /></div>{searchResults.length > 0 && <div className="search-results">{searchResults.map((person) => <button key={person.id} onClick={() => { setProfileUsername(person.username); setSearchQuery(''); setSearchResults([]) }}><Avatar user={person} /><span><strong>{person.displayName}</strong><small>@{person.username}</small></span></button>)}</div>}</div><div className="top-actions"><div className="notification-wrap"><button className="icon-button hide-mobile" onClick={()=>user?setNotificationsOpen(!notificationsOpen):setAuthMode('login')}><Icon name="bell" />{unreadNotifications>0&&<b>{unreadNotifications>99?'99+':unreadNotifications}</b>}</button>{user&&notificationsOpen&&<div className="notification-panel"><div><h3>Notifications</h3><button onClick={readAllNotifications}>Mark all read</button></div>{notifications.length===0&&<p>No notifications yet.</p>}{notifications.map((notification)=><button className={notification.read?'':'unread'} key={notification.id} onClick={()=>readNotification(notification)}><Avatar user={notification.actor}/><span><strong>{notification.message}</strong><small>{new Date(notification.createdAt).toLocaleString()}</small></span>{!notification.read&&<i/>}</button>)}</div>}</div>{user ? <div className="user-menu"><button className="profile-menu-button" onClick={() => setProfileUsername(user.username)}><Avatar user={user} /><span>@{user.username}</span></button><button className="text-button" onClick={logout}>Log out</button></div> : <><button className="text-button" onClick={() => setAuthMode('login')}>Log in</button><button className="primary-button" onClick={() => setAuthMode('register')}>Join the crew</button></>}</div></header>
    {showAbout ? <AboutPage currentUser={user} onExplore={()=>{window.location.hash='top';setShowAbout(false);loadFeed('all')}} onJoin={()=>setAuthMode('register')}/> : <div className="layout" id="top">
      <aside className="left-sidebar"><nav><a className={feedMode !== 'articles' && feedMode !== 'marketplace' ? 'active' : ''} href="#top" onClick={() => loadFeed('all')}><Icon name="home" />Home</a><a className={feedMode === 'articles' ? 'active' : ''} href="#articles" onClick={() => loadFeed('articles')}><Icon name="compass" />Articles</a><a className={feedMode === 'marketplace' ? 'active' : ''} href="#marketplace" onClick={() => loadMarketplace('all')}><Icon name="market" />Marketplace</a><a href="#connections" onClick={(event) => { event.preventDefault(); openConnections() }}><Icon name="users" />Connections</a><a href="#communities" onClick={(event) => { event.preventDefault(); openCommunities() }}><Icon name="hash" />Communities</a><a href="#messages" onClick={(event) => { event.preventDefault(); openMessages() }}><Icon name="message" />Messages{unreadMessages > 0 && <b className="message-nav-badge">{unreadMessages > 99 ? '99+' : unreadMessages}</b>}</a></nav><div className="garage-card"><div className="garage-icon"><Icon name="wrench" /></div><h3>Build your garage</h3><p>Add your rides and share every stage of the journey.</p><button onClick={() => {if(!user){setAuthMode('register');return}setProfileGarageFocus(true);setProfileUsername(user.username)}}>Add a vehicle <Icon name="chevron" size={16} /></button></div></aside>
      <main className="feed"><section className="feed-intro"><div><p className="eyebrow">{feedMode === 'marketplace' ? 'BUY · SELL · BUILD' : feedMode === 'articles' ? 'THE OPEN ROAD JOURNAL' : 'YOUR DAILY DRIVE'}</p><h1>{feedMode === 'marketplace' ? 'Find your next machine' : feedMode === 'articles' ? 'Stories worth the long read' : 'What’s happening?'}</h1></div><p>{feedMode === 'marketplace' ? 'Vehicles, parts, and accessories from the community.' : feedMode === 'articles' ? 'Guides, reviews, journeys, and build diaries.' : 'Stories, builds, and advice from the community.'}</p></section>
        {feedMode !== 'articles' && feedMode !== 'marketplace' && <><section className="moments" aria-label="Moments"><button className="moment add-moment" onClick={() => user ? setMomentUploadOpen(true) : setAuthMode('login')}><span><Icon name="plus" /></span><small>Add Moment</small></button>{momentGroups.map((group) => { const allSeen = group.moments.every((moment) => moment.viewedByCurrentUser); return <button className={`moment live-moment ${allSeen ? 'seen' : ''}`} key={group.author.username} onClick={() => openMomentGroup(group)}><span><Avatar user={group.author} /></span><small>{group.author.username === user?.username ? 'Your Moment' : group.author.displayName.split(' ')[0]}</small>{group.moments.length > 1 && <b>{group.moments.length}</b>}</button> })}</section>
        <form className="composer" onSubmit={createPost}><Avatar user={user} /><div className="composer-main"><textarea value={composer.body} onChange={(event) => setComposer({ ...composer, body: event.target.value })} required maxLength="5000" placeholder={user ? `Share something, ${user.displayName.split(' ')[0]}…` : 'Share your latest drive, build, or question…'} /><input ref={fileInputRef} className="file-input" type="file" accept="image/jpeg,image/png,image/gif" onChange={chooseImage} />{imagePreview && <div className="composer-image-preview"><img src={imagePreview} alt="Selected upload preview" /><button type="button" onClick={removeImage} aria-label="Remove selected image"><Icon name="x" size={16} /></button><span>{imageFile?.name}</span></div>}<div className="composer-actions"><button type="button" className="attach" onClick={() => user ? fileInputRef.current?.click() : setAuthMode('login')}><Icon name="image" />{imageFile ? 'Change photo' : 'Photo'}</button><button className="post-button" disabled={publishing || !composer.body.trim()}>{publishing ? 'Uploading…' : 'Post'} <Icon name="send" size={17} /></button></div>{notice && <p className="form-error">{notice}</p>}</div></form></>}
        {feedMode === 'marketplace' ? <><div className="market-view-tabs"><button className={marketView==='all'?'active':''} onClick={()=>loadMarketplace('all')}>Browse</button><button className={marketView==='mine'?'active':''} onClick={()=>loadMarketplace('mine')}>My listings</button><button className={marketView==='saved'?'active':''} onClick={()=>loadMarketplace('saved')}>Saved</button><button className="sell-button" onClick={()=>user?setMarketEditor('new'):setAuthMode('login')}><Icon name="plus" size={15}/> Sell</button></div><form className="market-filters" onSubmit={(event)=>{event.preventDefault();loadMarketplace('all',marketFilters)}}><div><Icon name="search" size={16}/><input value={marketFilters.q} onChange={(event)=>setMarketFilters({...marketFilters,q:event.target.value})} placeholder="Search vehicles, parts, brands…"/></div><select value={marketFilters.category} onChange={(event)=>setMarketFilters({...marketFilters,category:event.target.value})}><option value="">All categories</option>{marketplaceCategories.map((category)=><option key={category}>{category}</option>)}</select><input type="number" min="0" value={marketFilters.minPrice} onChange={(event)=>setMarketFilters({...marketFilters,minPrice:event.target.value})} placeholder="Min ₹"/><input type="number" min="0" value={marketFilters.maxPrice} onChange={(event)=>setMarketFilters({...marketFilters,maxPrice:event.target.value})} placeholder="Max ₹"/><select value={marketFilters.sort} onChange={(event)=>setMarketFilters({...marketFilters,sort:event.target.value})}><option value="newest">Newest</option><option value="price_asc">Price: low to high</option><option value="price_desc">Price: high to low</option></select><button>Apply</button></form></> : <div className="feed-filter"><strong>{feedMode === 'articles' ? 'Community articles' : feedMode === 'following' ? 'From your crew' : 'Latest from the community'}</strong><div className="feed-tabs"><button className={feedMode === 'all' ? 'active' : ''} onClick={() => loadFeed('all')}>All</button><button className={feedMode === 'following' ? 'active' : ''} onClick={() => loadFeed('following')}>Following</button><button className={feedMode === 'articles' ? 'active' : ''} onClick={() => loadFeed('articles')}>Articles</button></div>{feedMode === 'articles' && <button className="write-article-button" onClick={() => user ? setArticleEditor('new') : setAuthMode('login')}><Icon name="plus" size={16} /> Write</button>}</div>}
        {feedState === 'loading' && <div className="state-card"><span className="spinner" />Loading the feed…</div>}{feedState === 'offline' && <div className="state-card"><strong>The garage door is closed.</strong><span>Start the Spring Boot server to connect the live feed.</span></div>}
        {feedMode === 'marketplace' ? <>{feedState==='ready'&&listings.length===0&&<div className="empty-feed"><div className="empty-emblem"><Icon name="market" size={28}/></div><h2>No listings found</h2><p>Change the filters or publish the first listing in this category.</p></div>}<div className="market-grid">{listings.map((listing)=><MarketplaceCard key={listing.id} listing={listing} currentUser={user} onOpen={setMarketDetail} onAuth={()=>setAuthMode('login')} onUpdated={updateListing}/>)}</div></> : feedMode === 'articles' ? <>{feedState === 'ready' && articles.length === 0 && <div className="empty-feed"><div className="empty-emblem"><Icon name="wrench" size={28} /></div><h2>Start the CartaLaap journal</h2><p>Publish a guide, review, road story, or detailed build diary.</p></div>}{articles.map((article) => <ArticleCard key={article.id} article={article} onOpenProfile={setProfileUsername} onEdit={setArticleEditor} onDelete={deleteArticle} />)}</> : <>{feedState === 'ready' && posts.length === 0 && <div className="empty-feed"><div className="empty-emblem"><Icon name="wrench" size={28} /></div><h2>{feedMode === 'following' ? 'Your crew feed is ready' : 'Be the first one in the garage'}</h2><p>{feedMode === 'following' ? 'Follow other enthusiasts to see their latest posts here.' : 'Share a project, ask a question, or tell the community about your favorite drive.'}</p><button className="primary-button" onClick={() => !user && setAuthMode('register')}>{user ? 'Write your first post above' : 'Join and post'}</button></div>}{posts.map((post) => <PostCard post={post} currentUser={user} onAuthRequired={() => setAuthMode('login')} onOpenProfile={setProfileUsername} onUpdated={(updated) => setPosts((current) => current.map((item) => item.id === updated.id ? updated : item))} onDeleted={(postId) => setPosts((current) => current.filter((item) => item.id !== postId))} key={post.id} />)}</>}
      </main>
      <aside className="right-sidebar"><section className="side-panel"><div className="panel-title"><div><p className="eyebrow">TRENDING ARTICLES</p><h2>In the fast lane</h2></div><span className="live-dot" /></div>{trendingTopics.map((topic, index) => <a className="trend" href={`#topic-${topic.slug}`} key={topic.slug} onClick={(event) => { event.preventDefault(); setTopicModal(topic.slug) }}><span>{String(index + 1).padStart(2, '0')}</span><div><strong>{topic.name}</strong><small>{topic.articles} {topic.articles === 1 ? 'article' : 'articles'}</small></div><Icon name="chevron" size={16} /></a>)}{trendingTopics.length === 0 && <p className="topics-panel-empty">No trending article topics yet. Publish an article to start one.</p>}<button className="panel-link" onClick={() => setTopicModal('all')}>Explore all topics <Icon name="chevron" size={15} /></button></section><section className="side-panel members-panel"><p className="eyebrow">FIND YOUR PEOPLE</p><h2>Suggested crew</h2>{suggestedUsers.slice(0, 3).map((person) => <div className="member" key={person.id}><button className="avatar-button" onClick={() => setProfileUsername(person.username)}><Avatar user={person} /></button><button className="member-name" onClick={() => setProfileUsername(person.username)}><strong>{person.displayName}</strong><small>@{person.username}</small></button><button onClick={() => followSuggested(person)}>Follow</button></div>)}{suggestedUsers.length === 0 && <p className="suggestions-empty">You’re caught up—search for more enthusiasts anytime.</p>}</section><RoadThoughtCard /></aside>
    </div>}
    <footer className="site-footer"><div className="footer-inner"><div className="footer-brand"><span className="brand-mark">CL</span><div><strong>Carta<span>Laap</span></strong><p>A community built for the road.</p></div></div><nav><a href="#top" onClick={()=>setShowAbout(false)}>Home</a><a href="#about" onClick={()=>setShowAbout(true)}>About</a></nav><p className="footer-copyright">© 2026 CartaLaap. Built for the road.</p></div></footer>
    {!showAbout&&<nav className="mobile-nav"><a className="active" href="#top" onClick={() => loadFeed('all')}><Icon name="home" /></a><a href="#articles" onClick={() => loadFeed('articles')}><Icon name="compass" /></a><button onClick={() => user ? (feedMode === 'marketplace' ? setMarketEditor('new') : feedMode === 'articles' ? setArticleEditor('new') : document.querySelector('.composer textarea')?.focus()) : setAuthMode('register')}><Icon name="plus" /></button><a href="#communities" onClick={(event)=>{event.preventDefault();openCommunities()}}><Icon name="hash"/></a><a href="#marketplace" onClick={()=>loadMarketplace('all')}><Icon name="market" /></a><a className="mobile-message-link" href="#messages" onClick={(event) => { event.preventDefault(); openMessages() }}><Icon name="message" />{unreadMessages > 0 && <b>{unreadMessages > 99 ? '99+' : unreadMessages}</b>}</a></nav>}
    {authMode && <AuthModal initialMode={authMode} onClose={() => setAuthMode(null)} onAuthenticated={(nextUser) => { setUser(nextUser); setAuthMode(null); loadFeed(feedMode); window.setTimeout(refreshMoments, 0) }} />}
    {profileUsername && <ProfileModal username={profileUsername} currentUser={user} focusGarage={profileGarageFocus} onClose={() => {setProfileUsername(null);setProfileGarageFocus(false)}} onAuthRequired={() => { setProfileUsername(null); setProfileGarageFocus(false); setAuthMode('login') }} onCurrentUserUpdated={setUser} onMessage={openMessages} onGarageEdit={openGarageEditor} />}
    {messagesOpen && <MessagingModal currentUser={user} initialUsername={messageTarget} realtime={realtime} onInboxChanged={updateUnreadMessages} onClose={() => { setMessagesOpen(false); setMessageTarget(null) }} onOpenProfile={(username) => { setMessagesOpen(false); setProfileUsername(username) }} />}
    {communitiesOpen && <CommunitiesModal currentUser={user} realtime={realtime} onClose={()=>setCommunitiesOpen(false)} onOpenProfile={(username)=>{setCommunitiesOpen(false);setProfileUsername(username)}}/>}
    {articleEditor && <ArticleEditor article={articleEditor === 'new' ? null : articleEditor} onClose={() => setArticleEditor(null)} onSaved={saveArticle} />}
    {momentUploadOpen && <MomentUploadModal onClose={() => setMomentUploadOpen(false)} onCreated={(created) => { setMoments((current) => [created, ...current]); setMomentUploadOpen(false); setMomentIndex(0) }} />}
    {momentIndex !== null && <MomentViewer moments={moments} initialIndex={momentIndex} currentUser={user} onClose={() => setMomentIndex(null)} onUpdated={(updated) => setMoments((current) => current.map((moment) => moment.id === updated.id ? updated : moment))} onDeleted={(id) => setMoments((current) => current.filter((moment) => moment.id !== id))} />}
    {marketEditor && <MarketplaceEditor listing={marketEditor==='new'?null:marketEditor} onClose={()=>setMarketEditor(null)} onSaved={saveListing}/>} 
    {marketDetail && <MarketplaceDetail listing={marketDetail} currentUser={user} onClose={()=>setMarketDetail(null)} onUpdated={updateListing} onDeleted={deleteListing} onEdit={(listing)=>{setMarketDetail(null);setMarketEditor(listing)}} onMessage={(username)=>{setMarketDetail(null);openMessages(username)}} onAuth={()=>{setMarketDetail(null);setAuthMode('login')}}/>}
    {garageEditor && <GarageEditor vehicle={garageEditor==='new'?null:garageEditor} onClose={()=>setGarageEditor(null)} onSaved={saveGarageVehicle}/>} 
    {connectionsOpen && <ConnectionsModal currentUser={user} onClose={() => setConnectionsOpen(false)} onOpenProfile={(username) => { setConnectionsOpen(false); setProfileUsername(username) }} />}
    {topicModal && <TopicsModal initialSlug={topicModal === 'all' ? null : topicModal} onClose={() => setTopicModal(false)} onOpenProfile={(username) => { setTopicModal(false); setProfileUsername(username) }} onEditArticle={setArticleEditor} onTopicsChanged={() => api('/topics/trending?limit=4').then(setTrendingTopics).catch(() => {})} />}
  </div>
}

export default App
