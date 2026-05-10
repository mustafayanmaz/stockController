package com.musyan.stok.service;

import com.musyan.stok.dto.ImportResultDto;
import com.musyan.stok.dto.ProductDto;
import com.musyan.stok.dto.ProductFilterDto;
import com.musyan.stok.entity.Product;
import com.musyan.stok.exception.ProductAlreadyExistsException;
import com.musyan.stok.exception.ResourceNotFoundException;
import com.musyan.stok.mapper.ProductMapper;
import com.musyan.stok.repository.ProductRepository;
import com.musyan.stok.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CacheService cacheService;

    @Transactional
    public void createProduct(ProductDto productDto) {
        if (productRepository.existsByProductCode(productDto.getProductCode())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with code: " + productDto.getProductCode());
        }
        if (productRepository.existsByProductName(productDto.getProductName())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with name: " + productDto.getProductName());
        }

        Product product = ProductMapper.mapToProduct(productDto, new Product());
        product.setQuantity(productDto.getQuantity() != null ? productDto.getQuantity() : 0);
        product.setMinStockLevel(productDto.getMinStockLevel() != null ? productDto.getMinStockLevel() : 0);
        productRepository.save(product);
        cacheService.setProduct(ProductMapper.mapToProductDto(product, new ProductDto()));
    }






    public ProductDto fetchProductByCode(String productCode) {
        ProductDto cached = cacheService.getProduct(productCode);
        if (cached != null) return cached;

        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));
        ProductDto dto = ProductMapper.mapToProductDto(product, new ProductDto());
        cacheService.setProduct(dto);
        return dto;
    }

    public List<ProductDto> fetchAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(product -> ProductMapper.mapToProductDto(product, new ProductDto()))
                .toList();
    }

    public List<ProductDto> filterProducts(ProductFilterDto filterDto) {
        return productRepository.findAll(ProductSpecification.buildFilter(filterDto))
                .stream()
                .map(product -> ProductMapper.mapToProductDto(product, new ProductDto()))
                .toList();
    }

    @Transactional
    public boolean updateProduct(ProductDto productDto) {
        cacheService.acquireLock(productDto.getProductCode());

        Product existingProduct = productRepository.findByProductCode(productDto.getProductCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product", "productCode", productDto.getProductCode()));

        if (!existingProduct.getProductName().equals(productDto.getProductName()) &&
                productRepository.existsByProductName(productDto.getProductName())) {
            throw new ProductAlreadyExistsException(
                    "Product already exists with name: " + productDto.getProductName());
        }

        ProductMapper.mapToProduct(productDto, existingProduct);
        productRepository.save(existingProduct);
        cacheService.evictProduct(productDto.getProductCode());
        return true;
    }

    @Transactional
    public boolean deleteProductByCode(String productCode) {
        cacheService.acquireLock(productCode);

        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));
        productRepository.delete(product);
        cacheService.evictProduct(productCode);
        return true;
    }

    public boolean existsByProductCode(String productCode) {
        return productRepository.existsByProductCode(productCode);
    }

    /**
     * Satır satır bağımsız işler: geçerli satırlar kaydedilir, hatalı satırlar
     * ImportResultDto.errors listesine eklenerek atlanır.
     * Metot @Transactional DEĞİL; her satır productRepository.save() ile
     * kendi transaction'ında commit edilir.
     */
    public ImportResultDto importFromExcel(MultipartFile file) throws IOException {
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IOException("Excel başlık satırı yok");

            java.util.Map<String, Integer> headerMap = new java.util.HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = dataFormatter.formatCellValue(headerRow.getCell(c))
                        .toLowerCase().replaceAll("[ ._-]", "").trim();
                headerMap.put(header, c);
            }

            // Başlıklar için hem Türkçe hem İngilizce anahtarlar
            String[] codeKeys     = {"ürünkodu", "productcode"};
            String[] nameKeys     = {"ürünadı", "productname"};
            String[] catKeys      = {"kategori", "category"};
            String[] costKeys     = {"birimmaliyet", "unitcost"};
            String[] activeKeys   = {"aktif", "active"};
            String[] qtyKeys      = {"adet", "quantity"};
            String[] minStockKeys = {"minstok", "minstocklevel"};
            String[] unitKeys     = {"birim", "unit"};
            String[] dateKeys     = {"oluşturulmatarihi", "createdat"};

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isRowBlank(row, dataFormatter)) continue;

                int rowNum = i + 1;
                try {
                    ProductDto dto = new ProductDto();
                    int idx;

                    idx = findHeaderIndex(headerMap, codeKeys);
                    if (idx >= 0) dto.setProductCode(getCellString(row, idx, dataFormatter));

                    idx = findHeaderIndex(headerMap, nameKeys);
                    if (idx >= 0) dto.setProductName(getCellString(row, idx, dataFormatter));

                    idx = findHeaderIndex(headerMap, catKeys);
                    if (idx >= 0) dto.setCategory(getCellString(row, idx, dataFormatter));

                    idx = findHeaderIndex(headerMap, costKeys);
                    if (idx >= 0) {
                        BigDecimal cost = parseBigDecimal(row, idx, dataFormatter);
                        dto.setUnitCost(cost);
                    }

                    idx = findHeaderIndex(headerMap, activeKeys);
                    if (idx >= 0) {
                        String activeStr = getCellString(row, idx, dataFormatter);
                        if (!activeStr.isBlank()) dto.setActive(Boolean.parseBoolean(activeStr));
                    }

                    idx = findHeaderIndex(headerMap, qtyKeys);
                    if (idx >= 0) {
                        Integer qty = parseInteger(row, idx, dataFormatter);
                        if (qty != null) dto.setQuantity(qty);
                    }

                    idx = findHeaderIndex(headerMap, minStockKeys);
                    if (idx >= 0) {
                        Integer minStock = parseInteger(row, idx, dataFormatter);
                        if (minStock != null) dto.setMinStockLevel(minStock);
                    }

                    idx = findHeaderIndex(headerMap, unitKeys);
                    if (idx >= 0) dto.setUnit(getCellString(row, idx, dataFormatter));

                    idx = findHeaderIndex(headerMap, dateKeys);
                    if (idx >= 0) {
                        String dateStr = getCellString(row, idx, dataFormatter);
                        if (!dateStr.isBlank()) {
                            dto.setCreatedAt(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                        }
                    }

                    // Zorunlu alan validasyonu
                    List<String> rowErrors = validateImportRow(dto, rowNum);
                    if (!rowErrors.isEmpty()) {
                        errors.addAll(rowErrors);
                        continue;
                    }

                    // Tekil (unique) kısıt kontrolleri
                    if (productRepository.existsByProductCode(dto.getProductCode())) {
                        errors.add("Satır " + rowNum + ": Ürün kodu zaten mevcut: " + dto.getProductCode());
                        continue;
                    }
                    if (productRepository.existsByProductName(dto.getProductName())) {
                        errors.add("Satır " + rowNum + ": Ürün adı zaten mevcut: " + dto.getProductName());
                        continue;
                    }

                    // Kaydet — kendi transaction'ında commit olur (SimpleJpaRepository @Transactional)
                    Product product = ProductMapper.mapToProduct(dto, new Product());
                    product.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 0);
                    product.setMinStockLevel(dto.getMinStockLevel() != null ? dto.getMinStockLevel() : 0);
                    productRepository.save(product);
                    successCount++;

                } catch (Exception e) {
                    errors.add("Satır " + rowNum + ": " + e.getMessage());
                }
            }
        }
        return new ImportResultDto(successCount, errors);
    }

    // ------------------------------------------------------------------ helpers

    /** Satırdaki tüm hücreler boşsa true döner (tamamen boş satırları atla). */
    private boolean isRowBlank(Row row, DataFormatter dataFormatter) {
        if (row == null) return true;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            if (!dataFormatter.formatCellValue(row.getCell(c)).isBlank()) return false;
        }
        return true;
    }

    /** İmport satırı için zorunlu alan validasyonu; hata mesajlarını döner. */
    private List<String> validateImportRow(ProductDto dto, int rowNum) {
        List<String> rowErrors = new ArrayList<>();
        if (dto.getProductCode() == null || dto.getProductCode().isBlank())
            rowErrors.add("Satır " + rowNum + ": Ürün kodu boş olamaz");
        else if (dto.getProductCode().length() < 3 || dto.getProductCode().length() > 50)
            rowErrors.add("Satır " + rowNum + ": Ürün kodu 3-50 karakter arasında olmalıdır");
        if (dto.getProductName() == null || dto.getProductName().isBlank())
            rowErrors.add("Satır " + rowNum + ": Ürün adı boş olamaz");
        if (dto.getCategory() == null || dto.getCategory().isBlank())
            rowErrors.add("Satır " + rowNum + ": Kategori boş olamaz");
        if (dto.getUnitCost() == null)
            rowErrors.add("Satır " + rowNum + ": Birim maliyet boş olamaz");
        else if (dto.getUnitCost().compareTo(BigDecimal.ZERO) <= 0)
            rowErrors.add("Satır " + rowNum + ": Birim maliyet 0'dan büyük olmalıdır");
        if (dto.getActive() == null)
            rowErrors.add("Satır " + rowNum + ": Aktif durumu boş olamaz");
        return rowErrors;
    }

    /** Header anahtarlarından ilk bulduğunun indexini döner, yoksa -1. */
    private int findHeaderIndex(java.util.Map<String, Integer> headerMap, String[] keys) {
        for (String k : keys) {
            Integer idx = headerMap.get(k.toLowerCase().replaceAll("[ ._-]", "").trim());
            if (idx != null) return idx;
        }
        return -1;
    }

    /**
     * Hücreyi güvenli string olarak okur. POI 5.x uyumlu (setCellType kullanmaz).
     */
    private String getCellString(Row row, int col, DataFormatter dataFormatter) {
        if (col < 0) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return dataFormatter.formatCellValue(cell).trim();
    }

    /**
     * Sayısal hücreleri BigDecimal'a çevirir.
     * Hem Türkçe (1.234,56) hem İngilizce (1,234.56) formatı destekler.
     */
    private BigDecimal parseBigDecimal(Row row, int col, DataFormatter dataFormatter) {
        if (col < 0) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String str = dataFormatter.formatCellValue(cell).trim().replaceAll("[^\\d,.]", "");
        if (str.isBlank()) return null;
        int lastDot   = str.lastIndexOf('.');
        int lastComma = str.lastIndexOf(',');
        if (lastComma > lastDot) {
            // Virgül ondalık ayraç (Türkçe): "1.234,56" → "1234.56"
            str = str.replace(".", "").replace(",", ".");
        } else {
            // Nokta ondalık ayraç (İngilizce): "1,234.56" → "1234.56"
            str = str.replace(",", "");
        }
        return new BigDecimal(str);
    }

    /**
     * Sayısal hücreleri Integer'a çevirir.
     */
    private Integer parseInteger(Row row, int col, DataFormatter dataFormatter) {
        if (col < 0) return null;
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String str = dataFormatter.formatCellValue(cell).trim().replaceAll("[^\\d-]", "");
        if (str.isBlank()) return null;
        return Integer.parseInt(str);
    }
}
