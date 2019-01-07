package de.fbeutel.coloranalyzer.product.domain;

import java.util.List;
import java.util.Map;

import de.fbeutel.coloranalyzer.color.domain.ColorTile;
import lombok.Builder;
import lombok.Data;

import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;

@Data
@Builder(toBuilder = true)
public class ProductImage {

  private final String id;
  private final String url;

  private final ColorDistribution colorDistribution;
  private final Map<ColorTile, Double> imageScores;
}
