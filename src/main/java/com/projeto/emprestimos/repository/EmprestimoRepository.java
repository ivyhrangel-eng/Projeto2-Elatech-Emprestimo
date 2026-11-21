package com.projeto.emprestimos.repository;

import com.projeto.emprestimos.model.Emprestimo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, Long> {

    // Histórico da pessoa
    List<Emprestimo> findByColaboradorId(Long colaboradorId);

    // R9: o equipamento já teve algum empréstimo?
    boolean existsByEquipamentoId(Long equipamentoId);

    // R8: filtro dos atrasados (não devolvidos e com a data prevista antes de "data")
    List<Emprestimo> findByDataDevolucaoIsNullAndDataPrevistaDevolucaoBefore(LocalDate data);

    // R10: essa pessoa tem algum empréstimo atrasado?
    boolean existsByColaboradorIdAndDataDevolucaoIsNullAndDataPrevistaDevolucaoBefore(
            Long colaboradorId, LocalDate data);
}
