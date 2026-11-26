import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';

/** Alterna entre o tema escuro "Espresso" (padrão) e o claro "Latte". */
export default function ThemeToggle({ className = '' }) {
  const { tema, alternarTema } = useTheme();
  const escuro = tema === 'dark';
  return (
    <button
      type="button"
      onClick={alternarTema}
      className={`group inline-flex items-center gap-2 rounded-full border border-line bg-card px-1 py-1 text-xs font-semibold text-ink-soft transition-colors hover:text-ink ${className}`}
      aria-label={escuro ? 'Mudar para o tema claro (Latte)' : 'Mudar para o tema escuro (Espresso)'}
      title={escuro ? 'Tema claro (Latte)' : 'Tema escuro (Espresso)'}
    >
      <span className={`flex h-7 w-7 items-center justify-center rounded-full transition-all ${escuro ? 'bg-accent text-on-accent' : 'text-ink-muted'}`}>
        <Moon className="h-4 w-4" aria-hidden />
      </span>
      <span className={`flex h-7 w-7 items-center justify-center rounded-full transition-all ${!escuro ? 'bg-accent text-on-accent' : 'text-ink-muted'}`}>
        <Sun className="h-4 w-4" aria-hidden />
      </span>
    </button>
  );
}
