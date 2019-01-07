package de.fbeutel.coloranalyzer.color.domain;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ColorDistribution {

  private final List<ColorDistributionEntry> colorDistributionEntries;
}
