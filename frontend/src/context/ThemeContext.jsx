import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

const CHAVE = 'cruel-tema';
const ThemeContext = createContext(null);

function temaInicial() {
  try {
    return localStorage.getItem(CHAVE) === 'light' ? 'light' : 'dark';
  } catch {
    return 'dark';
  }
}

/** Lê as cores de gráfico (CSS variables) do tema ativo — o Recharts precisa de valores concretos. */
function lerPaleta() {
  const css = getComputedStyle(document.documentElement);
  const v = (nome) => css.getPropertyValue(nome).trim();
  return {
    series: [v('--chart-1'), v('--chart-2'), v('--chart-3'), v('--chart-4')],
    grid: v('--chart-grid'),
    axis: v('--chart-axis'),
    card: v('--bg-card'),
    ink: v('--text-primary'),
    inkSoft: v('--text-secondary'),
    border: v('--border-subtle'),
  };
}

export function ThemeProvider({ children }) {
  const [tema, setTema] = useState(temaInicial);
  const [paleta, setPaleta] = useState(null);

  useEffect(() => {
    const raiz = document.documentElement;
    raiz.classList.add('theme-transition');
    raiz.classList.toggle('dark', tema === 'dark');
    try {
      localStorage.setItem(CHAVE, tema);
    } catch {
      /* armazenamento indisponível: o tema vale só para esta sessão */
    }
    setPaleta(lerPaleta());
    const t = setTimeout(() => raiz.classList.remove('theme-transition'), 350);
    return () => clearTimeout(t);
  }, [tema]);

  const alternarTema = useCallback(() => setTema((t) => (t === 'dark' ? 'light' : 'dark')), []);
  const valor = useMemo(
    () => ({ tema, alternarTema, paleta: paleta ?? lerPaleta() }),
    [tema, alternarTema, paleta],
  );

  return <ThemeContext.Provider value={valor}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  return useContext(ThemeContext);
}
