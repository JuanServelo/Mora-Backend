CREATE DATABASE mora;

USE mora;

CREATE TABLE tb_meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    data_hora_inicio DATETIME(6) NOT NULL,
    data_hora_fim DATETIME(6) NOT NULL,
    id_organizador BIGINT NOT NULL,
    meet_link VARCHAR(255),
    google_event_id VARCHAR(255) UNIQUE
) ENGINE=InnoDB;

CREATE TABLE tb_meeting_convidados (
    meeting_id BIGINT NOT NULL,
    convidado_id BIGINT NOT NULL,
    CONSTRAINT fk_meeting_convidados FOREIGN KEY (meeting_id) REFERENCES tb_meetings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE tb_meetings
ADD COLUMN status ENUM('AGENDADA', 'CANCELADA', 'FINALIZADA');

CREATE TABLE tb_atas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL UNIQUE,
    topicos_discutidos TEXT NOT NULL,
    decisoes_tomadas TEXT NOT NULL,
    data_publicacao DATETIME(6) NOT NULL,
    CONSTRAINT fk_ata_meeting FOREIGN KEY (meeting_id) REFERENCES tb_meetings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE tb_ata_presentes (
    ata_id BIGINT NOT NULL,
    morador_id BIGINT NOT NULL,
    CONSTRAINT fk_ata_presentes FOREIGN KEY (ata_id) REFERENCES tb_atas(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE tb_polls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    meeting_id BIGINT NOT NULL,

    CONSTRAINT fk_poll_meeting FOREIGN KEY (meeting_id) REFERENCES tb_meetings(id)
);

CREATE TABLE tb_poll_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(100) NOT NULL,
    poll_id BIGINT NOT NULL,

    CONSTRAINT fk_option_poll FOREIGN KEY (poll_id) REFERENCES tb_polls(id) ON DELETE CASCADE
);

CREATE TABLE tb_poll_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    poll_id BIGINT NOT NULL,
    poll_option_id BIGINT NOT NULL,

    CONSTRAINT fk_vote_poll FOREIGN KEY (poll_id) REFERENCES tb_polls(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_option FOREIGN KEY (poll_option_id) REFERENCES tb_poll_options(id) ON DELETE CASCADE,

    CONSTRAINT uk_vote_usuario_poll UNIQUE (poll_id, usuario_id)
);

ALTER TABLE tb_meeting_convidados
ADD COLUMN status_presenca VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';