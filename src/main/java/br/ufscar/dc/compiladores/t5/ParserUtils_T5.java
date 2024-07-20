package br.ufscar.dc.compiladores.t5;

// Importações
import static br.ufscar.dc.compiladores.t5.Parser_T5.dadosFuncaoProcedimento;
import static br.ufscar.dc.compiladores.t5.Parser_T5.escoposAninhados;
import br.ufscar.dc.compiladores.t5.TabelaDeSimbolos.Tipos;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import org.antlr.v4.runtime.Token;

public class ParserUtils_T5 {

    // Criação da lista que armazenará os erros identificados pelo analisador
    public static List<String> errosSemanticos = new ArrayList<>();

    // Método para adicionar um novo erro
    public static void adicionaErroSemantico(Token tok, String mensagem) {
        int linha = tok.getLine();

        // Adiciona o novo erro apenas se ele não tiver sido identificado
        if (!errosSemanticos.contains("Linha " + linha + ": " + mensagem))
            errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }

    // Método para verificar a compatibilidade dos Tipos: Inteiro x Real
    public static boolean verificaCompatibilidade(Tipos T1, Tipos T2) {
        return (
                T1 == Tipos.INTEIRO && T2 == Tipos.REAL
                        || T1 == Tipos.REAL && T2 == Tipos.INTEIRO
                        || T1 == Tipos.REAL && T2 == Tipos.REAL
        );
    }

    // Método para verificação de compatibilidade lógica entre os valores
    public static boolean verificaCompatibilidadeLogica(Tipos T1, Tipos T2) {
        return (
                T1 == Tipos.INTEIRO && T2 == Tipos.REAL ||
                        T1 == Tipos.REAL && T2 == Tipos.INTEIRO
        );
    }

    // Trata-se de uma função para pegar apenas o nome da variavel que representa um procedimento/função
    // O símbolo pode ser, por exemplo, "(" ou "["
    // Exemplo: vetor[i] vira apenas vetor
    public static String procFuncNome(String nome, String simbolo) {
        int index = nome.indexOf(simbolo);
        if (index != -1) {
            nome = nome.substring(0, index);
        }
        return nome;
    }

    // Método auxiliar utilizado para retornar o Tipos referente ao literal que está sendo analisado
    public static Tipos verificarTipo (HashMap<String, ArrayList<String>> tabela, String tipoRetorno) {
        Tipos tipoAux;

        // Remoção do caractere especial de ponteiro
        if (tipoRetorno.charAt(0) == '^') {
            tipoRetorno = tipoRetorno.substring(1);
        }

        if (tabela.containsKey(tipoRetorno))
            tipoAux = Tipos.REGISTRO;
        else if (tipoRetorno.equals("literal"))
            tipoAux = Tipos.LITERAL;
        else if (tipoRetorno.equals("inteiro"))
            tipoAux = Tipos.INTEIRO;
        else if (tipoRetorno.equals("real"))
            tipoAux = Tipos.REAL;
        else if (tipoRetorno.equals("logico"))
            tipoAux = Tipos.LOGICO;
        else
            tipoAux = Tipos.INVALIDO;

        return tipoAux;
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.Exp_aritmeticaContext ctx) {
        // A variável que será retornada ao fim da execução é inicializada com o tipo
        // do primeiro elemento que será verificado, para fins de comparação
        Tipos tipoRetorno = verificarTipo(tabela, ctx.termo().get(0));

        for (var termoArit : ctx.termo()) {
            Tipos tipoAtual = verificarTipo(tabela, termoArit);
            if ((verificaCompatibilidade(tipoAtual, tipoRetorno)) && (tipoAtual != Tipos.INVALIDO))
                tipoRetorno = Tipos.REAL;
            else
                tipoRetorno = tipoAtual;
        }
        return tipoRetorno;
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.TermoContext ctx) {
        // A variável que será retornada ao fim da execução é inicializada com o tipo
        // do primeiro elemento que será verificado, para fins de comparação
        Tipos tipoRetorno = verificarTipo(tabela, ctx.fator().get(0));

        for (LAParser.FatorContext fatorArit : ctx.fator()) {
            Tipos tipoAtual = verificarTipo(tabela, fatorArit);
            if ((verificaCompatibilidade(tipoAtual, tipoRetorno)) && (tipoAtual != Tipos.INVALIDO))
                tipoRetorno = Tipos.REAL;
            else
                tipoRetorno = tipoAtual;
        }
        return tipoRetorno;
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.FatorContext ctx) {
        Tipos tipoRetorno = null;

        for (LAParser.ParcelaContext parcela : ctx.parcela()) {
            tipoRetorno = verificarTipo(tabela, parcela);

            // Trata o caso de uma variavel do usuario (registro)
            if (tipoRetorno == Tipos.REGISTRO) {
                String nome = parcela.getText();
                nome = procFuncNome(nome, "(");
                tipoRetorno = verificarTipo(tabela, nome);
            }
        }
        return tipoRetorno;
    }

    // Distingue uma parcela unária de uma não unária
    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null)
            return verificarTipo(tabela, ctx.parcela_unario());
        else
            return verificarTipo(tabela, ctx.parcela_nao_unario());
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_unarioContext ctx) {
        Tipos tipoRetorno = null;
        String nome;

        if (ctx.identificador() != null) {
            // Se a dimensão não é vazia, pega o nome do vetor
            if (!ctx.identificador().dimensao().exp_aritmetica().isEmpty())
                nome = ctx.identificador().IDENT().get(0).getText();
            else
                nome = ctx.identificador().getText();

            // Caso a variável já tenha sido declarada, retorne o tipo dela
            if (tabela.existe(nome)) {
                tipoRetorno = tabela.verificar(nome);
            }
            // Verifica a existencia da variavel com uma tabela auxiliar e adiciona o erro se ele não existir
            else {
                TabelaDeSimbolos tabelaAux = escoposAninhados.obterEscopoAtual();

                if (!tabelaAux.existe(nome)) {
                    adicionaErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                    tipoRetorno = Tipos.INVALIDO;
                } else
                    tipoRetorno = tabelaAux.verificar(nome);
            }
        } else if (ctx.IDENT() != null) {
            if (dadosFuncaoProcedimento.containsKey(ctx.IDENT().getText())) {
                List<Tipos> aux = dadosFuncaoProcedimento.get(ctx.IDENT().getText());

                if (aux.size() == ctx.expressao().size()) {
                    for (int i = 0; i < ctx.expressao().size(); i++)
                        if (aux.get(i) != verificarTipo(tabela, ctx.expressao().get(i)))
                            adicionaErroSemantico(ctx.expressao().get(i).getStart(), "incompatibilidade de parametros na chamada de " + ctx.IDENT().getText());

                    tipoRetorno = aux.get(aux.size() - 1);

                } else
                    adicionaErroSemantico(ctx.IDENT().getSymbol(), "incompatibilidade de parametros na chamada de " + ctx.IDENT().getText());
            } else
                tipoRetorno = Tipos.INVALIDO;
        } else if (ctx.NUM_INT() != null)
            tipoRetorno = Tipos.INTEIRO;
        else if (ctx.NUM_REAL() != null)
            tipoRetorno = Tipos.REAL;
        else
            tipoRetorno = verificarTipo(tabela, ctx.expressao().get(0));

        return tipoRetorno;
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_nao_unarioContext ctx) {
        Tipos tipoRetorno;
        String nome;

        // Novamente, verifica se a variável existe e adiciona um erro conforme necessário
        if (ctx.identificador() != null) {
            nome = ctx.identificador().getText();

            if (!tabela.existe(nome)) {
                adicionaErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                tipoRetorno = Tipos.INVALIDO;
            } else
                tipoRetorno = tabela.verificar(ctx.identificador().getText());
        } else
            tipoRetorno = Tipos.LITERAL;

        return tipoRetorno;
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.ExpressaoContext ctx) {
        Tipos tipoRetorno = verificarTipo(tabela, ctx.termo_logico(0));

        // Aqui basta verificar se os tipos são diferentes
        for (LAParser.Termo_logicoContext termoLogico : ctx.termo_logico()) {
            Tipos tipoAtual = verificarTipo(tabela, termoLogico);
            if (tipoRetorno != tipoAtual && tipoAtual != Tipos.INVALIDO)
                tipoRetorno = Tipos.INVALIDO;
        }

        return tipoRetorno;
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.Termo_logicoContext ctx) {
        Tipos tipoRetorno = verificarTipo(tabela, ctx.fator_logico(0));

        // Aqui basta verificar se os tipos são diferentes
        for (LAParser.Fator_logicoContext fatorLogico : ctx.fator_logico()) {
            Tipos tipoAtual = verificarTipo(tabela, fatorLogico);
            if (tipoRetorno != tipoAtual && tipoAtual != Tipos.INVALIDO)
                tipoRetorno = Tipos.INVALIDO;
        }

        return tipoRetorno;
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.Fator_logicoContext ctx) {
        return verificarTipo(tabela, ctx.parcela_logica());
    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_logicaContext ctx) {
        Tipos tipoRetorno;

        if (ctx.exp_relacional() != null)
            tipoRetorno = verificarTipo(tabela, ctx.exp_relacional());
        else
            tipoRetorno = Tipos.LOGICO;

        return tipoRetorno;

    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.Exp_relacionalContext ctx) {
        Tipos tipoRetorno = verificarTipo(tabela, ctx.exp_aritmetica().get(0));

        if (ctx.exp_aritmetica().size() > 1) {
            Tipos tipoAtual = verificarTipo(tabela, ctx.exp_aritmetica().get(1));
            if (tipoRetorno == tipoAtual || verificaCompatibilidadeLogica(tipoRetorno, tipoAtual))
                tipoRetorno = Tipos.LOGICO;
            else
                tipoRetorno = Tipos.INVALIDO;
        }

        return tipoRetorno;

    }

    public static Tipos verificarTipo(TabelaDeSimbolos tabela, LAParser.IdentificadorContext ctx) {
        String nomeVar = ctx.IDENT().get(0).getText();

        return tabela.verificar(nomeVar);
    }

    // Verificação padrão de Tipos de variáveis a partir da tabela
    public static Tipos verificarTipo(TabelaDeSimbolos tabela, String nomeVar) {
        return tabela.verificar(nomeVar);
    }
}
