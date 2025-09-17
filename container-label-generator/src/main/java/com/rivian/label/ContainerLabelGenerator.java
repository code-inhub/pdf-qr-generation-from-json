package com.rivian.label;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.IOException;

public class ContainerLabelGenerator {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(new java.io.File("container-label-generator/src/input.json"));
        generateSimplePdf(root, "container-label-generator/src/sample_output.pdf");
    }

    // Simple PDF generation for local testing
    public static void generateSimplePdf(JsonNode json, String outputPath) throws IOException {
        PDDocument document = new PDDocument();
        // Use 6" x 4" label at 72 DPI (6*72=432, 4*72=288)
        PDPage page = new PDPage(new PDRectangle(432, 288));
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        // Read fields
        JsonNode label = json.path("container_label");
    String shipFromName = label.path("ship_from").path("name").asText("XYZ COMPANY");
    String shipFromAddr = label.path("ship_from").path("address").asText("345 SOUTH STREET\nPLYMOUTH, MI 48170");
    String shipToName = label.path("ship_to").path("name").asText("RIVIAN");
    String shipToAddr = label.path("ship_to").path("address").asText("100 N RIVIAN MOTORWAY\nNORMAL, IL 61761");
        String partNumber = label.path("partNumber").asText("PT00001234-A");
        String partDesc = label.path("partDescription").asText("FOG LAMP FR FASCIA, RR");
    String quantity = label.path("quantity").asText("1000");
        String po = label.path("poNumber").asText("5500000001");
        String poLine = label.path("poLineNumber").asText("00010");
        String lot = label.path("lotNumber").asText("123456789012345");
        String prodDate = label.path("productionDate").asText("2022-10-22");
        String expDate = label.path("expirationDate").asText("2023-11-26");
        String lpn = label.path("lpn_1j").asText("1J5124509271900001");
        String qrData = label.path("qr_code").path("encoded_string").asText("QR_DATA");

        // Layout constants
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        float margin = 8f;
    float rightColumnWidth = 136f; // guideline column on the far right
    float startLeftWidth = width - rightColumnWidth - margin * 2;
    // We'll split the left area into a main wide column and a narrower column to create the 3-col top row
    float leftMainWidth = startLeftWidth * 0.68f; // wide left portion
    float leftSideWidth = startLeftWidth - leftMainWidth; // smaller column next to right guideline

    // cursorY kept for historical layout but not needed with explicit grid
        float gap = 6f;

    // Draw outer border for label (for visual alignment)
    contentStream.setStrokingColor(Color.BLACK);
        contentStream.setLineWidth(1f);
        // outer
        contentStream.addRect(margin, margin, width - margin * 2, height - margin * 2);
        contentStream.stroke();

    // Top row: split left area into 3 columns: ShipFrom (small), ShipTo (medium), QR-area (small)
    float shipFromCol = leftMainWidth * 0.45f;
    float shipToCol = leftMainWidth * 0.45f;
    float qrCol = leftSideWidth; // small column for QR (between left main and right guideline)

    // Row heights tuned to reference proportions
    float topRowH = 56f;
    float midRowH = 78f;
    float descRowH = 52f;
    float lowRowH = 74f;

        float startX = margin;
        float startY = height - margin;

    // Draw vertical separator between left area and right guideline column
    contentStream.moveTo(margin + startLeftWidth + 2, margin);
    contentStream.lineTo(margin + startLeftWidth + 2, height - margin);
        contentStream.stroke();

        // Draw horizontal separators for rows inside left area
        float yTop = startY - topRowH;
        float yMid = yTop - midRowH - gap;
        float yDesc = yMid - descRowH - gap;
        float yLow = yDesc - lowRowH - gap;

    // Top row box (left area split into ShipFrom | ShipTo | QR-area)
    contentStream.addRect(startX, yTop, shipFromCol, topRowH);
    contentStream.addRect(startX + shipFromCol, yTop, shipToCol, topRowH);
    contentStream.addRect(startX + shipFromCol + shipToCol, yTop, qrCol, topRowH);
        contentStream.stroke();

    // Middle row box (part barcode area across shipFrom+shipTo) and small area over QR column for qty
    contentStream.addRect(startX, yMid, shipFromCol + shipToCol, midRowH);
    contentStream.addRect(startX + shipFromCol + shipToCol, yMid, qrCol, midRowH);
        contentStream.stroke();

    // Description row
    contentStream.addRect(startX, yDesc, startLeftWidth, descRowH);
        contentStream.stroke();

    // Lower row
    contentStream.addRect(startX, yLow, startLeftWidth, lowRowH);
        contentStream.stroke();

// Middle row: left (for part number/barcode), right (for qty barcode)
float midLeftW = shipFromCol + shipToCol;
float midRightW = qrCol;
contentStream.addRect(startX, yMid, midLeftW, midRowH);
contentStream.addRect(startX + midLeftW, yMid, midRightW, midRowH);
contentStream.stroke();

// Row 3: left small, right large (PO details)
float row3LeftW = 54f;
float row3RightW = startLeftWidth + rightColumnWidth - row3LeftW;
contentStream.addRect(startX, yDesc, row3LeftW, descRowH);
contentStream.addRect(startX + row3LeftW, yDesc, row3RightW, descRowH);
contentStream.stroke();

// Row 4: left small, right large (Supplier internal)
float row4LeftW = 54f;
float row4RightW = startLeftWidth + rightColumnWidth - row4LeftW;
contentStream.addRect(startX, yLow, row4LeftW, lowRowH);
contentStream.addRect(startX + row4LeftW, yLow, row4RightW, lowRowH);
contentStream.stroke();

        // Now place content into the boxes

        // Top-left: Ship From
        float sfX = startX + 6;
        float sfY = startY - 12;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 8);
        contentStream.newLineAtOffset(sfX, sfY);
        contentStream.showText("SHIP FROM:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 7);
        contentStream.newLineAtOffset(sfX, sfY - 12);
        contentStream.showText(shipFromName);
        contentStream.newLineAtOffset(0, -9);
        for (String line : shipFromAddr.split("\\n")) {
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, -9);
        }
        contentStream.endText();

        // Top-middle: Ship To
    float stX = startX + shipFromCol + 6;
        float stY = sfY;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 8);
        contentStream.newLineAtOffset(stX, stY);
        contentStream.showText("SHIP TO:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 7);
        contentStream.newLineAtOffset(stX, stY - 12);
        contentStream.showText(shipToName);
        contentStream.newLineAtOffset(0, -9);
        for (String line : shipToAddr.split("\\n")) {
            contentStream.showText(line);
            contentStream.newLineAtOffset(0, -9);
        }
        contentStream.endText();

        // Top-right of left area: QR - place inside QR-area cell (centered)
        try {
            BufferedImage qrImg = generateQRCodeImage(qrData, 300, 300);
            PDImageXObject pdQr = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(qrImg), "qr");
            float qrSize = Math.min(qrCol - 12, topRowH - 16);
            float qrPosX = startX + shipFromCol + shipToCol + (qrCol - qrSize) / 2;
            float qrPosY = startY - 8 - qrSize;
            contentStream.drawImage(pdQr, qrPosX, qrPosY, qrSize, qrSize);
        } catch (IOException e) {
            // ignore
        }
        // --- Middle row: Part number and barcode ---
        float partTop = yMid + midRowH - 8;
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 9);
        contentStream.newLineAtOffset(startX + 6, partTop);
        contentStream.showText(partNumber);
        contentStream.endText();

        try {
            // Part barcode across shipFrom + shipTo columns
            int pBarW = (int)(shipFromCol + shipToCol - 28);
            BufferedImage pBar = generateBarcodeImage(partNumber.replaceAll("\\s+",""), pBarW, 40);
            PDImageXObject pdPart = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(pBar), "partBar2");
            float pBarX = startX + 6;
            float pBarY = yMid + 8;
            contentStream.drawImage(pdPart, pBarX, pBarY, pBarW, 36);
        } catch (IOException e) {
            // ignore
        }

        // Qty barcode to the right side of middle row
        try {
            int qtyW = (int)(qrCol - 20);
            BufferedImage qBar = generateBarcodeImage(quantity, qtyW, 48);
            PDImageXObject pdQty2 = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(qBar), "qtyBar2");
            float qBarX = startX + shipFromCol + shipToCol + 8;
            float qBarY = yMid + 6;
            contentStream.drawImage(pdQty2, qBarX, qBarY, qtyW, 36);

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
            contentStream.newLineAtOffset(qBarX + 2, qBarY - 6);
            contentStream.showText(quantity);
            contentStream.endText();
        } catch (IOException e) {
            // ignore
        }

        // --- Description row ---
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.newLineAtOffset(startX + 6, yDesc + descRowH - 12);
        contentStream.showText("DESCRIPTION");
        contentStream.endText();

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.newLineAtOffset(startX + 6, yDesc + descRowH - 28);
        contentStream.showText(partDesc);
        contentStream.endText();

        // --- Row 3, column 2: PO details ---
    float poX = startX + row3LeftW + 10f;
    float poY = yDesc + descRowH - 16f;
    contentStream.beginText();
    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 9);
    contentStream.newLineAtOffset(poX, poY);
    contentStream.showText("PO NO: " + po);
    contentStream.newLineAtOffset(0, -12);
    contentStream.showText("PO LINE: " + poLine);
    contentStream.newLineAtOffset(0, -12);
    contentStream.showText("PROD DATE: " + prodDate);
    contentStream.newLineAtOffset(0, -12);
    contentStream.showText("EXP DATE: " + expDate);
    contentStream.newLineAtOffset(0, -12);
    contentStream.showText("LOT: " + lot);
    contentStream.newLineAtOffset(0, -12);
    contentStream.showText("QML: " + label.path("qml").asText() + "   PCD: " + label.path("pcd").asText());
    contentStream.endText();

    // --- Lower row: LPN barcode (row 4, col 1) ---
        try {
            int lpnW = (int)(row4LeftW - 10);
            BufferedImage lpnBarImg = generateBarcodeImage(lpn, lpnW, 40);
            PDImageXObject pdLpn2 = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(lpnBarImg), "lpnBar2");
            float lpnX = startX + 6;
            float lpnY = yLow + 10;
            contentStream.drawImage(pdLpn2, lpnX, lpnY, lpnW, 36);

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 8);
            contentStream.newLineAtOffset(lpnX, lpnY + 40);
            contentStream.showText(lpn);
            contentStream.endText();
        } catch (IOException e) {
            // ignore
        }

    // --- Row 4, column 2: Supplier internal ---
    float suppX = startX + row4LeftW + 10f;
    float suppY = yLow + lowRowH - 24f;
    contentStream.beginText();
    contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
    contentStream.newLineAtOffset(suppX, suppY);
    contentStream.showText("Supplier internal");
    contentStream.endText();

        contentStream.close();
        document.save(outputPath);
        document.close();
    }

    private static BufferedImage generateQRCodeImage(String data, int width, int height) throws IOException {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            throw new IOException("Failed to generate QR code", e);
        }
    }

    private static BufferedImage generateBarcodeImage(String data, int width, int height) throws IOException {
        try {
            MultiFormatWriter barcodeWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = barcodeWriter.encode(data, BarcodeFormat.CODE_128, width, height);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            throw new IOException("Failed to generate barcode", e);
        }
    }

    private static byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
