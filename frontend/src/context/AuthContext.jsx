import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, CHAVE_SESSAO, lerSessao } from '../services/api';

const AuthContext = createContext(null);

/** Sessão simplificada (seção 12 do edital): usuário autenticado guardado no localStorage. */
export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(lerSessao);

  const salvar = (u) => {
    try {
      if (u) localStorage.setItem(CHAVE_SESSAO, JSON.stringify(u));
      else localStorage.removeItem(CHAVE_SESSAO);
    } catch {
      /* sem armazenamento: a sessão dura até recarregar a página */
    }
    setUsuario(u);
  };

  const login = useCallback(async (loginInformado, senha) => {
    const u = await api.login(loginInformado, senha);
    salvar(u);
    return u;
  }, []);

  const logout = useCallback(() => salvar(null), []);

  useEffect(() => {
    const expirar = () => salvar(null);
    window.addEventListener('cruel:sessao-expirada', expirar);
    return () => window.removeEventListener('cruel:sessao-expirada', expirar);
  }, []);

  const valor = useMemo(
    () => ({
      usuario,
      login,
      logout,
      isAdmin: usuario?.perfil === 'ADMIN',
      isGestao: usuario?.perfil === 'ADMIN' || usuario?.perfil === 'DOCENTE',
      isAluno: usuario?.perfil === 'ALUNO',
    }),
    [usuario, login, logout],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
