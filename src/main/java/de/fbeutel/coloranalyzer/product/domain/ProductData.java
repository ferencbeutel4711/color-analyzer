package de.fbeutel.coloranalyzer.product.domain;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Builder;
import lombok.Data;

@Data
@Document
@Builder(toBuilder = true)
public class ProductData {

  @Id
  private final String id;
  private final List<String> images;
}
