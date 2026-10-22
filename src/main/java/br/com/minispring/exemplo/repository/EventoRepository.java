package br.com.minispring.exemplo.repository;

import br.com.minispring.exemplo.model.Evento;

import java.util.List;
import java.util.Optional;

/**
 * A aplicação depende de uma abstração; o container encontra a implementação.
 */
public interface EventoRepository {
    Evento create(String titulo, int participantes);

    List<Evento> findAll();

    Optional<Evento> findById(long id);

    Optional<Evento> update(long id, String titulo, int participantes);

    boolean delete(long id);
}
