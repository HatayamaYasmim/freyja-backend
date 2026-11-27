
package br.com.freyja.service;

import br.com.freyja.dto.receita.ReceitaRequestDTO;
import br.com.freyja.dto.receita.ReceitaResponseDTO;
import br.com.freyja.enums.StatusReceita;
import br.com.freyja.exception.ResourceNotFoundException;
import br.com.freyja.model.Grupo;
import br.com.freyja.model.Receita;
import br.com.freyja.repository.GrupoRepository;
import br.com.freyja.repository.ReceitaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceitaServiceTest {

    private static final Long GRUPO_ID = 1L;
    private static final Long RECEITA_ID = 10L;

    private static final YearMonth MES =
            YearMonth.of(2026, 11);

    @Mock
    private ReceitaRepository receitaRepository;

    @Mock
    private GrupoRepository grupoRepository;

    @InjectMocks
    private ReceitaService receitaService;

    private Grupo grupo;

    @BeforeEach
    void setUp() {
        grupo = new Grupo();
        grupo.setId(GRUPO_ID);
        grupo.setNome("Pessoal");
    }

    private ReceitaRequestDTO criarRequest() {
        return new ReceitaRequestDTO(
                GRUPO_ID,
                "Salário mensal",
                new BigDecimal("5000.00"),
                MES.atDay(1),
                MES.atDay(5)
        );
    }

    private Receita criarReceita() {
        Receita receita = new Receita();
        receita.setId(RECEITA_ID);
        receita.setGrupo(grupo);
        receita.setDescricao("Salário mensal");
        receita.setValorPrevisto(new BigDecimal("5000.00"));
        receita.setValorRecebido(BigDecimal.ZERO);
        receita.setCompetencia(MES.atDay(1));
        receita.setDataPrevista(MES.atDay(5));
        receita.setStatus(StatusReceita.PENDENTE);
        receita.setVersion(0L);
        return receita;
    }

    @Test
    void deveCadastrarReceitaPendente() {
        when(grupoRepository.findById(GRUPO_ID))
                .thenReturn(Optional.of(grupo));

        when(receitaRepository.saveAndFlush(any(Receita.class)))
                .thenAnswer(invocation -> {
                    Receita receita = invocation.getArgument(0);
                    receita.setId(RECEITA_ID);
                    receita.setVersion(0L);
                    return receita;
                });

        ReceitaResponseDTO resultado =
                receitaService.cadastrar(criarRequest());

        assertAll(
                () -> assertEquals(RECEITA_ID, resultado.id()),
                () -> assertEquals(GRUPO_ID, resultado.grupoId()),
                () -> assertEquals("Salário mensal", resultado.descricao()),
                () -> assertEquals(
                        0,
                        new BigDecimal("5000.00")
                                .compareTo(resultado.valorPrevisto())
                ),
                () -> assertEquals(
                        0,
                        BigDecimal.ZERO.compareTo(resultado.valorRecebido())
                ),
                () -> assertEquals(StatusReceita.PENDENTE, resultado.status()),
                () -> assertNull(resultado.dataRecebimento()),
                () -> assertEquals(0L, resultado.version())
        );

        verify(receitaRepository).saveAndFlush(any(Receita.class));
    }

    @Test
    void deveRejeitarGrupoInexistente() {
        when(grupoRepository.findById(GRUPO_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> receitaService.cadastrar(criarRequest())
        );

        verify(receitaRepository, never())
                .saveAndFlush(any(Receita.class));
    }

    @Test
    void deveRejeitarCompetenciaForaDoPrimeiroDia() {
        ReceitaRequestDTO request = new ReceitaRequestDTO(
                GRUPO_ID,
                "Salário mensal",
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 11, 15),
                MES.atDay(5)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> receitaService.cadastrar(request)
        );

        verifyNoInteractions(grupoRepository, receitaRepository);
    }

    @Test
    void deveListarReceitasDoGrupoNoMes() {
        when(grupoRepository.existsById(GRUPO_ID))
                .thenReturn(true);

        when(receitaRepository
                .findByGrupo_IdAndCompetenciaGreaterThanEqualAndCompetenciaLessThanOrderByDataPrevistaAscIdAsc(
                        GRUPO_ID,
                        MES.atDay(1),
                        MES.plusMonths(1).atDay(1)
                ))
                .thenReturn(List.of(criarReceita()));

        List<ReceitaResponseDTO> resultado =
                receitaService.listarPorGrupoEMes(GRUPO_ID, MES);

        assertEquals(1, resultado.size());
        assertEquals(RECEITA_ID, resultado.get(0).id());
        assertEquals(StatusReceita.PENDENTE, resultado.get(0).status());

        verify(receitaRepository)
                .findByGrupo_IdAndCompetenciaGreaterThanEqualAndCompetenciaLessThanOrderByDataPrevistaAscIdAsc(
                        GRUPO_ID,
                        MES.atDay(1),
                        MES.plusMonths(1).atDay(1)
                );
    }
}
