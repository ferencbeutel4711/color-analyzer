package de.fbeutel.coloranalyzer.color.service;

import de.fbeutel.coloranalyzer.color.domain.BorderColorDeterminationResult;
import de.fbeutel.coloranalyzer.color.domain.ImageEdge;
import de.fbeutel.coloranalyzer.color.domain.RgbColor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static de.fbeutel.coloranalyzer.color.domain.ImageEdge.*;

@Slf4j
@Service
public class ImageBorderService {

  public static final double ALLOWED_BORDER_COLOR_DISTANCE = 7.0;

  private final ImageService imageService;
  private final ColorDistanceService colorDistanceService;

  public ImageBorderService(ImageService imageService, ColorDistanceService colorDistanceService) {
    this.imageService = imageService;
    this.colorDistanceService = colorDistanceService;
  }

  public BorderColorDeterminationResult determineBorderColor(final BufferedImage bufferedImage) {
    final byte[] data = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();

    final int width = bufferedImage.getWidth();
    final int height = bufferedImage.getHeight();
    final int pixelLength = bufferedImage.getAlphaRaster() != null ? 4 : 3;

    final Pair<Double, RgbColor> upperResult = determineUniformity(data, UPPER, width, height, pixelLength);
    final Pair<Double, RgbColor> leftResult = determineUniformity(data, LEFT, width, height, pixelLength);
    final Pair<Double, RgbColor> rightResult = determineUniformity(data, RIGHT, width, height, pixelLength);
    final Pair<Double, RgbColor> lowerResult = determineUniformity(data, LOWER, width, height, pixelLength);

    return BorderColorDeterminationResult.builder()
            .upperEdgeUniformity(upperResult.getLeft())
            .leftEdgeUniformity(leftResult.getLeft())
            .rightEdgeUniformity(rightResult.getLeft())
            .lowerEdgeUniformity(lowerResult.getLeft())
            .borderColor(imageService.getMeanColor(upperResult.getRight(), leftResult.getRight(),
                    rightResult.getRight(), lowerResult.getRight()))
            .build();
  }

  private Pair<Double, RgbColor> determineUniformity(final byte[] data, final ImageEdge edge, final int width,
                                                     final int height, final int pixelLength) {
    final RgbColor baseColor = imageService.getRgb(data, 0, 0, width, pixelLength);

    RgbColor meanColor = baseColor;
    double amountOfNonUniformPixels = 0;

    final int iterBound = edge == UPPER || edge == LOWER ? width : height;
    for (int counter = 0; counter < iterBound; counter++) {
      final RgbColor currentColor = imageService.getRgb(data, determineCol(edge, counter, width),
              determineRow(edge, counter, height), width, pixelLength);
      if (colorDistanceService.calculateDistance(currentColor, baseColor) > ALLOWED_BORDER_COLOR_DISTANCE) {
        amountOfNonUniformPixels++;
      }
      meanColor = imageService.getMeanColor(currentColor, meanColor);
    }

    final double uniformity = 1 - amountOfNonUniformPixels / iterBound;
    return Pair.of(uniformity, meanColor);
  }

  private int determineCol(final ImageEdge edge, final int counter, final int max) {
    return edge == LEFT ? 0 : edge == RIGHT ? max - 1 : counter;
  }

  private int determineRow(final ImageEdge edge, final int counter, final int max) {
    return edge == UPPER ? 0 : edge == LOWER ? max - 1 : counter;
  }
}
