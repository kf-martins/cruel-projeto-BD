import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, BarChart3, ClipboardCheck, ClipboardList, FileQuestion, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { Alerta, Card, Carregando, PageHeader, SeloSituacao, formatarData, formatarNumero } from '../components/ui';

function Indicador({ icone: Icone, rotulo, valor, detalhe }) {
  return (
    <div className="card flex flex-col gap-2 p-5">
      <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-ink-muted">
        <Icone className="h-4 w-4 text-accent" aria-hidden /> {rotulo}
      </div>
      <p className="font-display text-3xl font-bold text-ink">{valor}</p>
      {detalhe && <p className="text-xs text-ink-soft">{detalhe}</p>}
    </div>
  );
}

/**
 * Painel de ADMIN e DOCENTE. Os dados já chegam filtrados pelo back-end: o docente recebe
 * apenas as avaliações, questões e adesão das ofertas que ministra.
 */
function PainelGestao() {
  const { isAdmin } = useAuth();
  const [dados, setDados] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    Promise.all([api.avaliacoes(), api.questoes(), api.relatorio.adesao({})])
      .then(([avaliacoes, questoes, adesao]) => setDados({ avaliacoes, questoes, adesao }))
      .catch((e) => setErro(e.message));
  }, []);

  if (erro) return <Alerta tipo="erro">{erro}</Alerta>;
  if (!dados) return <Carregando />;

  const abertas = dados.avaliacoes.filter((a) => a.situacao === 'ABERTA');
  const matriculados = dados.adesao.reduce((s, a) => s + a.matriculados, 0);
  const respondentes = dados.adesao.reduce((s, a) => s + a.respondentes, 0);
  const taxa = matriculados ? (100 * respondentes) / matriculados : 0;

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Indicador icone={ClipboardList} rotulo="Avaliações abertas" valor={abertas.length} detalhe={`${dados.avaliacoes.length} cadastradas`} />
        <Indicador icone={FileQuestion} rotulo="Questões ativas" valor={dados.questoes.filter((q) => q.ativa).length} detalhe={isAdmin ? `${dados.questoes.length} no banco` : `${dados.questoes.length} nas suas avaliações`} />
        <Indicador icone={ClipboardCheck} rotulo="Submissões anônimas" valor={respondentes} detalhe={`de ${matriculados} alunos elegíveis`} />
        <Indicador icone={Users} rotulo="Adesão geral" valor={`${formatarNumero(taxa, 1)}%`} detalhe="respondentes / matriculados" />
      </div>
      <Card
        className="mt-6"
        titulo={isAdmin ? 'Avaliações em andamento' : 'Suas avaliações'}
        acoes={
          <Link to="/relatorios" className="btn-outline">
            <BarChart3 className="h-4 w-4" /> Ver relatórios
          </Link>
        }
      >
        <div className="overflow-x-auto">
          <table className="table-base">
            <thead>
              <tr>
                <th>Avaliação</th>
                <th>Período</th>
                <th>Situação</th>
                <th className="text-right">Respondentes</th>
              </tr>
            </thead>
            <tbody>
              {dados.avaliacoes.map((a) => (
                <tr key={a.idAvaliacao}>
                  <td>
                    <p className="font-medium">{a.nomeDisciplina}</p>
                    <p className="text-xs text-ink-muted">{a.nomeDocente} · {a.ano}/{a.semestre} · turma {a.turma}</p>
                  </td>
                  <td className="whitespace-nowrap text-ink-soft">{formatarData(a.dataAvaliacao)} – {formatarData(a.dataEncerramento)}</td>
                  <td><SeloSituacao situacao={a.situacao} /></td>
                  <td className="text-right tabular-nums">{a.totalRespondentes} / {a.totalMatriculados}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </>
  );
}

function PainelAluno() {
  const [avaliacoes, setAvaliacoes] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    api.minhasAvaliacoes().then(setAvaliacoes).catch((e) => setErro(e.message));
  }, []);

  if (erro) return <Alerta tipo="erro">{erro}</Alerta>;
  if (!avaliacoes) return <Carregando />;
  const pendentes = avaliacoes.filter((a) => !a.respondida && a.situacao === 'ABERTA');

  return (
    <>
      <div className="grid gap-4 sm:grid-cols-3">
        <Indicador icone={ClipboardList} rotulo="Pendentes" valor={pendentes.length} detalhe="abertas e ainda não respondidas" />
        <Indicador icone={ClipboardCheck} rotulo="Respondidas" valor={avaliacoes.filter((a) => a.respondida).length} />
        <Indicador icone={Users} rotulo="Disciplinas" valor={avaliacoes.length} detalhe="em que você está matriculado(a)" />
      </div>
      <Link to="/minhas-avaliacoes" className="btn-primary mt-6">
        Ir para minhas avaliações <ArrowRight className="h-4 w-4" />
      </Link>
    </>
  );
}

export default function DashboardPage() {
  const { usuario, isGestao, isAdmin } = useAuth();
  const subtitulo = isAdmin
    ? 'Panorama das avaliações de disciplinas e docentes.'
    : isGestao
      ? 'Panorama das avaliações das disciplinas que você ministra.'
      : 'Sua opinião é anônima e ajuda a melhorar o curso.';
  return (
    <>
      <PageHeader
        titulo={`Olá, ${usuario.nome.split(' ').slice(0, usuario.perfil === 'ALUNO' ? 1 : 3).join(' ')}`}
        subtitulo={subtitulo}
      />
      {isGestao ? <PainelGestao /> : <PainelAluno />}
    </>
  );
}
