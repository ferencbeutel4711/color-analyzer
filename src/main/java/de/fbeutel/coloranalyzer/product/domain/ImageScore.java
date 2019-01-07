package de.fbeutel.coloranalyzer.product.domain;

import lombok.Builder;
import lombok.Data;

import de.fbeutel.coloranalyzer.color.domain.RgbColor;

@Data
@Builder(toBuilder = true)
public class ImageScore {

  private final RgbColor referenceColor;
  private final double score;
}
