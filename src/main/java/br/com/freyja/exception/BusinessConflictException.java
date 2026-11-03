package br.com.freyja.exception;

public class BusinessConflictException extends RuntimeException {
    public BusinessConflictException(String mensagem) {
        super(mensagem);
    }
}