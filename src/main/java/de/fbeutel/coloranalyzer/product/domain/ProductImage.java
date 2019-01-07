package de.fbeutel.coloranalyzer.product.domain;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;

@Data
@Builder(toBuilder = true)
public class ProductImage {

  private final String id;
  private final String url;

  private final ColorDistribution colorDistribution;
  private final List<ImageScore> imageScores;
}
