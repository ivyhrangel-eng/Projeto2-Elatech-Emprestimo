package com.projeto.emprestimos.service;

import com.projeto.emprestimos.model.Equipamento;
import com.projeto.emprestimos.model.TipoEquipamento;
import com.projeto.emprestimos.repository.EmprestimoRepository;
import com.projeto.emprestimos.repository.EquipamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EquipamentoService {

    private final EquipamentoRepository repository;
    private final EmprestimoRepository emprestimoRepository;

    public EquipamentoService(EquipamentoRepository repository,
                              EmprestimoRepository emprestimoRepository) {
        this.repository = repository;
        this.emprestimoRepository = emprestimoRepository;
    }

    public List<Equipamento> listar(TipoEquipamento tipo, Boolean disponivel) {
        if (tipo != null && disponivel != null) {
            return repository.findByTipoAndDisponivel(tipo, disponivel);
        }
        if (tipo != null) {
            return repository.findByTipo(tipo);
        }
        if (disponivel != null) {
            return repository.findByDisponivel(disponivel);
        }
        return repository.findAll();
    }

    public Equipamento buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));
    }

    public Equipamento cadastrar(Equipamento equipamento) {
        // R1: não existem dois equipamentos com o mesmo patrimônio
        if (repository.existsByPatrimonio(equipamento.getPatrimonio())) {
            throw new IllegalStateException("Patrimônio já cadastrado");
        }
        // R2: todo equipamento novo nasce disponível, mesmo que o JSON mande false
        equipamento.setDisponivel(true);
        return repository.save(equipamento);
    }

    public Equipamento atualizar(Long id, Equipamento dados) {
        // Busca o que está no banco e copia só nome e tipo
        // (assim o patrimonio e o disponivel não são apagados)
        Equipamento equipamento = buscar(id);
        equipamento.setNome(dados.getNome());
        equipamento.setTipo(dados.getTipo());
        return repository.save(equipamento);
    }

    public void remover(Long id) {
        Equipamento equipamento = buscar(id);
        // R9: equipamento com histórico de empréstimo não pode ser removido
        if (emprestimoRepository.existsByEquipamentoId(id)) {
            throw new IllegalStateException("Equipamento tem histórico de empréstimos e não pode ser removido");
        }
        repository.delete(equipamento);
    }
}
