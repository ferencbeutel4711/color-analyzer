package de.fbeutel.coloranalyzer.color.domain;

import java.util.Comparator;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@Builder(toBuilder = true)
public class RgbColor {

  public static final Comparator<RgbColor> RED_COMPARATOR = Comparator.comparing(RgbColor::getR);
  public static final Comparator<RgbColor> GREEN_COMPARATOR = Comparator.comparing(RgbColor::getG);
  public static final Comparator<RgbColor> BLUE_COMPARATOR = Comparator.comparing(RgbColor::getB);

  private final int r;
  private final int g;
  private final int b;

  public static Comparator<RgbColor> determineComparator(final RgbDimension rgbDimension) {
    if (rgbDimension == RgbDimension.RED) {
      return RED_COMPARATOR;
    }
    if (rgbDimension == RgbDimension.GREEN) {
      return GREEN_COMPARATOR;
    }
    return BLUE_COMPARATOR;
  }
}
