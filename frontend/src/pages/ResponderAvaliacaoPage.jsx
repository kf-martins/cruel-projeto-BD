import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Loader2, Lock, Send, ShieldCheck } from 'lucide-react';
import { api } from '../services/api';
import { Alerta, Carregando, Etiqueta, PageHeader, ROTULO_CATEGORIA } from '../components/ui';

const ROTULOS_ESCALA = ['Discordo totalmente', 'Discordo', 'Neutro', 'Concordo', 'Concordo totalmente'];

function respondida(questao, valor) {
  if (questao.tipo === 'TEXTO_LIVRE') return Boolean(valor && valor.trim());
  return valor !== undefined && valor !== null;
}

/** RF5/RF6 — questionário com inputs por tipo, selo de anonimato e bloqueio de reenvio. */
export default function ResponderAvaliacaoPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [avaliacao, setAvaliacao] = useState(null);
  const [jaRespondida, setJaRespondida] = useState(false);
  const [respostas, setRespostas] = useState({});
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [concluida, setConcluida] = useState(false);
  const [tentouEnviar, setTentouEnviar] = useState(false);

  useEffect(() => {
    Promise.all([api.avaliacao(id), api.minhasAvaliacoes()])
      .then(([av, minhas]) => {
        setAvaliacao(av);
        setJaRespondida(Boolean(minhas.find((m) => String(m.idAvaliacao) === String(id))?.respondida));
      })
      .catch((e) => setErro(e.message));
  }, [id]);

  const itens = avaliacao?.itens ?? [];
  const obrigatoriasPendentes = useMemo(
    () => itens.filter((it) => it.obrigatoria && !respondida(it.questao, respostas[it.questao.idQuestao])),
    [itens, respostas],
  );
  const totalRespondidas = itens.filter((it) => respondida(it.questao, respostas[it.questao.idQuestao])).length;
  const progresso = itens.length ? Math.round((100 * totalRespondidas) / itens.length) : 0;

  const definir = (idQuestao, valor) => setRespostas((r) => ({ ...r, [idQuestao]: valor }));

  const enviar = async (e) => {
    e.preventDefault();
    setTentouEnviar(true);
    if (obrigatoriasPendentes.length > 0) {
      setErro(`Responda as questões obrigatórias: ${obrigatoriasPendentes.map((it) => it.ordem).join(', ')}.`);
      return;
    }
    setErro('');
    setEnviando(true);
    const payload = itens
      .filter((it) => respondida(it.questao, respostas[it.questao.idQuestao]))
      .map(({ questao: q }) => {
        const v = respostas[q.idQuestao];
        if (q.tipo === 'ESCALA_1_5') return { idQuestao: q.idQuestao, notaEscala: v };
        if (q.tipo === 'MULTIPLA_ESCOLHA') return { idQuestao: q.idQuestao, idAlternativa: v };
        return { idQuestao: q.idQuestao, textoResposta: v.trim() };
      });
    try {
      await api.responder(Number(id), payload);
      setConcluida(true);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (err) {
      if (err.status === 409) setJaRespondida(true);
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  };

  if (concluida) {
    return (
      <div className="card mx-auto flex max-w-xl flex-col items-center gap-4 p-8 text-center">
        <CheckCircle2 className="h-12 w-12 text-good" aria-hidden />
        <h1 className="font-display text-2xl font-bold text-ink">Obrigado por avaliar!</h1>
        <p className="text-sm text-ink-soft">
          Suas respostas foram registradas <strong className="text-ink">de forma anônima</strong>. Nenhum identificador
          da sua submissão foi devolvido ou armazenado junto ao seu usuário.
        </p>
        <button type="button" className="btn-primary" onClick={() => navigate('/minhas-avaliacoes')}>Voltar às minhas avaliações</button>
      </div>
    );
  }

  if (!avaliacao) {
    return erro ? <Alerta tipo="erro">{erro}</Alerta> : <Carregando />;
  }

  const bloqueada = jaRespondida || avaliacao.situacao !== 'ABERTA';

  return (
    <>
      <Link to="/minhas-avaliacoes" className="btn-ghost -ml-2 mb-2 px-2"><ArrowLeft className="h-4 w-4" /> Minhas avaliações</Link>
      <PageHeader titulo={avaliacao.nomeDisciplina} subtitulo={`${avaliacao.nomeDocente} · ${avaliacao.ano}/${avaliacao.semestre} · turma ${avaliacao.turma}`} />

      <div className="mb-6 flex items-start gap-3 rounded-2xl border border-accent bg-accent-soft p-4">
        <ShieldCheck className="mt-0.5 h-6 w-6 shrink-0 text-accent" aria-hidden />
        <div className="text-sm text-ink-soft">
          <p className="font-semibold text-ink">Anonimato garantido</p>
          <p>Registramos apenas que você participou. As respostas vão para uma tabela sem nenhuma referência ao seu usuário.</p>
        </div>
      </div>

      {bloqueada ? (
        <div className="card flex flex-col items-center gap-3 p-8 text-center">
          <Lock className="h-10 w-10 text-ink-muted" aria-hidden />
          <p className="text-base font-semibold text-ink">
            {jaRespondida ? 'Você já respondeu esta avaliação' : 'Avaliação fora do período de respostas'}
          </p>
          <p className="max-w-md text-sm text-ink-soft">
            {jaRespondida
              ? 'Cada aluno pode avaliar cada disciplina apenas uma vez por semestre.'
              : 'Respostas só são aceitas dentro da janela de data e horário definida pela coordenação.'}
          </p>
        </div>
      ) : (
        <form onSubmit={enviar} className="flex flex-col gap-4">
          <div className="sticky top-14 z-20 -mx-1 rounded-xl bg-primary px-1 py-2 lg:top-0">
            <div className="flex items-center justify-between text-xs text-ink-muted">
              <span>{totalRespondidas} de {itens.length} respondidas</span>
              <span>{progresso}%</span>
            </div>
            <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-secondary" role="progressbar" aria-valuenow={progresso} aria-valuemin={0} aria-valuemax={100}>
              <div className="h-full rounded-full bg-accent transition-all" style={{ width: `${progresso}%` }} />
            </div>
          </div>

          {avaliacao.descricao && <p className="text-sm text-ink-soft">{avaliacao.descricao}</p>}

          {itens.map(({ ordem, obrigatoria, questao: q }) => {
            const valor = respostas[q.idQuestao];
            const faltando = tentouEnviar && obrigatoria && !respondida(q, valor);
            return (
              <fieldset key={q.idQuestao} className={`card p-5 ${faltando ? 'border-bad' : ''}`}>
                <legend className="sr-only">Questão {ordem}</legend>
                <div className="mb-3 flex flex-wrap items-center gap-2">
                  <span className="text-xs font-semibold text-ink-muted">{ordem}.</span>
                  <Etiqueta>{ROTULO_CATEGORIA[q.categoria]}</Etiqueta>
                  {obrigatoria ? <span className="text-xs text-bad">* obrigatória</span> : <span className="text-xs text-ink-muted">opcional</span>}
                </div>
                <p className="mb-4 text-sm font-medium text-ink">{q.enunciado}</p>

                {q.tipo === 'ESCALA_1_5' && (
                  <div className="grid grid-cols-5 gap-2">
                    {[1, 2, 3, 4, 5].map((n) => (
                      <button
                        key={n}
                        type="button"
                        onClick={() => definir(q.idQuestao, n)}
                        aria-pressed={valor === n}
                        title={ROTULOS_ESCALA[n - 1]}
                        className={`flex flex-col items-center gap-1 rounded-xl border px-1 py-2 text-center transition-colors ${
                          valor === n ? 'border-accent bg-accent text-on-accent' : 'border-line bg-primary text-ink hover:border-accent'}`}
                      >
                        <span className="text-lg font-bold">{n}</span>
                        <span className={`hidden text-[10px] leading-tight sm:block ${valor === n ? '' : 'text-ink-muted'}`}>{ROTULOS_ESCALA[n - 1]}</span>
                      </button>
                    ))}
                  </div>
                )}

                {q.tipo === 'MULTIPLA_ESCOLHA' && (
                  <div className="flex flex-col gap-2">
                    {q.alternativas.map((a) => (
                      <label key={a.idAlternativa}
                        className={`flex cursor-pointer items-center gap-3 rounded-xl border px-3 py-2 text-sm transition-colors ${
                          valor === a.idAlternativa ? 'border-accent bg-accent-soft' : 'border-line hover:border-accent'}`}>
                        <input type="radio" name={`q${q.idQuestao}`} className="h-4 w-4 accent-[var(--accent-primary)]"
                          checked={valor === a.idAlternativa} onChange={() => definir(q.idQuestao, a.idAlternativa)} />
                        <span className="text-ink">{a.texto}</span>
                      </label>
                    ))}
                  </div>
                )}

                {q.tipo === 'TEXTO_LIVRE' && (
                  <div>
                    <textarea className="input min-h-[96px]" maxLength={2000} placeholder="Escreva livremente (opcional)…"
                      value={valor ?? ''} onChange={(e) => definir(q.idQuestao, e.target.value)} />
                    <p className="mt-1 text-right text-xs text-ink-muted">{(valor ?? '').length}/2000</p>
                  </div>
                )}
              </fieldset>
            );
          })}

          <Alerta tipo="erro">{erro}</Alerta>
          <button type="submit" className="btn-primary self-end px-6 py-3" disabled={enviando}>
            {enviando ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />} Enviar respostas anonimamente
          </button>
        </form>
      )}
    </>
  );
}
