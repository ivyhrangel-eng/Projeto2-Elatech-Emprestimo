package com.projeto.emprestimos.service;

import com.projeto.emprestimos.model.Emprestimo;
import com.projeto.emprestimos.model.Equipamento;
import com.projeto.emprestimos.repository.ColaboradorRepository;
import com.projeto.emprestimos.repository.EmprestimoRepository;
import com.projeto.emprestimos.repository.EquipamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmprestimoService {

    private final EmprestimoRepository repository;
    private final EquipamentoRepository equipamentoRepository;
    private final ColaboradorRepository colaboradorRepository;

    public EmprestimoService(EmprestimoRepository repository,
                             EquipamentoRepository equipamentoRepository,
                             ColaboradorRepository colaboradorRepository) {
        this.repository = repository;
        this.equipamentoRepository = equipamentoRepository;
        this.colaboradorRepository = colaboradorRepository;
    }

    public List<Emprestimo> listar(Boolean atrasados) {
        // R8: com ?atrasados=true, só os atrasados (o null não quebra assim)
        if (Boolean.TRUE.equals(atrasados)) {
            return repository.findByDataDevolucaoIsNullAndDataPrevistaDevolucaoBefore(LocalDate.now());
        }
        return repository.findAll();
    }

    public Emprestimo buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Empréstimo não encontrado"));
    }

    public List<Emprestimo> listarDoColaborador(Long colaboradorId) {
        if (!colaboradorRepository.existsById(colaboradorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado");
        }
        return repository.findByColaboradorId(colaboradorId);
    }

    @Transactional
    public Emprestimo emprestar(Long equipamentoId, Long colaboradorId) {
        // Primeiro todas as conferências, depois as mudanças
        Equipamento equipamento = equipamentoRepository.findById(equipamentoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));

        if (!colaboradorRepository.existsById(colaboradorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado");
        }

        // R10: quem está com empréstimo atrasado não pega outro
        LocalDate hoje = LocalDate.now();
        if (repository.existsByColaboradorIdAndDataDevolucaoIsNullAndDataPrevistaDevolucaoBefore(colaboradorId, hoje)) {
            throw new IllegalStateException("Colaborador tem empréstimo atrasado e não pode pegar outro equipamento");
        }

        // R4 (dentro do emprestar) e R5: o equipamento fica indisponível
        equipamento.emprestar();
        // R5: as datas são calculadas no construtor
        Emprestimo emprestimo = new Emprestimo(equipamentoId, colaboradorId, hoje);

        equipamentoRepository.save(equipamento);
        return repository.save(emprestimo);
    }

    @Transactional
    public Emprestimo devolver(Long id) {
        Emprestimo emprestimo = buscar(id);

        // R6 (dentro do devolver) e R7: guarda a data de hoje
        emprestimo.devolver(LocalDate.now());

        // R7: o equipamento volta a ficar disponível
        Equipamento equipamento = equipamentoRepository.findById(emprestimo.getEquipamentoId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));
        equipamento.devolver();

        equipamentoRepository.save(equipamento);
        return repository.save(emprestimo);
    }
}
