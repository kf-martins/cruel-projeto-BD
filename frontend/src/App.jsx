import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import { useAuth } from './context/AuthContext';
import AvaliacoesAdminPage from './pages/AvaliacoesAdminPage';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import MinhasAvaliacoesPage from './pages/MinhasAvaliacoesPage';
import QuestoesPage from './pages/QuestoesPage';
import RelatoriosPage from './pages/RelatoriosPage';
import ResponderAvaliacaoPage from './pages/ResponderAvaliacaoPage';

/** Exige login e (opcionalmente) um dos perfis informados. */
function Protegida({ perfis, children }) {
  const { usuario } = useAuth();
  if (!usuario) return <Navigate to="/login" replace />;
  if (perfis && !perfis.includes(usuario.perfil)) return <Navigate to="/" replace />;
  return children;
}

const GESTAO = ['ADMIN', 'DOCENTE'];

export default function App() {
  const { usuario } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={usuario ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route
        element={
          <Protegida>
            <Layout />
          </Protegida>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="questoes" element={<Protegida perfis={GESTAO}><QuestoesPage /></Protegida>} />
        <Route path="avaliacoes" element={<Protegida perfis={GESTAO}><AvaliacoesAdminPage /></Protegida>} />
        <Route path="relatorios" element={<Protegida perfis={GESTAO}><RelatoriosPage /></Protegida>} />
        <Route path="minhas-avaliacoes" element={<Protegida perfis={['ALUNO']}><MinhasAvaliacoesPage /></Protegida>} />
        <Route path="responder/:id" element={<Protegida perfis={['ALUNO']}><ResponderAvaliacaoPage /></Protegida>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
