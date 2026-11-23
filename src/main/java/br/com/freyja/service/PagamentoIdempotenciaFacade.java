package br.com.freyja.service;

import br.com.freyja.dto.lancamento.LancamentoResponseDTO;
import br.com.freyja.dto.pagamento.RegistrarPagamentoRequestDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class PagamentoIdempotenciaFacade {
    private final PagamentoService pagamentoService;

    public PagamentoIdempotenciaFacade(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    public LancamentoResponseDTO registrar(
            Long lancamentoId,
            Long versaoEsperada,
            RegistrarPagamentoRequestDTO request,
            String idempotencykey
            ) {
        try {
            return pagamentoService.registrarPagamentoParcial(
                    lancamentoId, versaoEsperada, request, idempotencykey
            );
        } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException exception) {
            if(idempotencykey == null) {
                throw exception;
            }

            return  pagamentoService.recuperarRetryConfirmado(
                    lancamentoId,
                    request,
                    idempotencykey
            ).orElseThrow(() -> exception);
        }
    }
}

