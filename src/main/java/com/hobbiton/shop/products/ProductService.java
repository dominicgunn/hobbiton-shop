package com.hobbiton.shop.products;

import com.hobbiton.shop.products.exceptions.ProductNotFoundException;
import com.hobbiton.shop.products.models.Product;
import com.hobbiton.shop.utils.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * @author Dominic Gunn
 */
@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private static final Integer CACHE_REFRESH_TIME_IN_MINUTES = 5;

    @Autowired
    private ProductServiceClient productServiceClient;

    private Date lastFetchDate = new Date(0);
    private LRUCache<String, Product> productCache = new LRUCache<>(100);

    public List<Product> getProducts() {
        if (shouldRefreshCache()) {
            cacheProducts();
        }
        return new ArrayList<>(productCache.values());
    }

    public Product getProduct(String productId) {
        return getOrFetchProduct(productId);
    }

    private Product getOrFetchProduct(String productId) {
        final Product cachedProduct = productCache.get(productId);
        if (cachedProduct == null) {
            logger.debug("Product [{}] did not existing in the cache, fetching and caching it", productId);
            try {
                final Product product = productServiceClient.fetchProduct(productId);
                productCache.put(productId, product);
                return product;
            } catch (HttpClientErrorException ex) {
                logger.warn("Unable to fetch product [{}] from the product service", productId);
                throw new ProductNotFoundException(String.format("Product %s not found!", productId));
            }
        }
        return cachedProduct;
    }

    private void cacheProducts() {
        try {
            // Query product service for existing products and cache them.
            final List<Product> existingProducts = productServiceClient.fetchProducts();

            // Update last fetch date.
            lastFetchDate = new Date();

            // Convert products to a map, using their Id as the key.
            final Map<String, Product> productMap = existingProducts.stream().collect(Collectors.toMap(Product::getId, product -> product));

            // Put all the products into the cache.
            productCache.putAll(productMap);
        } catch (RestClientException ex) {
            logger.error("Unable to fetch products from the product service", ex);
        }
    }

    private boolean shouldRefreshCache() {
        return MINUTES.between(lastFetchDate.toInstant(), Instant.now()) > CACHE_REFRESH_TIME_IN_MINUTES;
    }
}