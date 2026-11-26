import { useEffect, useMemo, useState } from 'react';
import {
  Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts';
import {
  ArrowDownRight, ArrowUpRight, BarChart3, Database, MessageSquareQuote, Minus, TrendingDown, TrendingUp,
} from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { api } from '../services/api';
import {
  Alerta, Card, Carregando, Etiqueta, PageHeader, ROTULO_CATEGORIA, ROTULO_TIPO, Vazio, formatarNumero,
} from '../components/ui';

const ABAS = [
  { id: 'r1', titulo: '1. Média por disciplina' },
  { id: 'r2', titulo: '2. Categorias vs. curso' },
  { id: 'r3', titulo: '3. Ranking de questões' },
  { id: 'r4', titulo: '4. Adesão e pendentes' },
  { id: 'com', titulo: 'Comentários anônimos' },
];

const ABREVIACOES = {
  'Banco de Dados I': 'BD I',
  'Engenharia de Software': 'Eng. Soft.',
  'Sistemas Operacionais': 'Sist. Oper.',
  'Redes de Computadores': 'Redes',
};
const abreviar = (nome) => ABREVIACOES[nome] ?? (nome.length > 18 ? `${nome.slice(0, 17)}…` : nome);

// ------------------------------------------------------------------ peças comuns dos gráficos

function useEstiloGrafico() {
  const { paleta } = useTheme();
  return useMemo(() => ({
    paleta,
    eixo: { stroke: paleta.axis, fontSize: 12, tickLine: false, axisLine: { stroke: paleta.border } },
    grade: { stroke: paleta.grid, strokeDasharray: '0', vertical: false },
    cursor: { fill: paleta.grid, opacity: 0.6 },
  }), [paleta]);
}

/** Tooltip com tokens do tema: texto em tinta primária/secundária, cor apenas no marcador. */
function DicaGrafico({ active, payload, label, formatar = (v) => formatarNumero(v), rotulo }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-xl border border-line bg-card px-3 py-2 text-xs shadow-card">
      <p className="mb-1 font-semibold text-ink">{rotulo ? rotulo(payload[0].payload) : label}</p>
      {payload.map((p) => (
        <p key={p.dataKey} className="flex items-center gap-2 text-ink-soft">
          <span className="inline-block h-2.5 w-2.5 rounded-sm" style={{ background: p.color }} aria-hidden />
          {p.name}: <span className="font-semibold tabular-nums text-ink">{formatar(p.value)}</span>
        </p>
      ))}
    </div>
  );
}

/** Rótulo do eixo em duas linhas ("Disciplina" / "ano/semestre") para não sobrepor. */
function TickDuasLinhas({ x, y, payload, fill }) {
  const [nome, periodo] = String(payload.value).split(' · ');
  return (
    <g transform={`translate(${x},${y})`}>
      <text textAnchor="middle" fill={fill} fontSize={11}>
        <tspan x={0} dy={12}>{nome}</tspan>
        {periodo && <tspan x={0} dy={14}>{periodo}</tspan>}
      </text>
    </g>
  );
}

function RecursosSql({ itens }) {
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      <Database className="h-3.5 w-3.5 text-ink-muted" aria-hidden />
      {itens.map((i) => <Etiqueta key={i}>{i}</Etiqueta>)}
    </div>
  );
}

function legendaFormatada(valor) {
  return <span className="text-xs text-ink-soft">{valor}</span>;
}

function Variacao({ valor }) {
  const n = Number(valor);
  if (n > 0.05) return <span className="inline-flex items-center gap-1 font-semibold text-good"><ArrowUpRight className="h-3.5 w-3.5" aria-hidden />+{formatarNumero(n)}</span>;
  if (n < -0.05) return <span className="inline-flex items-center gap-1 font-semibold text-bad"><ArrowDownRight className="h-3.5 w-3.5" aria-hidden />{formatarNumero(n)}</span>;
  return <span className="inline-flex items-center gap-1 text-ink-soft"><Minus className="h-3.5 w-3.5" aria-hidden />{formatarNumero(n)}</span>;
}

/** Carrega um relatório sempre que os filtros mudam. */
function useRelatorio(buscar, deps) {
  const [estado, setEstado] = useState({ dados: null, erro: '' });
  useEffect(() => {
    let ativo = true;
    setEstado({ dados: null, erro: '' });
    buscar()
      .then((dados) => ativo && setEstado({ dados, erro: '' }))
      .catch((e) => ativo && setEstado({ dados: null, erro: e.message }));
    return () => {
      ativo = false;
    };
  }, deps); // eslint-disable-line react-hooks/exhaustive-deps
  return estado;
}

function Estado({ estado, children }) {
  if (estado.erro) return <Alerta tipo="erro">{estado.erro}</Alerta>;
  if (!estado.dados) return <Carregando />;
  if (estado.dados.length === 0) return <Vazio icone={BarChart3} titulo="Sem dados para os filtros selecionados" />;
  return children(estado.dados);
}

// ------------------------------------------------------------------ Relatório 1

function Relatorio1({ filtro, coresDisciplina }) {
  const { paleta, eixo, grade, cursor } = useEstiloGrafico();
  const estado = useRelatorio(() => api.relatorio.mediaDisciplinas(filtro), [filtro]);

  return (
    <Estado estado={estado}>
      {(linhas) => {
        const barras = linhas.map((l) => ({ ...l, rotulo: `${abreviar(l.disciplina)} · ${l.ano}/${l.semestre}`, media: Number(l.mediaEscala) }));
        const periodos = [...new Set(linhas.map((l) => `${l.ano}/${l.semestre}`))].sort();
        const disciplinas = [...new Set(linhas.map((l) => l.disciplina))];
        const evolucao = periodos.map((p) => {
          const ponto = { periodo: p };
          linhas.filter((l) => `${l.ano}/${l.semestre}` === p).forEach((l) => { ponto[l.disciplina] = Number(l.mediaEscala); });
          return ponto;
        });
        return (
          <div className="flex flex-col gap-6">
            <div className="grid gap-6 lg:grid-cols-2">
              <Card titulo="Média de satisfação (escala 1–5)" descricao="Uma barra por oferta avaliada">
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={barras} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                    <CartesianGrid {...grade} />
                    <XAxis dataKey="rotulo" {...eixo} interval={0} height={36} tick={<TickDuasLinhas fill={paleta.axis} />} />
                    <YAxis domain={[0, 5]} ticks={[0, 1, 2, 3, 4, 5]} {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                    <Tooltip cursor={cursor} content={<DicaGrafico />} />
                    <Bar dataKey="media" name="Média" fill={paleta.series[0]} radius={[4, 4, 0, 0]} maxBarSize={48} />
                  </BarChart>
                </ResponsiveContainer>
              </Card>
              <Card titulo="Evolução por semestre" descricao={periodos.length < 2 ? 'Remova o filtro de período para comparar semestres' : 'Média por disciplina em cada período'}>
                <ResponsiveContainer width="100%" height={280}>
                  <LineChart data={evolucao} margin={{ top: 8, right: 16, left: -16, bottom: 0 }}>
                    <CartesianGrid {...grade} />
                    <XAxis dataKey="periodo" {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} padding={{ left: 24, right: 24 }} />
                    <YAxis domain={[0, 5]} ticks={[0, 1, 2, 3, 4, 5]} {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                    <Tooltip content={<DicaGrafico />} cursor={{ stroke: paleta.axis, strokeWidth: 1 }} />
                    <Legend formatter={legendaFormatada} iconType="circle" iconSize={8} />
                    {disciplinas.map((d) => (
                      <Line key={d} type="linear" dataKey={d} name={abreviar(d)} stroke={coresDisciplina(d)} strokeWidth={2}
                        dot={{ r: 4, strokeWidth: 2, stroke: paleta.card, fill: coresDisciplina(d) }} activeDot={{ r: 6, stroke: paleta.card, strokeWidth: 2 }}
                        connectNulls isAnimationActive={false} />
                    ))}
                  </LineChart>
                </ResponsiveContainer>
              </Card>
            </div>
            <Card titulo="Tabela detalhada" descricao="Distribuição das notas da escala 1–5 por oferta">
              <div className="overflow-x-auto">
                <table className="table-base">
                  <thead>
                    <tr>
                      <th>Disciplina</th><th>Docente</th><th>Período</th>
                      <th className="text-right">Submissões</th><th className="text-right">Respostas</th><th className="text-right">Média</th>
                      {[1, 2, 3, 4, 5].map((n) => <th key={n} className="text-right">Nota {n}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {linhas.map((l) => (
                      <tr key={`${l.codigo}-${l.ano}-${l.semestre}`}>
                        <td className="font-medium">{l.disciplina}</td>
                        <td className="text-ink-soft">{l.docente}</td>
                        <td>{l.ano}/{l.semestre}</td>
                        <td className="text-right tabular-nums">{l.totalSubmissoes}</td>
                        <td className="text-right tabular-nums">{l.totalRespostas}</td>
                        <td className="text-right font-semibold tabular-nums">{formatarNumero(l.mediaEscala)}</td>
                        {[l.qtdNota1, l.qtdNota2, l.qtdNota3, l.qtdNota4, l.qtdNota5].map((q, i) => (
                          <td key={i} className="text-right tabular-nums text-ink-soft">{q}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          </div>
        );
      }}
    </Estado>
  );
}

// ------------------------------------------------------------------ Relatório 2

function Relatorio2({ filtro }) {
  const { paleta, eixo, grade, cursor } = useEstiloGrafico();
  const estado = useRelatorio(() => api.relatorio.comparativo(filtro), [filtro]);
  const [oferta, setOferta] = useState('');

  return (
    <Estado estado={estado}>
      {(linhas) => {
        const chave = (l) => `${l.disciplina} · ${l.ano}/${l.semestre}`;
        // Períodos mais recentes primeiro (o SQL ordena do mais antigo para o mais novo).
        const ofertas = [...new Set([...linhas].reverse().map(chave))];
        const selecionada = ofertas.includes(oferta) ? oferta : ofertas[0];
        const dados = linhas.filter((l) => chave(l) === selecionada).map((l) => ({
          categoria: ROTULO_CATEGORIA[l.categoria],
          oferta: Number(l.mediaOferta),
          curso: Number(l.mediaCurso),
        }));
        return (
          <div className="flex flex-col gap-6">
            <Card
              titulo="Média da oferta vs. média do curso, por categoria"
              descricao="A média do curso considera todas as ofertas do mesmo período (subconsulta correlacionada)"
              acoes={
                <select className="input w-auto" value={selecionada} onChange={(e) => setOferta(e.target.value)} aria-label="Oferta">
                  {ofertas.map((o) => <option key={o}>{o}</option>)}
                </select>
              }
            >
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={dados} margin={{ top: 8, right: 8, left: -16, bottom: 0 }} barGap={2}>
                  <CartesianGrid {...grade} />
                  <XAxis dataKey="categoria" {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                  <YAxis domain={[0, 5]} ticks={[0, 1, 2, 3, 4, 5]} {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                  <Tooltip cursor={cursor} content={<DicaGrafico />} />
                  <Legend formatter={legendaFormatada} iconType="square" iconSize={10} />
                  <Bar dataKey="oferta" name={abreviar(selecionada.split(' · ')[0])} fill={paleta.series[0]} radius={[4, 4, 0, 0]} maxBarSize={44} />
                  <Bar dataKey="curso" name="Média do curso" fill={paleta.series[1]} radius={[4, 4, 0, 0]} maxBarSize={44} />
                </BarChart>
              </ResponsiveContainer>
            </Card>
            <Card titulo="Tabela comparativa" descricao="Apenas grupos com pelo menos 5 respostas (HAVING COUNT(*) >= 5)">
              <div className="overflow-x-auto">
                <table className="table-base">
                  <thead>
                    <tr>
                      <th>Disciplina</th><th>Docente</th><th>Período</th><th>Categoria</th>
                      <th className="text-right">Respostas</th><th className="text-right">Média oferta</th>
                      <th className="text-right">Média curso</th><th className="text-right">Diferença</th>
                    </tr>
                  </thead>
                  <tbody>
                    {linhas.map((l) => (
                      <tr key={`${chave(l)}-${l.categoria}`}>
                        <td className="font-medium">{l.disciplina}</td>
                        <td className="text-ink-soft">{l.docente}</td>
                        <td>{l.ano}/{l.semestre}</td>
                        <td>{ROTULO_CATEGORIA[l.categoria]}</td>
                        <td className="text-right tabular-nums">{l.totalRespostas}</td>
                        <td className="text-right tabular-nums">{formatarNumero(l.mediaOferta)}</td>
                        <td className="text-right tabular-nums text-ink-soft">{formatarNumero(l.mediaCurso)}</td>
                        <td className="text-right tabular-nums"><Variacao valor={l.diferenca} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          </div>
        );
      }}
    </Estado>
  );
}

// ------------------------------------------------------------------ Relatório 3

function Relatorio3({ filtro }) {
  const { paleta, eixo, cursor } = useEstiloGrafico();
  const [ordem, setOrdem] = useState('melhores');
  const estado = useRelatorio(() => api.relatorio.ranking(filtro, ordem), [filtro, ordem]);

  const seletor = (
    <div className="inline-flex rounded-xl border border-line p-0.5" role="group" aria-label="Ordem do ranking">
      {[
        { v: 'melhores', r: 'Mais bem avaliadas', i: TrendingUp },
        { v: 'criticas', r: 'Mais críticas', i: TrendingDown },
      ].map(({ v, r, i: Icone }) => (
        <button key={v} type="button" onClick={() => setOrdem(v)} aria-pressed={ordem === v}
          className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold ${ordem === v ? 'bg-accent text-on-accent' : 'text-ink-soft hover:text-ink'}`}>
          <Icone className="h-3.5 w-3.5" aria-hidden /> {r}
        </button>
      ))}
    </div>
  );

  return (
    <div className="flex flex-col gap-6">
      <div>{seletor}</div>
      <Estado estado={estado}>
        {(linhas) => {
          const dados = linhas.map((l) => ({ ...l, rotulo: `#${l.posicao} · Q${l.idQuestao}`, indice: Number(l.indiceAprovacao) }));
          return (
            <>
              <Card titulo={`Top 10 — ${ordem === 'melhores' ? 'mais bem avaliadas' : 'mais críticas'}`}
                descricao="Índice de aprovação: % de notas 4–5 (escala) ou alternativas de valor 3–4 (múltipla escolha)">
                <ResponsiveContainer width="100%" height={Math.max(260, dados.length * 34)}>
                  <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 24, left: 8, bottom: 0 }}>
                    <CartesianGrid stroke={paleta.grid} horizontal={false} />
                    <XAxis type="number" domain={[0, 100]} unit="%" {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                    <YAxis type="category" dataKey="rotulo" width={92} {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                    <Tooltip cursor={cursor}
                      content={<DicaGrafico formatar={(v) => `${formatarNumero(v, 1)}%`} rotulo={(p) => `Q${p.idQuestao} — ${p.enunciado}`} />} />
                    <Bar dataKey="indice" name="Aprovação" fill={paleta.series[0]} radius={[0, 4, 4, 0]} maxBarSize={22} />
                  </BarChart>
                </ResponsiveContainer>
              </Card>
              <Card titulo="Tabela do ranking" descricao="RANK() OVER (ORDER BY índice) · FETCH FIRST 10 ROWS ONLY">
                <div className="overflow-x-auto">
                  <table className="table-base">
                    <thead>
                      <tr>
                        <th className="text-right">#</th><th>Questão</th><th>Tipo</th><th>Categoria</th>
                        <th className="text-right">Respostas</th><th className="text-right">Aprovação</th><th className="text-right">Média (1–5)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {linhas.map((l) => (
                        <tr key={l.idQuestao}>
                          <td className="text-right font-semibold tabular-nums">{l.posicao}</td>
                          <td><span className="text-xs text-ink-muted">Q{l.idQuestao}</span> {l.enunciado}</td>
                          <td className="whitespace-nowrap text-ink-soft">{ROTULO_TIPO[l.tipo]}</td>
                          <td className="text-ink-soft">{ROTULO_CATEGORIA[l.categoria]}</td>
                          <td className="text-right tabular-nums">{l.totalRespostas}</td>
                          <td className="text-right font-semibold tabular-nums">{formatarNumero(l.indiceAprovacao, 1)}%</td>
                          <td className="text-right tabular-nums">{formatarNumero(l.mediaNormalizada)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Card>
            </>
          );
        }}
      </Estado>
    </div>
  );
}

// ------------------------------------------------------------------ Relatório 4

const NIVEL = {
  ALTA: 'bg-good-soft text-good',
  'MÉDIA': 'bg-warn-soft text-warn',
  BAIXA: 'bg-bad-soft text-bad',
};

function Relatorio4({ filtro }) {
  const { paleta, eixo, cursor } = useEstiloGrafico();
  const adesao = useRelatorio(() => api.relatorio.adesao(filtro), [filtro]);
  const pendentes = useRelatorio(() => api.relatorio.pendentes(filtro), [filtro]);
  const naoUsadas = useRelatorio(() => api.relatorio.questoesNaoUtilizadas(), []);

  return (
    <div className="flex flex-col gap-6">
      <Estado estado={adesao}>
        {(linhas) => {
          const dados = linhas.map((l) => ({ ...l, rotulo: `${abreviar(l.disciplina)} · ${l.ano}/${l.semestre}` }));
          return (
            <>
              <Card titulo="Respondentes x pendentes por avaliação" descricao="Calculado só com participacao_avaliacao — as respostas continuam anônimas">
                <ResponsiveContainer width="100%" height={Math.max(220, dados.length * 46)}>
                  <BarChart data={dados} layout="vertical" margin={{ top: 4, right: 24, left: 8, bottom: 0 }}>
                    <CartesianGrid stroke={paleta.grid} horizontal={false} />
                    <XAxis type="number" allowDecimals={false} {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                    <YAxis type="category" dataKey="rotulo" width={150} {...eixo} tick={{ fill: paleta.axis, fontSize: 12 }} />
                    <Tooltip cursor={cursor} content={<DicaGrafico formatar={(v) => v} />} />
                    <Legend formatter={legendaFormatada} iconType="square" iconSize={10} />
                    <Bar dataKey="respondentes" name="Respondentes" stackId="a" fill={paleta.series[0]} stroke={paleta.card} strokeWidth={2} maxBarSize={26} />
                    <Bar dataKey="pendentes" name="Pendentes" stackId="a" fill={paleta.series[1]} stroke={paleta.card} strokeWidth={2} radius={[0, 4, 4, 0]} maxBarSize={26} />
                  </BarChart>
                </ResponsiveContainer>
              </Card>
              <Card titulo="Taxa de adesão" descricao="LEFT JOIN matricula_oferta × participacao_avaliacao, com CASE WHEN para situação e nível">
                <div className="overflow-x-auto">
                  <table className="table-base">
                    <thead>
                      <tr>
                        <th>Avaliação</th><th>Situação</th>
                        <th className="text-right">Matriculados</th><th className="text-right">Respondentes</th>
                        <th className="text-right">Pendentes</th><th className="text-right">Adesão</th><th>Nível</th>
                      </tr>
                    </thead>
                    <tbody>
                      {linhas.map((l) => (
                        <tr key={l.idAvaliacao}>
                          <td><p className="font-medium">{l.disciplina}</p><p className="text-xs text-ink-muted">{l.ano}/{l.semestre}</p></td>
                          <td className="text-ink-soft">{l.situacao}</td>
                          <td className="text-right tabular-nums">{l.matriculados}</td>
                          <td className="text-right tabular-nums">{l.respondentes}</td>
                          <td className="text-right tabular-nums">{l.pendentes}</td>
                          <td className="text-right font-semibold tabular-nums">{formatarNumero(l.taxaAdesao, 1)}%</td>
                          <td><span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${NIVEL[l.nivelAdesao] ?? ''}`}>{l.nivelAdesao}</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </Card>
            </>
          );
        }}
      </Estado>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card titulo="Alunos pendentes" descricao="Matriculados sem registro em participacao_avaliacao (anti-join)">
          <Estado estado={pendentes}>
            {(linhas) => (
              <div className="max-h-96 overflow-auto">
                <table className="table-base">
                  <thead><tr><th>Aluno</th><th>Matrícula</th><th>Disciplina</th></tr></thead>
                  <tbody>
                    {linhas.map((l) => (
                      <tr key={`${l.idAvaliacao}-${l.matricula}`}>
                        <td className="font-medium">{l.aluno}</td>
                        <td className="text-ink-soft">{l.matricula}</td>
                        <td className="text-ink-soft">{abreviar(l.disciplina)} · {l.ano}/{l.semestre}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Estado>
        </Card>
        <Card titulo="Questões não utilizadas" descricao="Banco de questões sem vínculo em avaliacao_questao (LEFT JOIN … IS NULL)">
          <Estado estado={naoUsadas}>
            {(linhas) => (
              <ul className="flex flex-col gap-2">
                {linhas.map((q) => (
                  <li key={q.idQuestao} className="rounded-xl border border-line px-3 py-2 text-sm">
                    <span className="text-xs text-ink-muted">Q{q.idQuestao}</span> <span className="text-ink">{q.enunciado}</span>
                    <span className="mt-1 flex gap-1"><Etiqueta>{ROTULO_TIPO[q.tipo]}</Etiqueta><Etiqueta>{ROTULO_CATEGORIA[q.categoria]}</Etiqueta></span>
                  </li>
                ))}
              </ul>
            )}
          </Estado>
        </Card>
      </div>
    </div>
  );
}

// ------------------------------------------------------------------ Comentários anônimos

function Comentarios({ avaliacoes }) {
  const [idAvaliacao, setIdAvaliacao] = useState('');
  const id = idAvaliacao || avaliacoes[0]?.idAvaliacao;
  const estado = useRelatorio(() => (id ? api.relatorio.comentarios(id) : Promise.resolve([])), [id]);

  const grupos = useMemo(() => {
    const m = new Map();
    (estado.dados ?? []).forEach((c) => {
      if (!m.has(c.idQuestao)) m.set(c.idQuestao, { enunciado: c.enunciado, textos: [] });
      m.get(c.idQuestao).textos.push(c.texto);
    });
    return [...m.values()];
  }, [estado.dados]);

  return (
    <Card
      titulo="Comentários anônimos (texto livre)"
      descricao="Sem identificador de submissão e ordenados alfabeticamente — não é possível ligar um comentário a quem respondeu"
      acoes={
        <select className="input w-auto" value={id ?? ''} onChange={(e) => setIdAvaliacao(e.target.value)} aria-label="Avaliação">
          {avaliacoes.map((a) => <option key={a.idAvaliacao} value={a.idAvaliacao}>{a.titulo}</option>)}
        </select>
      }
    >
      <Estado estado={estado}>
        {() => (
          <div className="flex flex-col gap-5">
            {grupos.map((g) => (
              <div key={g.enunciado}>
                <p className="mb-2 text-sm font-semibold text-ink">{g.enunciado} <span className="font-normal text-ink-muted">({g.textos.length})</span></p>
                <ul className="grid gap-2 sm:grid-cols-2">
                  {g.textos.map((t, i) => (
                    <li key={i} className="flex gap-2 rounded-xl bg-secondary px-3 py-2 text-sm text-ink-soft">
                      <MessageSquareQuote className="mt-0.5 h-4 w-4 shrink-0 text-accent" aria-hidden /> {t}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </Estado>
    </Card>
  );
}

// ------------------------------------------------------------------ página

const RECURSOS = {
  r1: ['JOIN', 'GROUP BY', 'AVG', 'COUNT', 'SUM(CASE WHEN)', 'WHERE período'],
  r2: ['JOIN', 'Subconsulta correlacionada', 'Tabela derivada', 'GROUP BY', 'HAVING', 'WHERE'],
  r3: ['CTE (WITH)', 'JOIN', 'LEFT JOIN', 'RANK() OVER', 'ORDER BY', 'FETCH FIRST 10'],
  r4: ['LEFT JOIN', 'Anti-join (IS NULL)', 'COUNT', 'CASE WHEN', 'NULLIF', 'WHERE'],
  com: ['JOIN', 'WHERE', 'ORDER BY'],
};

export default function RelatoriosPage() {
  const { paleta } = useTheme();
  const [aba, setAba] = useState('r1');
  const [filtroForm, setFiltroForm] = useState({ ano: '', semestre: '', idDisciplina: '' });
  const [base, setBase] = useState({ disciplinas: [], ofertas: [], avaliacoes: [] });
  const [erro, setErro] = useState('');

  useEffect(() => {
    Promise.all([api.disciplinas(), api.ofertas(), api.avaliacoes()])
      .then(([disciplinas, ofertas, avaliacoes]) => setBase({ disciplinas, ofertas, avaliacoes }))
      .catch((e) => setErro(e.message));
  }, []);

  // Objeto estável: só muda quando um filtro muda (dispara o recarregamento dos relatórios).
  const filtro = useMemo(() => ({ ...filtroForm }), [filtroForm.ano, filtroForm.semestre, filtroForm.idDisciplina]); // eslint-disable-line react-hooks/exhaustive-deps
  const anos = [...new Set(base.ofertas.map((o) => o.ano))].sort();

  // A cor acompanha a disciplina (ordem fixa por código), nunca a posição no resultado filtrado.
  const coresDisciplina = useMemo(() => {
    const ordem = base.disciplinas.map((d) => d.nome);
    return (nome) => paleta.series[Math.max(0, ordem.indexOf(nome)) % paleta.series.length];
  }, [base.disciplinas, paleta]);

  return (
    <>
      <PageHeader titulo="Relatórios" subtitulo="RF7 — consultas analíticas de database/relatorios.sql, com tabela e gráfico." />

      <div className="mb-4 grid gap-2 sm:grid-cols-3">
        <select className="input" value={filtroForm.ano} onChange={(e) => setFiltroForm({ ...filtroForm, ano: e.target.value })} aria-label="Ano">
          <option value="">Todos os anos</option>
          {anos.map((a) => <option key={a} value={a}>{a}</option>)}
        </select>
        <select className="input" value={filtroForm.semestre} onChange={(e) => setFiltroForm({ ...filtroForm, semestre: e.target.value })} aria-label="Semestre">
          <option value="">Todos os semestres</option>
          <option value="1">1º semestre</option>
          <option value="2">2º semestre</option>
        </select>
        <select className="input" value={filtroForm.idDisciplina} onChange={(e) => setFiltroForm({ ...filtroForm, idDisciplina: e.target.value })} aria-label="Disciplina">
          <option value="">Todas as disciplinas</option>
          {base.disciplinas.map((d) => <option key={d.idDisciplina} value={d.idDisciplina}>{d.codigo} — {d.nome}</option>)}
        </select>
      </div>

      <div className="mb-4 flex gap-1 overflow-x-auto border-b border-line" role="tablist">
        {ABAS.map((a) => (
          <button key={a.id} type="button" role="tab" aria-selected={aba === a.id} onClick={() => setAba(a.id)}
            className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-semibold transition-colors ${
              aba === a.id ? 'border-accent text-ink' : 'border-transparent text-ink-muted hover:text-ink'}`}>
            {a.titulo}
          </button>
        ))}
      </div>

      <div className="mb-5"><RecursosSql itens={RECURSOS[aba]} /></div>
      <Alerta tipo="erro">{erro}</Alerta>

      {aba === 'r1' && <Relatorio1 filtro={filtro} coresDisciplina={coresDisciplina} />}
      {aba === 'r2' && <Relatorio2 filtro={filtro} />}
      {aba === 'r3' && <Relatorio3 filtro={filtro} />}
      {aba === 'r4' && <Relatorio4 filtro={filtro} />}
      {aba === 'com' && (base.avaliacoes.length ? <Comentarios avaliacoes={base.avaliacoes} /> : <Carregando />)}
    </>
  );
}
