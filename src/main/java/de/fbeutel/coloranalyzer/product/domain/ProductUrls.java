package de.fbeutel.coloranalyzer.product.domain;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class ProductUrls {

  private final List<String> urls;
}
