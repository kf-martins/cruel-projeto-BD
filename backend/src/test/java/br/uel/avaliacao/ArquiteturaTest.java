package br.uel.avaliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import br.uel.avaliacao.model.RespostaQuestao;
import br.uel.avaliacao.model.SubmissaoAnonima;

/** Verificações estruturais das restrições do edital: sem JPA e anonimato no esquema e no modelo. */
class ArquiteturaTest {

    @Test
    void naoHaJpaNemHibernateNoClasspath() {
        assertThatThrownBy(() -> Class.forName("jakarta.persistence.Entity")).isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("org.hibernate.Session")).isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void modelosDaSubmissaoNaoReferenciamUsuario() {
        for (Class<?> tipo : new Class<?>[] {SubmissaoAnonima.class, RespostaQuestao.class}) {
            assertThat(Arrays.stream(tipo.getRecordComponents()).map(c -> c.getName().toLowerCase(Locale.ROOT)))
                    .as("componentes de %s", tipo.getSimpleName())
                    .noneMatch(nome -> nome.contains("usuario"));
        }
    }

    @Test
    void tabelasAnonimasNoSchemaNaoTemColunaOuFkParaUsuario() throws IOException {
        String schema = Files.readString(Path.of("..", "database", "schema.sql"), StandardCharsets.UTF_8);
        for (String tabela : new String[] {"submissao_anonima", "resposta_questao"}) {
            Matcher m = Pattern.compile("CREATE TABLE " + tabela + " \\((.*?)\\n\\);", Pattern.DOTALL).matcher(schema);
            assertThat(m.find()).as("definição de %s encontrada", tabela).isTrue();
            assertThat(m.group(1).toLowerCase(Locale.ROOT)).as("DDL de %s", tabela).doesNotContain("usuario");
        }
    }
}
