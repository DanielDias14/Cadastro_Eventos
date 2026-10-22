package br.com.minispring.exemplo.controller;

import br.com.minispring.framework.annotation.*;
import br.com.minispring.framework.web.HttpResult;
import br.com.minispring.exemplo.model.*;
import br.com.minispring.exemplo.service.EventoService;
import java.util.List;

@RestController("/eventos")
public final class EventoController {
    private final EventoService service;
    // DI por construtor: o container fornece o service. O controller não escolhe
    // como construí-lo, e a dependência fica explícita para quem lê ou testa a classe.
    public EventoController(EventoService service) { this.service = service; }

    // GET /eventos?titulo=java. Sem query param, o default vazio lista todos os eventos.
    // Retornamos objetos Java; o dispatcher usa Jackson para produzir JSON.
    @Route(method = "GET")
    public List<Evento> list(@RequestParam("titulo") String titulo) { return service.list(titulo); }

    // O "id" da anotação corresponde ao {id} da rota. O dispatcher converte texto em long.
    @Route(method = "GET", path = "/{id}")
    public Evento get(@PathVariable("id") long id) { return service.get(id); }

    // Binding por requisição: input vem do JSON, não do container IoC.
    // A validação das anotações do DTO já ocorreu quando este método começa.
    @Route(method = "POST")
    public HttpResult create(@RequestBody EventoInput input) {
        Evento evento = service.create(input);
        return HttpResult.created("/eventos/" + evento.id(), evento);
    }

    // Uma mesma chamada combina dados da URL (identidade) e do corpo (novos valores).
    @Route(method = "PUT", path = "/{id}")
    public Evento update(@PathVariable("id") long id, @RequestBody EventoInput input) {
        return service.update(id, input);
    }

    // Não há representação para devolver após excluir; usamos explicitamente 204 sem corpo.
    @Route(method = "DELETE", path = "/{id}")
    public HttpResult delete(@PathVariable("id") long id) {
        service.delete(id);
        return HttpResult.noContent();
    }
}
