package com.mora.portaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.mora.portaria.entity.ArtigoConhecimento;
import com.mora.portaria.enums.CategoriaArtigo;

import java.util.List;
import java.util.UUID;

public interface ArtigoConhecimentoRepository extends JpaRepository<ArtigoConhecimento, UUID> {

    List<ArtigoConhecimento> findByPublicado(boolean publicado);

    List<ArtigoConhecimento> findByCategoria(CategoriaArtigo categoria);

    List<ArtigoConhecimento> findByCategoriaAndPublicado(CategoriaArtigo categoria, boolean publicado);

    List<ArtigoConhecimento> findByTituloContainingIgnoreCase(String titulo);
}

