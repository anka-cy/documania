/*
  Client Server-Sent Events (portage Angular de core/api/sse.js, D-039/D-042).
  fetch + ReadableStream au lieu d'EventSource : EventSource NE PEUT PAS
  envoyer d'en-tête Authorization, or l'API est stateless Bearer pur.

  Contrat : openEventStream(path, onEvent, options) -> { close, isOpen }.
  - reconnexion auto avec backoff exponentiel (max 15 s) tant que
    shouldContinue() renvoie true ;
  - 401 -> refresh silencieux puis reconnexion immédiate ; échec -> stop net ;
  - 403 -> stop net (autorisé une seule fois à l'ouverture) ;
  - commentaires de heartbeat (":ping") ignorés silencieusement.
*/

import { apiUrl } from '../config';
import { getAccessToken, isAuthenticated, refresh } from '../auth/auth';

const MAX_BACKOFF_MS = 15000;

export interface EventStream {
  close(): void;
  isOpen(): boolean;
}

export function openEventStream(
  path: string,
  onEvent: (event: string, data: object | string | null) => void,
  options: { shouldContinue?: () => boolean } = {},
): EventStream {
  const shouldContinue = options.shouldContinue || (() => true);
  let closed = false;
  let attempt = 0;
  let abortController: AbortController | null = null;
  let timer: ReturnType<typeof setTimeout> | null = null;

  function close(): void {
    closed = true;
    if (timer) clearTimeout(timer);
    if (abortController) abortController.abort();
  }

  function schedule(delayMs?: number): void {
    if (closed) return;
    const delay = delayMs === 0 ? 0 : Math.min(1000 * (2 ** attempt), MAX_BACKOFF_MS);
    attempt += 1;
    timer = setTimeout(connect, delay);
  }

  async function connect(): Promise<void> {
    if (closed || !shouldContinue()) {
      close();
      return;
    }
    abortController = new AbortController();
    const headers: Record<string, string> = { Accept: 'text/event-stream' };
    const token = getAccessToken();
    if (token) headers.Authorization = `Bearer ${token}`;

    let response: Response;
    try {
      response = await fetch(apiUrl(path), { headers, signal: abortController.signal });
    } catch (networkError) {
      if (!closed) schedule();
      return;
    }

    if (response.status === 401) {
      // Jeton expiré : un refresh silencieux puis reconnexion immédiate.
      if (!isAuthenticated()) {
        close();
        return;
      }
      try {
        await refresh();
        schedule(0);
      } catch {
        close(); // session expirée : le routeur redirige déjà vers le login
      }
      return;
    }
    if (response.status === 403) {
      close();
      return;
    }
    if (!response.ok || !response.body) {
      schedule();
      return;
    }

    attempt = 0;
    await readFrames(response.body);
    if (!closed && shouldContinue()) schedule(0);
    else close();
  }

  async function readFrames(body: ReadableStream<Uint8Array>): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    for (;;) {
      let chunk: ReadableStreamReadResult<Uint8Array>;
      try {
        chunk = await reader.read();
      } catch {
        return; // flux interrompu (close/abort/réseau)
      }
      if (chunk.done) return;
      buffer += decoder.decode(chunk.value, { stream: true });
      let boundary = buffer.indexOf('\n\n');
      while (boundary !== -1) {
        const frame = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);
        handleFrame(frame);
        boundary = buffer.indexOf('\n\n');
      }
      if (!shouldContinue()) {
        try {
          await reader.cancel();
        } catch {
          // ignoré : la lecture est déjà finie
        }
        closed = true;
        return;
      }
    }
  }

  function handleFrame(frame: string): void {
    let eventName = 'message';
    const dataLines: string[] = [];
    for (const rawLine of frame.split('\n')) {
      const line = rawLine.replace(/\r$/, '');
      if (line.startsWith(':')) continue; // commentaire heartbeat
      if (line.startsWith('event:')) eventName = line.slice(6).trim();
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart());
    }
    if (!dataLines.length) return;
    const raw = dataLines.join('\n');
    let data: object | string;
    try {
      data = JSON.parse(raw);
    } catch {
      data = raw;
    }
    onEvent(eventName, data);
  }

  connect();
  return { close, isOpen: () => !closed };
}
