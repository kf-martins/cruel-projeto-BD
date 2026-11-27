import { useEffect, useMemo, useState } from 'react';
import { FileQuestion, Loader2, Plus, Search, Trash2, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { api } from '../services/api';
import { Alerta, Card, Carregando, Etiqueta, PageHeader, ROTULO_CATEGORIA, ROTULO_TIPO, Vazio } from '../components/ui';

const FORM_VAZIO = {
  enunciado: '',
  tipo: 'ESCALA_1_5',
  categoria: 'DIDATICA',
  alternativas: [
    { texto: '', valorNumerico: 1 },
    { texto: '', valorNumerico: 2 },
  ],
};

/** Formulário de nova questão (RF1). Múltipla escolha permite adicionar/remover alternativas. */
function FormularioQuestao({ onCriada, onCancelar }) {
  const [form, setForm] = useState(FORM_VAZIO);
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);
  const multipla = form.tipo === 'MULTIPLA_ESCOLHA';

  const alterarAlternativa = (i, campo, valor) =>
    setForm((f) => ({
      ...f,
      alternativas: f.alternativas.map((a, j) => (j === i ? { ...a, [campo]: valor } : a)),
    }));

  const enviar = async (e) => {
    e.preventDefault();
    setErro('');
    setEnviando(true);
    try {
      const corpo = {
        enunciado: form.enunciado,
        tipo: form.tipo,
        categoria: form.categoria,
        alternativas: multipla
          ? form.alternativas.map((a) => ({ texto: a.texto, valorNumerico: Number(a.valorNumerico) }))
          : [],
      };
      const criada = await api.criarQuestao(corpo);
      setForm(FORM_VAZIO);
      onCriada(criada);
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Card titulo="Nova questão" descricao="Será adicionada ao banco e poderá ser usada em qualquer avaliação." className="mb-6"
      acoes={<button type="button" className="btn-ghost px-2" onClick={onCancelar} aria-label="Fechar"><X className="h-4 w-4" /></button>}>
      <form onSubmit={enviar} className="grid gap-4 md:grid-cols-2">
        <div className="md:col-span-2">
          <label className="label" htmlFor="enunciado">Enunciado</label>
          <textarea id="enunciado" className="input min-h-[80px]" required maxLength={1000}
            value={form.enunciado} onChange={(e) => setForm({ ...form, enunciado: e.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="tipo">Tipo</label>
          <select id="tipo" className="input" value={form.tipo} onChange={(e) => setForm({ ...form, tipo: e.target.value })}>
            {Object.entries(ROTULO_TIPO).map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="categoria">Categoria</label>
          <select id="categoria" className="input" value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })}>
            {Object.entries(ROTULO_CATEGORIA).map(([v, r]) => <option key={v} value={v}>{r}</option>)}
          </select>
        </div>

        {multipla && (
          <div className="md:col-span-2">
            <p className="label">Alternativas (valor numérico usado nos relatórios: maior = melhor)</p>
            <div className="flex flex-col gap-2">
              {form.alternativas.map((a, i) => (
                <div key={i} className="flex items-center gap-2">
                  <span className="w-6 text-center text-xs font-semibold text-ink-muted">{i + 1}</span>
                  <input className="input" placeholder={`Alternativa ${i + 1}`} required value={a.texto}
                    onChange={(e) => alterarAlternativa(i, 'texto', e.target.value)} />
                  <input className="input w-24" type="number" required value={a.valorNumerico} aria-label="Valor numérico"
                    onChange={(e) => alterarAlternativa(i, 'valorNumerico', e.target.value)} />
                  <button type="button" className="btn-ghost px-2" disabled={form.alternativas.length <= 2}
                    aria-label="Remover alternativa"
                    onClick={() => setForm((f) => ({ ...f, alternativas: f.alternativas.filter((_, j) => j !== i) }))}>
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
            <button type="button" className="btn-outline mt-2" disabled={form.alternativas.length >= 10}
              onClick={() => setForm((f) => ({ ...f, alternativas: [...f.alternativas, { texto: '', valorNumerico: f.alternativas.length + 1 }] }))}>
              <Plus className="h-4 w-4" /> Adicionar alternativa
            </button>
          </div>
        )}

        <div className="md:col-span-2 flex flex-col gap-3">
          <Alerta tipo="erro">{erro}</Alerta>
          <div className="flex justify-end gap-2">
            <button type="button" className="btn-ghost" onClick={onCancelar}>Cancelar</button>
            <button type="submit" className="btn-primary" disabled={enviando}>
              {enviando && <Loader2 className="h-4 w-4 animate-spin" />} Salvar questão
            </button>
          </div>
        </div>
      </form>
    </Card>
  );
}

export default function QuestoesPage() {
  const { isAdmin } = useAuth();
  const [questoes, setQuestoes] = useState(null);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');
  const [mostrarForm, setMostrarForm] = useState(false);
  const [filtro, setFiltro] = useState({ busca: '', tipo: '', categoria: '', status: '' });

  const carregar = () => api.questoes().then(setQuestoes).catch((e) => setErro(e.message));
  useEffect(() => {
    carregar();
  }, []);

  const visiveis = useMemo(() => {
    if (!questoes) return [];
    const busca = filtro.busca.trim().toLowerCase();
    return questoes.filter((q) =>
      (!busca || q.enunciado.toLowerCase().includes(busca))
      && (!filtro.tipo || q.tipo === filtro.tipo)
      && (!filtro.categoria || q.categoria === filtro.categoria)
      && (!filtro.status || String(q.ativa) === filtro.status));
  }, [questoes, filtro]);

  const alternarStatus = async (q) => {
    setErro('');
    try {
      await api.statusQuestao(q.idQuestao, !q.ativa);
      setQuestoes((lista) => lista.map((x) => (x.idQuestao === q.idQuestao ? { ...x, ativa: !x.ativa } : x)));
    } catch (e) {
      setErro(e.message);
    }
  };

  return (
    <>
      <PageHeader
        titulo="Banco de questões"
        subtitulo="RF1 — questões de múltipla escolha, escala 1–5 e texto livre."
        acoes={isAdmin && !mostrarForm && (
          <button type="button" className="btn-primary" onClick={() => setMostrarForm(true)}>
            <Plus className="h-4 w-4" /> Nova questão
          </button>
        )}
      />

      {mostrarForm && (
        <FormularioQuestao
          onCancelar={() => setMostrarForm(false)}
          onCriada={(q) => {
            setQuestoes((lista) => [...(lista ?? []), q]);
            setMostrarForm(false);
            setSucesso(`Questão #${q.idQuestao} criada com sucesso.`);
          }}
        />
      )}

      <div className="mb-4 flex flex-col gap-3">
        <Alerta tipo="sucesso" onFechar={() => setSucesso('')}>{sucesso}</Alerta>
        <Alerta tipo="erro" onFechar={() => setErro('')}>{erro}</Alerta>
      </div>

      <div className="mb-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-ink-muted" aria-hidden />
          <input className="input pl-9" placeholder="Buscar enunciado…" value={filtro.busca}
            onChange={(e) => setFiltro({ ...filtro, busca: e.target.value })} aria-label="Buscar enunciado" />
        </div>
        <select className="input" value={filtro.tipo} onChange={(e) => setFiltro({ ...filtro, tipo: e.target.value })} aria-label="Filtrar por tipo">
          <option value="">Todos os tipos</option>
          {Object.entries(ROTULO_TIPO).map(([v, r]) => <option key={v} value={v}>{r}</option>)}
        </select>
        <select className="input" value={filtro.categoria} onChange={(e) => setFiltro({ ...filtro, categoria: e.target.value })} aria-label="Filtrar por categoria">
          <option value="">Todas as categorias</option>
          {Object.entries(ROTULO_CATEGORIA).map(([v, r]) => <option key={v} value={v}>{r}</option>)}
        </select>
        <select className="input" value={filtro.status} onChange={(e) => setFiltro({ ...filtro, status: e.target.value })} aria-label="Filtrar por status">
          <option value="">Ativas e inativas</option>
          <option value="true">Somente ativas</option>
          <option value="false">Somente inativas</option>
        </select>
      </div>

      {!questoes ? (
        <Carregando />
      ) : visiveis.length === 0 ? (
        <Vazio icone={FileQuestion} titulo="Nenhuma questão encontrada">Ajuste os filtros ou cadastre uma nova questão.</Vazio>
      ) : (
        <div className="flex flex-col gap-3">
          <p className="text-xs text-ink-muted">{visiveis.length} de {questoes.length} questões</p>
          {visiveis.map((q) => (
            <article key={q.idQuestao} className={`card p-4 ${q.ativa ? '' : 'opacity-60'}`}>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="mb-1 flex flex-wrap items-center gap-2">
                    <span className="text-xs font-semibold text-ink-muted">#{q.idQuestao}</span>
                    <Etiqueta>{ROTULO_TIPO[q.tipo]}</Etiqueta>
                    <Etiqueta>{ROTULO_CATEGORIA[q.categoria]}</Etiqueta>
                    {!q.ativa && <Etiqueta className="bg-bad-soft text-bad">Inativa</Etiqueta>}
                  </div>
                  <p className="text-sm font-medium text-ink">{q.enunciado}</p>
                  {q.alternativas.length > 0 && (
                    <ul className="mt-2 flex flex-wrap gap-1.5">
                      {q.alternativas.map((a) => (
                        <li key={a.idAlternativa} className="rounded-lg border border-line px-2 py-0.5 text-xs text-ink-soft">
                          {a.texto} <span className="text-ink-muted">({a.valorNumerico})</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
                {isAdmin && (
                  <button type="button" className="btn-outline px-3 py-1 text-xs" onClick={() => alternarStatus(q)}>
                    {q.ativa ? 'Desativar' : 'Reativar'}
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </>
  );
}
