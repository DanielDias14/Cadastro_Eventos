package br.com.minispring.failurefixture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Componente exclusivo dos testes; nunca é incluído no JAR da aplicação. */
@RestController
public class FailureController {
    @GetMapping("/failure")
    public Object fail() { throw new IllegalStateException("detalhe-interno-do-teste"); }
}
