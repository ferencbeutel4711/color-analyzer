package de.fbeutel.coloranalyzer.color.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ColorTile {
  private final String name;
  private final RgbColor color;
}
