
package br.com.freyja.exception;

import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(
            ResourceNotFoundException ex
    ) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ProblemDetail> conflict(
            BusinessConflictException ex
    ) {
        return erro(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> badRequest(
            IllegalArgumentException ex
    ) {
        return erro(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validation(
            MethodArgumentNotValidException ex
    ) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados inválidos");

        return erro(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<ProblemDetail> tratarConflitoConcorrencia(
            Exception exception
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Este lançamento foi modificado por outra operação. " +
                        "Atualize os dados antes de tentar novamente."
        );

        problema.setTitle("Conflito de concorrência");

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problema);
    }

    private ResponseEntity<ProblemDetail> erro(
            HttpStatus status,
            String mensagem
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                mensagem
        );

        return ResponseEntity.status(status).body(problem);
    }
}
