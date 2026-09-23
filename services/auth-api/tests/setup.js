/**
 * Ambiente mínimo para o app subir dentro do runner.
 *
 * Definido aqui, e não num `.env.test`, porque um arquivo a mais é um arquivo a
 * mais para esquecer de versionar — e porque nenhum destes valores é segredo:
 * são o bastante para o Express montar, e nada além disso.
 *
 * `NODE_ENV=test` importa: `ehProducao()` passa a ser falso, então o cookie de
 * sessão não exige HTTPS e a ausência de `SESSION_SECRET` não derruba nada.
 */
process.env.NODE_ENV = 'test';
process.env.JWT_SECRET ??= 'segredo-de-teste-com-tamanho-suficiente';
process.env.SESSION_SECRET ??= 'sessao-de-teste-com-tamanho-suficiente';
process.env.FRONTEND_URL ??= 'http://localhost:5173';

// Sem isso, cada arquivo de teste imprime o aviso do seed e o do OAuth.
process.env.ADMIN_SEED_EMAIL ??= 'admin@teste.local';
process.env.ADMIN_SEED_PASSWORD ??= 'SenhaDeTeste123';
