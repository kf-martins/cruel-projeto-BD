import { useEffect, useMemo, useState } from 'react';
import { ArrowDown, ArrowLeft, ArrowRight, ArrowUp, Check, ClipboardList, Loader2, Plus, Users, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import {
  Alerta, Card, Carregando, Etiqueta, PageHeader, ROTULO_CATEGORIA, ROTULO_TIPO, SeloSituacao, Vazio,
  formatarData, formatarHora,
} from '../components/ui';

const PASSOS = ['Oferta', 'Dados', 'Questões', 'Alunos'];

function hojeISO(deslocamentoDias = 0) {
  const d = new Date();
  d.setDate(d.getDate() + deslocamentoDias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Lista de alunos com caixas de seleção; os já matriculados aparecem marcados e travados. */
function SeletorAlunos({ alunos, matriculados, selecionados, onAlterar }) {
  const [busca, setBusca] = useState('');
  const visiveis = alunos.filter((a) => `${a.nome} ${a.matricula}`.toLowerCase().includes(busca.toLowerCase()));
  const novos = alunos.filter((a) => !matriculados.has(a.idUsuario)).map((a) => a.idUsuario);
  return (
    <div>
      <div className="mb-2 flex flex-wrap items-center gap-2">
        <input className="input max-w-xs" placeholder="Buscar aluno…" value={busca} onChange={(e) => setBusca(e.target.value)} aria-label="Buscar aluno" />
        <button type="button" className="btn-ghost text-xs" onClick={() => onAlterar(new Set(novos))}>Selecionar todos</button>
        <button type="button" className="btn-ghost text-xs" onClick={() => onAlterar(new Set())}>Limpar</button>
        <span className="text-xs text-ink-muted">{matriculados.size} já matriculados · {selecionados.size} novos selecionados</span>
      </div>
      <div className="grid max-h-72 gap-1 overflow-y-auto rounded-xl border border-line p-2 sm:grid-cols-2">
        {visiveis.map((a) => {
          const jaMatriculado = matriculados.has(a.idUsuario);
          const marcado = jaMatriculado || selecionados.has(a.idUsuario);
          return (
            <label key={a.idUsuario} className={`flex cursor-pointer items-center gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-card-hover ${jaMatriculado ? 'opacity-70' : ''}`}>
              <input
                type="checkbox"
                className="h-4 w-4 accent-[var(--accent-primary)]"
                checked={marcado}
                disabled={jaMatriculado}
                onChange={() => {
                  const s = new Set(selecionados);
                  if (s.has(a.idUsuario)) s.delete(a.idUsuario);
                  else s.add(a.idUsuario);
                  onAlterar(s);
                }}
              />
              <span className="text-ink">{a.nome}</span>
              <span className="text-xs text-ink-muted">{a.matricula}</span>
            </label>
          );
        })}
      </div>
    </div>
  );
}

/** Assistente de criação de avaliação: RF2 (avaliação + período), RF3 (questões) e RF4 (alunos). */
function AssistenteAvaliacao({ onConcluir, onCancelar }) {
  const [passo, setPasso] = useState(0);
  const [base, setBase] = useState(null);
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  const [idOferta, setIdOferta] = useState('');
  const [novaOferta, setNovaOferta] = useState(null);
  const [dados, setDados] = useState({
    titulo: '', descricao: 'Avalie a disciplina e o docente. Suas respostas são anônimas.',
    dataAvaliacao: hojeISO(), dataEncerramento: hojeISO(30), horaInicio: '00:00', horaFim: '23:59',
  });
  const [escolhidas, setEscolhidas] = useState([]); // [{idQuestao, obrigatoria}]
  const [matriculados, setMatriculados] = useState(new Set());
  const [novosAlunos, setNovosAlunos] = useState(new Set());

  useEffect(() => {
    Promise.all([api.ofertas(), api.questoes(), api.usuarios('ALUNO'), api.usuarios('DOCENTE'), api.disciplinas()])
      .then(([ofertas, questoes, alunos, docentes, disciplinas]) => setBase({ ofertas, questoes, alunos, docentes, disciplinas }))
      .catch((e) => setErro(e.message));
  }, []);

  const ofertasLivres = useMemo(() => (base?.ofertas ?? []).filter((o) => !o.idAvaliacao), [base]);
  const oferta = base?.ofertas.find((o) => String(o.idOferta) === String(idOferta));
  const questoesPorId = useMemo(() => new Map((base?.questoes ?? []).map((q) => [q.idQuestao, q])), [base]);

  useEffect(() => {
    if (!oferta) return;
    setDados((d) => ({ ...d, titulo: d.titulo || `Avaliação de ${oferta.nomeDisciplina} — ${oferta.ano}/${oferta.semestre}` }));
    api.alunosDaOferta(oferta.idOferta)
      .then((lista) => setMatriculados(new Set(lista.map((a) => a.idUsuario))))
      .catch((e) => setErro(e.message));
  }, [oferta?.idOferta]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!base) return <Card className="mb-6">{erro ? <Alerta tipo="erro">{erro}</Alerta> : <Carregando />}</Card>;

  const criarOferta = async () => {
    setErro('');
    setEnviando(true);
    try {
      const criada = await api.criarOferta({
        ...novaOferta,
        idDisciplina: Number(novaOferta.idDisciplina),
        idDocente: Number(novaOferta.idDocente),
        ano: Number(novaOferta.ano),
        semestre: Number(novaOferta.semestre),
      });
      setBase((b) => ({ ...b, ofertas: [criada, ...b.ofertas] }));
      setIdOferta(String(criada.idOferta));
      setNovaOferta(null);
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  };

  const alternarQuestao = (q) =>
    setEscolhidas((lista) => (lista.some((x) => x.idQuestao === q.idQuestao)
      ? lista.filter((x) => x.idQuestao !== q.idQuestao)
      : [...lista, { idQuestao: q.idQuestao, obrigatoria: q.tipo !== 'TEXTO_LIVRE' }]));

  const mover = (i, delta) =>
    setEscolhidas((lista) => {
      const nova = [...lista];
      const [item] = nova.splice(i, 1);
      nova.splice(i + delta, 0, item);
      return nova;
    });

  const podeAvancar = [
    Boolean(oferta),
    dados.titulo.trim() && dados.dataAvaliacao && dados.dataEncerramento && dados.dataEncerramento >= dados.dataAvaliacao && dados.horaFim > dados.horaInicio,
    escolhidas.length > 0,
    true,
  ][passo];

  const concluir = async () => {
    setErro('');
    setEnviando(true);
    try {
      const criada = await api.criarAvaliacao({
        idOferta: oferta.idOferta,
        ...dados,
        questoes: escolhidas,
        idsAlunos: [...novosAlunos],
      });
      onConcluir(criada);
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Card className="mb-6" titulo="Nova avaliação"
      acoes={<button type="button" className="btn-ghost px-2" onClick={onCancelar} aria-label="Fechar"><X className="h-4 w-4" /></button>}>
      <ol className="mb-6 flex flex-wrap gap-2">
        {PASSOS.map((p, i) => (
          <li key={p} className={`flex items-center gap-2 rounded-full px-3 py-1 text-xs font-semibold ${
            i === passo ? 'bg-accent text-on-accent' : i < passo ? 'bg-accent-soft text-ink' : 'bg-secondary text-ink-muted'}`}>
            {i < passo ? <Check className="h-3 w-3" /> : <span>{i + 1}</span>} {p}
          </li>
        ))}
      </ol>

      {passo === 0 && (
        <div className="flex flex-col gap-4">
          <div>
            <label className="label" htmlFor="oferta">Oferta (disciplina · docente · semestre)</label>
            <select id="oferta" className="input" value={idOferta} onChange={(e) => setIdOferta(e.target.value)}>
              <option value="">Selecione…</option>
              {ofertasLivres.map((o) => (
                <option key={o.idOferta} value={o.idOferta}>
                  {o.codigoDisciplina} {o.nomeDisciplina} · {o.nomeDocente} · {o.ano}/{o.semestre} turma {o.turma}
                </option>
              ))}
            </select>
            <p className="mt-1 text-xs text-ink-muted">
              Cada oferta admite uma única avaliação. Ofertas que já possuem avaliação não aparecem aqui.
            </p>
          </div>
          {!novaOferta ? (
            <button type="button" className="btn-outline self-start"
              onClick={() => setNovaOferta({ idDisciplina: '', idDocente: '', ano: new Date().getFullYear(), semestre: 2, turma: 'A' })}>
              <Plus className="h-4 w-4" /> Cadastrar nova oferta
            </button>
          ) : (
            <div className="grid gap-3 rounded-xl border border-line p-4 sm:grid-cols-5">
              <select className="input sm:col-span-2" value={novaOferta.idDisciplina} aria-label="Disciplina"
                onChange={(e) => setNovaOferta({ ...novaOferta, idDisciplina: e.target.value })}>
                <option value="">Disciplina…</option>
                {base.disciplinas.map((d) => <option key={d.idDisciplina} value={d.idDisciplina}>{d.codigo} {d.nome}</option>)}
              </select>
              <select className="input sm:col-span-3" value={novaOferta.idDocente} aria-label="Docente"
                onChange={(e) => setNovaOferta({ ...novaOferta, idDocente: e.target.value })}>
                <option value="">Docente…</option>
                {base.docentes.map((d) => <option key={d.idUsuario} value={d.idUsuario}>{d.nome}</option>)}
              </select>
              <input className="input" type="number" min="2020" value={novaOferta.ano} aria-label="Ano"
                onChange={(e) => setNovaOferta({ ...novaOferta, ano: e.target.value })} />
              <select className="input" value={novaOferta.semestre} aria-label="Semestre"
                onChange={(e) => setNovaOferta({ ...novaOferta, semestre: e.target.value })}>
                <option value={1}>1º semestre</option>
                <option value={2}>2º semestre</option>
              </select>
              <input className="input" maxLength={10} value={novaOferta.turma} aria-label="Turma"
                onChange={(e) => setNovaOferta({ ...novaOferta, turma: e.target.value })} />
              <div className="flex gap-2 sm:col-span-2">
                <button type="button" className="btn-primary" disabled={enviando || !novaOferta.idDisciplina || !novaOferta.idDocente} onClick={criarOferta}>
                  Criar oferta
                </button>
                <button type="button" className="btn-ghost" onClick={() => setNovaOferta(null)}>Cancelar</button>
              </div>
            </div>
          )}
        </div>
      )}

      {passo === 1 && (
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="label" htmlFor="titulo">Título</label>
            <input id="titulo" className="input" maxLength={150} value={dados.titulo} onChange={(e) => setDados({ ...dados, titulo: e.target.value })} />
          </div>
          <div className="sm:col-span-2">
            <label className="label" htmlFor="descricao">Descrição / instruções</label>
            <textarea id="descricao" className="input" value={dados.descricao} onChange={(e) => setDados({ ...dados, descricao: e.target.value })} />
          </div>
          <div>
            <label className="label" htmlFor="dataAvaliacao">Data de abertura</label>
            <input id="dataAvaliacao" type="date" className="input" value={dados.dataAvaliacao} onChange={(e) => setDados({ ...dados, dataAvaliacao: e.target.value })} />
          </div>
          <div>
            <label className="label" htmlFor="dataEncerramento">Data de encerramento</label>
            <input id="dataEncerramento" type="date" className="input" value={dados.dataEncerramento} onChange={(e) => setDados({ ...dados, dataEncerramento: e.target.value })} />
          </div>
          <div>
            <label className="label" htmlFor="horaInicio">Hora de início (no dia de abertura)</label>
            <input id="horaInicio" type="time" className="input" value={dados.horaInicio} onChange={(e) => setDados({ ...dados, horaInicio: e.target.value })} />
          </div>
          <div>
            <label className="label" htmlFor="horaFim">Hora de fim (no dia de encerramento)</label>
            <input id="horaFim" type="time" className="input" value={dados.horaFim} onChange={(e) => setDados({ ...dados, horaFim: e.target.value })} />
          </div>
        </div>
      )}

      {passo === 2 && (
        <div className="grid gap-4 lg:grid-cols-2">
          <div>
            <p className="label">Banco de questões ativas</p>
            <div className="flex max-h-96 flex-col gap-1 overflow-y-auto rounded-xl border border-line p-2">
              {base.questoes.filter((q) => q.ativa).map((q) => {
                const marcada = escolhidas.some((x) => x.idQuestao === q.idQuestao);
                return (
                  <label key={q.idQuestao} className="flex cursor-pointer items-start gap-2 rounded-lg px-2 py-1.5 text-sm hover:bg-card-hover">
                    <input type="checkbox" className="mt-0.5 h-4 w-4 accent-[var(--accent-primary)]" checked={marcada} onChange={() => alternarQuestao(q)} />
                    <span className="flex-1">
                      <span className="text-ink">{q.enunciado}</span>
                      <span className="mt-0.5 flex gap-1">
                        <Etiqueta>{ROTULO_TIPO[q.tipo]}</Etiqueta>
                        <Etiqueta>{ROTULO_CATEGORIA[q.categoria]}</Etiqueta>
                      </span>
                    </span>
                  </label>
                );
              })}
            </div>
          </div>
          <div>
            <p className="label">Questionário ({escolhidas.length} questões, na ordem de exibição)</p>
            {escolhidas.length === 0 ? (
              <Vazio icone={ClipboardList} titulo="Nenhuma questão selecionada">Marque questões à esquerda.</Vazio>
            ) : (
              <ol className="flex flex-col gap-1">
                {escolhidas.map((e, i) => (
                  <li key={e.idQuestao} className="flex items-center gap-2 rounded-lg border border-line px-2 py-1.5 text-sm">
                    <span className="w-6 text-center text-xs font-semibold text-ink-muted">{i + 1}</span>
                    <span className="flex-1 text-ink">{questoesPorId.get(e.idQuestao)?.enunciado}</span>
                    <label className="flex items-center gap-1 text-xs text-ink-soft">
                      <input type="checkbox" className="h-3.5 w-3.5 accent-[var(--accent-primary)]" checked={e.obrigatoria}
                        onChange={() => setEscolhidas((l) => l.map((x, j) => (j === i ? { ...x, obrigatoria: !x.obrigatoria } : x)))} />
                      obrigatória
                    </label>
                    <button type="button" className="btn-ghost px-1 py-1" disabled={i === 0} onClick={() => mover(i, -1)} aria-label="Subir"><ArrowUp className="h-3.5 w-3.5" /></button>
                    <button type="button" className="btn-ghost px-1 py-1" disabled={i === escolhidas.length - 1} onClick={() => mover(i, 1)} aria-label="Descer"><ArrowDown className="h-3.5 w-3.5" /></button>
                  </li>
                ))}
              </ol>
            )}
          </div>
        </div>
      )}

      {passo === 3 && (
        <div className="flex flex-col gap-3">
          <p className="text-sm text-ink-soft">
            RF4 — alunos elegíveis da oferta <strong className="text-ink">{oferta?.nomeDisciplina} {oferta?.ano}/{oferta?.semestre}</strong>.
            Apenas alunos matriculados podem responder.
          </p>
          <SeletorAlunos alunos={base.alunos} matriculados={matriculados} selecionados={novosAlunos} onAlterar={setNovosAlunos} />
        </div>
      )}

      <div className="mt-6 flex flex-col gap-3">
        <Alerta tipo="erro" onFechar={() => setErro('')}>{erro}</Alerta>
        <div className="flex justify-between gap-2">
          <button type="button" className="btn-ghost" disabled={passo === 0} onClick={() => setPasso((p) => p - 1)}>
            <ArrowLeft className="h-4 w-4" /> Voltar
          </button>
          {passo < PASSOS.length - 1 ? (
            <button type="button" className="btn-primary" disabled={!podeAvancar} onClick={() => setPasso((p) => p + 1)}>
              Avançar <ArrowRight className="h-4 w-4" />
            </button>
          ) : (
            <button type="button" className="btn-primary" disabled={enviando} onClick={concluir}>
              {enviando ? <Loader2 className="h-4 w-4 animate-spin" /> : <Check className="h-4 w-4" />} Criar avaliação
            </button>
          )}
        </div>
      </div>
    </Card>
  );
}

/** RF4 — painel para matricular alunos em uma oferta que já possui avaliação. */
function PainelTurma({ avaliacao, onFechar, onAtualizado }) {
  const [alunos, setAlunos] = useState(null);
  const [matriculados, setMatriculados] = useState(new Set());
  const [novos, setNovos] = useState(new Set());
  const [msg, setMsg] = useState({ tipo: 'info', texto: '' });

  useEffect(() => {
    Promise.all([api.usuarios('ALUNO'), api.alunosDaOferta(avaliacao.idOferta)])
      .then(([todos, daOferta]) => {
        setAlunos(todos);
        setMatriculados(new Set(daOferta.map((a) => a.idUsuario)));
      })
      .catch((e) => setMsg({ tipo: 'erro', texto: e.message }));
  }, [avaliacao.idOferta]);

  const salvar = async () => {
    try {
      const r = await api.matricular(avaliacao.idOferta, [...novos]);
      setMatriculados((m) => new Set([...m, ...novos]));
      setNovos(new Set());
      setMsg({ tipo: 'sucesso', texto: `${r.novasMatriculas} aluno(s) matriculado(s).` });
      onAtualizado();
    } catch (e) {
      setMsg({ tipo: 'erro', texto: e.message });
    }
  };

  return (
    <Card className="mb-6" titulo={`Turma — ${avaliacao.nomeDisciplina} ${avaliacao.ano}/${avaliacao.semestre}`}
      descricao="Alunos matriculados na oferta são os elegíveis para responder."
      acoes={<button type="button" className="btn-ghost px-2" onClick={onFechar} aria-label="Fechar"><X className="h-4 w-4" /></button>}>
      {!alunos ? <Carregando /> : (
        <>
          <SeletorAlunos alunos={alunos} matriculados={matriculados} selecionados={novos} onAlterar={setNovos} />
          <div className="mt-3 flex flex-col gap-3">
            <Alerta tipo={msg.tipo}>{msg.texto}</Alerta>
            <button type="button" className="btn-primary self-end" disabled={novos.size === 0} onClick={salvar}>
              <Users className="h-4 w-4" /> Matricular selecionados
            </button>
          </div>
        </>
      )}
    </Card>
  );
}

export default function AvaliacoesAdminPage() {
  const { isAdmin } = useAuth();
  const [avaliacoes, setAvaliacoes] = useState(null);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [criando, setCriando] = useState(false);
  const [turma, setTurma] = useState(null);

  const carregar = () => api.avaliacoes().then(setAvaliacoes).catch((e) => setErro(e.message));
  useEffect(() => {
    carregar();
  }, []);

  const alternarStatus = async (a) => {
    try {
      await api.statusAvaliacao(a.idAvaliacao, !a.ativa);
      carregar();
    } catch (e) {
      setErro(e.message);
    }
  };

  return (
    <>
      <PageHeader
        titulo="Avaliações"
        subtitulo="RF2, RF3 e RF4 — monte avaliações por oferta, escolha as questões e defina os alunos elegíveis."
        acoes={isAdmin && !criando && (
          <button type="button" className="btn-primary" onClick={() => { setCriando(true); setTurma(null); }}>
            <Plus className="h-4 w-4" /> Nova avaliação
          </button>
        )}
      />

      {criando && (
        <AssistenteAvaliacao
          onCancelar={() => setCriando(false)}
          onConcluir={(a) => {
            setCriando(false);
            setSucesso(`Avaliação "${a.titulo}" criada com ${a.itens.length} questões e ${a.totalMatriculados} alunos elegíveis.`);
            carregar();
          }}
        />
      )}
      {turma && <PainelTurma avaliacao={turma} onFechar={() => setTurma(null)} onAtualizado={carregar} />}

      <div className="mb-4 flex flex-col gap-3">
        <Alerta tipo="sucesso" onFechar={() => setSucesso('')}>{sucesso}</Alerta>
        <Alerta tipo="erro" onFechar={() => setErro('')}>{erro}</Alerta>
      </div>

      <Card>
        {!avaliacoes ? <Carregando /> : avaliacoes.length === 0 ? (
          <Vazio icone={ClipboardList} titulo="Nenhuma avaliação cadastrada" />
        ) : (
          <div className="overflow-x-auto">
            <table className="table-base">
              <thead>
                <tr>
                  <th>Avaliação</th>
                  <th>Janela de resposta</th>
                  <th>Situação</th>
                  <th className="text-right">Questões</th>
                  <th className="text-right">Adesão</th>
                  {isAdmin && <th className="text-right">Ações</th>}
                </tr>
              </thead>
              <tbody>
                {avaliacoes.map((a) => (
                  <tr key={a.idAvaliacao}>
                    <td>
                      <p className="font-medium">{a.titulo}</p>
                      <p className="text-xs text-ink-muted">{a.codigoDisciplina} · {a.nomeDocente} · turma {a.turma}</p>
                    </td>
                    <td className="whitespace-nowrap text-xs text-ink-soft">
                      {formatarData(a.dataAvaliacao)} {formatarHora(a.horaInicio)}<br />até {formatarData(a.dataEncerramento)} {formatarHora(a.horaFim)}
                    </td>
                    <td><SeloSituacao situacao={a.situacao} /></td>
                    <td className="text-right tabular-nums">{a.totalQuestoes}</td>
                    <td className="text-right tabular-nums">{a.totalRespondentes}/{a.totalMatriculados}</td>
                    {isAdmin && (
                      <td className="whitespace-nowrap text-right">
                        <button type="button" className="btn-ghost px-2 py-1 text-xs" onClick={() => { setTurma(a); setCriando(false); }}>
                          <Users className="h-3.5 w-3.5" /> Turma
                        </button>
                        <button type="button" className="btn-ghost px-2 py-1 text-xs" onClick={() => alternarStatus(a)}>
                          {a.ativa ? 'Desativar' : 'Ativar'}
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
