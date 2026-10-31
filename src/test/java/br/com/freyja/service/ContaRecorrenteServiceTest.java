package br.com.freyja.service;

import br.com.freyja.dto.ContaRecorrenteRequestDTO;
import br.com.freyja.dto.ContaRecorrenteResponseDTO;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.ContaRecorrente;
import br.com.freyja.model.Grupo;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.ContaRecorrenteRepository;
import br.com.freyja.repository.GrupoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaRecorrenteServiceTest {

    @Mock
    private ContaRecorrenteRepository repository;

    @Mock
    private GrupoRepository grupoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ContaRecorrenteService service;

    private Grupo grupo;
    private Categoria categoria;
    private ContaRecorrenteRequestDTO dto;

    @BeforeEach
    void setUp() {
        grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNome("Casa");

        categoria = new Categoria();
        categoria.setId(2L);
        categoria.setNome("Moradia");

        dto = criarDTO("MENSAL",
                LocalDate.of(2026, 10, 1), null);
    }

    private ContaRecorrenteRequestDTO criarDTO(
            String periodicidade,
            LocalDate inicio,
            LocalDate fim
    ) {
        return new ContaRecorrenteRequestDTO(
                "Internet",
                1L,
                2L,
                new BigDecimal("120.00"),
                periodicidade,
                10,
                inicio,
                fim,
                null
        );
    }

    private ContaRecorrente criarEntidade() {
        ContaRecorrente conta = new ContaRecorrente();
        conta.setId(1L);
        conta.setGrupo(grupo);
        conta.setCategoria(categoria);
        conta.setDescricao("Internet");
        conta.setValorPrevisto(new BigDecimal("120.00"));
        conta.setPeriodicidade(
                ContaRecorrente.Periodicidade.MENSAL
        );
        conta.setDiaVencimento(10);
        conta.setDataInicio(LocalDate.of(2026, 10, 1));
        conta.setAtiva(true);
        return conta;
    }

    @Test
    void cadastrar() {
        when(grupoRepository.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(categoriaRepository.findById(2L))
                .thenReturn(Optional.of(categoria));

        when(repository.save(any(ContaRecorrente.class)))
                .thenAnswer(invocation -> {
                    ContaRecorrente conta = invocation.getArgument(0);
                    conta.setId(1L);
                    return conta;
                });

        ContaRecorrenteResponseDTO result = service.cadastrar(dto);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Internet", result.descricao());
        assertEquals("Casa", result.grupoNome());
        assertEquals("Moradia", result.categoriaNome());
        assertEquals("MENSAL", result.periodicidade());
        assertEquals(new BigDecimal("120.00"),
                result.valorPrevisto());

        verify(repository).save(any(ContaRecorrente.class));
    }

    @Test
    void rejeitarGrupoInexistente() {
        when(grupoRepository.findById(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(dto)
        );

        assertEquals("Grupo não encontrado",
                exception.getMessage());

        verify(repository, never()).save(any());
    }

    @Test
    void rejeitarCategoriaInexistente() {
        when(grupoRepository.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(categoriaRepository.findById(2L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(dto)
        );

        assertEquals(
                "Categoria não encontrada",
                exception.getMessage()
        );

        verify(grupoRepository).findById(1L);
        verify(categoriaRepository).findById(2L);
        verify(repository, never()).save(any());
    }

    @Test
    void deveRejeitarDatasInvalidas() {
        dto = criarDTO(
                "MENSAL",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 9, 30)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(dto)
        );

        assertEquals(
                "A data final não pode ser anterior à inicial",
                exception.getMessage()
        );

        verifyNoInteractions(
                grupoRepository,
                categoriaRepository,
                repository
        );
    }

    @Test
    void deveRejeitarPeriodicidadeInvalida() {
        dto = criarDTO(
                "SEMANAL",
                LocalDate.of(2026, 10, 1),
                null
        );

        when(grupoRepository.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(categoriaRepository.findById(2L))
                .thenReturn(Optional.of(categoria));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(dto)
        );

        assertEquals(
                "Periodicidade inválida. Use MENSAL ou ANUAL",
                exception.getMessage()
        );

        verify(repository, never()).save(any());
    }

    @Test
    void deveBuscarContaPorId() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(criarEntidade()));

        ContaRecorrenteResponseDTO resultado =
                service.findByGrupo_Id(1L);

        assertEquals(1L, resultado.id());
        assertEquals("Internet", resultado.descricao());

        verify(repository).findById(1L);
    }

    @Test
    void deveListarContasPorGrupo() {
        when(repository.findByGrupo_Id(1L))
                .thenReturn(List.of(criarEntidade()));

        List<ContaRecorrenteResponseDTO> resultado =
                service.listarPorGrupo(1L);

        assertEquals(1, resultado.size());
        assertEquals("Internet",
                resultado.get(0).descricao());

        verify(repository).findByGrupo_Id(1L);
    }

}
