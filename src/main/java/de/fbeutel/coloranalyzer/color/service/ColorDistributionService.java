package de.fbeutel.coloranalyzer.color.service;

import static java.util.Map.Entry.comparingByValue;

import static de.fbeutel.coloranalyzer.color.service.ImageBorderService.ALLOWED_BORDER_COLOR_DISTANCE;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;
import de.fbeutel.coloranalyzer.color.domain.ColorDistributionEntry;
import de.fbeutel.coloranalyzer.color.domain.RgbColor;
import de.fbeutel.coloranalyzer.color.domain.RgbDimension;

@Service
public class ColorDistributionService {

  private final ImageService imageService;
  private final ColorDistanceService colorDistanceService;

  public ColorDistributionService(ImageService imageService, ColorDistanceService colorDistanceService) {
    this.imageService = imageService;
    this.colorDistanceService = colorDistanceService;
  }

  public ColorDistribution colorDistribution(final BufferedImage image, final RgbColor borderColor) {
    final byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

    final int imageHeight = image.getHeight();
    final int imageWidth = image.getWidth();
    final int pixelLength = image.getAlphaRaster() != null ? 4 : 3;

    final List<RgbColor> rgbColors = new ArrayList<>();

    for (int row = 0; row < imageHeight; row++) {
      for (int col = 0; col < imageWidth; col++) {
        final RgbColor rgbColor = imageService.getRgb(imageData, col, row, imageWidth, pixelLength);

        if (colorDistanceService.calculateDistance(rgbColor, borderColor) > ALLOWED_BORDER_COLOR_DISTANCE) {
          rgbColors.add(rgbColor);
        }
      }
    }

    final List<ColorDistributionEntry> colorDistributionEntries = groupColors(applyMedianCut(rgbColors)).stream()
      .map(rgbColorSharePair -> ColorDistributionEntry.builder()
        .color(rgbColorSharePair.getLeft())
        .share(rgbColorSharePair.getRight())
        .build())
      .sorted(Collections.reverseOrder())
      .collect(Collectors.toList());

    return ColorDistribution.builder()
      .colorDistributionEntries(colorDistributionEntries)
      .build();
  }

  private List<Pair<RgbColor, Integer>> groupColors(final List<Pair<RgbColor, Integer>> input) {
    final int groupingThreshold = 5;
    final int iterThreshold = 100;

    int iterator = 0;
    while (input.size() > groupingThreshold && iterator <= iterThreshold) {
      final Pair<Pair<RgbColor, Integer>, Pair<RgbColor, Integer>> bestGroupingResult = bestGroupingResult(input);

      final Pair<RgbColor, Integer> color1Pair = bestGroupingResult.getLeft();
      final Pair<RgbColor, Integer> color2Pair = bestGroupingResult.getRight();

      input.remove(color1Pair);
      input.remove(color2Pair);
      final RgbColor meanColor = imageService.getMeanColor(color1Pair.getLeft(), color2Pair.getLeft());
      input.add(Pair.of(meanColor, color1Pair.getRight() + color2Pair.getRight()));

      iterator++;
    }

    return input;
  }

  private Pair<Pair<RgbColor, Integer>, Pair<RgbColor, Integer>> bestGroupingResult(final List<Pair<RgbColor, Integer>> input) {
    Map<Pair<Integer, Integer>, Double> index = new ConcurrentHashMap<>();

    for (int i = 0; i < input.size() - 1; i++) {
      final double colorDistance = colorDistanceService.calculateDistance(input.get(i).getLeft(), input.get(i + 1).getLeft());
      index.put(Pair.of(i, i + 1), colorDistance);
    }

    final Pair<Integer, Integer> bestGroupingPairIndices = index.entrySet().stream().min(comparingByValue()).get().getKey();
    return Pair.of(input.get(bestGroupingPairIndices.getLeft()), input.get(bestGroupingPairIndices.getRight()));
  }

  private List<Pair<RgbColor, Integer>> applyMedianCut(final List<RgbColor> input) {
    List<List<RgbColor>> buckets = new ArrayList<>();
    buckets.add(input);

    int iterCount = 0;
    while (buckets.size() < 32 && iterCount < 100) {
      buckets = medianCutBuckets(buckets);
      iterCount++;
    }

    final List<Pair<RgbColor, Integer>> cutBuckets = new ArrayList<>();
    for (final List<RgbColor> bucket : buckets) {
      final List<Long> redValues = new ArrayList<>();
      final List<Long> greenValues = new ArrayList<>();
      final List<Long> blueValues = new ArrayList<>();

      bucket.forEach(color -> {
        redValues.add(color.getR());
        greenValues.add(color.getG());
        blueValues.add(color.getB());
      });

      cutBuckets.add(Pair.of(RgbColor.builder()
        .r(redValues.size() == 0 ? 0 : redValues.stream().mapToLong(v -> v).sum() / redValues.size())
        .g(greenValues.size() == 0 ? 0 : greenValues.stream().mapToLong(v -> v).sum() / greenValues.size())
        .b(blueValues.size() == 0 ? 0 : blueValues.stream().mapToLong(v -> v).sum() / blueValues.size())
        .build(), bucket.size()));
    }

    return cutBuckets;
  }

  private List<List<RgbColor>> medianCutBuckets(final List<List<RgbColor>> buckets) {
    final List<List<RgbColor>> resultBuckets = new ArrayList<>(buckets);
    for (final List<RgbColor> bucket : buckets) {
      if (bucket.size() > 1) {
        final RgbDimension dominantDimension = determineDominantDimension(bucket);
        final Comparator<RgbColor> comparator = RgbColor.determineComparator(dominantDimension);

        bucket.sort(comparator);

        final List<RgbColor> lowerCut = new ArrayList<>(bucket.subList(0, bucket.size() / 2 - 1));
        final List<RgbColor> upperCut = new ArrayList<>(bucket.subList(bucket.size() / 2, bucket.size() - 1));

        resultBuckets.add(lowerCut);
        resultBuckets.add(upperCut);
        resultBuckets.remove(bucket);
      }
    }
    return resultBuckets;
  }

  private RgbDimension determineDominantDimension(final List<RgbColor> bucket) {
    final long redRange = getColorRange(bucket, RgbColor::getR);
    final long greenRange = getColorRange(bucket, RgbColor::getG);
    final long blueRange = getColorRange(bucket, RgbColor::getB);

    if (redRange > greenRange && redRange > blueRange) {
      return RgbDimension.RED;
    }
    if (greenRange > redRange && greenRange > blueRange) {
      return RgbDimension.GREEN;
    }
    return RgbDimension.BLUE;
  }

  private long getColorRange(final List<RgbColor> bucket, final ToLongFunction<RgbColor> toLongMapper) {
    return Math.abs(getSmallestValue(bucket, toLongMapper) - getBiggestValue(bucket, toLongMapper));
  }

  private long getSmallestValue(final List<RgbColor> bucket, final ToLongFunction<RgbColor> toLongMapper) {
    return bucket.stream().mapToLong(toLongMapper).min().orElse(1000);
  }

  private long getBiggestValue(final List<RgbColor> bucket, final ToLongFunction<RgbColor> toLongMapper) {
    return bucket.stream().mapToLong(toLongMapper).max().orElse(1000);
  }
}
