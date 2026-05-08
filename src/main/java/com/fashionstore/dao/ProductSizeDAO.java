package com.fashionstore.dao;

import com.fashionstore.model.ProductSize;
import java.util.List;

public interface ProductSizeDAO {

    int addProductSize(ProductSize size);

    int updateProductSize(ProductSize size);

    int deleteProductSize(int productSizeId);

    ProductSize getProductSizeById(int productSizeId);

    List<ProductSize> getSizesByProductId(int productId);

    List<ProductSize> getAvailableSizesByProductId(int productId);

    boolean updateStock(int productSizeId, int quantity);

    boolean reduceStock(int productSizeId, int quantity);

    boolean reduceStock(int productId, String sizeLabel, int quantity);

    void addOrUpdateSize(ProductSize size);
}