package br.uel.avaliacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * crUEL — Sistema de Avaliação Anônima de Disciplinas e Docentes.
 *
 * <p>Arquitetura em camadas: Controller → Service (regras e transações) → DAO (JDBC puro) → PostgreSQL.
 */
@SpringBootApplication
public class AvaliacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvaliacaoApplication.class, args);
    }
}
