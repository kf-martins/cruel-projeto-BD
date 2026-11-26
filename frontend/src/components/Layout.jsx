import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  BarChart3,
  ClipboardList,
  Coffee,
  FileQuestion,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldCheck,
  X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import ThemeToggle from './ThemeToggle';

const NAV = [
  { para: '/', rotulo: 'Início', icone: LayoutDashboard, perfis: ['ADMIN', 'DOCENTE', 'ALUNO'] },
  { para: '/questoes', rotulo: 'Questões', icone: FileQuestion, perfis: ['ADMIN', 'DOCENTE'] },
  { para: '/avaliacoes', rotulo: 'Avaliações', icone: ClipboardList, perfis: ['ADMIN', 'DOCENTE'] },
  { para: '/minhas-avaliacoes', rotulo: 'Minhas avaliações', icone: ClipboardList, perfis: ['ALUNO'] },
  { para: '/relatorios', rotulo: 'Relatórios', icone: BarChart3, perfis: ['ADMIN', 'DOCENTE'] },
];

const ROTULO_PERFIL = { ADMIN: 'Administrador', DOCENTE: 'Docente', ALUNO: 'Aluno' };

export default function Layout() {
  const { usuario, logout } = useAuth();
  const navigate = useNavigate();
  const [menuAberto, setMenuAberto] = useState(false);
  const itens = NAV.filter((n) => n.perfis.includes(usuario.perfil));

  const sair = () => {
    logout();
    navigate('/login');
  };

  const links = (
    <nav className="flex flex-col gap-1">
      {itens.map(({ para, rotulo, icone: Icone }) => (
        <NavLink
          key={para}
          to={para}
          end={para === '/'}
          onClick={() => setMenuAberto(false)}
          className={({ isActive }) =>
            `flex items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-colors ${
              isActive ? 'bg-accent text-on-accent' : 'text-ink-soft hover:bg-secondary hover:text-ink'
            }`
          }
        >
          <Icone className="h-4 w-4" aria-hidden />
          {rotulo}
        </NavLink>
      ))}
    </nav>
  );

  const marca = (
    <div className="flex items-center gap-2">
      <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent text-on-accent">
        <Coffee className="h-5 w-5" aria-hidden />
      </span>
      <div className="leading-tight">
        <p className="font-display text-lg font-bold text-ink">crUEL</p>
        <p className="text-[11px] text-ink-muted">Feedback anônimo, forte e sem açúcar</p>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-primary">
      {/* Barra superior (mobile) */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-line bg-secondary px-4 py-3 lg:hidden">
        {marca}
        <div className="flex items-center gap-2">
          <ThemeToggle />
          <button type="button" className="btn-ghost px-2" onClick={() => setMenuAberto((v) => !v)} aria-label="Abrir menu">
            {menuAberto ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </header>
      {menuAberto && (
        <div className="border-b border-line bg-secondary px-4 pb-4 lg:hidden">
          {links}
          <button type="button" onClick={sair} className="btn-ghost mt-2 w-full justify-start">
            <LogOut className="h-4 w-4" /> Sair
          </button>
        </div>
      )}

      <div className="flex">
        {/* Barra lateral (desktop) */}
        <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col justify-between border-r border-line bg-secondary p-5 lg:flex">
          <div className="flex flex-col gap-8">
            {marca}
            {links}
          </div>
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2 rounded-xl bg-accent-soft px-3 py-2 text-xs text-ink-soft">
              <ShieldCheck className="h-4 w-4 shrink-0 text-accent" aria-hidden />
              Respostas 100% anônimas
            </div>
            <div className="flex items-center justify-between">
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-ink">{usuario.nome}</p>
                <p className="text-xs text-ink-muted">{ROTULO_PERFIL[usuario.perfil]}</p>
              </div>
              <button type="button" onClick={sair} className="btn-ghost px-2" aria-label="Sair" title="Sair">
                <LogOut className="h-4 w-4" />
              </button>
            </div>
            <ThemeToggle className="self-start" />
          </div>
        </aside>

        <main className="min-w-0 flex-1 px-4 py-6 sm:px-8 lg:py-10">
          <div className="mx-auto max-w-6xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
