package comunicacao.model.enums;

/**
 * Os dois formatos de conversa, e a diferença não é cosmética.
 *
 * `DIRETA` é entre pessoas nomeadas: quem abre escolhe com quem fala, então
 * precisa conseguir listar usuários — o que só a gestão consegue no auth-api.
 *
 * `ADMINISTRACAO` é o morador falando com *a administração*, não com uma
 * pessoa. É o caso real: ninguém quer descobrir o nome do síndico para relatar
 * um vazamento, e a conversa precisa sobreviver à troca de síndico. Do lado da
 * gestão a visibilidade é **por regra** — mesmo condomínio e perfil de gestão —
 * e não por linha em `conversa_participantes`.
 *
 * O segundo formato também resolve um limite concreto: o auth-api recorta a
 * listagem de usuários que o morador alcança na unidade dele, e o síndico não
 * está nela. Sem `ADMINISTRACAO` não haveria destinatário a escolher.
 */
public enum TipoConversa {
    DIRETA,
    ADMINISTRACAO,
}
