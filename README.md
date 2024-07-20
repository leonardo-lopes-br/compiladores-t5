## Integrantes do grupo

Gabriel Ripper de Mendonça Furtado - RA: 804070 - Curso: BCC

Leonardo da Silva Lopes - RA: 761880 - Curso: BCC

Maria Luíza Edwards de Magalhães Cordeiro - RA: 802645 - Curso: BCC

## Passo a passo para a execução do analisador sintático

## Instalações

Uma vez que o ANTLR é feito na linguagem Java, é necessário o ter instalado. Isso pode ser feito por meio do link abaixo:
[Download Java](https://www.java.com/pt-BR/download/)

Caso não tenha o Maven instalado, pode-se baixá-lo por meio do link abaixo:
[Download Maven](https://maven.apache.org/download.cgi)

Por fim, para a parte de testes, temos o corretor automático fornecido pelo professor. Este encontra-se no link abaixo:
[Download corretor automático](https://github.com/dlucredio/compiladores-corretor-automatico/blob/master/target/compiladores-corretor-automatico-1.0-SNAPSHOT-jar-with-dependencies.jar)

### Ambiente
Recomenda-se o uso de uma das IDE's: IntelliJ ou NetBeans

### Execução

O programa principal `Main_T5.java` recebe dois parâmetros: o caminho para o arquivo de entrada e o caminho para o arquivo de saída, respectivamente.
Uma vez que o projeto utiliza o plugin do Maven para a execução e estamos utilizando o ANTLR, é necessário manter a hierarquia de diretórios na pasta src/main, onde o aninhamento de pastas em `antlr4` deve ser o mesmo que em `java`.
Assim, executamos o comando para gerar a linguagem com o ANTLR:
`mvn generate-sources`

Em seguida, executamos o comando para a compilação e geração do arquivo executável:
`mvn package`

Feito isso, já é possível testar o programa. Será gerado um arquivo com um nome: `t5-1.0-SNAPSHOT-jar-with-dependencies.jar` dentro da pasta `target` gerada.
Para executá-lo podemos rodar o comando abaixo, substituindo os caminhos absolutos em cada caso:
`java -jar "caminho/para/t5-1.0-SNAPSHOT-jar-with-dependencies.jar" "caminho/para/arquivo/entrada.txt" "caminho/arquivo/saida.txt" `


### Teste

Para a verificação dos resultados, utilizando o corretor automático fornecido pelo professor, utilizamos o comando abaixo. Para executá-lo, substitua os campos com o caminho absoluto correspondente em cada caso:
`java -jar "caminho/para/corretor-automatico.jar>" "java -jar caminho/para/t5-1.0-SNAPSHOT-jar-with-dependencies.jar"  "/caminho/para/compilador_gcc>" "caminho/para/pasta/saida_gerada>" "caminho/para/pasta/casos-de-teste>" "761880, 802645, 804070" "tipoTeste (t5|gabarito)"`

### Resultados

![Resultados](/imagens/casos_passando.png)