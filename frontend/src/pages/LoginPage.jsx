import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Coffee, GraduationCap, Loader2, LogIn, ShieldCheck, UserCog, Presentation } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from '../components/ThemeToggle';
import { Alerta } from '../components/ui';

/** Perfis de demonstração (carga.sql — senha de todos: 123456). */
const ACESSO_RAPIDO = [
  { rotulo: 'Entrar como Coordenador Admin', detalhe: 'coordenacao.dc@uel.br', login: 'coordenacao.dc@uel.br', icone: UserCog },
  { rotulo: 'Entrar como Aluno — Ana Silva', detalhe: 'ana.silva@uel.br', login: 'ana.silva@uel.br', icone: GraduationCap },
  { rotulo: 'Entrar como Docente — Prof. Anderson', detalhe: 'anderson.santos@uel.br', login: 'anderson.santos@uel.br', icone: Presentation },
];

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ login: '', senha: '' });
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  const entrar = async (credenciais) => {
    setErro('');
    setEnviando(true);
    try {
      const u = await login(credenciais.login, credenciais.senha);
      navigate(u.perfil === 'ALUNO' ? '/minhas-avaliacoes' : '/');
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-primary px-4 py-10">
      <ThemeToggle className="absolute right-4 top-4" />
      <div className="grid w-full max-w-4xl gap-6 md:grid-cols-2">
        <div className="flex flex-col justify-center gap-5">
          <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-accent text-on-accent">
            <Coffee className="h-6 w-6" aria-hidden />
          </span>
          <div>
            <h1 className="font-display text-4xl font-bold text-ink">crUEL</h1>
            <p className="mt-1 text-sm text-ink-soft">Consulta e Relatório Unificado de Ensino e Lecionamento</p>
          </div>
          <p className="text-base text-ink-soft">
            Avaliação de disciplinas e docentes da UEL — <strong className="text-ink">feedback anônimo, forte e sem açúcar.</strong>
          </p>
          <div className="flex items-start gap-3 rounded-2xl border border-line bg-card p-4">
            <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-accent" aria-hidden />
            <p className="text-sm text-ink-soft">
              Quem respondeu e o que foi respondido ficam em tabelas separadas, sem nenhuma ligação entre elas.
              Cada aluno avalia cada disciplina <strong className="text-ink">uma única vez</strong> por semestre.
            </p>
          </div>
        </div>

        <div className="card p-6 sm:p-8">
          <h2 className="text-lg font-semibold text-ink">Entrar</h2>
          <p className="mb-5 text-xs text-ink-muted">Use seu e-mail institucional ou matrícula.</p>
          <form
            className="flex flex-col gap-4"
            onSubmit={(e) => {
              e.preventDefault();
              entrar(form);
            }}
          >
            <div>
              <label className="label" htmlFor="login">E-mail ou matrícula</label>
              <input
                id="login"
                className="input"
                autoComplete="username"
                value={form.login}
                onChange={(e) => setForm({ ...form, login: e.target.value })}
                placeholder="ana.silva@uel.br"
                required
              />
            </div>
            <div>
              <label className="label" htmlFor="senha">Senha</label>
              <input
                id="senha"
                type="password"
                className="input"
                autoComplete="current-password"
                value={form.senha}
                onChange={(e) => setForm({ ...form, senha: e.target.value })}
                required
              />
            </div>
            <Alerta tipo="erro">{erro}</Alerta>
            <button type="submit" className="btn-primary py-2.5" disabled={enviando}>
              {enviando ? <Loader2 className="h-4 w-4 animate-spin" /> : <LogIn className="h-4 w-4" />} Entrar
            </button>
          </form>

          <div className="my-6 flex items-center gap-3 text-xs text-ink-muted">
            <span className="h-px flex-1 bg-line" /> acesso rápido (demonstração) <span className="h-px flex-1 bg-line" />
          </div>
          <div className="flex flex-col gap-2">
            {ACESSO_RAPIDO.map(({ rotulo, detalhe, login: l, icone: Icone }) => (
              <button
                key={l}
                type="button"
                disabled={enviando}
                onClick={() => entrar({ login: l, senha: '123456' })}
                className="flex items-center gap-3 rounded-xl border border-line bg-primary px-4 py-3 text-left transition-colors hover:border-accent hover:bg-card-hover"
              >
                <Icone className="h-5 w-5 shrink-0 text-accent" aria-hidden />
                <span>
                  <span className="block text-sm font-semibold text-ink">{rotulo}</span>
                  <span className="block text-xs text-ink-muted">{detalhe} · senha 123456</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
