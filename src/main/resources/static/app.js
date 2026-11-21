// =====================================================================
// Front do Balcão de equipamentos
// Conversa com a API usando fetch (o mesmo que o Postman faz, só que
// pelo navegador). Como esta página é servida pelo próprio Spring,
// os endereços começam direto com "/equipamentos", "/colaboradores"...
// =====================================================================

const NOMES_DOS_TIPOS = {
    NOTEBOOK: "Notebook", MONITOR: "Monitor", HEADSET: "Headset",
    TECLADO: "Teclado", MOUSE: "Mouse", OUTRO: "Outro"
};

// Guardamos as listas aqui para mostrar nomes em vez de ids nos empréstimos
let equipamentos = [];
let colaboradores = [];
let emprestimos = [];

// Id do equipamento que está sendo editado (null = modo cadastro)
let equipamentoEmEdicao = null;


// ---------------------------------------------------------------------
// 1. Função que chama a API
// ---------------------------------------------------------------------
async function api(metodo, caminho, corpo) {
    const opcoes = { method: metodo, headers: {} };

    if (corpo) {
        opcoes.headers["Content-Type"] = "application/json";
        opcoes.body = JSON.stringify(corpo);   // objeto JS -> texto JSON
    }

    const resposta = await fetch(caminho, opcoes);

    // Status 4xx ou 5xx: pega a mensagem da regra que a API mandou
    if (!resposta.ok) {
        let mensagem = "Erro " + resposta.status;
        try {
            const erro = await resposta.json();
            if (erro.message) mensagem = erro.message;
        } catch (e) { /* resposta sem JSON: fica a mensagem padrão */ }
        throw new Error(mensagem);
    }

    // 204 No Content (o DELETE) não tem corpo para ler
    if (resposta.status === 204) return null;

    return resposta.json();   // texto JSON -> objeto JS
}


// ---------------------------------------------------------------------
// 2. Pequenos ajudantes
// ---------------------------------------------------------------------

// Evita que um texto digitado vire HTML (segurança básica)
function seguro(texto) {
    const div = document.createElement("div");
    div.textContent = texto ?? "";
    return div.innerHTML;
}

// "2026-11-21" -> "21/11/2026"
function data(texto) {
    if (!texto) return "";
    const [ano, mes, dia] = texto.split("-");
    return `${dia}/${mes}/${ano}`;
}

// Mostra o aviso embaixo da tela por alguns segundos
let temporizadorDoAviso;
function avisar(mensagem, ehErro = false) {
    const aviso = document.getElementById("aviso");
    aviso.textContent = mensagem;
    aviso.classList.toggle("erro", ehErro);
    aviso.classList.add("visivel");
    clearTimeout(temporizadorDoAviso);
    temporizadorDoAviso = setTimeout(() => aviso.classList.remove("visivel"), ehErro ? 6000 : 3500);
}

function nomeDoEquipamento(id) {
    const e = equipamentos.find(e => e.id === id);
    return e ? e.nome : "Equipamento " + id;
}

function patrimonioDoEquipamento(id) {
    const e = equipamentos.find(e => e.id === id);
    return e ? e.patrimonio : "";
}

function nomeDaPessoa(id) {
    const c = colaboradores.find(c => c.id === id);
    return c ? c.nome : "Pessoa " + id;
}

// Situação de um empréstimo: devolvido, atrasado ou em dia
function seloDoEmprestimo(emp) {
    if (emp.dataDevolucao) return `<span class="selo selo-livre">Devolvido em ${data(emp.dataDevolucao)}</span>`;
    if (emp.atrasado)      return `<span class="selo selo-atraso">Atrasado</span>`;
    return `<span class="selo selo-uso">Em dia</span>`;
}


// ---------------------------------------------------------------------
// 3. Carregar dados da API
// ---------------------------------------------------------------------
async function carregarTudo() {
    try {
        // As três chamadas saem ao mesmo tempo
        [equipamentos, colaboradores, emprestimos] = await Promise.all([
            api("GET", "/equipamentos"),
            api("GET", "/colaboradores"),
            api("GET", "/emprestimos")
        ]);
        mostrarResumo();
        await mostrarEquipamentos();
        mostrarColaboradores();
        await mostrarEmprestimos();
        preencherSelectsDoEmprestimo();
    } catch (erro) {
        avisar("Não foi possível falar com a API: " + erro.message, true);
    }
}

function mostrarResumo() {
    const disponiveis = equipamentos.filter(e => e.disponivel).length;
    const emprestados = equipamentos.length - disponiveis;
    const atrasados = emprestimos.filter(e => e.atrasado).length;

    document.getElementById("resumo").innerHTML =
        `<strong>${disponiveis}</strong> ${disponiveis === 1 ? "disponível" : "disponíveis"}, ` +
        `<strong>${emprestados}</strong> ${emprestados === 1 ? "emprestado" : "emprestados"} e ` +
        `<strong class="${atrasados > 0 ? "numero-atraso" : ""}">${atrasados}</strong> ` +
        `${atrasados === 1 ? "empréstimo atrasado" : "empréstimos atrasados"}`;
}


// ---------------------------------------------------------------------
// 4. Equipamentos
// ---------------------------------------------------------------------
async function mostrarEquipamentos() {
    // Os filtros viram query params: /equipamentos?tipo=NOTEBOOK&disponivel=true
    const tipo = document.getElementById("filtro-tipo").value;
    const disponivel = document.getElementById("filtro-disponivel").value;
    const params = new URLSearchParams();
    if (tipo) params.set("tipo", tipo);
    if (disponivel) params.set("disponivel", disponivel);

    const lista = params.toString()
        ? await api("GET", "/equipamentos?" + params)
        : equipamentos;

    const area = document.getElementById("lista-equipamentos");

    if (lista.length === 0) {
        area.innerHTML = `<p class="vazio">${tipo || disponivel
            ? "Nenhum equipamento com esses filtros."
            : "Nenhum equipamento cadastrado ainda. Use o formulário ao lado para cadastrar o primeiro."}</p>`;
        return;
    }

    area.innerHTML = lista.map(e => `
        <article class="etiqueta">
            <div class="etiqueta-faixa">
                <div class="etiqueta-codigo">${seguro(e.patrimonio)}</div>
                <div class="etiqueta-barras" aria-hidden="true"></div>
            </div>
            <div class="etiqueta-corpo">
                <p class="etiqueta-nome">${seguro(e.nome)}</p>
                <p class="etiqueta-tipo">${NOMES_DOS_TIPOS[e.tipo] ?? e.tipo}</p>
                <div class="etiqueta-rodape">
                    ${e.disponivel
                        ? `<span class="selo selo-livre">Disponível</span>`
                        : `<span class="selo selo-uso">Emprestado</span>`}
                    <div class="etiqueta-acoes">
                        <button class="botao-pequeno" onclick="editarEquipamento(${e.id})">Editar</button>
                        <button class="botao-pequeno botao-perigo" onclick="removerEquipamento(${e.id})">Remover</button>
                    </div>
                </div>
            </div>
        </article>
    `).join("");
}

// Cadastrar (POST) ou salvar a edição (PUT), conforme o modo do formulário
document.getElementById("form-equipamento").addEventListener("submit", async (evento) => {
    evento.preventDefault();   // não deixa a página recarregar
    const form = evento.target;
    const dados = {
        patrimonio: form.patrimonio.value.trim(),
        nome: form.nome.value.trim(),
        tipo: form.tipo.value
    };

    try {
        if (equipamentoEmEdicao) {
            await api("PUT", "/equipamentos/" + equipamentoEmEdicao, dados);
            avisar("Alterações salvas.");
        } else {
            await api("POST", "/equipamentos", dados);
            avisar("Equipamento cadastrado.");
        }
        sairDoModoEdicao();
        await carregarTudo();
    } catch (erro) {
        avisar(erro.message, true);
    }
});

// Preenche o formulário com o equipamento escolhido
function editarEquipamento(id) {
    const e = equipamentos.find(e => e.id === id);
    const form = document.getElementById("form-equipamento");
    equipamentoEmEdicao = id;

    form.patrimonio.value = e.patrimonio;
    form.patrimonio.disabled = true;   // o PUT só muda nome e tipo
    form.nome.value = e.nome;
    form.tipo.value = e.tipo;

    document.getElementById("titulo-form-equipamento").textContent = "Editar " + e.patrimonio;
    document.getElementById("botao-salvar-equipamento").textContent = "Salvar alterações";
    document.getElementById("botao-cancelar-edicao").hidden = false;
    form.nome.focus();
}

function sairDoModoEdicao() {
    const form = document.getElementById("form-equipamento");
    equipamentoEmEdicao = null;
    form.reset();
    form.patrimonio.disabled = false;
    document.getElementById("titulo-form-equipamento").textContent = "Cadastrar equipamento";
    document.getElementById("botao-salvar-equipamento").textContent = "Cadastrar";
    document.getElementById("botao-cancelar-edicao").hidden = true;
}

document.getElementById("botao-cancelar-edicao").addEventListener("click", sairDoModoEdicao);

async function removerEquipamento(id) {
    if (!confirm("Remover " + nomeDoEquipamento(id) + "?")) return;
    try {
        await api("DELETE", "/equipamentos/" + id);
        avisar("Equipamento removido.");
        if (equipamentoEmEdicao === id) sairDoModoEdicao();
        await carregarTudo();
    } catch (erro) {
        avisar(erro.message, true);   // ex.: R9, tem histórico de empréstimos
    }
}

document.getElementById("filtro-tipo").addEventListener("change", mostrarEquipamentos);
document.getElementById("filtro-disponivel").addEventListener("change", mostrarEquipamentos);


// ---------------------------------------------------------------------
// 5. Pessoas (colaboradores)
// ---------------------------------------------------------------------
function mostrarColaboradores() {
    const lista = document.getElementById("lista-colaboradores");

    if (colaboradores.length === 0) {
        lista.innerHTML = `<li class="vazio">Nenhuma pessoa cadastrada ainda. Use o formulário ao lado.</li>`;
        return;
    }

    lista.innerHTML = colaboradores.map(c => `
        <li>
            <div>
                <p class="item-titulo">${seguro(c.nome)}</p>
                <p class="item-detalhe">${c.cargo ? seguro(c.cargo) + ", " : ""}${seguro(c.email)}</p>
            </div>
            <div class="item-lado">
                <button class="botao-pequeno" onclick="mostrarHistorico(${c.id})">Ver empréstimos</button>
            </div>
        </li>
    `).join("");
}

document.getElementById("form-colaborador").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const form = evento.target;
    try {
        await api("POST", "/colaboradores", {
            nome: form.nome.value.trim(),
            email: form.email.value.trim(),
            cargo: form.cargo.value.trim()
        });
        avisar("Pessoa cadastrada.");
        form.reset();
        await carregarTudo();
    } catch (erro) {
        avisar(erro.message, true);   // ex.: R3, e-mail já cadastrado
    }
});

// GET /colaboradores/{id}/emprestimos
async function mostrarHistorico(id) {
    const area = document.getElementById("historico");
    try {
        const historico = await api("GET", `/colaboradores/${id}/emprestimos`);
        area.hidden = false;
        area.innerHTML = `
            <div class="historico-topo">
                <h3>Empréstimos de ${seguro(nomeDaPessoa(id))}</h3>
                <button class="botao-pequeno" onclick="document.getElementById('historico').hidden = true">Fechar</button>
            </div>
            <ul class="emprestimos">
                ${historico.length === 0
                    ? `<li class="vazio">Essa pessoa ainda não pegou nenhum equipamento.</li>`
                    : historico.map(emp => `
                        <li class="${emp.atrasado ? "atrasado" : ""}">
                            <div>
                                <p class="item-titulo">${seguro(nomeDoEquipamento(emp.equipamentoId))}</p>
                                <p class="item-detalhe">Emprestado em ${data(emp.dataEmprestimo)}, devolver até ${data(emp.dataPrevistaDevolucao)}</p>
                            </div>
                            <div class="item-lado">${seloDoEmprestimo(emp)}</div>
                        </li>`).join("")}
            </ul>`;
        area.scrollIntoView({ behavior: "smooth", block: "nearest" });
    } catch (erro) {
        avisar(erro.message, true);
    }
}


// ---------------------------------------------------------------------
// 6. Empréstimos
// ---------------------------------------------------------------------
function preencherSelectsDoEmprestimo() {
    const disponiveis = equipamentos.filter(e => e.disponivel);

    document.getElementById("select-equipamento").innerHTML = disponiveis.length
        ? disponiveis.map(e => `<option value="${e.id}">${seguro(e.nome)} (${seguro(e.patrimonio)})</option>`).join("")
        : `<option value="">Nenhum equipamento disponível</option>`;

    document.getElementById("select-colaborador").innerHTML = colaboradores.length
        ? colaboradores.map(c => `<option value="${c.id}">${seguro(c.nome)}</option>`).join("")
        : `<option value="">Cadastre uma pessoa primeiro</option>`;
}

async function mostrarEmprestimos() {
    const soAtrasados = document.getElementById("filtro-atrasados").checked;
    const lista = soAtrasados ? await api("GET", "/emprestimos?atrasados=true") : emprestimos;
    const area = document.getElementById("lista-emprestimos");

    if (lista.length === 0) {
        area.innerHTML = `<li class="vazio">${soAtrasados
            ? "Nenhum empréstimo atrasado."
            : "Nenhum empréstimo registrado ainda."}</li>`;
        return;
    }

    // Os mais recentes primeiro
    const ordenada = [...lista].sort((a, b) => b.id - a.id);

    area.innerHTML = ordenada.map(emp => `
        <li class="${emp.atrasado ? "atrasado" : ""}">
            <div>
                <p class="item-titulo">${seguro(nomeDoEquipamento(emp.equipamentoId))}
                    <span class="codigo">${seguro(patrimonioDoEquipamento(emp.equipamentoId))}</span></p>
                <p class="item-detalhe">${seguro(nomeDaPessoa(emp.colaboradorId))},
                    emprestado em ${data(emp.dataEmprestimo)}, devolver até ${data(emp.dataPrevistaDevolucao)}</p>
            </div>
            <div class="item-lado">
                ${seloDoEmprestimo(emp)}
                ${emp.dataDevolucao ? "" : `<button class="botao-pequeno" onclick="devolver(${emp.id})">Registrar devolução</button>`}
            </div>
        </li>
    `).join("");
}

// POST /emprestimos?equipamentoId=1&colaboradorId=2
document.getElementById("form-emprestimo").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const equipamentoId = document.getElementById("select-equipamento").value;
    const colaboradorId = document.getElementById("select-colaborador").value;
    if (!equipamentoId || !colaboradorId) {
        avisar("Escolha um equipamento disponível e uma pessoa.", true);
        return;
    }
    try {
        const emp = await api("POST", `/emprestimos?equipamentoId=${equipamentoId}&colaboradorId=${colaboradorId}`);
        avisar(`Empréstimo registrado. Devolver até ${data(emp.dataPrevistaDevolucao)}.`);
        await carregarTudo();
    } catch (erro) {
        avisar(erro.message, true);   // ex.: R10, pessoa com empréstimo atrasado
    }
});

// PUT /emprestimos/{id}/devolucao
async function devolver(id) {
    try {
        await api("PUT", `/emprestimos/${id}/devolucao`);
        avisar("Devolução registrada. O equipamento está disponível de novo.");
        await carregarTudo();
    } catch (erro) {
        avisar(erro.message, true);
    }
}

document.getElementById("filtro-atrasados").addEventListener("change", mostrarEmprestimos);


// ---------------------------------------------------------------------
// 7. Abas
// ---------------------------------------------------------------------
document.querySelectorAll(".aba").forEach(botao => {
    botao.addEventListener("click", () => {
        document.querySelectorAll(".aba").forEach(b => b.setAttribute("aria-selected", "false"));
        botao.setAttribute("aria-selected", "true");
        document.querySelectorAll(".secao").forEach(s => s.hidden = true);
        document.getElementById("secao-" + botao.dataset.aba).hidden = false;
    });
});


// Começa carregando tudo
carregarTudo();
