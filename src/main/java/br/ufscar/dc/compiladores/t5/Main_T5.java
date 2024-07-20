package br.ufscar.dc.compiladores.t5;

// Importações
import br.ufscar.dc.compiladores.t5.LAParser.ProgramaContext;
import java.io.IOException;
import java.io.PrintWriter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Main_T5 {

    public static void main(String[] args) throws IOException {
        CharStream cs = CharStreams.fromFileName(args[0]);

        // Analisador léxico
        LALexer lexer = new LALexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Analisador semântico
        LAParser parser = new LAParser(tokens);
        ProgramaContext arvore = parser.programa();
        Parser_T5 t5s = new Parser_T5();

        t5s.visitPrograma(arvore);

        // Erros detectados
        ParserUtils_T5.errosSemanticos.forEach(System.out::println);

        // Caso não tenham erros no programa, é gerado o código em C
        if (ParserUtils_T5.errosSemanticos.isEmpty()) {
            GeradorDeCodigoC agc = new GeradorDeCodigoC();
            agc.visitPrograma(arvore);

            try(PrintWriter pw = new PrintWriter(args[1])) {
                pw.print(agc.saida.toString());
            }
        }
    }
}
