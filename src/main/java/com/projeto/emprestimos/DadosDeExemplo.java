package com.projeto.emprestimos;

import com.projeto.emprestimos.model.Colaborador;
import com.projeto.emprestimos.model.Emprestimo;
import com.projeto.emprestimos.model.Equipamento;
import com.projeto.emprestimos.model.TipoEquipamento;
import com.projeto.emprestimos.repository.ColaboradorRepository;
import com.projeto.emprestimos.repository.EmprestimoRepository;
import com.projeto.emprestimos.repository.EquipamentoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// Dados de exemplo para demonstrar o sistema, incluindo um empréstimo atrasado.
// Só roda com o perfil "demo" (--spring.profiles.active=demo) e num banco vazio,
// então não interfere no Render nem no plano de testes.
@Component
@Profile("demo")
public class DadosDeExemplo implements CommandLineRunner {

    private final EquipamentoRepository equipamentoRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final EmprestimoRepository emprestimoRepository;

    public DadosDeExemplo(EquipamentoRepository equipamentoRepository,
                          ColaboradorRepository colaboradorRepository,
                          EmprestimoRepository emprestimoRepository) {
        this.equipamentoRepository = equipamentoRepository;
        this.colaboradorRepository = colaboradorRepository;
        this.emprestimoRepository = emprestimoRepository;
    }

    @Override
    public void run(String... args) {
        // Banco com dados: não mexe em nada
        if (equipamentoRepository.count() > 0) {
            return;
        }

        Equipamento monitor = equipamentoRepository.save(novoEquipamento("MN-001", "Monitor LG 24", TipoEquipamento.MONITOR));
        equipamentoRepository.save(novoEquipamento("HS-001", "Headset JBL", TipoEquipamento.HEADSET));

        Colaborador ana = colaboradorRepository.save(novoColaborador("Ana Souza", "ana@elatech.com", "Analista"));
        colaboradorRepository.save(novoColaborador("Bruno Lima", "bruno@elatech.com", "Designer"));

        // Empréstimo feito há 28 dias: o prazo de 14 dias venceu há 14 dias,
        // então ele aparece como atrasado (R8), com as datas coerentes
        monitor.emprestar();
        equipamentoRepository.save(monitor);
        emprestimoRepository.save(new Emprestimo(monitor.getId(), ana.getId(), LocalDate.now().minusDays(28)));
    }

    private Equipamento novoEquipamento(String patrimonio, String nome, TipoEquipamento tipo) {
        Equipamento equipamento = new Equipamento();
        equipamento.setPatrimonio(patrimonio);
        equipamento.setNome(nome);
        equipamento.setTipo(tipo);
        return equipamento;
    }

    private Colaborador novoColaborador(String nome, String email, String cargo) {
        Colaborador colaborador = new Colaborador();
        colaborador.setNome(nome);
        colaborador.setEmail(email);
        colaborador.setCargo(cargo);
        return colaborador;
    }
}
