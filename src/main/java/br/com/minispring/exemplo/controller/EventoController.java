package br.com.minispring.exemplo.controller;

import br.com.minispring.exemplo.model.Evento;
import br.com.minispring.exemplo.model.EventoInput;
import br.com.minispring.exemplo.service.EventoService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos")
public class EventoController {
    private final EventoService service;

    public EventoController(EventoService service) { this.service = service; }

    @GetMapping
    public List<Evento> list(@RequestParam(defaultValue = "") String titulo) { return service.list(titulo); }

    @GetMapping("/{id}")
    public Evento get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<Evento> create(@Valid @RequestBody EventoInput input) {
        Evento evento = service.create(input);
        return ResponseEntity.created(URI.create("/eventos/" + evento.id())).body(evento);
    }

    @PutMapping("/{id}")
    public Evento update(@PathVariable long id, @Valid @RequestBody EventoInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
