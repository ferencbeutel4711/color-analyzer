package de.fbeutel.coloranalyzer.color.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ColorTile {

  RED(RgbColor.builder().r(219).g(40).b(20).build()),
  GREEN(RgbColor.builder().r(65).g(219).b(20).build()),
  BLUE(RgbColor.builder().r(10).g(20).b(150).build()),
  YELLOW(RgbColor.builder().r(225).g(230).b(10).build()),
  WHITE(RgbColor.builder().r(255).g(255).b(255).build()),
  BLACK(RgbColor.builder().r(0).g(0).b(0).build());

  private final RgbColor color;
}
