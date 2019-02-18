package de.fbeutel.coloranalyzer.color.service;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;
import de.fbeutel.coloranalyzer.color.domain.ColorDistributionEntry;
import de.fbeutel.coloranalyzer.color.domain.ColorTile;

@Slf4j
@Service
public class ImageScoringService {

  private final ColorDistanceService colorDistanceService;
  private final ColorTileService colorTileService;

  public ImageScoringService(ColorDistanceService colorDistanceService,
                             final ColorTileService colorTileService) {
    this.colorDistanceService = colorDistanceService;
    this.colorTileService = colorTileService;
  }

  public Map<String, Double> calculateScores(final ColorDistribution colorDistribution) {
    final double shareAmount = colorDistribution.getColorDistributionEntries().stream()
      .mapToInt(ColorDistributionEntry::getShare)
      .sum();

    return colorTileService.getColorTiles().values().stream()
      .flatMap(List::stream)
      .collect(toMap(ColorTile::getName,
        tile -> colorDistribution.getColorDistributionEntries().stream()
          .mapToDouble(calculateColorDistanceShare(tile, shareAmount))
          .average()
          .orElse(100000)));
  }

  private ToDoubleFunction<ColorDistributionEntry> calculateColorDistanceShare(final ColorTile tile, final double shareAmount) {
    return colorDistributionEntry -> {
      final double colorPercentageOfImage = colorDistributionEntry.getShare() / shareAmount;
      return colorPercentageOfImage * colorDistanceService.calculateDistance(colorDistributionEntry.getColor(), tile.getColor());
    };
  }
}
