package br.com.freyja.service;

import br.com.freyja.dto.GastoCartaoPreviewDTO;
import br.com.freyja.enums.OrigemImportacao;
import br.com.freyja.repository.GastosCartaoRepository;
import org.apache.pdfbox.Loader;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@Service
public class ImportacaoFaturaService {
    private  final GastosCartaoRepository gastoRepo;

    public  ImportacaoFaturaService(GastosCartaoRepository gastoRepo){
        this.gastoRepo = gastoRepo;
    }

    public List<GastoCartaoPreviewDTO> previewPDF(MultipartFile file) {
        try {
            String texto = extrairTextoPdf(file);
            return extrairComprasNubank(texto);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler PDF da fatura", e);
        }
    }

    private String extrairTextoPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private List<GastoCartaoPreviewDTO> extrairComprasNubank(String texto) {
        List<GastoCartaoPreviewDTO> itens = new ArrayList<>();

        String[] linhas = texto.split("\\r?\\n");

        Pattern patternCompra = Pattern.compile(
                "^(\\d{2})\\s+([A-Z]{3})\\s+••••\\s+\\d{4}\\s+(.+?)\\s+R\\$\\s+([\\d.,]+)$"
        );

        for (String linha : linhas) {
            linha = linha.trim();

            if (linha.isBlank()) continue;

            if (linha.contains("Pagamento em")) continue;
            if (linha.contains("Pagamentos e Financiamentos")) continue;
            if (linha.contains("Saldo restante")) continue;
            if (linha.contains("Juros")) continue;
            if (linha.contains("Encerramento")) continue;
            if (linha.contains("Crédito de parcelamento")) continue;
            if (linha.startsWith("IOF")) continue;

            Matcher matcher = patternCompra.matcher(linha);

            if (matcher.find()) {
                int dia = Integer.parseInt(matcher.group(1));
                String mesTexto = matcher.group(2);
                String descricaoCompleta = matcher.group(3).trim();
                BigDecimal valor = converterValor(matcher.group(4));

                LocalDate dataCompra = LocalDate.of(2026, converterMes(mesTexto), dia);

                Integer parcela = 1;
                Integer quantidadeParcelas = 1;

                Matcher parcelaMatcher = Pattern
                        .compile("(.+?)\\s+-\\s+Parcela\\s+(\\d+)/(\\d+)")
                        .matcher(descricaoCompleta);

                String descricao = descricaoCompleta;

                if (parcelaMatcher.find()) {
                    descricao = parcelaMatcher.group(1).trim();
                    parcela = Integer.parseInt(parcelaMatcher.group(2));
                    quantidadeParcelas = Integer.parseInt(parcelaMatcher.group(3));
                }

                String idExterno = gerarIdExterno(dataCompra, descricao, valor, parcela, quantidadeParcelas);

                GastoCartaoPreviewDTO dto = new GastoCartaoPreviewDTO();
                dto.setDataCompra(dataCompra);
                dto.setDescricao(descricao);
                dto.setValor(valor);
                dto.setParcela(parcela);
                dto.setQuantidadeParcelas(quantidadeParcelas);
                dto.setIdExterno(idExterno);

                boolean duplicado = gastoRepo.existsByIdExterno(idExterno);
                dto.setDuplicado(duplicado);
                dto.setImportar(!duplicado);

                itens.add(dto);
            }
        }

        return itens;
    }

    private BigDecimal converterValor(String valorTexto) {
        String normalizado = valorTexto
                .replace(".", "")
                .replace(",", ".");

        return new BigDecimal(normalizado);
    }

    private int converterMes(String mes) {
        return switch (mes.toUpperCase()) {
            case "JAN" -> 1;
            case "FEV" -> 2;
            case "MAR" -> 3;
            case "ABR" -> 4;
            case "MAI" -> 5;
            case "JUN" -> 6;
            case "JUL" -> 7;
            case "AGO" -> 8;
            case "SET" -> 9;
            case "OUT" -> 10;
            case "NOV" -> 11;
            case "DEZ" -> 12;
            default -> throw new IllegalArgumentException("Mês inválido: " + mes);
        };
    }

    private String gerarIdExterno(
            LocalDate dataCompra,
            String descricao,
            BigDecimal valor,
            Integer parcela,
            Integer quantidadeParcelas
    ) {
        String chave = dataCompra + "|" +
                normalizarDescricao(descricao) + "|" +
                valor + "|" +
                parcela + "/" +
                quantidadeParcelas;

        return DigestUtils.md5DigestAsHex(chave.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizarDescricao(String descricao) {
        return descricao == null
                ? ""
                : descricao.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    public List<GastoCartaoPreviewDTO> previewOFX(MultipartFile file) {
        try {
            String texto = new String(file.getBytes(), StandardCharsets.UTF_8);
            return extrairComprasOFX(texto);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler OFX", e);
        }


    }

    private List<GastoCartaoPreviewDTO> extrairComprasOFX(String texto) {
        List<GastoCartaoPreviewDTO> itens = new ArrayList<>();

        Pattern transacaoPattern = Pattern.compile(
                "<STMTTRN>(.*?)</STMTTRN>",
                Pattern.DOTALL
        );

        Matcher transacoes = transacaoPattern.matcher(texto);

        while (transacoes.find()) {
            String bloco = transacoes.group(1);

            String tipo = extrairTag(bloco, "TRNTYPE");
            String dataTexto = extrairTag(bloco, "DTPOSTED");
            String valorTexto = extrairTag(bloco, "TRNAMT");
            String fitId = extrairTag(bloco, "FITID");
            String descricao = extrairTag(bloco, "MEMO");

            if (descricao == null || descricao.isBlank()) {
                descricao = extrairTag(bloco, "NAME");
            }

            if (dataTexto == null || valorTexto == null) {
                continue;
            }

            BigDecimal valor = new BigDecimal(valorTexto.trim()).abs();

            LocalDate dataCompra = LocalDate.parse(
                    dataTexto.substring(0, 8),
                    java.time.format.DateTimeFormatter.BASIC_ISO_DATE
            );

            String idExterno = fitId != null && !fitId.isBlank()
                    ? "OFX-" + fitId
                    : gerarIdExterno(dataCompra, descricao, valor, 1, 1);

            GastoCartaoPreviewDTO dto = new GastoCartaoPreviewDTO();
            dto.setDataCompra(dataCompra);
            dto.setDescricao(descricao);
            dto.setValor(valor);
            dto.setParcela(1);
            dto.setQuantidadeParcelas(1);
            dto.setIdExterno(idExterno);

            boolean duplicado = gastoRepo.existsByIdExterno(idExterno);
            dto.setDuplicado(duplicado);
            dto.setImportar(!duplicado);

            itens.add(dto);
        }

        return itens;
    }

    private String extrairTag(String bloco, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">([^<\\r\\n]+)");
        Matcher matcher = pattern.matcher(bloco);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    public List<GastoCartaoPreviewDTO> preview(
            MultipartFile file,
            OrigemImportacao tipoImportacao
    ) {
        return switch (tipoImportacao) {
            case PDF -> previewPDF(file);
            case OFX -> previewOFX(file);
//            case XLSX -> previewXLSX(file);
            default -> throw new IllegalArgumentException(
                    "Tipo de importação não suportado: " + tipoImportacao
            );
        };
    }

}
