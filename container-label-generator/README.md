# Container Label Generator

This Java project generates a PDF label for shipping containers based on a JSON payload, following the layout and specifications shown in the provided reference image.

## Features
- Accepts JSON input for label details
- Generates PDF with specified layout, fonts, and sizes
- Includes QR code and barcodes using ZXing
- Uses Apache PDFBox for PDF creation

## Usage
1. Place your JSON payload in the `main` method of `ContainerLabelGenerator.java` or modify to read from a file.
2. Run the project using Maven:
   ```shell
   mvn clean package
   mvn exec:java -Dexec.mainClass="com.rivian.label.ContainerLabelGenerator"
   ```
3. The output PDF will be generated as `container_label.pdf` in the project directory.

## Dependencies
- Apache PDFBox
- ZXing (core, javase)
- Jackson Databind

## Customization
- Adjust layout, fonts, and field positions in `ContainerLabelGenerator.java` as needed to match your requirements.

## License
This project is for demonstration purposes and may require further customization for production use.
