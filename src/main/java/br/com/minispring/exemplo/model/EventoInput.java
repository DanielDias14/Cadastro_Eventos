package br.com.minispring.exemplo.model;

import br.com.minispring.framework.validation.NotBlank;
import br.com.minispring.framework.validation.Positive;

/** DTO de entrada separado do modelo: o cliente não escolhe o ID. */

public record EventoInput(@NotBlank String titulo, @Positive int participantes) {}
