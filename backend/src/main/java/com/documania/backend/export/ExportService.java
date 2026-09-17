package com.documania.backend.export;

import com.documania.backend.client.Client;
import com.documania.backend.client.ClientRepository;
import com.documania.backend.order.CustomerOrder;
import com.documania.backend.order.CustomerOrderRepository;
import com.documania.backend.subscription.Subscription;
import com.documania.backend.subscription.SubscriptionRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClientRepository clientRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final SubscriptionRepository subscriptionRepository;

    public ExportService(
        ClientRepository clientRepository,
        CustomerOrderRepository customerOrderRepository,
        SubscriptionRepository subscriptionRepository
    ) {
        this.clientRepository = clientRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportClients() {
        List<Client> clients = new ArrayList<>();
        clients.addAll(clientRepository.findAllByArchivedOrderByCreatedAtDesc(false));
        clients.addAll(clientRepository.findAllByArchivedOrderByCreatedAtDesc(true));

        String[] headers = {"Société", "E-mail", "Prénom", "Nom", "Téléphone", "Adresse", "Secteur", "Créé le", "Archivé"};
        List<Object[]> rows = clients.stream()
            .map(client -> new Object[]{
                client.getCompanyName(),
                client.getUserAccount().getEmail(),
                client.getUserAccount().getFirstName(),
                client.getUserAccount().getLastName(),
                client.getPhone(),
                client.getAddress(),
                client.getSector(),
                client.getCreatedAt(),
                client.isArchived()
            })
            .toList();

        return renderXlsx("Clients", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportOrders() {
        List<CustomerOrder> orders = customerOrderRepository.findAllByOrderByCreatedAtDesc();

        String[] headers = {"N° commande", "Client", "Service", "Offre", "Statut", "Prix", "Créée le", "Traitée le"};
        List<Object[]> rows = orders.stream()
            .map(order -> new Object[]{
                order.getOrderNumber(),
                order.getClientCompanyNameSnapshot(),
                order.getServiceNameSnapshot(),
                order.getOfferNameSnapshot(),
                order.getStatus(),
                order.getPriceSnapshot(),
                order.getCreatedAt(),
                order.getProcessedAt()
            })
            .toList();

        return renderXlsx("Commandes", headers, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAllByOrderByCreatedAtDesc();

        String[] headers = {"Client", "Offre", "Statut", "Début", "Fin", "Créé le"};
        List<Object[]> rows = subscriptions.stream()
            .map(subscription -> new Object[]{
                subscription.getClientCompanyNameSnapshot(),
                subscription.getOfferNameSnapshot(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getCreatedAt()
            })
            .toList();

        return renderXlsx("Abonnements", headers, rows);
    }

    private byte[] renderXlsx(String sheetName, String[] headers, List<Object[]> rows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Object[] rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowData.length; i++) {
                    row.createCell(i).setCellValue(cellValue(rowData[i]));
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Échec de la génération du fichier Excel", exception);
        }
    }

    private String cellValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_TIME_FORMATTER);
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof BigDecimal amount) {
            return amount.toPlainString();
        }
        if (value instanceof Boolean archived) {
            return archived ? "Oui" : "Non";
        }
        if (value instanceof Enum<?> status) {
            return status.name();
        }
        return neutralizeFormula(value.toString());
    }

    // Empêche l'injection de formule tableur : préfixe d'une apostrophe toute
    // chaîne commençant par un caractère de formule.
    private String neutralizeFormula(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        char first = text.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + text;
        }
        return text;
    }
}