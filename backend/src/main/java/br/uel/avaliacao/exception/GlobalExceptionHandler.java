package br.uel.avaliacao.exception;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte exceções em respostas JSON padronizadas: {@code {status, erro, mensagem, timestamp}}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraNegocio(RegraNegocioException e) {
        return corpo(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return corpo(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> requisicaoMalFormada(Exception e) {
        log.warn("Requisição mal formada: {}", e.getMessage());
        return corpo(HttpStatus.BAD_REQUEST, "Requisição mal formada.");
    }

    /** Violação de PK/UNIQUE — ex.: PK (id_avaliacao, id_usuario) de participacao_avaliacao. */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> duplicidade(DuplicateKeyException e) {
        log.warn("Violação de unicidade: {}", e.getMostSpecificCause().getMessage());
        return corpo(HttpStatus.CONFLICT, "Registro duplicado: já existe um registro com estes dados.");
    }

    /** Violação de FK/CHECK/NOT NULL detectada pelo PostgreSQL. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return corpo(HttpStatus.BAD_REQUEST, "Dados inválidos: violação de restrição do banco de dados.");
    }

    @ExceptionHandler({CannotGetJdbcConnectionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<Map<String, Object>> bancoIndisponivel(Exception e) {
        log.error("Banco de dados indisponível", e);
        return corpo(HttpStatus.SERVICE_UNAVAILABLE, "Banco de dados indisponível. Verifique se o PostgreSQL está em execução.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return corpo(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor.");
    }

    private static ResponseEntity<Map<String, Object>> corpo(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "erro", status.getReasonPhrase(),
                "mensagem", mensagem == null ? "" : mensagem,
                "timestamp", OffsetDateTime.now().toString()));
    }
}
