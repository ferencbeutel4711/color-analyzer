package de.fbeutel.coloranalyzer.color.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ColorDistributionEntry implements Comparable<ColorDistributionEntry> {

  private final RgbColor color;
  private final int share;

  @Override
  public int compareTo(final ColorDistributionEntry otherColorDistributionEntry) {
    return this.share - otherColorDistributionEntry.getShare();
  }
}
