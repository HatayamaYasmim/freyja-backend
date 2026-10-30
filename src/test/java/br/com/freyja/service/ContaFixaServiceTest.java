package br.com.freyja.service;

import br.com.freyja.dto.ContaFixaRequestDTO;
import br.com.freyja.model.Categoria;
import br.com.freyja.model.ContaFixa;
import br.com.freyja.model.Grupo;
import br.com.freyja.repository.CategoriaRepository;
import br.com.freyja.repository.ContaFixaRepository;
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
class ContaFixaServiceTest {

    @Mock
    private ContaFixaRepository contaFixaRepo;

    @Mock
    private GrupoRepository grupoRepo;

    @Mock
    private CategoriaRepository  categoriaRepo;

    @InjectMocks
    private ContaFixaService service;

    private ContaFixaRequestDTO dto;
    private Grupo grupo;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        grupo = new Grupo();
        grupo.setId(1L);
        grupo.setNome("Casa");

        categoria = new Categoria();
        categoria.setId(2L);
        categoria.setNome("Moradia");

        dto = new ContaFixaRequestDTO();
        dto.setGrupoId(1L);
        dto.setCategoriaId(2L);
        dto.setTipo("1");
        dto.setValor(new BigDecimal("250.00"));
        dto.setUsuarioCriacao("teste");
        dto.setDataConta(LocalDate.of(2026, 10, 15));
        dto.setGastoRecorrente(true);
        dto.setDescricao("Conta de luz");
    }

    @Test
    void cadastrarConta() {
        when(grupoRepo.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(categoriaRepo.findById(2L))
                .thenReturn(Optional.of(categoria));

        when(contaFixaRepo.save(any(ContaFixa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContaFixa result = service.save(dto);

        assertNotNull(result);
        assertEquals(grupo, result.getGrupo());
        assertEquals(categoria, result.getCategoria());
        assertEquals(new BigDecimal("250.00"), result.getValor());
        assertEquals("Conta de luz", result.getDescricao());
        assertTrue(result.isGastoRecorrente());

        verify(contaFixaRepo).save(any(ContaFixa.class));

    }

    @Test
    void lancarExcecaoGrupo() {
        when(grupoRepo.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.save(dto)
        );

        assertEquals("Grupo não encontrado.", exception.getMessage());

        verify(contaFixaRepo, never()).save(any());
    }

    @Test
    void deveLancarExcecaoQuandoCategoriaNaoExiste() {
        when(grupoRepo.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(categoriaRepo.findById(2L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.save(dto)
        );

        assertEquals("Categoria não encontrada", exception.getMessage());

        verify(contaFixaRepo, never()).save(any());
    }

    @Test
    void deveDefinirStatusPadraoQuandoNaoInformado() {
        when(grupoRepo.findById(1L))
                .thenReturn(Optional.of(grupo));

        when(categoriaRepo.findById(2L))
                .thenReturn(Optional.of(categoria));

        when(contaFixaRepo.save(any(ContaFixa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContaFixa resultado = service.save(dto);

        assertEquals("F", resultado.getStatusPagamento());
    }

    @Test
    void deveListarContasDoMesCorreto() {
        LocalDate inicio = LocalDate.of(2026, 10, 1);
        LocalDate fim = LocalDate.of(2026, 10, 31);

        ContaFixa conta = new ContaFixa();
        conta.setDescricao("Conta de luz");

        when(contaFixaRepo.findByGrupoIdAndDataContaBetween(
                1L, inicio, fim
        )).thenReturn(List.of(conta));

        List<ContaFixa> resultado =
                service.listByMesGrupo(1L, 10, 2026);

        assertEquals(1, resultado.size());
        assertEquals("Conta de luz", resultado.get(0).getDescricao());

        verify(contaFixaRepo).findByGrupoIdAndDataContaBetween(
                1L, inicio, fim
        );
    }
}
