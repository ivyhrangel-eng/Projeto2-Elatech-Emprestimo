package com.projeto.emprestimos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Equipamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String patrimonio;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEquipamento tipo;

    @Column(nullable = false)
    private boolean disponivel = true;

    public Equipamento() { }

    // R4: só se empresta equipamento disponível
    public void emprestar() {
        if (!disponivel) {
            throw new IllegalStateException("Equipamento indisponível");
        }
        disponivel = false;
    }

    // R7: ao devolver, o equipamento volta a ficar disponível
    public void devolver() {
        disponivel = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatrimonio() { return patrimonio; }
    public void setPatrimonio(String patrimonio) { this.patrimonio = patrimonio; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoEquipamento getTipo() { return tipo; }
    public void setTipo(TipoEquipamento tipo) { this.tipo = tipo; }

    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
}
