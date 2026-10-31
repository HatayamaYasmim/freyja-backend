
package br.com.freyja.controller;

import br.com.freyja.dto.ContaRecorrenteResponseDTO;
import br.com.freyja.service.ContaRecorrenteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ContaRecorrenteControllerTest {

    @Mock
    private ContaRecorrenteService service;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    private ContaRecorrenteResponseDTO response;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ContaRecorrenteController controller =
                new ContaRecorrenteController(service);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();

        response = new ContaRecorrenteResponseDTO(
                1L,
                "Internet residencial",
                1L,
                "Casa",
                1L,
                "Internet",
                new BigDecimal("120.00"),
                "MENSAL",
                10,
                LocalDate.of(2026, 10, 1),
                null,
                null
        );
    }

    @Test
    void deveCadastrarContaComSucesso() throws Exception {
        when(service.cadastrar(any()))
                .thenReturn(response);

        String json = """
                {
                    "descricao": "Internet residencial",
                    "grupoId": 1,
                    "categoriaId": 1,
                    "valorPrevisto": 120.00,
                    "periodicidade": "MENSAL",
                    "diaVencimento": 10,
                    "dataInicio": "2026-10-01"
                }
                """;

        mockMvc.perform(post("/contas-recorrentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/contas-recorrentes/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao")
                        .value("Internet residencial"));

        verify(service).cadastrar(any());
    }

    @Test
    void deveRejeitarCadastroComDadosInvalidos()
            throws Exception {

        String json = """
                {
                    "descricao": "",
                    "grupoId": 1,
                    "categoriaId": 1,
                    "valorPrevisto": -120.00,
                    "periodicidade": "MENSAL",
                    "diaVencimento": 40,
                    "dataInicio": "2026-10-01"
                }
                """;

        mockMvc.perform(post("/contas-recorrentes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void deveBuscarContaPorId() throws Exception {
        when(service.findByGrupo_Id(1L))
                .thenReturn(response);

        mockMvc.perform(get("/contas-recorrentes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.descricao")
                        .value("Internet residencial"));

        verify(service).findByGrupo_Id(1L);
    }

    @Test
    void deveListarContasPorGrupo() throws Exception {
        when(service.listarPorGrupo(1L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/contas-recorrentes/grupo/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].grupoNome")
                        .value("Casa"));

        verify(service).listarPorGrupo(1L);
    }
}
