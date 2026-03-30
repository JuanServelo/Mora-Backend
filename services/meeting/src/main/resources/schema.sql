-- Cria a tabela de reuniões
CREATE TABLE tb_meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    descricao VARCHAR(500),
    data_hora_inicio DATETIME(6) NOT NULL,
    data_hora_fim DATETIME(6) NOT NULL,
    id_organizador BIGINT NOT NULL,
    meet_link VARCHAR(255),
    google_event_id VARCHAR(255) UNIQUE,
    status ENUM('AGENDADA', 'CANCELADA', 'FINALIZADA')
) ENGINE=InnoDB;

-- Cria a tabela de convidados da reunião
CREATE TABLE tb_meeting_convidados (
    meeting_id BIGINT NOT NULL,
    convidado_id BIGINT NOT NULL,
    CONSTRAINT fk_meeting_convidados FOREIGN KEY (meeting_id) REFERENCES tb_meetings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Cria a tabela principal da Ata
CREATE TABLE tb_atas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id BIGINT NOT NULL UNIQUE,
    topicos_discutidos TEXT NOT NULL,
    decisoes_tomadas TEXT NOT NULL,
    data_publicacao DATETIME(6) NOT NULL,
    CONSTRAINT fk_ata_meeting FOREIGN KEY (meeting_id) REFERENCES tb_meetings(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Cria a tabela de presentes na Ata
CREATE TABLE tb_ata_presentes (
    ata_id BIGINT NOT NULL,
    morador_id BIGINT NOT NULL,
    CONSTRAINT fk_ata_presentes FOREIGN KEY (ata_id) REFERENCES tb_atas(id) ON DELETE CASCADE
) ENGINE=InnoDB;