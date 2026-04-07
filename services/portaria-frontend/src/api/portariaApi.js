const BASE = '/api'

async function req(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, options)
  if (!res.ok) {
    const err = await res.json().catch(() => ({ erro: 'Erro desconhecido' }))
    throw new Error(err.erro || `HTTP ${res.status}`)
  }
  if (res.status === 204) return null
  return res.json()
}

const json = (method, body) => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
})

const text = (method, body) => ({
  method,
  headers: { 'Content-Type': 'text/plain' },
  body,
})

export const api = {
  moradores: {
    listar:    ()        => req('/moradores'),
    listarTodos: ()      => req('/moradores/todos'),
    buscar:    (id)      => req(`/moradores/${id}`),
    cadastrar: (data)    => req('/moradores/cadastrar', json('POST', data)),
    atualizar: (id, data)=> req(`/moradores/${id}`, json('PUT', data)),
    desativar: (id)      => req(`/moradores/${id}`, { method: 'DELETE' }),
  },

  funcionarios: {
    listar:    ()        => req('/funcionarios'),
    listarTodos: ()      => req('/funcionarios/todos'),
    buscar:    (id)      => req(`/funcionarios/${id}`),
    cadastrar: (data)    => req('/funcionarios/cadastrar', json('POST', data)),
    atualizar: (id, data)=> req(`/funcionarios/${id}`, json('PUT', data)),
    desativar: (id)      => req(`/funcionarios/${id}`, { method: 'DELETE' }),
  },

  visitantes: {
    listar:   ()   => req('/visitantes'),
    dentro:   ()   => req('/visitantes/dentro'),
    buscar:   (id) => req(`/visitantes/${id}`),
    entrada:  (data)=> req('/visitantes/entrada', json('POST', data)),
    saida:    (id) => req(`/visitantes/${id}/saida`, { method: 'POST' }),
  },

  carros: {
    listar:  ()    => req('/carros'),
    dentro:  ()    => req('/carros/dentro'),
    buscar:  (id)  => req(`/carros/${id}`),
    entrada: (data)=> req('/carros/entrada', json('POST', data)),
    saida:   (id)  => req(`/carros/${id}/saida`, { method: 'POST' }),
  },

  chaves: {
    listar:       ()           => req('/chaves'),
    disponiveis:  ()           => req('/chaves/disponiveis'),
    buscar:       (id)         => req(`/chaves/${id}`),
    cadastrar:    (data)       => req('/chaves/cadastrar', json('POST', data)),
    deletar:      (id)         => req(`/chaves/${id}`, { method: 'DELETE' }),
    retirar:      (id, data)   => req(`/chaves/${id}/retirar`, json('POST', data)),
    devolver:     (id)         => req(`/chaves/${id}/devolver`, { method: 'POST' }),
  },

  entregas: {
    listar:    ()           => req('/entregas'),
    pendentes: ()           => req('/entregas/pendentes'),
    buscar:    (id)         => req(`/entregas/${id}`),
    registrar: (data)       => req('/entregas/registrar', json('POST', data)),
    retirar:   (id, nome)   => req(`/entregas/${id}/retirar`, text('POST', nome)),
  },

  turnos: {
    listar:    ()   => req('/turnos'),
    buscar:    (id) => req(`/turnos/${id}`),
    iniciar:   (data)=> req('/turnos/iniciar', json('POST', data)),
    retomar:   (id) => req(`/turnos/${id}/retomar`, { method: 'POST' }),
    finalizar: (id) => req(`/turnos/${id}/finalizar`, { method: 'POST' }),
  },
}
