package de.fbeutel.coloranalyzer.color.domain;

import java.util.Comparator;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder(toBuilder = true)
public class RgbColor {

  public static final Comparator<RgbColor> RED_COMPARATOR = Comparator.comparing(RgbColor::getR);
  public static final Comparator<RgbColor> GREEN_COMPARATOR = Comparator.comparing(RgbColor::getG);
  public static final Comparator<RgbColor> BLUE_COMPARATOR = Comparator.comparing(RgbColor::getB);

  private final long r;
  private final long g;
  private final long b;

  public static Comparator<RgbColor> determineComparator(final RgbDimension rgbDimension) {
    if (rgbDimension == RgbDimension.RED) {
      return RED_COMPARATOR;
    }
    if (rgbDimension == RgbDimension.GREEN) {
      return GREEN_COMPARATOR;
    }
    return BLUE_COMPARATOR;
  }

  @Override
  public String toString() {
    final String delimiter = ",";
    final String[] className = this.getClass().getName().split("\\.");
    return className[className.length - 1] + ":" + r + delimiter + g + delimiter + b;
  }
}
