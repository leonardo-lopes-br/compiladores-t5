package br.ufscar.dc.compiladores.t5;

// Importações
import java.util.HashMap;
import java.util.Map;
import static br.ufscar.dc.compiladores.t5.ParserUtils_T5.procFuncNome;

// Classe baseada em código disponibilizado de exemplo pelo professor
public class TabelaDeSimbolos {

    private final Map<String, EntradaTabelaDeSimbolos> tabela;
    
    public TabelaDeSimbolos() {
        this.tabela = new HashMap<>();
    }

    public enum Tipos {
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        VOID,
        REGISTRO,
        TIPOESTENDIDO,
        INVALIDO
    }

    public enum TipoEntrada {
        VARIAVEL,
        PROCEDIMENTO,
        FUNCAO
    }

    static class EntradaTabelaDeSimbolos {
        String nome;
        Tipos tipo;
        TipoEntrada tipoE;

        private EntradaTabelaDeSimbolos(String nome, Tipos tipo, TipoEntrada tipoE) {
            this.nome = nome;
            this.tipo = tipo;
            this.tipoE = tipoE;
        }
    }

    public Tipos verificar(String nome) {
        nome = procFuncNome(nome, "[");
        return tabela.get(nome).tipo;
    }

    public void adicionar(String nome, Tipos tipo, TipoEntrada tipoE) {
        nome = procFuncNome(nome, "[");
        tabela.put(nome, new EntradaTabelaDeSimbolos(nome, tipo, tipoE));
    }    

    public boolean existe(String nome) {
        nome = procFuncNome(nome, "[");
        return tabela.containsKey(nome);
    }
}
