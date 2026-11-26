package br.uel.avaliacao.config;

import java.time.Clock;
import java.time.ZoneId;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuração explícita da camada de acesso a dados via JDBC.
 *
 * <p>O {@link DataSource} (pool HikariCP) é criado pelo Spring Boot a partir das propriedades
 * {@code spring.datasource.*} (lidas do {@code .env}). Aqui declaramos os componentes JDBC usados
 * pelos DAOs e o gerenciador de transações que dá suporte ao {@code @Transactional} dos services —
 * internamente ele executa {@code connection.setAutoCommit(false)}, {@code commit()} e
 * {@code rollback()} na mesma {@link java.sql.Connection}.
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    /** Relógio injetável (fuso de Londrina) usado para validar a janela de resposta das avaliações. */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
