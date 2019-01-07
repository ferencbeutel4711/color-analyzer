package de.fbeutel.coloranalyzer.product.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Document
@Builder(toBuilder = true)
public class Product {

  @Id
  private final String id;
  private final ProductImage productImage;
}
