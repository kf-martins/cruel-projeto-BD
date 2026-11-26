import { AlertTriangle, CheckCircle2, Info, Loader2, XCircle } from 'lucide-react';

/** Componentes visuais reutilizáveis (cards, avisos, selos de status, estados vazios). */

export function PageHeader({ titulo, subtitulo, acoes }) {
  return (
    <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="font-display text-2xl font-bold text-ink sm:text-3xl">{titulo}</h1>
        {subtitulo && <p className="mt-1 text-sm text-ink-soft">{subtitulo}</p>}
      </div>
      {acoes && <div className="flex flex-wrap gap-2">{acoes}</div>}
    </div>
  );
}

export function Card({ titulo, descricao, acoes, children, className = '' }) {
  return (
    <section className={`card p-5 ${className}`}>
      {(titulo || acoes) && (
        <div className="mb-4 flex flex-wrap items-start justify-between gap-2">
          <div>
            {titulo && <h2 className="text-base font-semibold text-ink">{titulo}</h2>}
            {descricao && <p className="mt-0.5 text-xs text-ink-muted">{descricao}</p>}
          </div>
          {acoes}
        </div>
      )}
      {children}
    </section>
  );
}

const ESTILOS_ALERTA = {
  erro: { icone: XCircle, classe: 'bg-bad-soft text-bad' },
  sucesso: { icone: CheckCircle2, classe: 'bg-good-soft text-good' },
  aviso: { icone: AlertTriangle, classe: 'bg-warn-soft text-warn' },
  info: { icone: Info, classe: 'bg-accent-soft text-ink' },
};

export function Alerta({ tipo = 'info', children, onFechar }) {
  if (!children) return null;
  const { icone: Icone, classe } = ESTILOS_ALERTA[tipo];
  return (
    <div role={tipo === 'erro' ? 'alert' : 'status'} className={`flex items-start gap-2 rounded-xl px-4 py-3 text-sm ${classe}`}>
      <Icone className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <div className="flex-1">{children}</div>
      {onFechar && (
        <button type="button" onClick={onFechar} className="text-xs font-semibold underline-offset-2 hover:underline">
          fechar
        </button>
      )}
    </div>
  );
}

export function Carregando({ texto = 'Carregando…' }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-sm text-ink-muted">
      <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> {texto}
    </div>
  );
}

export function Vazio({ icone: Icone = Info, titulo, children }) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 py-10 text-center">
      <Icone className="h-8 w-8 text-ink-muted" aria-hidden />
      <p className="text-sm font-semibold text-ink">{titulo}</p>
      {children && <p className="max-w-sm text-xs text-ink-muted">{children}</p>}
    </div>
  );
}

const SITUACOES = {
  ABERTA: { classe: 'bg-good-soft text-good', icone: CheckCircle2, texto: 'Aberta' },
  AGENDADA: { classe: 'bg-warn-soft text-warn', icone: AlertTriangle, texto: 'Agendada' },
  ENCERRADA: { classe: 'bg-secondary text-ink-soft', icone: XCircle, texto: 'Encerrada' },
  INATIVA: { classe: 'bg-bad-soft text-bad', icone: XCircle, texto: 'Inativa' },
};

/** Selo de situação: cor de status sempre acompanhada de ícone + texto. */
export function SeloSituacao({ situacao }) {
  const s = SITUACOES[situacao] ?? SITUACOES.ENCERRADA;
  const Icone = s.icone;
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${s.classe}`}>
      <Icone className="h-3 w-3" aria-hidden /> {s.texto}
    </span>
  );
}

export function Etiqueta({ children, className = '' }) {
  return (
    <span className={`inline-flex items-center rounded-full bg-secondary px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-ink-soft ${className}`}>
      {children}
    </span>
  );
}

export const ROTULO_TIPO = {
  MULTIPLA_ESCOLHA: 'Múltipla escolha',
  ESCALA_1_5: 'Escala 1–5',
  TEXTO_LIVRE: 'Texto livre',
};

export const ROTULO_CATEGORIA = {
  DIDATICA: 'Didática',
  CONTEUDO: 'Conteúdo',
  INFRAESTRUTURA: 'Infraestrutura',
  GERAL: 'Geral',
};

export function formatarData(iso) {
  if (!iso) return '—';
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}

export function formatarHora(hora) {
  return hora ? hora.slice(0, 5) : '—';
}

export function formatarNumero(n, casas = 2) {
  if (n === null || n === undefined) return '—';
  return Number(n).toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas });
}
