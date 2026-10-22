package br.com.minispring.exemplo.service;

import br.com.minispring.framework.annotation.Service;
import br.com.minispring.framework.web.HttpException;
import br.com.minispring.exemplo.model.*;
import br.com.minispring.exemplo.repository.EventoRepository;

import java.util.List;
import java.util.Locale;

@Service
public final class EventoService {
    private final EventoRepository repository;

    public EventoService(EventoRepository repository) {
        this.repository = repository;
    }

    public List<Evento> list(String titulo) {

        String filter = titulo.toLowerCase(Locale.ROOT);
        return repository.findAll().stream().filter(c -> c.titulo().toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    public Evento get(long id) {
        return repository.findById(id).orElseThrow(this::notFound);
    }

    public Evento create(EventoInput input) {
        validateBusinessRule(input);
        return repository.create(input.titulo().trim(), input.participantes());
    }

    public Evento update(long id, EventoInput input) {
        validateBusinessRule(input);
        return repository.update(id, input.titulo().trim(), input.participantes()).orElseThrow(this::notFound);
    }

    public void delete(long id) {
        if (!repository.delete(id)) throw notFound();
    }

    private void validateBusinessRule(EventoInput input) {
        if (input.participantes() > 5000) throw new HttpException(422, "Um evento pode ter no máximo 5000 participantes");
    }

    private HttpException notFound() {
        return new HttpException(404, "Evento não encontrado");
    }
}
