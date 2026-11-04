package br.com.freyja.service;

import br.com.freyja.dto.contarecorrente.AtualizarContaRecorrenteRequestDTO;
import br.com.freyja.dto.contarecorrente.ContaRecorrenteRequestDTO;
import br.com.freyja.dto.contarecorrente.ContaRecorrenteResponseDTO;
import br.com.freyja.dto.contarecorrente.EncerrarContaRecorrenteRequestDTO;
import br.com.freyja.exception.BusinessConflictException;
import br.com.freyja.exception.ResourceNotFoundException;
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


    @Test
    void deveAtualizarContaRecorrente() {
        ContaRecorrente conta = criarEntidade();

        AtualizarContaRecorrenteRequestDTO request =
                new AtualizarContaRecorrenteRequestDTO(
                        "Internet atualizada",
                        null,
                        new BigDecimal("150.00"),
                        15
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.save(any(ContaRecorrente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContaRecorrenteResponseDTO resultado =
                service.atualizar(1L, request);

        assertEquals("Internet atualizada", resultado.descricao());
        assertEquals(new BigDecimal("150.00"), resultado.valorPrevisto());
        assertEquals(15, resultado.diaVencimento());

        // Campos não enviados devem permanecer inalterados.
        assertEquals(2L, resultado.categoriaId());
        assertEquals("MENSAL", resultado.periodicidade());
        assertEquals(LocalDate.of(2026, 10, 1), resultado.dataInicio());
        assertTrue(resultado.ativa());

        verify(repository).save(conta);
    }


    @Test
    void naoDeveAtualizarComDescricaoVazia() {
        ContaRecorrente conta = criarEntidade();

        AtualizarContaRecorrenteRequestDTO request =
                new AtualizarContaRecorrenteRequestDTO(
                        "   ",
                        null,
                        null,
                        null
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizar(1L, request)
        );

        assertEquals("Internet", conta.getDescricao());

        verify(repository, never()).save(any());
    }


    @Test
    void naoDeveAtualizarComCategoriaInexistente() {
        ContaRecorrente conta = criarEntidade();

        AtualizarContaRecorrenteRequestDTO request =
                new AtualizarContaRecorrenteRequestDTO(
                        null,
                        999L,
                        null,
                        null
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(categoriaRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.atualizar(1L, request)
        );

        assertEquals(2L, conta.getCategoria().getId());

        verify(repository, never()).save(any());
    }


    @Test
    void deveDesativarContaRecorrente() {
        ContaRecorrente conta = criarEntidade();

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.save(any(ContaRecorrente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContaRecorrenteResponseDTO resultado =
                service.desativar(1L);

        assertFalse(resultado.ativa());
        assertFalse(conta.isAtiva());

        verify(repository).save(conta);
    }

    @Test
    void naoDeveDesativarContaJaInativa() {
        ContaRecorrente conta = criarEntidade();
        conta.setAtiva(false);

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        assertThrows(
                BusinessConflictException.class,
                () -> service.desativar(1L)
        );

        verify(repository, never()).save(any());
    }


    @Test
    void deveEncerrarContaRecorrente() {
        ContaRecorrente conta = criarEntidade();

        EncerrarContaRecorrenteRequestDTO request =
                new EncerrarContaRecorrenteRequestDTO(
                        LocalDate.of(2027, 2, 28)
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        when(repository.save(any(ContaRecorrente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContaRecorrenteResponseDTO resultado =
                service.encerrar(1L, request);

        assertEquals(
                LocalDate.of(2027, 2, 28),
                resultado.dataFim()
        );

        assertTrue(resultado.ativa());

        verify(repository).save(conta);
    }


    @Test
    void naoDeveEncerrarComDataAnteriorAoInicio() {
        ContaRecorrente conta = criarEntidade();

        EncerrarContaRecorrenteRequestDTO request =
                new EncerrarContaRecorrenteRequestDTO(
                        LocalDate.of(2026, 9, 30)
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.encerrar(1L, request)
        );

        assertNull(conta.getDataFim());

        verify(repository, never()).save(any());
    }


    @Test
    void naoDeveEncerrarContaJaEncerrada() {
        ContaRecorrente conta = criarEntidade();
        conta.setDataFim(LocalDate.of(2027, 2, 28));

        EncerrarContaRecorrenteRequestDTO request =
                new EncerrarContaRecorrenteRequestDTO(
                        LocalDate.of(2027, 3, 31)
                );

        when(repository.findById(1L))
                .thenReturn(Optional.of(conta));

        assertThrows(
                BusinessConflictException.class,
                () -> service.encerrar(1L, request)
        );

        assertEquals(
                LocalDate.of(2027, 2, 28),
                conta.getDataFim()
        );

        verify(repository, never()).save(any());
    }


}
