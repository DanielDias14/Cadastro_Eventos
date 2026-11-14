package br.com.minispring.exemplo.controller;

import br.com.minispring.exemplo.model.Pessoa;
import br.com.minispring.exemplo.model.PessoaInput;
import br.com.minispring.exemplo.service.PessoaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pessoas")
public class PessoaController {
    private final PessoaService service;

    public PessoaController(PessoaService service) { this.service = service; }

    @GetMapping
    public List<Pessoa> list(@RequestParam(defaultValue = "") String nome) { return service.list(nome); }

    @GetMapping("/{id}")
    public Pessoa get(@PathVariable long id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<Pessoa> create(@Valid @RequestBody PessoaInput input) {
        Pessoa pessoa = service.create(input);
        return ResponseEntity.created(URI.create("/pessoas/" + pessoa.id())).body(pessoa);
    }

    @PutMapping("/{id}")
    public Pessoa update(@PathVariable long id, @Valid @RequestBody PessoaInput input) {
        return service.update(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
