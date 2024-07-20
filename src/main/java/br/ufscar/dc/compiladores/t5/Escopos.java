package br.ufscar.dc.compiladores.t5;

// Importações
import java.util.LinkedList;
import java.util.List;

// Classe baseada em código disponibilizado de exemplo pelo professor
public final class Escopos {

    private final LinkedList<TabelaDeSimbolos> pilhaDeTabelas;

    public Escopos() {
        pilhaDeTabelas = new LinkedList<>();
        criarNovoEscopo();
    }

    public void criarNovoEscopo() {
        pilhaDeTabelas.push(new TabelaDeSimbolos());
    }

    public TabelaDeSimbolos obterEscopoAtual() {
        return pilhaDeTabelas.peek();
    }

    public List<TabelaDeSimbolos> percorrerEscoposAninhados() {
        return pilhaDeTabelas;
    }
    
    public void abandonarEscopo(){
        pilhaDeTabelas.pop();
    }
}
