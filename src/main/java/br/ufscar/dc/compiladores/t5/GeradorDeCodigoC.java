package br.ufscar.dc.compiladores.t5;

import static br.ufscar.dc.compiladores.t5.ParserUtils_T5.verificarTipo;
import br.ufscar.dc.compiladores.t5.TabelaDeSimbolos.TipoEntrada;
import br.ufscar.dc.compiladores.t5.TabelaDeSimbolos.Tipos;

public class GeradorDeCodigoC extends LABaseVisitor<Void> {

    // ‘String’ que armazenará o programa em C
    StringBuilder saida = new StringBuilder();

    // Tabela principal e inicialização de escopos
    TabelaDeSimbolos tabela = new TabelaDeSimbolos();
    Escopos escopos = new Escopos();
    static Escopos escoposAninhados = new Escopos();

    // Converte Tipos para a ‘string’ equivalente em C
    public String converteTipo(Tipos tipoAuxT5) {
        String tipoRetorno = null;
        if (tipoAuxT5 != null) {
            switch (tipoAuxT5) {
                case INTEIRO:
                    tipoRetorno = "int";
                    break;
                case LITERAL:
                    tipoRetorno = "char";
                    break;
                case REAL:
                    tipoRetorno = "float";
                    break;
                default:
                    break;
            }
        }
        return tipoRetorno;
    }

    // Converte string de tipo para Tipos
    public Tipos converteTipos(String tipo) {
        Tipos tipoRetorno = Tipos.INVALIDO;
        switch (tipo) {
            case "literal":
                tipoRetorno = Tipos.LITERAL;
                break;
            case "inteiro":
                tipoRetorno = Tipos.INTEIRO;
                break;
            case "real":
                tipoRetorno = Tipos.REAL;
                break;
            case "logico":
                tipoRetorno = Tipos.LOGICO;
                break;
            default:
                break;
        }
        return tipoRetorno;
    }

    // Verifica tipo e retorna string equivalente em C
    public String verificaTipoC(String tipo) {
        String tipoRetorno = null;
        switch (tipo) {
            case "inteiro":
                tipoRetorno = "int";
                break;
            case "literal":
                tipoRetorno = "char";
                break;
            case "real":
                tipoRetorno = "float";
                break;
            default:
                break;
        }
        return tipoRetorno;
    }

    // Converte Tipos para ‘string’ de leitura/impressão em C
    public String verificaParamTipos(Tipos tipoAuxT5) {
        String tipoRetorno = null;
        if (tipoAuxT5 != null) {
            switch (tipoAuxT5) {
                case INTEIRO:
                    tipoRetorno = "d";
                    break;
                case REAL:
                    tipoRetorno = "f";
                    break;
                case LITERAL:
                    tipoRetorno = "s";
                    break;
                default:
                    break;
            }
        }
        return tipoRetorno;
    }

    // Verifica se tipo existe na tabela
    public boolean verificaTipoTabela(TabelaDeSimbolos tabela, String tipo) {
        return tabela.existe(tipo);
    }

    // Extrai limites de um comando Caso (Switch)
    public String getLimitesCaso(String str, boolean ehEsquerda) {
        if (str.contains("..")) {
            int pos = str.indexOf("..");
            return ehEsquerda ? str.substring(0, pos) : str.substring(pos + 2);
        }
        return str;
    }

    // Separa argumentos de uma expressão relacional
    public String separaArg(String total, int valor) {
        String argAux;
        int pos = total.indexOf("=");
        if (pos == -1) pos = total.indexOf("<>");

        if (valor == 0) {
            argAux = total.substring(1, pos);
        } else {
            total = total.substring(pos + (total.charAt(pos) == '=' ? 1 : 2));
            pos = total.indexOf(")");
            argAux = total.substring(0, pos);
        }

        return argAux;
    }


    // Separa parcelas de uma expressão aritmética
    public String separaExp(String total, int valor) {
        int pos = total.indexOf('+');
        if (pos == -1) pos = total.indexOf('-');
        if (pos == -1) pos = total.indexOf('*');
        if (pos == -1) pos = total.indexOf('/');
        if (valor == 0) {
            return total.substring(0, pos);
        } else {
            return total.substring(pos + 1);
        }
    }

    // Retorna operador de uma expressão
    public String verificaOp(String total) {
        String opRetorno = null;
        if (total.contains("+"))
            opRetorno = "+";
        else if (total.contains("-"))
            opRetorno = "-";
        else if (total.contains("*"))
            opRetorno = "*";
        else if (total.contains("/"))
            opRetorno = "/";
        return opRetorno;
    }

    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx) {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");
        visitDeclaracoes(ctx.declaracoes());
        saida.append("\nint main() {\n");
        visitCorpo(ctx.corpo());
        saida.append("\nreturn 0;\n");
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        String str;
        if (ctx.valor_constante() != null) {
            str = "#define " + ctx.IDENT().getText() + " " + ctx.valor_constante().getText() + "\n";
            saida.append(str);
        } else if (ctx.tipo() != null) {
            TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
            escopos.criarNovoEscopo();
            saida.append("typedef struct {\n");
            super.visitRegistro(ctx.tipo().registro());
            escopos.abandonarEscopo();
            escopoAtual.adicionar(ctx.IDENT().getText(), Tipos.REGISTRO, TipoEntrada.VARIAVEL);
            str = "} " + ctx.IDENT().getText() + ";\n";
            saida.append(str);
        } else if (ctx.variavel() != null)
            visitVariavel(ctx.variavel());
        return null;
    }

    @Override
    public Void visitVariavel(LAParser.VariavelContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        boolean tipoEstendido = false;
        String str;
        if (ctx.tipo().tipo_estendido() != null) {
            String nomeVar;
            String tipoVariavel = ctx.tipo().getText();
            Tipos tipoAuxT5;
            boolean ehPonteiro = false;
            if (tipoVariavel.contains("^")) {
                ehPonteiro = true;
                tipoVariavel = tipoVariavel.substring(1);
            }
            if (verificaTipoTabela(escopoAtual, tipoVariavel)) {
                tipoEstendido = true;
                tipoAuxT5 = Tipos.TIPOESTENDIDO;
            } else {
                tipoAuxT5 = converteTipos(tipoVariavel);
                tipoVariavel = converteTipo(tipoAuxT5);
            }
            if (ehPonteiro) {
                tipoVariavel += "*";
            }
            for (LAParser.IdentificadorContext ictx : ctx.identificador()) {
                nomeVar = ictx.getText();
                if (tipoEstendido)
                    escopoAtual.adicionar(nomeVar, Tipos.REGISTRO, TipoEntrada.VARIAVEL);
                else
                    escopoAtual.adicionar(nomeVar, tipoAuxT5, TipoEntrada.VARIAVEL);
                if (tipoAuxT5 == Tipos.LITERAL) {
                    str = tipoVariavel + " " + nomeVar + "[80];\n";
                    saida.append(str);
                } else {
                    str = tipoVariavel + " " + nomeVar + ";\n";
                    saida.append(str);
                }
            }
        } else {
            escopos.criarNovoEscopo();
            saida.append("struct{\n");
            for (LAParser.VariavelContext vctx : ctx.tipo().registro().variavel()) {
                visitVariavel(vctx);
            }
            str = "}" + ctx.identificador(0).getText() + ";\n";
            saida.append(str);
            escopos.abandonarEscopo();
            escopoAtual.adicionar(ctx.identificador(0).getText(), Tipos.REGISTRO, TipoEntrada.VARIAVEL);
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        String str;
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        escopos.criarNovoEscopo();
        TabelaDeSimbolos escopoParametros = escopos.obterEscopoAtual();
        String tipo;
        StringBuilder nomeVariaveis;
        if (ctx.tipo_estendido() != null)
            saida.append(verificaTipoC(ctx.tipo_estendido().getText()));
        else
            saida.append("void");
        str = " " + ctx.IDENT().getText() + "(";
        saida.append(str);
        if (ctx.parametros() != null) {
            for (LAParser.ParametroContext pctx : ctx.parametros().parametro()) {
                tipo = verificaTipoC(pctx.tipo_estendido().getText());
                nomeVariaveis = new StringBuilder();
                for (LAParser.IdentificadorContext ictx : pctx.identificador()) {
                    nomeVariaveis.append(ictx.getText());
                    escopoParametros.adicionar(ictx.getText(), converteTipos(pctx.tipo_estendido().getText()), TipoEntrada.VARIAVEL);
                }
                if (tipo.equals("char")) {
                    tipo = "char*";
                }
                str = tipo + " " + nomeVariaveis;
                saida.append(str);
            }
        }
        saida.append(") {\n");
        if (ctx.tipo_estendido() != null)
            escopoAtual.adicionar(ctx.IDENT().getText(), converteTipos(ctx.tipo_estendido().getText()), TipoEntrada.FUNCAO);
        else
            escopoAtual.adicionar(ctx.IDENT().getText(), Tipos.VOID, TipoEntrada.PROCEDIMENTO);
        for (LAParser.CmdContext cctx : ctx.cmd()) {
            visitCmd(cctx);
        }
        saida.append("}\n");
        escopos.abandonarEscopo();
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null)
            saida.append(ctx.identificador().getText());
        super.visitParcela_nao_unario(ctx);
        return null;
    }

    @Override
    public Void visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
        if (!ctx.expressao().get(0).getText().contains("(")) {
            saida.append(ctx.getText());
        } else {
            saida.append("(");
            super.visitParcela_unario(ctx);
            saida.append(")");
        }
        return null;
    }

    @Override
    public Void visitOp_relacional(LAParser.Op_relacionalContext ctx) {
        String strRetorno = ctx.getText();
        if (ctx.getText().contains("="))
            if (!ctx.getText().contains("<=") || !ctx.getText().contains(">="))
                strRetorno = "==";
        saida.append(strRetorno);
        super.visitOp_relacional(ctx);
        return null;
    }

    @Override
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        saida.append("return ");
        super.visitExpressao(ctx.expressao());
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        String str;
        tabela = escoposAninhados.obterEscopoAtual();
        if (ctx.getText().contains("^")) {
            str = "*" + ctx.identificador().getText() + " = " + ctx.expressao().getText() + ";\n";
            saida.append(str);
        } else if (ctx.identificador().getText().contains(".") && ctx.getText().contains("\"")) {
            str = "strcpy(" + ctx.identificador().getText() + "," + ctx.expressao().getText() + ");\n";
            saida.append(str);
        } else {
            str = ctx.identificador().getText() + " = " + ctx.expressao().getText() + ";\n";
            saida.append(str);
        }
        return null;
    }

    @Override
    public Void visitExpressao(LAParser.ExpressaoContext ctx) {
        if (ctx.termo_logico().size() > 1) {
            for (LAParser.Termo_logicoContext termoLogico : ctx.termo_logico()) {
                saida.append(" || ");
                visitTermo_logico(termoLogico);
            }
        } else
            visitTermo_logico(ctx.termo_logico(0));
        return null;
    }

    @Override
    public Void visitTermo_logico(LAParser.Termo_logicoContext ctx) {
        if (ctx.fator_logico().size() > 1) {
            for (LAParser.Fator_logicoContext fatorLogico : ctx.fator_logico()) {
                saida.append(" && ");
                visitFator_logico(fatorLogico);
            }
        } else
            visitFator_logico(ctx.fator_logico(0));
        return null;
    }

    @Override
    public Void visitFator_logico(LAParser.Fator_logicoContext ctx) {
        if (ctx.getText().contains("nao"))
            saida.append("!");
        visitParcela_logica(ctx.parcela_logica());
        return null;
    }

    @Override
    public Void visitOp2(LAParser.Op2Context ctx) {
        saida.append(ctx.getText());
        super.visitOp2(ctx);
        return null;
    }

    @Override
    public Void visitParcela_logica(LAParser.Parcela_logicaContext ctx) {
        if (ctx.getText().contains("falso"))
            saida.append("false");
        else if (ctx.getText().contains("verdadeiro"))
            saida.append("true");
        else
            visitExp_relacional(ctx.exp_relacional());
        return null;
    }

    @Override
    public Void visitExp_relacional(LAParser.Exp_relacionalContext ctx) {
        String str;
        String opAtual = ctx.getText();
        String expAtual = ctx.exp_aritmetica().get(0).getText();
        if (expAtual.contains("<>"))
            opAtual = "<>";
        else if (expAtual.contains("="))
            if (!expAtual.contains("<=") || !expAtual.contains(">="))
                opAtual = "=";
        if (ctx.op_relacional() != null) {
            saida.append(expAtual);
            saida.append(ctx.op_relacional().getText());
            saida.append(ctx.exp_aritmetica(1).getText());
        } else {
            String arg1, arg2;
            switch (opAtual) {
                case "=":
                    arg1 = separaArg(expAtual, 0);
                    arg2 = separaArg(expAtual, 1);
                    str = "(" + arg1;
                    saida.append(str);
                    saida.append("==");
                    str = arg2 + ")";
                    saida.append(str);
                    break;
                case "<>":
                    saida.append("!=");
                    break;
                default:
                    arg1 = separaExp(expAtual, 0);
                    arg2 = separaExp(expAtual, 1);
                    saida.append(arg1);
                    String op = verificaOp(opAtual);
                    saida.append(op);
                    saida.append(arg2);
                    break;
            }
        }
        return null;
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        String str;
        String textoExpressao;
        textoExpressao = ctx.expressao().getText().replace("e", "&&");
        textoExpressao = textoExpressao.replace("=", "==");
        str = "if (" + textoExpressao + "){\n";
        saida.append(str);
        for (LAParser.CmdContext cctx : ctx.cmdEntao)
            super.visitCmd(cctx);
        saida.append("}\n");
        if (ctx.getText().contains("senao")) {
            saida.append("else{\n");
            for (LAParser.CmdContext cctx : ctx.cmdSenao)
                super.visitCmd(cctx);
            saida.append("}\n");
        }
        return null;
    }

    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        String nomeVar;
        Tipos tipoAuxT5;
        String codigoTipo;
        String str;
        for (LAParser.IdentificadorContext ictx : ctx.identificador()) {
            nomeVar = ictx.getText();
            tipoAuxT5 = escopoAtual.verificar(nomeVar);
            codigoTipo = verificaParamTipos(tipoAuxT5);
            if (tipoAuxT5 == Tipos.LITERAL) {
                str = "gets(" + nomeVar + ");\n";
                saida.append(str);
            } else {
                str = "scanf(\"%" + codigoTipo + "\",&" + nomeVar + ");\n";
                saida.append(str);
            }
        }
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        saida.append("while(");
        super.visitExpressao(ctx.expressao());
        saida.append("){\n");
        for (LAParser.CmdContext cctx : ctx.cmd())
            super.visitCmd(cctx);
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        String str;
        String nomeVariavel, limiteEsq, limiteDir;
        nomeVariavel = ctx.IDENT().getText();
        limiteEsq = ctx.exp_aritmetica(0).getText();
        limiteDir = ctx.exp_aritmetica(1).getText();
        str = "for(" + nomeVariavel + " = " + limiteEsq + "; " + nomeVariavel + " <= " + limiteDir + "; " + nomeVariavel + "++){\n";
        saida.append(str);
        for (LAParser.CmdContext cctx : ctx.cmd())
            visitCmd(cctx);
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        saida.append("do{\n");
        for (LAParser.CmdContext cctx : ctx.cmd())
            super.visitCmd(cctx);
        saida.append("}while(");
        super.visitExpressao(ctx.expressao());
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        Tipos tipoAuxT5Exp;
        String codTipoExp;
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        for (LAParser.ExpressaoContext ectx : ctx.expressao()) {
            String str;
            saida.append("printf(\"");
            if (ectx.getText().contains("\"")) {
                str = ectx.getText().replace("\"", "") + "\");\n";
                saida.append(str);
            } else {
                tipoAuxT5Exp = verificarTipo(escopoAtual, ectx);
                if (tipoAuxT5Exp == Tipos.LITERAL) {
                    str = "%s" + "\", " + ectx.getText() + ");\n";
                    saida.append(str);
                } else {
                    codTipoExp = verificaParamTipos(tipoAuxT5Exp);
                    str = "%" + codTipoExp + "\", " + ectx.getText() + ");\n";
                    saida.append(str);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        String str;
        String limiteEsq, limiteDir;
        str = "switch (" + ctx.exp_aritmetica().getText() + "){\n";
        saida.append(str);
        for (LAParser.Item_selecaoContext sctx : ctx.selecao().item_selecao()) {
            String strOriginal = sctx.constantes().numero_intervalo(0).getText();
            if (strOriginal.contains(".")) {
                limiteEsq = getLimitesCaso(strOriginal, true);
                limiteDir = getLimitesCaso(strOriginal, false);
            } else {
                limiteEsq = getLimitesCaso(strOriginal, true);
                limiteDir = getLimitesCaso(strOriginal, true);
            }
            if (!sctx.constantes().isEmpty()) {
                for (int i = Integer.parseInt(limiteEsq); i <= Integer.parseInt(limiteDir); i++) {
                    str = "case " + i + ":\n";
                    saida.append(str);
                }
            } else {
                str = "case " + limiteEsq + ":\n";
                saida.append(str);
            }
            for (LAParser.CmdContext cctx : sctx.cmd())
                visitCmd(cctx);
            saida.append("break;\n");
        }
        saida.append("default:\n");
        for (LAParser.CmdContext cctx : ctx.cmd())
            visitCmd(cctx);
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        String str;
        str = ctx.IDENT().getText() + "(";
        saida.append(str);
        int cont = 0;
        for (LAParser.ExpressaoContext ectx : ctx.expressao()) {
            if (cont >= 1)
                saida.append(", ");
            saida.append(ectx.getText());
            cont += 1;
        }
        saida.append(");\n");
        return null;
    }
}
