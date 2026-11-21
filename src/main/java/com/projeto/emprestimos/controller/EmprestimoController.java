package com.projeto.emprestimos.controller;

import com.projeto.emprestimos.model.Emprestimo;
import com.projeto.emprestimos.service.EmprestimoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {

    private final EmprestimoService service;

    public EmprestimoController(EmprestimoService service) {
        this.service = service;
    }

    // POST /emprestimos?equipamentoId=1&colaboradorId=2
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Emprestimo emprestar(@RequestParam Long equipamentoId, @RequestParam Long colaboradorId) {
        return service.emprestar(equipamentoId, colaboradorId);
    }

    @GetMapping
    public List<Emprestimo> listar(@RequestParam(required = false) Boolean atrasados) {
        return service.listar(atrasados);
    }

    @GetMapping("/{id}")
    public Emprestimo buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}/devolucao")
    public Emprestimo devolver(@PathVariable Long id) {
        return service.devolver(id);
    }
}
