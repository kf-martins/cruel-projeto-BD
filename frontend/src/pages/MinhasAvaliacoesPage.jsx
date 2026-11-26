import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarDays, CheckCircle2, ClipboardList, Lock, PenLine, ShieldCheck } from 'lucide-react';
import { api } from '../services/api';
import { Alerta, Carregando, PageHeader, SeloSituacao, Vazio, formatarData } from '../components/ui';

/** RF5 — avaliações das ofertas em que o aluno está matriculado; bloqueia as já respondidas. */
export default function MinhasAvaliacoesPage() {
  const [avaliacoes, setAvaliacoes] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    api.minhasAvaliacoes().then(setAvaliacoes).catch((e) => setErro(e.message));
  }, []);

  // Pendentes abertas primeiro, depois as demais.
  const ordenadas = (avaliacoes ?? []).slice().sort((a, b) => {
    const peso = (x) => (!x.respondida && x.situacao === 'ABERTA' ? 0 : 1);
    return peso(a) - peso(b);
  });

  return (
    <>
      <PageHeader titulo="Minhas avaliações" subtitulo="Avalie cada disciplina uma única vez por semestre." />

      <div className="mb-6 flex items-start gap-3 rounded-2xl border border-line bg-accent-soft p-4">
        <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-accent" aria-hidden />
        <p className="text-sm text-ink-soft">
          <strong className="text-ink">Anonimato garantido.</strong> O sistema registra apenas <em>que</em> você respondeu
          (para impedir uma segunda resposta). Suas respostas são gravadas em outra tabela, sem qualquer vínculo com seu usuário.
        </p>
      </div>

      <Alerta tipo="erro">{erro}</Alerta>
      {!avaliacoes && !erro && <Carregando />}
      {avaliacoes && avaliacoes.length === 0 && (
        <Vazio icone={ClipboardList} titulo="Nenhuma avaliação disponível">
          Você ainda não está matriculado(a) em nenhuma oferta com avaliação ativa.
        </Vazio>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {ordenadas.map((a) => {
          const podeResponder = !a.respondida && a.situacao === 'ABERTA';
          return (
            <article key={a.idAvaliacao} className={`card flex flex-col gap-4 p-5 ${podeResponder ? '' : 'opacity-80'}`}>
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-ink-muted">{a.codigoDisciplina} · {a.ano}/{a.semestre}</p>
                  <h2 className="mt-0.5 text-lg font-semibold text-ink">{a.nomeDisciplina}</h2>
                  <p className="text-sm text-ink-soft">{a.nomeDocente}</p>
                </div>
                {a.respondida ? (
                  <span className="inline-flex items-center gap-1 rounded-full bg-good-soft px-2.5 py-0.5 text-xs font-semibold text-good">
                    <CheckCircle2 className="h-3 w-3" aria-hidden /> Respondida
                  </span>
                ) : (
                  <SeloSituacao situacao={a.situacao} />
                )}
              </div>
              <div className="flex flex-wrap gap-4 text-xs text-ink-muted">
                <span className="inline-flex items-center gap-1"><CalendarDays className="h-3.5 w-3.5" aria-hidden /> {formatarData(a.dataAvaliacao)} – {formatarData(a.dataEncerramento)}</span>
                <span className="inline-flex items-center gap-1"><ClipboardList className="h-3.5 w-3.5" aria-hidden /> {a.totalQuestoes} questões</span>
              </div>
              {podeResponder ? (
                <Link to={`/responder/${a.idAvaliacao}`} className="btn-primary self-start">
                  <PenLine className="h-4 w-4" /> Responder agora
                </Link>
              ) : (
                <p className="inline-flex items-center gap-2 text-sm text-ink-muted">
                  <Lock className="h-4 w-4" aria-hidden />
                  {a.respondida ? 'Você já respondeu esta avaliação — não é possível responder novamente.' : 'Fora do período de respostas.'}
                </p>
              )}
            </article>
          );
        })}
      </div>
    </>
  );
}
