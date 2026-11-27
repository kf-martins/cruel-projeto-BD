/**
 * Cliente HTTP da API REST (Spring Boot). A URL base vem de VITE_API_URL (.env).
 * A sessão simplificada é enviada nos cabeçalhos X-User-Id / X-User-Role;
 * o back-end confere o perfil no banco a partir do X-User-Id.
 */
const BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/$/, '');
export const CHAVE_SESSAO = 'cruel-sessao';

export class ApiError extends Error {
  constructor(status, mensagem) {
    super(mensagem);
    this.status = status;
  }
}

export function lerSessao() {
  try {
    return JSON.parse(localStorage.getItem(CHAVE_SESSAO)) ?? null;
  } catch {
    return null;
  }
}

async function requisicao(metodo, caminho, corpo) {
  const sessao = lerSessao();
  const headers = { Accept: 'application/json' };
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';
  if (sessao) {
    headers['X-User-Id'] = String(sessao.idUsuario);
    headers['X-User-Role'] = sessao.perfil;
  }

  let resposta;
  try {
    resposta = await fetch(`${BASE_URL}${caminho}`, {
      method: metodo,
      headers,
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Não foi possível conectar à API. Verifique se o back-end está em execução.');
  }

  if (resposta.status === 204) return null;
  const dados = await resposta.json().catch(() => null);
  if (!resposta.ok) {
    if (resposta.status === 401 && caminho !== '/auth/login') {
      window.dispatchEvent(new Event('cruel:sessao-expirada'));
    }
    throw new ApiError(resposta.status, dados?.mensagem || `Erro ${resposta.status}`);
  }
  return dados;
}

function query(params) {
  const q = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.append(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : '';
}

export const api = {
  login: (login, senha) => requisicao('POST', '/auth/login', { login, senha }),

  // Catálogo
  disciplinas: () => requisicao('GET', '/disciplinas'),
  ofertas: () => requisicao('GET', '/ofertas'),
  criarOferta: (dados) => requisicao('POST', '/ofertas', dados),
  alunosDaOferta: (idOferta) => requisicao('GET', `/ofertas/${idOferta}/alunos`),
  matricular: (idOferta, idsAlunos) => requisicao('POST', `/ofertas/${idOferta}/alunos`, { idsAlunos }),
  usuarios: (perfil) => requisicao('GET', `/usuarios${query({ perfil })}`),

  // RF1 — questões
  questoes: () => requisicao('GET', '/questoes'),
  criarQuestao: (dados) => requisicao('POST', '/questoes', dados),
  statusQuestao: (id, ativa) => requisicao('PATCH', `/questoes/${id}/status`, { ativa }),

  // RF2/RF3/RF5 — avaliações
  avaliacoes: () => requisicao('GET', '/avaliacoes'),
  minhasAvaliacoes: () => requisicao('GET', '/avaliacoes/minhas'),
  avaliacao: (id) => requisicao('GET', `/avaliacoes/${id}`),
  criarAvaliacao: (dados) => requisicao('POST', '/avaliacoes', dados),
  statusAvaliacao: (id, ativa) => requisicao('PATCH', `/avaliacoes/${id}/status`, { ativa }),

  // RF5/RF6 — submissão anônima
  responder: (idAvaliacao, itens) => requisicao('POST', '/respostas', { idAvaliacao, itens }),

  // RF7 — relatórios
  relatorio: {
    mediaDisciplinas: (f) => requisicao('GET', `/relatorios/media-disciplinas${query(f)}`),
    comparativo: (f) => requisicao('GET', `/relatorios/comparativo-categorias${query(f)}`),
    ranking: (f, ordem) => requisicao('GET', `/relatorios/ranking-questoes${query({ ...f, ordem })}`),
    adesao: (f) => requisicao('GET', `/relatorios/adesao${query(f)}`),
    pendentes: (f) => requisicao('GET', `/relatorios/pendentes${query(f)}`),
    questoesNaoUtilizadas: () => requisicao('GET', '/relatorios/questoes-nao-utilizadas'),
    comentarios: (idAvaliacao) => requisicao('GET', `/relatorios/comentarios/${idAvaliacao}`),
  },
};
