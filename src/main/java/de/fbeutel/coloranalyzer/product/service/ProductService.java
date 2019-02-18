package de.fbeutel.coloranalyzer.product.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import de.fbeutel.coloranalyzer.product.domain.Product;
import de.fbeutel.coloranalyzer.product.domain.ProductRepository;

@Slf4j
@Service
public class ProductService {

  private final ProductRepository productRepository;

  public ProductService(final ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  public void create(final Product product) {
    productRepository.save(product);
  }
}
