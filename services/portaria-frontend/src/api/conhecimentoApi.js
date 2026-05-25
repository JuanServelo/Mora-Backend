import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8090',
  headers: { 'Content-Type': 'application/json' },
});

export const conhecimentoApi = {
  listarTodos: () => api.get('/conhecimento'),
  listarPublicados: () => api.get('/conhecimento/publicados'),
  listarPorCategoria: (categoria) => api.get(`/conhecimento/categoria/${categoria}`),
  buscarPorTitulo: (titulo) => api.get(`/conhecimento/buscar`, { params: { titulo } }),
  buscarPorId: (id) => api.get(`/conhecimento/${id}`),
  criar: (dados) => api.post('/conhecimento', dados),
  atualizar: (id, dados) => api.put(`/conhecimento/${id}`, dados),
  excluir: (id) => api.delete(`/conhecimento/${id}`),
};

export default api;
