package de.fbeutel.coloranalyzer.color.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Comparator;

@Data
@Builder(toBuilder = true)
public class XyzColor {

  public static final double[] XYZ_D65 = {95.047, 100.000, 108.883};

  private final double x;
  private final double y;
  private final double z;

  @Override
  public String toString() {
    final String delimiter = ",";
    final String[] className = this.getClass().getName().split("\\.");
    return className[className.length - 1] + ":" + x + delimiter + y + delimiter + z;
  }
}
