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
import java.io.File;
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
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        // Read fields
        JsonNode label = json.path("container_label");
        String shipFromName = label.path("ship_from").path("name").asText();
        String shipFromAddr = label.path("ship_from").path("address").asText();
        String supplierCode = label.path("ship_from").path("supplierCode").asText();
        String shipToName = label.path("ship_to").path("name").asText();
        String shipToAddr = label.path("ship_to").path("address").asText();
        String partNumber = label.path("partNumber").asText();
        String partDesc = label.path("partDescription").asText();
        String quantity = label.path("quantity").asText();
        String uom = label.path("uom").asText();
        String po = label.path("poNumber").asText();
        String poLine = label.path("poLineNumber").asText();
        String lot = label.path("lotNumber").asText();
        String prodDate = label.path("productionDate").asText();
        String expDate = label.path("expirationDate").asText();
        String lpn = label.path("lpn_1j").asText();
        String qrData = label.path("qr_code").path("encoded_string").asText();

        // Top row: Ship From | Ship To | QR
        float margin = 36;
        float usableWidth = PDRectangle.A4.getWidth() - margin * 2;
        float col1X = margin;
        float col2X = margin + usableWidth * 0.45f;
        float col3X = margin + usableWidth * 0.75f;
        float topY = PDRectangle.A4.getHeight() - 50;

        // Ship From block
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.newLineAtOffset(col1X, topY);
        contentStream.showText("SHIP FROM:");
        contentStream.newLineAtOffset(0, -14);
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.showText(shipFromName);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText(shipFromAddr);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText("Code: " + supplierCode + "  C/O: " + label.path("ship_from").path("countryOfOrigin").asText());
        contentStream.endText();

        // Ship To block
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.newLineAtOffset(col2X, topY);
        contentStream.showText("SHIP TO:");
        contentStream.newLineAtOffset(0, -14);
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.showText(shipToName);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText(shipToAddr);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText("Plant: " + label.path("ship_to").path("plant").asText() + "  SLoc: " + label.path("ship_to").path("sLoc").asText());
        contentStream.endText();

        // QR block
        try {
            BufferedImage qrImg = generateQRCodeImage(qrData, 180, 180);
            PDImageXObject qrPd = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(qrImg), "qr");
            contentStream.drawImage(qrPd, col3X, topY - 120, 120, 120);
        } catch (IOException e) {
            // ignore
        }

        // Middle row: Part barcode (left) and Qty barcode (right)
        float midY = topY - 160;
        try {
            BufferedImage partBarcode = generateBarcodeImage(partNumber, 600, 80);
            PDImageXObject partBarcodeImg = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(partBarcode), "partBarcode");
            contentStream.drawImage(partBarcodeImg, margin, midY, usableWidth * 0.6f, 80);

            BufferedImage qtyBarcode = generateBarcodeImage(quantity, 200, 60);
            PDImageXObject qtyImg = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(qtyBarcode), "qtyBarcode");
            contentStream.drawImage(qtyImg, margin + usableWidth * 0.65f, midY + 10, usableWidth * 0.25f, 60);
        } catch (IOException e) {
            // ignore
        }

        // Description row
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
        contentStream.newLineAtOffset(margin, midY - 40);
        contentStream.showText(partDesc);
        contentStream.endText();

        // Lower row: LPN barcode left, PO/lot block right
        float lowY = midY - 100;
        try {
            BufferedImage lpnBarcode = generateBarcodeImage(lpn, 600, 60);
            PDImageXObject lpnBarcodeImg = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(lpnBarcode), "lpnBarcode");
            contentStream.drawImage(lpnBarcodeImg, margin, lowY, usableWidth * 0.6f, 60);
        } catch (IOException e) {
            // ignore
        }

        // PO/Lot block on right
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
        contentStream.newLineAtOffset(margin + usableWidth * 0.65f, lowY + 40);
        contentStream.showText("PO NO: " + po);
        contentStream.newLineAtOffset(0, -12);
        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.showText("PO LINE NO: " + poLine);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText("PROD DATE: " + prodDate);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText("EXP DATE: " + expDate);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText("LOT NO: " + lot);
        contentStream.newLineAtOffset(0, -12);
        contentStream.showText("QML: " + label.path("qml").asText() + "   PCD: " + label.path("pcd").asText());
        contentStream.endText();

        // Generate and draw barcodes and QR
        try {
            BufferedImage partBarcode = generateBarcodeImage(partNumber, 400, 60);
            PDImageXObject partBarcodeImg = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(partBarcode), "partBarcode");
            contentStream.drawImage(partBarcodeImg, 50, 540, 300, 60);

            BufferedImage lpnBarcode = generateBarcodeImage(lpn, 300, 50);
            PDImageXObject lpnBarcodeImg = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(lpnBarcode), "lpnBarcode");
            contentStream.drawImage(lpnBarcodeImg, 50, 480, 300, 50);

            BufferedImage qrImg = generateQRCodeImage(qrData, 150, 150);
            PDImageXObject qrPd = PDImageXObject.createFromByteArray(document, bufferedImageToByteArray(qrImg), "qr");
            contentStream.drawImage(qrPd, 420, 540, 150, 150);
        } catch (IOException e) {
            // ignore barcode failures for now
        }

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
