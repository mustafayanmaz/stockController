package com.musyan.stok.service;

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
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

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
    }

    public ProductDto fetchProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));
        return ProductMapper.mapToProductDto(product, new ProductDto());
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
        return true;
    }

    @Transactional
    public boolean deleteProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));
        productRepository.delete(product);
        return true;
    }

    public boolean existsByProductCode(String productCode) {
        return productRepository.existsByProductCode(productCode);
    }

    @Transactional
    public int importFromExcel(MultipartFile file) throws IOException {
        int count = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new IOException("Excel başlık satırı yok");
            java.util.Map<String, Integer> headerMap = new java.util.HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = getCellString(headerRow, c).toLowerCase().replaceAll("[ ._-]", "").trim();
                headerMap.put(header, c);
            }
            // Başlıklar için hem Türkçe hem İngilizce anahtarlar
            String[] codeKeys = {"ürünkodu", "productcode"};
            String[] nameKeys = {"ürünadı", "productname"};
            String[] catKeys = {"kategori", "category"};
            String[] costKeys = {"birimmaliyet", "unitcost"};
            String[] activeKeys = {"aktif", "active"};
            String[] qtyKeys = {"adet", "quantity"};
            String[] minStockKeys = {"minstok", "minstocklevel"};
            String[] unitKeys = {"birim", "unit"};
            String[] dateKeys = {"oluşturulmatarihi", "createdat"};

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                ProductDto dto = new ProductDto();
                int idx;
                idx = findHeaderIndex(headerMap, codeKeys); if (idx >= 0) dto.setProductCode(getCellString(row, idx));
                idx = findHeaderIndex(headerMap, nameKeys); if (idx >= 0) dto.setProductName(getCellString(row, idx));
                idx = findHeaderIndex(headerMap, catKeys); if (idx >= 0) dto.setCategory(getCellString(row, idx));
                idx = findHeaderIndex(headerMap, costKeys); if (idx >= 0) {
                    String unitCostStr = getCellString(row, idx);
                    if (!unitCostStr.isEmpty()) dto.setUnitCost(new BigDecimal(unitCostStr));
                }
                idx = findHeaderIndex(headerMap, activeKeys); if (idx >= 0) {
                    String activeStr = getCellString(row, idx);
                    if (!activeStr.isEmpty()) dto.setActive(Boolean.parseBoolean(activeStr));
                }
                idx = findHeaderIndex(headerMap, qtyKeys); if (idx >= 0) {
                    String qtyStr = getCellString(row, idx);
                    if (!qtyStr.isEmpty()) dto.setQuantity(Integer.parseInt(qtyStr));
                }
                idx = findHeaderIndex(headerMap, minStockKeys); if (idx >= 0) {
                    String minStockStr = getCellString(row, idx);
                    if (!minStockStr.isEmpty()) dto.setMinStockLevel(Integer.parseInt(minStockStr));
                }
                idx = findHeaderIndex(headerMap, unitKeys); if (idx >= 0) dto.setUnit(getCellString(row, idx));
                idx = findHeaderIndex(headerMap, dateKeys); if (idx >= 0) {
                    String dateStr = getCellString(row, idx);
                    if (!dateStr.isEmpty()) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        dto.setCreatedAt(LocalDate.parse(dateStr, formatter));
                    }
                }
                createProduct(dto);
                count++;
            }
        }
        return count;
    }

    // Header anahtarlarından ilk bulduğunun indexini döner, yoksa -1
    private int findHeaderIndex(java.util.Map<String, Integer> headerMap, String[] keys) {
        for (String k : keys) {
            Integer idx = headerMap.get(k.toLowerCase().replaceAll("[ ._-]", "").trim());
            if (idx != null) return idx;
        }
        return -1;
    }

    private String getCellString(Row row, int col) {
        if (col < 0) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}
