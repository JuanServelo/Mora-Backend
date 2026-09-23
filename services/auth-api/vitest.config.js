import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    // A API do Jest sem o `import` em cada arquivo. Os testes que já existiam
    // rodam sem alteração nenhuma.
    globals: true,
    environment: 'node',
    setupFiles: ['./tests/setup.js'],
    include: ['tests/**/*.test.js'],

    // Os testes de contrato compartilham um mock do model `User` por arquivo.
    // Em paralelo, um arquivo trocaria o mock debaixo do outro.
    fileParallelism: false,

    coverage: {
      provider: 'v8',
      reporter: ['text-summary', 'html', 'json-summary'],
      reportsDirectory: './coverage',
      include: ['routes/**', 'middleware/**', 'services/**', 'utils/**', 'constants/**'],
      // Migrações são scripts de uma vez só, disparados na subida contra um
      // banco real; cobri-las exigiria Postgres e não diria nada sobre a API.
      exclude: ['migrations/**', 'config/**', 'tests/**'],
    },
  },
});
