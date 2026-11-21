package com.projeto.emprestimos.service;

import com.projeto.emprestimos.model.Colaborador;
import com.projeto.emprestimos.repository.ColaboradorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ColaboradorService {

    private final ColaboradorRepository repository;

    public ColaboradorService(ColaboradorRepository repository) {
        this.repository = repository;
    }

    public List<Colaborador> listar() {
        return repository.findAll();
    }

    public Colaborador buscar(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Colaborador não encontrado"));
    }

    public Colaborador cadastrar(Colaborador colaborador) {
        // R3: não existem duas pessoas com o mesmo e-mail
        if (repository.existsByEmail(colaborador.getEmail())) {
            throw new IllegalStateException("E-mail já cadastrado");
        }
        return repository.save(colaborador);
    }
}
