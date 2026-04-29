package com.till.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.till.model.OrderItem;
import com.till.model.ReceiptData;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class ReceiptService {
    public String generateReceiptPdf(ReceiptData data) throws Exception {
        Objects.requireNonNull(data, "Receipt data cannot be null");
        String timestamp = data.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filePath = "receipts/receipt_" + timestamp + ".pdf";
        new File("receipts").mkdirs();

        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Till POS Receipt").setBold().setFontSize(18));
            document.add(new Paragraph("Date: " + data.getCreatedAt()));
            document.add(new Paragraph("----------------------------------------"));

            for (OrderItem item : data.getItems()) {
                document.add(new Paragraph(
                        String.format(Locale.UK, "%-25s %3d x £%.2f = £%.2f",
                                item.getProduct().getName(),
                                item.getQuantity(),
                                item.getProduct().getPrice(),
                                item.getSubtotal())
                ));
            }

            document.add(new Paragraph("----------------------------------------"));
            document.add(new Paragraph(String.format(Locale.UK, "Subtotal: £%.2f", data.getSubTotal())));
            if (data.getDiscountAmount() > 0) {
                document.add(new Paragraph(String.format(Locale.UK, "Discount: -£%.2f", data.getDiscountAmount())));
                if (data.getCouponCode() != null && !data.getCouponCode().isBlank()) {
                    document.add(new Paragraph("Coupon: " + data.getCouponCode()));
                }
            }
            document.add(new Paragraph(String.format(Locale.UK, "Total: £%.2f", data.getTotal())));
            if ("CASH".equals(data.getPaymentMethod())) {
                document.add(new Paragraph(String.format(Locale.UK, "Cash: £%.2f", data.getTendered())));
                document.add(new Paragraph(String.format(Locale.UK, "Change: £%.2f", data.getChange())));
            } else {
                document.add(new Paragraph("Paid by Card"));
            }
            document.add(new Paragraph("Thank you!"));
        }

        return filePath;
    }
}
