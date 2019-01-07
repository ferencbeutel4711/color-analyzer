package de.fbeutel.coloranalyzer.color.service;

import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;
import de.fbeutel.coloranalyzer.color.domain.ColorTile;
import de.fbeutel.coloranalyzer.product.domain.ImageScore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.lang.Math.round;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class ImageScoringService {

  private final ColorDistanceService colorDistanceService;

  public ImageScoringService(ColorDistanceService colorDistanceService) {
    this.colorDistanceService = colorDistanceService;
  }

  public Map<ColorTile, Double> calculateScores(final ColorDistribution colorDistribution) {
    return Arrays.stream(ColorTile.values()).collect(toMap(
            Function.identity(),
            tile -> colorDistanceService.calculateDistance(
                    colorDistribution.getColorDistributionEntries().get(0).getColor(), tile.getColor())));
  }
}
