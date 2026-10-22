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

    // Dependemos da interface, permitindo trocar o armazenamento ou usar um fake nos testes.
    // A implementação concreta é escolhida pelo container durante a inicialização.
    public EventoService(EventoRepository repository) {
        this.repository = repository;
    }

    public List<Evento> list(String titulo) {
        // Normalizar os dois lados permite busca sem distinguir maiúsculas de minúsculas.
        // Locale.ROOT evita que a regra varie conforme o idioma configurado na máquina.
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

    // Esta regra pertence ao domínio; @NotBlank e @Positive validam a forma da entrada.
    // A validação do DTO só ocorre automaticamente no pipeline HTTP, não em chamadas diretas.
    private void validateBusinessRule(EventoInput input) {
        if (input.participantes() > 5000) throw new HttpException(422, "Um evento pode ter no máximo 5000 participantes");
    }

    // Simplificação didática: o service conhece HTTP. Uma evolução é lançar exceções
    // de domínio e traduzi-las para status apenas na camada web.
    private HttpException notFound() {
        return new HttpException(404, "Evento não encontrado");
    }
}
