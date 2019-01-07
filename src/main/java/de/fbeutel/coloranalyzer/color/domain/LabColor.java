package de.fbeutel.coloranalyzer.color.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Comparator;

@Data
@Builder(toBuilder = true)
public class LabColor {

  public static final double CHROMA_CORRECTION = 6103515625.0;
  public static final double L_SCALE = 100;
  public static final double A_SCALE = 255;
  public static final double B_SCALE = 255;

  private final double l;
  private final double a;
  private final double b;

  @Override
  public String toString() {
    final String delimiter = ",";
    final String[] className = this.getClass().getName().split("\\.");
    return className[className.length - 1] + ":" + l + delimiter + a + delimiter + b;
  }
}
