package br.com.minispring.exemplo.service;

import br.com.minispring.framework.annotation.Service;
import br.com.minispring.framework.web.HttpException;
import br.com.minispring.exemplo.model.*;
import br.com.minispring.exemplo.repository.EventoRepository;
import br.com.minispring.exemplo.repository.PessoaRepository;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public final class PessoaService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final PessoaRepository repository;
    private final EventoRepository eventoRepository;

    public PessoaService(PessoaRepository repository, EventoRepository eventoRepository) {
        this.repository = repository;
        this.eventoRepository = eventoRepository;
    }

    public List<Pessoa> list(String nome) {

        String filter = nome.toLowerCase(Locale.ROOT);
        return repository.findAll().stream().filter(p -> p.nome().toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    public Pessoa get(long id) {
        return repository.findById(id).orElseThrow(this::notFound);
    }

    public Pessoa create(PessoaInput input) {
        validate(input);
        return repository.create(input.nome().trim(), input.email().trim(), input.eventoId());
    }

    public Pessoa update(long id, PessoaInput input) {
        validate(input);
        return repository.update(id, input.nome().trim(), input.email().trim(), input.eventoId()).orElseThrow(this::notFound);
    }

    public void delete(long id) {
        if (!repository.delete(id)) throw notFound();
    }

    // Validações acontecem antes de gravar: uma requisição rejeitada nunca altera os dados.
    private void validate(PessoaInput input) {
        if (!EMAIL.matcher(input.email().trim()).matches()) throw new HttpException(400, "E-mail inválido");
        if (eventoRepository.findById(input.eventoId()).isEmpty())
            throw new HttpException(422, "O evento informado não existe");
    }

    private HttpException notFound() {
        return new HttpException(404, "Pessoa não encontrada");
    }
}
