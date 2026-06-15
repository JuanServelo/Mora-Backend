export const PERFIS_DESCRICAO = {
  CONTRACTING_PROPERTY_MANAGER: {
    label: 'Administradora Contratante',
    categoria: 'plataforma',
    descricao: 'Empresa administradora de imóveis. Nível mais alto da plataforma.',
    permissoes: [
      'Criar e gerenciar clientes (condomínios)',
      'Cadastrar síndicos e administradores em qualquer condomínio',
      'Visualizar todos os usuários da plataforma',
      'Gerenciar todas as configurações do sistema',
    ],
    podeCadastrar: ['OPERATIONAL_SYNDIC', 'ADMINISTRATOR', 'DOORMAN', 'REAL_ESTATE_AGENCY', 'RESIDENT_OWNER', 'ABSENT_OWNER', 'LESSEE', 'OCCUPANT'],
  },

  CONTRACTING_SYNDIC: {
    label: 'Síndico Contratante',
    categoria: 'plataforma',
    descricao: 'Síndico profissional contratado pela administradora. Gerencia múltiplos condomínios.',
    permissoes: [
      'Gerenciar usuários do seu condomínio',
      'Cadastrar síndicos operacionais, administradores e porteiros',
      'Visualizar todos os usuários do condomínio',
    ],
    podeCadastrar: ['OPERATIONAL_SYNDIC', 'ADMINISTRATOR', 'DOORMAN', 'REAL_ESTATE_AGENCY', 'RESIDENT_OWNER'],
  },

  OPERATIONAL_SYNDIC: {
    label: 'Síndico Operacional',
    categoria: 'condominio',
    descricao: 'Síndico eleito pelos moradores. Gerencia o condomínio no dia a dia.',
    permissoes: [
      'Gerenciar usuários do condomínio',
      'Cadastrar porteiros e imobiliárias',
      'Visualizar moradores e estrutura do condomínio',
      'Aprovar e gerenciar reclamações',
    ],
    podeCadastrar: ['ADMINISTRATOR', 'DOORMAN', 'REAL_ESTATE_AGENCY', 'RESIDENT_OWNER'],
  },

  ADMINISTRATOR: {
    label: 'Administrador',
    categoria: 'condominio',
    descricao: 'Administrador do condomínio. Apoia o síndico na gestão.',
    permissoes: [
      'Gerenciar usuários do condomínio',
      'Cadastrar porteiros e proprietários',
      'Visualizar moradores e estrutura',
    ],
    podeCadastrar: ['DOORMAN', 'REAL_ESTATE_AGENCY', 'RESIDENT_OWNER'],
  },

  DOORMAN: {
    label: 'Porteiro',
    categoria: 'condominio',
    descricao: 'Porteiro do condomínio. Controla o acesso de moradores e visitantes.',
    permissoes: [
      'Registrar entrada e saída de visitantes',
      'Consultar dados básicos de moradores',
      'Registrar entregas',
      'Gerenciar chaves',
    ],
    podeCadastrar: [],
  },

  REAL_ESTATE_AGENCY: {
    label: 'Imobiliária',
    categoria: 'condominio',
    descricao: 'Imobiliária parceira do condomínio. Gerencia contratos de locação.',
    permissoes: [
      'Visualizar unidades disponíveis',
      'Consultar dados de locatários',
      'Gerenciar contratos relacionados ao condomínio',
    ],
    podeCadastrar: [],
  },

  RESIDENT_OWNER: {
    label: 'Proprietário Residente',
    categoria: 'unidade',
    descricao: 'Proprietário que mora no imóvel. Responsável financeiro da unidade.',
    permissoes: [
      'Cadastrar locatário (Lessee) na sua unidade',
      'Cadastrar ocupantes e hóspedes',
      'Transferir responsabilidade financeira ao locatário',
      'Visualizar dados da sua unidade',
    ],
    podeCadastrar: ['LESSEE', 'OCCUPANT', 'GUEST'],
  },

  ABSENT_OWNER: {
    label: 'Proprietário Ausente',
    categoria: 'unidade',
    descricao: 'Proprietário que não mora no imóvel. Transferiu a responsabilidade ao locatário.',
    permissoes: [
      'Cadastrar locatário na sua unidade',
      'Visualizar dados da sua unidade',
    ],
    podeCadastrar: ['LESSEE'],
  },

  LESSEE: {
    label: 'Locatário',
    categoria: 'unidade',
    descricao: 'Inquilino da unidade. Pode ser o responsável financeiro.',
    permissoes: [
      'Cadastrar ocupantes e hóspedes na sua unidade',
      'Visualizar dados da sua unidade',
    ],
    podeCadastrar: ['OCCUPANT', 'GUEST'],
  },

  OCCUPANT: {
    label: 'Ocupante',
    categoria: 'unidade',
    descricao: 'Pessoa que mora na unidade sem ser proprietário ou locatário (familiar, dependente).',
    permissoes: [
      'Visualizar dados da sua unidade',
      'Registrar visitantes',
    ],
    podeCadastrar: [],
  },

  GUEST: {
    label: 'Hóspede',
    categoria: 'unidade',
    descricao: 'Hóspede temporário da unidade. Sem acesso ao sistema.',
    permissoes: [
      'Identificação para controle de acesso pela portaria',
    ],
    podeCadastrar: [],
    semAcessoSistema: true,
  },
};

export const CATEGORIAS_PERFIS = {
  plataforma: {
    label: 'Nível Plataforma',
    descricao: 'Gerenciam múltiplos condomínios',
  },
  condominio: {
    label: 'Nível Condomínio',
    descricao: 'Atuam dentro de um condomínio específico',
  },
  unidade: {
    label: 'Nível Unidade',
    descricao: 'Vinculados a uma unidade (apartamento/casa)',
  },
};
