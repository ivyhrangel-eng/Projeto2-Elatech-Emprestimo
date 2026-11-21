package com.projeto.emprestimos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class Emprestimo {

    private static final int DIAS_DE_EMPRESTIMO = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long equipamentoId;

    @Column(nullable = false)
    private Long colaboradorId;

    @Column(nullable = false)
    private LocalDate dataEmprestimo;

    @Column(nullable = false)
    private LocalDate dataPrevistaDevolucao;

    private LocalDate dataDevolucao; // fica null até o equipamento voltar

    // O JPA exige o construtor vazio: não apagar
    public Emprestimo() { }

    // R5: guarda os ids, empresta hoje e prevê a devolução para hoje + 14 dias
    public Emprestimo(Long equipamentoId, Long colaboradorId, LocalDate hoje) {
        this.equipamentoId = equipamentoId;
        this.colaboradorId = colaboradorId;
        this.dataEmprestimo = hoje;
        this.dataPrevistaDevolucao = hoje.plusDays(DIAS_DE_EMPRESTIMO);
    }

    // R6: não devolve duas vezes · R7: guarda a data da devolução
    public void devolver(LocalDate data) {
        if (dataDevolucao != null) {
            throw new IllegalStateException("Empréstimo já devolvido");
        }
        this.dataDevolucao = data;
    }

    // R8: atrasado = ainda não devolvido e a data prevista já passou
    // (no próprio dia do prazo ainda não está atrasado)
    public boolean estaAtrasadoEm(LocalDate data) {
        return dataDevolucao == null && dataPrevistaDevolucao.isBefore(data);
    }

    // Aparece no JSON como "atrasado"
    public boolean isAtrasado() {
        return estaAtrasadoEm(LocalDate.now());
    }

    public Long getId() { return id; }
    public Long getEquipamentoId() { return equipamentoId; }
    public Long getColaboradorId() { return colaboradorId; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public LocalDate getDataPrevistaDevolucao() { return dataPrevistaDevolucao; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
}
