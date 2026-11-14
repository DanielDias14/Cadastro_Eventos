package br.com.minispring.exemplo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** DTO de entrada separado do modelo: o cliente não escolhe o ID. */
public record PessoaInput(@NotBlank String nome, @NotBlank String email, @Positive long eventoId) {}
