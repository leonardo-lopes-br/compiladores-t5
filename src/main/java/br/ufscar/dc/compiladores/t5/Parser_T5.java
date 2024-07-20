package br.ufscar.dc.compiladores.t5;

// Importações
import static br.ufscar.dc.compiladores.t5.ParserUtils_T5.verificarTipo;
import static br.ufscar.dc.compiladores.t5.ParserUtils_T5.adicionaErroSemantico;
import static br.ufscar.dc.compiladores.t5.ParserUtils_T5.verificaCompatibilidade;
import br.ufscar.dc.compiladores.t5.TabelaDeSimbolos.TipoEntrada;
import br.ufscar.dc.compiladores.t5.TabelaDeSimbolos.Tipos;
import org.antlr.v4.runtime.Token;
import java.util.ArrayList;
import java.util.HashMap;

// Classe do Parser para visitar a árvore da gramática
public class Parser_T5 extends LABaseVisitor<Void> {
    TabelaDeSimbolos tabela;
    static Escopos escoposAninhados = new Escopos();

    // Tabela auxiliar para os nomes e parâmetros das funções e procedimentos
    static HashMap<String, ArrayList<Tipos>> dadosFuncaoProcedimento = new HashMap<>();

    // Tabela que armazenará as variaveis de um registro
    HashMap<String, ArrayList<String>> tabelaRegistro = new HashMap<>();

    // Método que adiciona o símbolo que está sendo analisado à tabela
    public void addVariableIntoScopeTable(String nome, String tipo, Token nomeT, Token tipoT, TipoEntrada tipoE) {
        TabelaDeSimbolos tabelaLocal = escoposAninhados.obterEscopoAtual();
        Tipos tipoItem;

        // Se for ponteiro remove o caractere especial
        if (tipo.charAt(0) == '^')
            tipo = tipo.substring(1);

        switch (tipo) {
            case "literal":
                tipoItem = Tipos.LITERAL;
                break;
            case "inteiro":
                tipoItem = Tipos.INTEIRO;
                break;
            case "real":
                tipoItem = Tipos.REAL;
                break;
            case "logico":
                tipoItem = Tipos.LOGICO;
                break;
            case "void":
                tipoItem = Tipos.VOID;
                break;
            case "registro":
                tipoItem = Tipos.REGISTRO;
                break;
            default:
                tipoItem = Tipos.INVALIDO;
                break;
        }

        // Tipo não declarado
        if (tipoItem == Tipos.INVALIDO)
            adicionaErroSemantico(tipoT, "tipo " + tipo + " nao declarado");

        // Adiciona caso exista, senao, não pode ser redeclarado (erro)
        if (!tabelaLocal.existe(nome))
            tabelaLocal.adicionar(nome, tipoItem, tipoE);
        else
            adicionaErroSemantico(nomeT, "identificador " + nome + " ja declarado anteriormente");
    }

    // Início da análise semântica
    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        // Verifica se o comando "retorne" é usado no escopo do programa principal
        for (LAParser.CmdContext c : ctx.corpo().cmd())
            if (c.cmdRetorne() != null)
                adicionaErroSemantico(c.getStart(), "comando retorne nao permitido nesse escopo");

        return super.visitPrograma(ctx);
    }

    @Override
    public Void visitCorpo(LAParser.CorpoContext ctx) {
        super.visitCorpo(ctx);
        return null;
    }

    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        String tipoVariavel;
        String nomeVariavel;

        // Identifica uma declaração
        if (ctx.getText().contains("declare")) {
            // Verifica se é a declaração de um registro para adicionar ao escopo
            if (ctx.variavel().tipo().registro() != null) {
                for (LAParser.IdentificadorContext ic : ctx.variavel().identificador()) {
                    addVariableIntoScopeTable(ic.getText(), "registro", ic.getStart(), null, TipoEntrada.VARIAVEL);

                    for (LAParser.VariavelContext vc : ctx.variavel().tipo().registro().variavel()) {
                        tipoVariavel = vc.tipo().getText();

                        for (LAParser.IdentificadorContext icr : vc.identificador())
                            addVariableIntoScopeTable(ic.getText() + "." + icr.getText(), tipoVariavel, icr.getStart(), vc.tipo().getStart(), TipoEntrada.VARIAVEL);
                    }
                }
                // Trata-se de um tipo já declarado
            } else {
                tipoVariavel = ctx.variavel().tipo().getText();
                // Verifica se é um registro
                if (tabelaRegistro.containsKey(tipoVariavel)) {
                    ArrayList<String> variaveisRegistro = tabelaRegistro.get(tipoVariavel);

                    for (LAParser.IdentificadorContext ic : ctx.variavel().identificador()) {
                        nomeVariavel = ic.IDENT().get(0).getText();

                        if (tabela.existe(nomeVariavel) || tabelaRegistro.containsKey(nomeVariavel)) {
                            adicionaErroSemantico(ic.getStart(), "identificador " + nomeVariavel + " ja declarado anteriormente");
                        } else {
                            addVariableIntoScopeTable(nomeVariavel, "registro", ic.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL);

                            for (int i = 0; i < variaveisRegistro.size(); i = i + 2) {
                                addVariableIntoScopeTable(nomeVariavel + "." + variaveisRegistro.get(i), variaveisRegistro.get(i+1), ic.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL);
                            }
                        }
                    }
                    // Trata-se de um tipo primitivo
                } else {
                    for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                        nomeVariavel = ident.getText();

                        // Verifica se a declaração é o nome de uma função/procedimento
                        if (dadosFuncaoProcedimento.containsKey(nomeVariavel))
                            adicionaErroSemantico(ident.getStart(), "identificador " + nomeVariavel + " ja declarado anteriormente");
                        else
                            addVariableIntoScopeTable(nomeVariavel, tipoVariavel, ident.getStart(), ctx.variavel().tipo().getStart(), TipoEntrada.VARIAVEL);
                    }
                }
            }
            // Verifica se é a declaração de um novo tipo (registro)
        } else if (ctx.getText().contains("tipo")) {

            if (ctx.tipo().registro() != null) {
                ArrayList<String> variaveisRegistro = new ArrayList<>();

                for (LAParser.VariavelContext vc : ctx.tipo().registro().variavel()) {
                    tipoVariavel = vc.tipo().getText();

                    for (LAParser.IdentificadorContext ic : vc.identificador()) {
                        variaveisRegistro.add(ic.getText());
                        variaveisRegistro.add(tipoVariavel);
                    }
                }
                tabelaRegistro.put(ctx.IDENT().getText(), variaveisRegistro);
            }
        // Verifica se é a declaração de uma constante
        } else if (ctx.getText().contains("constante"))
            addVariableIntoScopeTable(ctx.IDENT().getText(), ctx.tipo_basico().getText(), ctx.IDENT().getSymbol(), ctx.IDENT().getSymbol(), TipoEntrada.VARIAVEL);

        return super.visitDeclaracao_local(ctx);
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        // Novo escopo para procedimento/função
        escoposAninhados.criarNovoEscopo();
        tabela = escoposAninhados.obterEscopoAtual();

        ArrayList<Tipos> tiposVariaveis = new ArrayList<>();
        ArrayList<String> variaveisRegistro;

        String tipoVariavel;
        Tipos tipoAux;

        // Verifica se o escopo atual pertence a um procedimento
        if (ctx.getText().contains("procedimento")) {

            for (LAParser.ParametroContext parametro : ctx.parametros().parametro()) {
                // Verifica se é um tipo básico válido
                if (parametro.tipo_estendido().tipo_basico_ident().tipo_basico() != null) {
                    // Adiciona o parâmetro na tabela
                    addVariableIntoScopeTable(parametro.identificador().get(0).getText(), parametro.tipo_estendido().tipo_basico_ident().tipo_basico().getText(), parametro.getStart(), parametro.getStart(), TipoEntrada.VARIAVEL);

                    // Obtém os tipos dos parâmetros para adicioná-los na tabela de funções/procedimentos
                    tipoVariavel = parametro.tipo_estendido().getText();
                    tipoAux = verificarTipo(tabelaRegistro, tipoVariavel);
                    tiposVariaveis.add(tipoAux);
                // Verifica se é um registro
                } else if (tabelaRegistro.containsKey(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText())) {
                    // Recupera os elementos do registro.
                    variaveisRegistro = tabelaRegistro.get(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText());

                    // Obtém os tipos dos parâmetros para adicioná-los na tabela de funções/procedimentos
                    tipoVariavel = parametro.tipo_estendido().getText();
                    tipoAux = verificarTipo(tabelaRegistro, tipoVariavel);
                    tiposVariaveis.add(tipoAux);

                    for (LAParser.IdentificadorContext ic : parametro.identificador())
                        // Adiciona os elementos do registro na tabela segundo os tipos
                        for (int i = 0; i < variaveisRegistro.size(); i = i + 2)
                            addVariableIntoScopeTable(ic.getText() + "." + variaveisRegistro.get(i), variaveisRegistro.get(i + 1), ic.getStart(), ic.getStart(), TipoEntrada.VARIAVEL);
                } else
                    adicionaErroSemantico(parametro.getStart(), "tipo nao declarado");
            }
            // Verifica se há algum comando "retorne" num procedimento
            for (LAParser.CmdContext c : ctx.cmd())
                if (c.cmdRetorne() != null)
                    adicionaErroSemantico(c.getStart(), "comando retorne nao permitido nesse escopo");

            // Adiciona o nome do procedimento e os tipos dos parâmetros na tabela de dados
            dadosFuncaoProcedimento.put(ctx.IDENT().getText(), tiposVariaveis);

        // Verifica se o escopo atual pertence a uma função
        } else if (ctx.getText().contains("funcao")) {
            for (LAParser.ParametroContext parametro : ctx.parametros().parametro()) {

                if (parametro.tipo_estendido().tipo_basico_ident().tipo_basico() != null) {

                    addVariableIntoScopeTable(parametro.identificador().get(0).getText(), parametro.tipo_estendido().tipo_basico_ident().tipo_basico().getText(), parametro.getStart(), parametro.getStart(), TipoEntrada.VARIAVEL);

                    tipoVariavel = parametro.tipo_estendido().getText();
                    tipoAux = verificarTipo(tabelaRegistro, tipoVariavel);
                    tiposVariaveis.add(tipoAux);
                } else if (tabelaRegistro.containsKey(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText())) {

                    variaveisRegistro = tabelaRegistro.get(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText());

                    tipoVariavel = parametro.tipo_estendido().tipo_basico_ident().IDENT().getText();
                    tipoAux = verificarTipo(tabelaRegistro, tipoVariavel);
                    tiposVariaveis.add(tipoAux);

                    for (LAParser.IdentificadorContext ic : parametro.identificador())
                        for (int i = 0; i < variaveisRegistro.size(); i = i + 2)
                                addVariableIntoScopeTable(ic.getText() + "." + variaveisRegistro.get(i), variaveisRegistro.get(i + 1), ic.getStart(), ic.getStart(), TipoEntrada.VARIAVEL);
                } else
                    adicionaErroSemantico(parametro.getStart(), "tipo nao declarado");
            }

            // Adiciona o nome da função e os tipos dos parâmetros na tabela de dados
            dadosFuncaoProcedimento.put(ctx.IDENT().getText(), tiposVariaveis);
        }

        super.visitDeclaracao_global(ctx);

        // Remove o escopo atual
        escoposAninhados.abandonarEscopo();

        // Adiciona o nome do procedimento/função na tabela
        if (ctx.getText().contains("procedimento"))
            addVariableIntoScopeTable(ctx.IDENT().getText(), "void", ctx.getStart(), ctx.getStart(), TipoEntrada.PROCEDIMENTO);
        else if (ctx.getText().contains("funcao"))
            addVariableIntoScopeTable(ctx.IDENT().getText(), ctx.tipo_estendido().tipo_basico_ident().tipo_basico().getText(), ctx.getStart(), ctx.getStart(), TipoEntrada.FUNCAO);

        return null;
    }

    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        for (LAParser.IdentificadorContext id : ctx.identificador())
            if (!tabela.existe(id.getText()))
                adicionaErroSemantico(id.getStart(), "identificador " + id.getText() + " nao declarado");

        return super.visitCmdLeia(ctx);
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        for (LAParser.ExpressaoContext expressao : ctx.expressao())
            verificarTipo(tabela, expressao);
        return super.visitCmdEscreva(ctx);
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        verificarTipo(tabela, ctx.expressao());
        return super.visitCmdEnquanto(ctx);
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        verificarTipo(tabela, ctx.expressao());
        return super.visitCmdSe(ctx);
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        tabela = escoposAninhados.obterEscopoAtual();
        Tipos tipoExpressao = verificarTipo(tabela, ctx.expressao());
        String varNome = ctx.identificador().getText();
        if (tipoExpressao != Tipos.INVALIDO) {
            // Variavel não declarada
            if (!tabela.existe(varNome))
                adicionaErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
            else {
                Tipos varTipo = verificarTipo(tabela, varNome);
                // Verificação de tipos numéricos
                if (varTipo == Tipos.INTEIRO || varTipo == Tipos.REAL) {
                    if (ctx.getText().contains("ponteiro")) {
                        if (!verificaCompatibilidade(varTipo, tipoExpressao))
                            if (tipoExpressao != Tipos.INTEIRO)
                                adicionaErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para ^" + ctx.identificador().getText());
                    } else if (!verificaCompatibilidade(varTipo, tipoExpressao))
                        if (tipoExpressao != Tipos.INTEIRO)
                            adicionaErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para " + ctx.identificador().getText());
                    // Se não for o caso especial dos números, retorna o erro de incompatibilidade
                } else if (varTipo != tipoExpressao)
                    adicionaErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para " + ctx.identificador().getText());
            }
        }

        return super.visitCmdAtribuicao(ctx);
    }
}