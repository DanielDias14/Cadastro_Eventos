package br.com.minispring.exemplo.repository;

import br.com.minispring.framework.annotation.Repository;
import br.com.minispring.exemplo.model.Evento;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public final class InMemoryEventoRepository implements EventoRepository {
    // Beans singleton são compartilhados entre requisições concorrentes.
    private final ConcurrentHashMap<Long, Evento> events = new ConcurrentHashMap<>();
    // incrementAndGet gera IDs de forma atômica; um simples long++ pode perder incrementos
    // quando duas threads executam ao mesmo tempo. A sequência reinicia junto com a aplicação.
    private final AtomicLong sequence = new AtomicLong();

    public Evento create(String titulo, int participantes) {
        long id = sequence.incrementAndGet();
        var evento = new Evento(id, titulo, participantes);
        events.put(id, evento);
        return evento;
    }

    // Ordenamos porque ConcurrentHashMap não garante ordem de iteração. Esta listagem
    // não é um snapshot transacional: outras requisições podem modificar o mapa durante a leitura.
    public List<Evento> findAll() {
        return events.values().stream().sorted(Comparator.comparingLong(Evento::id)).toList();
    }

    public Optional<Evento> findById(long id) {
        return Optional.ofNullable(events.get(id));
    }

    // computeIfPresent consulta e substitui atomicamente para esta chave. Isso evita
    // recriar um evento removido por outra thread entre uma consulta e uma gravação.
    // Essa garantia por operação não equivale a uma transação envolvendo vários eventos.
    public Optional<Evento> update(long id, String titulo, int participantes) {
        return Optional.ofNullable(events.computeIfPresent(id, (key, old) -> new Evento(id, titulo, participantes)));
    }

    public boolean delete(long id) {
        return events.remove(id) != null;
    }
}
