package com.banana.harvest.service;

import com.banana.harvest.entity.Batch;
import com.banana.harvest.entity.BatchCost;
import com.banana.harvest.entity.Sale;
import com.banana.harvest.entity.enums.SaleType;
import com.banana.harvest.exception.BusinessException;
import com.banana.harvest.repository.BatchCostRepository;
import com.banana.harvest.repository.SaleRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * Service for generating PDF invoices
 * Supports both Domestic (GST) and Export invoices
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfInvoiceService {

    private final SaleRepository saleRepository;
    private final BatchCostRepository batchCostRepository;

    // Company details (should come from config)
    private static final String COMPANY_NAME = "Banana Harvest Export Pvt. Ltd.";
    private static final String COMPANY_ADDRESS = "123 Agriculture Road, Mumbai, Maharashtra, India - 400001";
    private static final String COMPANY_GST = "27AABCU9603R1ZX";
    private static final String COMPANY_PHONE = "+91 98765 43210";
    private static final String COMPANY_EMAIL = "info@bananaharvest.com";

    // Colors
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(57, 255, 20); // Neon Green
    private static final DeviceRgb DARK_COLOR = new DeviceRgb(13, 17, 23); // Dark background
    private static final DeviceRgb TEXT_COLOR = new DeviceRgb(50, 50, 50);

    /**
     * Generates PDF invoice for a sale
     */
    public byte[] generateInvoice(UUID saleId) {
        try {
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new BusinessException("Sale not found"));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Add content based on sale type
            if (sale.getSaleType() == SaleType.DOMESTIC) {
                generateDomesticInvoice(document, sale);
            } else {
                generateExportInvoice(document, sale);
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating invoice PDF", e);
            throw new BusinessException("Failed to generate invoice PDF: " + e.getMessage());
        }
    }

    /**
     * Generates Domestic Tax Invoice with GST
     */
    private void generateDomesticInvoice(Document document, Sale sale) {
        // Header
        addInvoiceHeader(document, "TAX INVOUE");

        // Invoice Info
        addInvoiceInfo(document, sale);

        // Company and Buyer Details
        addCompanyAndBuyerDetails(document, sale);

        // Items Table
        addItemsTable(document, sale);

        // Totals
        addTotalsSection(document, sale);

        // GST Breakdown
        addGSTBreakdown(document, sale);

        // Bank Details
        addBankDetails(document);

        // Footer
        addFooter(document, "This is a computer-generated invoice.");
    }

    /**
     * Generates Export Commercial Invoice
     */
    private void generateExportInvoice(Document document, Sale sale) {
        // Header
        addInvoiceHeader(document, "COMMERCIAL INVOICE");

        // Invoice Info
        addInvoiceInfo(document, sale);

        // Exporter and Consignee Details
        addExporterAndConsigneeDetails(document, sale);

        // Items Table
        addExportItemsTable(document, sale);

        // Totals
        addExportTotalsSection(document, sale);

        // Declaration
        addExportDeclaration(document);

        // Footer
        addFooter(document, "Export Invoice - Original for Buyer");
    }

    private void addInvoiceHeader(Document document, String title) {
        // Company Name
        Paragraph companyName = new Paragraph(COMPANY_NAME)
                .setFontSize(20)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(companyName);

        // Title
        Paragraph invoiceTitle = new Paragraph(title)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(invoiceTitle);

        // Line separator
        document.add(new LineSeparator(new SolidLine(2f)));
        document.add(new Paragraph().setMarginBottom(10));
    }

    private void addInvoiceInfo(Document document, Sale sale) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}));
        infoTable.setWidth(UnitValue.createPercentValue(100));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        infoTable.addCell(createInfoCell("Invoice No:", sale.getInvoiceNumber()));
        infoTable.addCell(createInfoCell("Date:", dateFormat.format(sale.getSaleDate())));
        infoTable.addCell(createInfoCell("Batch ID:", sale.getBatch().getBatchId()));
        infoTable.addCell(createInfoCell("Currency:", sale.getCurrency()));

        document.add(infoTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private Cell createInfoCell(String label, String value) {
        Cell cell = new Cell();
        cell.add(new Paragraph(label).setBold().setFontSize(10));
        cell.add(new Paragraph(value).setFontSize(11));
        cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    private void addCompanyAndBuyerDetails(Document document, Sale sale) {
        Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        detailsTable.setWidth(UnitValue.createPercentValue(100));

        // Company Details
        Cell companyCell = new Cell();
        companyCell.add(new Paragraph("From:").setBold().setFontSize(11));
        companyCell.add(new Paragraph(COMPANY_NAME).setFontSize(10));
        companyCell.add(new Paragraph(COMPANY_ADDRESS).setFontSize(9));
        companyCell.add(new Paragraph("GST: " + COMPANY_GST).setFontSize(9));
        companyCell.add(new Paragraph("Phone: " + COMPANY_PHONE).setFontSize(9));
        companyCell.setBorder(new SolidBorder(1));
        companyCell.setPadding(10);
        detailsTable.addCell(companyCell);

        // Buyer Details
        Cell buyerCell = new Cell();
        buyerCell.add(new Paragraph("To (Buyer):").setBold().setFontSize(11));
        buyerCell.add(new Paragraph(sale.getBuyerName()).setFontSize(10));
        if (sale.getBuyerAddress() != null) {
            buyerCell.add(new Paragraph(sale.getBuyerAddress()).setFontSize(9));
        }
        if (sale.getBuyerContact() != null) {
            buyerCell.add(new Paragraph("Contact: " + sale.getBuyerContact()).setFontSize(9));
        }
        buyerCell.setBorder(new SolidBorder(1));
        buyerCell.setPadding(10);
        detailsTable.addCell(buyerCell);

        document.add(detailsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addItemsTable(Document document, Sale sale) {
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 3, 1, 1.5f, 1.5f}));
        itemsTable.setWidth(UnitValue.createPercentValue(100));

        // Header
        String[] headers = {"S.No", "Description", "Qty", "Rate", "Amount"};
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold());
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setFontColor(ColorConstants.WHITE);
            cell.setTextAlignment(TextAlignment.CENTER);
            itemsTable.addHeaderCell(cell);
        }

        // Item Row
        itemsTable.addCell(createCenteredCell("1"));
        itemsTable.addCell(createLeftCell("Fresh Bananas - Grade A\nBatch: " + sale.getBatch().getBatchId()));
        itemsTable.addCell(createCenteredCell(String.valueOf(sale.getTotalBoxes())));
        itemsTable.addCell(createRightCell(formatCurrency(sale.getPricePerBox())));
        itemsTable.addCell(createRightCell(formatCurrency(sale.getTotalAmount())));

        document.add(itemsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addTotalsSection(Document document, Sale sale) {
        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f}));
        totalsTable.setWidth(UnitValue.createPercentValue(50));
        totalsTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);

        totalsTable.addCell(createLabelCell("Subtotal:"));
        totalsTable.addCell(createValueCell(formatCurrency(sale.getTotalAmount())));

        if (sale.getTaxAmount() != null && sale.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            totalsTable.addCell(createLabelCell("GST (" + sale.getTaxPercentage() + "%):"));
            totalsTable.addCell(createValueCell(formatCurrency(sale.getTaxAmount())));
        }

        // Total row with highlight
        Cell totalLabelCell = createLabelCell("TOTAL:");
        totalLabelCell.setBackgroundColor(PRIMARY_COLOR);
        totalLabelCell.setFontColor(ColorConstants.WHITE);
        
        Cell totalValueCell = createValueCell(formatCurrency(sale.getGrandTotal()));
        totalValueCell.setBackgroundColor(PRIMARY_COLOR);
        totalValueCell.setFontColor(ColorConstants.WHITE);
        totalValueCell.setBold();

        totalsTable.addCell(totalLabelCell);
        totalsTable.addCell(totalValueCell);

        document.add(totalsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addGSTBreakdown(Document document, Sale sale) {
        if (sale.getTaxAmount() == null || sale.getTaxAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Paragraph gstTitle = new Paragraph("GST Breakdown")
                .setBold()
                .setFontSize(11);
        document.add(gstTitle);

        Table gstTable = new Table(UnitValue.createPercentArray(new float[]{2, 1.5f, 1.5f, 1.5f}));
        gstTable.setWidth(UnitValue.createPercentValue(80));

        // Headers
        gstTable.addHeaderCell(createHeaderCell("Tax Type"));
        gstTable.addHeaderCell(createHeaderCell("Taxable Value"));
        gstTable.addHeaderCell(createHeaderCell("Rate"));
        gstTable.addHeaderCell(createHeaderCell("Amount"));

        // CGST
        BigDecimal cgstAmount = sale.getTaxAmount().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        gstTable.addCell(createCenteredCell("CGST"));
        gstTable.addCell(createRightCell(formatCurrency(sale.getTotalAmount())));
        gstTable.addCell(createCenteredCell(sale.getTaxPercentage().divide(new BigDecimal("2"), 1, RoundingMode.HALF_UP) + "%"));
        gstTable.addCell(createRightCell(formatCurrency(cgstAmount)));

        // SGST
        gstTable.addCell(createCenteredCell("SGST"));
        gstTable.addCell(createRightCell(formatCurrency(sale.getTotalAmount())));
        gstTable.addCell(createCenteredCell(sale.getTaxPercentage().divide(new BigDecimal("2"), 1, RoundingMode.HALF_UP) + "%"));
        gstTable.addCell(createRightCell(formatCurrency(cgstAmount)));

        document.add(gstTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addBankDetails(Document document) {
        Paragraph bankTitle = new Paragraph("Bank Details")
                .setBold()
                .setFontSize(11);
        document.add(bankTitle);

        Table bankTable = new Table(UnitValue.createPercentArray(new float[]{1.5f, 3}));
        bankTable.setWidth(UnitValue.createPercentValue(80));

        bankTable.addCell(createBankLabelCell("Bank Name:"));
        bankTable.addCell(createBankValueCell("State Bank of India"));
        bankTable.addCell(createBankLabelCell("Account Number:"));
        bankTable.addCell(createBankValueCell("12345678901234"));
        bankTable.addCell(createBankLabelCell("IFSC Code:"));
        bankTable.addCell(createBankValueCell("SBIN0001234"));
        bankTable.addCell(createBankLabelCell("Account Name:"));
        bankTable.addCell(createBankValueCell(COMPANY_NAME));

        document.add(bankTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addExporterAndConsigneeDetails(Document document, Sale sale) {
        Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        detailsTable.setWidth(UnitValue.createPercentValue(100));

        // Exporter Details
        Cell exporterCell = new Cell();
        exporterCell.add(new Paragraph("Exporter:").setBold().setFontSize(11));
        exporterCell.add(new Paragraph(COMPANY_NAME).setFontSize(10));
        exporterCell.add(new Paragraph(COMPANY_ADDRESS).setFontSize(9));
        exporterCell.add(new Paragraph("IEC Code: 0311023456").setFontSize(9));
        exporterCell.setBorder(new SolidBorder(1));
        exporterCell.setPadding(10);
        detailsTable.addCell(exporterCell);

        // Consignee Details
        Cell consigneeCell = new Cell();
        consigneeCell.add(new Paragraph("Consignee:").setBold().setFontSize(11));
        consigneeCell.add(new Paragraph(sale.getBuyerName()).setFontSize(10));
        if (sale.getBuyerAddress() != null) {
            consigneeCell.add(new Paragraph(sale.getBuyerAddress()).setFontSize(9));
        }
        consigneeCell.setBorder(new SolidBorder(1));
        consigneeCell.setPadding(10);
        detailsTable.addCell(consigneeCell);

        document.add(detailsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addExportItemsTable(Document document, Sale sale) {
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 2.5f, 1, 1, 1.5f, 1.5f}));
        itemsTable.setWidth(UnitValue.createPercentValue(100));

        // Header
        String[] headers = {"S.No", "Description", "HSN", "Qty", "Rate (USD)", "Amount (USD)"};
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold());
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setFontColor(ColorConstants.WHITE);
            cell.setTextAlignment(TextAlignment.CENTER);
            itemsTable.addHeaderCell(cell);
        }

        // Item Row
        itemsTable.addCell(createCenteredCell("1"));
        itemsTable.addCell(createLeftCell("Fresh Cavendish Bananas\nBatch: " + sale.getBatch().getBatchId()));
        itemsTable.addCell(createCenteredCell("0803"));
        itemsTable.addCell(createCenteredCell(String.valueOf(sale.getTotalBoxes())));
        
        BigDecimal priceInUSD = sale.getPricePerBox().divide(sale.getExchangeRate(), 2, RoundingMode.HALF_UP);
        BigDecimal totalInUSD = sale.getGrandTotal().divide(sale.getExchangeRate(), 2, RoundingMode.HALF_UP);
        
        itemsTable.addCell(createRightCell("$" + priceInUSD));
        itemsTable.addCell(createRightCell("$" + totalInUSD));

        document.add(itemsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addExportTotalsSection(Document document, Sale sale) {
        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f}));
        totalsTable.setWidth(UnitValue.createPercentValue(50));
        totalsTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);

        BigDecimal totalInUSD = sale.getGrandTotal().divide(sale.getExchangeRate(), 2, RoundingMode.HALF_UP);

        totalsTable.addCell(createLabelCell("Total FOB Value (USD):"));
        totalsTable.addCell(createValueCell("$" + totalInUSD));

        totalsTable.addCell(createLabelCell("Exchange Rate:"));
        totalsTable.addCell(createValueCell("1 USD = ₹" + sale.getExchangeRate()));

        totalsTable.addCell(createLabelCell("Total Value (INR):"));
        totalsTable.addCell(createValueCell("₹" + sale.getGrandTotal()));

        document.add(totalsTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addExportDeclaration(Document document) {
        Paragraph declaration = new Paragraph()
                .add("We declare that this invoice shows the actual price of the goods described and that all particulars are true and correct.")
                .setFontSize(9)
                .setItalic();
        document.add(declaration);

        Paragraph origin = new Paragraph()
                .add("Country of Origin: India\n")
                .add("Port of Loading: JNPT, Mumbai\n")
                .add("Terms of Delivery: FOB")
                .setFontSize(9)
                .setMarginTop(10);
        document.add(origin);

        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addFooter(Document document, String note) {
        document.add(new LineSeparator(new SolidLine(1f)));
        
        Paragraph footer = new Paragraph(note)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(footer);

        Paragraph signature = new Paragraph("Authorized Signatory")
                .setFontSize(10)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(30);
        document.add(signature);
    }

    // Helper methods
    private Cell createCenteredCell(String text) {
        return new Cell().add(new Paragraph(text)).setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createLeftCell(String text) {
        return new Cell().add(new Paragraph(text)).setTextAlignment(TextAlignment.LEFT);
    }

    private Cell createRightCell(String text) {
        return new Cell().add(new Paragraph(text)).setTextAlignment(TextAlignment.RIGHT);
    }

    private Cell createHeaderCell(String text) {
        Cell cell = new Cell().add(new Paragraph(text).setBold());
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setFontColor(ColorConstants.WHITE);
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }

    private Cell createLabelCell(String text) {
        Cell cell = new Cell().add(new Paragraph(text).setBold());
        cell.setTextAlignment(TextAlignment.RIGHT);
        cell.setBorder(new SolidBorder(1));
        cell.setPadding(5);
        return cell;
    }

    private Cell createValueCell(String text) {
        Cell cell = new Cell().add(new Paragraph(text));
        cell.setTextAlignment(TextAlignment.RIGHT);
        cell.setBorder(new SolidBorder(1));
        cell.setPadding(5);
        return cell;
    }

    private Cell createBankLabelCell(String text) {
        Cell cell = new Cell().add(new Paragraph(text).setBold().setFontSize(9));
        cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    private Cell createBankValueCell(String text) {
        Cell cell = new Cell().add(new Paragraph(text).setFontSize(9));
        cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    private String formatCurrency(BigDecimal amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return "₹" + df.format(amount);
    }
}
