package de.fbeutel.coloranalyzer.color.service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;
import de.fbeutel.coloranalyzer.color.domain.ColorDistributionEntry;
import de.fbeutel.coloranalyzer.color.domain.RgbColor;
import de.fbeutel.coloranalyzer.color.domain.RgbDimension;

@Service
public class ColorDistributionService {

  public ColorDistribution determineColorDistribution(final BufferedImage image, final RgbColor borderColor) {
    final byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

    final int imageHeight = image.getHeight();
    final int imageWidth = image.getWidth();
    final int pixelLength = image.getAlphaRaster() != null ? 4 : 3;

    final List<RgbColor> rgbColors = new ArrayList<>();

    for (int row = 0; row < imageHeight; row++) {
      for (int col = 0; col < imageWidth; col++) {
        final RgbColor rgbColor = getRgb(imageData, col, row, imageWidth, pixelLength);

        if (!rgbColor.equals(borderColor)) {
          rgbColors.add(rgbColor);
        }
      }
    }

    final List<ColorDistributionEntry> colorDistributionEntries = applyMedianCut(rgbColors).stream()
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

  private RgbColor getRgb(final byte[] imageData, final int coordinateX, final int coordinateY, final int width,
                          final int pixelLength) {
    final int pos = coordinateY * pixelLength * width + coordinateX * pixelLength;

    return RgbColor.builder()
      .r(imageData[pos + 2] & 0xff)
      .g(imageData[pos + 1] & 0xff)
      .b(imageData[pos] & 0xff)
      .build();
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
      final List<Integer> redValues = new ArrayList<>();
      final List<Integer> greenValues = new ArrayList<>();
      final List<Integer> blueValues = new ArrayList<>();

      bucket.forEach(color -> {
        redValues.add(color.getR());
        greenValues.add(color.getG());
        blueValues.add(color.getB());
      });

      cutBuckets.add(Pair.of(RgbColor.builder()
        .r(redValues.stream().mapToInt(v -> v).sum() / redValues.size())
        .g(greenValues.stream().mapToInt(v -> v).sum() / greenValues.size())
        .b(blueValues.stream().mapToInt(v -> v).sum() / blueValues.size())
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
    final int redRange = getColorRange(bucket, RgbColor::getR);
    final int greenRange = getColorRange(bucket, RgbColor::getG);
    final int blueRange = getColorRange(bucket, RgbColor::getB);

    if (redRange > greenRange && redRange > blueRange) {
      return RgbDimension.RED;
    }
    if (greenRange > redRange && greenRange > blueRange) {
      return RgbDimension.GREEN;
    }
    return RgbDimension.BLUE;
  }

  private int getColorRange(final List<RgbColor> bucket, final ToIntFunction<RgbColor> toIntMapper) {
    return Math.abs(getSmallestValue(bucket, toIntMapper) - getBiggestValue(bucket, toIntMapper));
  }

  private int getSmallestValue(final List<RgbColor> bucket, final ToIntFunction<RgbColor> toIntMapper) {
    return bucket.stream().mapToInt(toIntMapper).min().orElse(1000);
  }

  private int getBiggestValue(final List<RgbColor> bucket, final ToIntFunction<RgbColor> toIntMapper) {
    return bucket.stream().mapToInt(toIntMapper).max().orElse(1000);
  }

  private Pair<Double, Double> updateRange(final double value, final Pair<Double, Double> oldPair) {
    double leftValue = oldPair.getLeft();
    double rightValue = oldPair.getRight();
    if (value < oldPair.getLeft()) {
      leftValue = value;
    }
    if (value > oldPair.getRight()) {
      rightValue = value;
    }
    return Pair.of(leftValue, rightValue);
  }
}
