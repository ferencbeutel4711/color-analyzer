package de.fbeutel.coloranalyzer.color.service;

import de.fbeutel.coloranalyzer.color.domain.LabColor;
import de.fbeutel.coloranalyzer.color.domain.RgbColor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static de.fbeutel.coloranalyzer.color.domain.LabColor.CHROMA_CORRECTION;

@Slf4j
@Service
public class ColorDistanceService {

  private final ColorConversionService colorConversionService;

  public ColorDistanceService(ColorConversionService colorConversionService) {
    this.colorConversionService = colorConversionService;
  }

  public double calculateDistance(final RgbColor color1, final RgbColor color2) {
    return calculateDistance(colorConversionService.toLab(color1), colorConversionService.toLab(color2));
  }

  public double calculateDistance(final LabColor color1, final LabColor color2) {
    // several corrections
    final double seventhExponentChroma = Math.pow((Math.sqrt(Math.pow(color1.getA(), 2) + Math.pow(color1.getB(), 2)) + Math
            .sqrt(Math.pow(color2.getA(), 2) + Math.pow(color2.getB(), 2))) / 2, 7);
    final double correctedChroma = Math.sqrt(seventhExponentChroma / (seventhExponentChroma + CHROMA_CORRECTION));
    final double shiftedCorrectedChroma = (1 - correctedChroma) / 2;
    final double myCorrectedA = (1 + shiftedCorrectedChroma) * color1.getA();
    final double othersCorrectedA = (1 + shiftedCorrectedChroma) * color2.getA();
    final double myAltChroma = Math.sqrt(Math.pow(myCorrectedA, 2) + Math.pow(color1.getB(), 2));
    final double othersAltChroma = Math.sqrt(Math.pow(othersCorrectedA, 2) + Math.pow(color2.getB(), 2));
    final double myHueAngle = getHueAngle(color1, myCorrectedA);
    final double othersHueAngle = getHueAngle(color2, othersCorrectedA);

    //calculating means + standardised values
    final double meanLightness = (color1.getL() + color2.getL()) / 2;
    final double standardisedLightness = 1 + (0.015 * Math.pow(meanLightness - 50, 2)) / (Math.sqrt(20 + Math.pow(meanLightness
            - 50, 2)));

    final double meanChroma = (myAltChroma + othersAltChroma) / 2;
    final double standardisedChroma = 1 + 0.045 * meanChroma;

    final double meanHue = getMeanHue(myHueAngle, othersHueAngle, myAltChroma, othersAltChroma);
    final double standardisedHue = 1 + 0.015 * meanChroma * (1 - 0.17 * Math.cos(Math.toRadians(meanHue - 30)) + 0.24 * Math
            .cos(Math.toRadians(2 * meanHue)) + 0.32 * Math.cos(Math.toRadians(3 * meanHue + 6)) - 0.2 * Math.cos(Math
            .toRadians(4 * meanHue - 63)));

    //calculating result
    final double deltaChroma = othersAltChroma - myAltChroma;
    final double deltaHue = 2 * Math.sqrt(myAltChroma * othersAltChroma) * Math.sin(Math.toRadians(getDeltaHue(myAltChroma,
            othersAltChroma, myHueAngle, othersHueAngle) / 2));

    return Math.sqrt(Math.pow((color2.getL() - color1.getL()) / standardisedLightness, 2) + Math.pow(deltaChroma /
            standardisedChroma, 2) + Math.pow(deltaHue / standardisedHue, 2) + (-2 * correctedChroma * Math.sin(Math.toRadians
            (60 * Math.exp(0 - Math.pow((meanHue - 275) / 25, 2))))) * (deltaChroma / standardisedChroma) * (deltaHue /
            standardisedHue)
    );
  }

  private double getMeanHue(final double hueAngleOne, final double hueAngleTwo, final double chromaOne, final double chromaTwo) {
    final double meanHue;
    final double totalHueAngle = hueAngleOne + hueAngleTwo;
    if (chromaOne * chromaTwo == 0) {
      meanHue = totalHueAngle;
    } else {
      final double deltaHueAngle = Math.abs(hueAngleOne - hueAngleTwo);
      if (deltaHueAngle <= 180) {
        meanHue = totalHueAngle / 2;
      } else {
        if (totalHueAngle < 360) {
          meanHue = (totalHueAngle + 360) / 2;
        } else {
          meanHue = (totalHueAngle - 360) / 2;
        }
      }
    }
    return meanHue;
  }

  private double getDeltaHue(final double chromaOne, final double chromaTwo, final double hueAngleOne, final double hueAngleTwo) {
    final double deltaHue;
    if (chromaOne * chromaTwo == 0) {
      deltaHue = 0;
    } else {
      final double deltaHueAngle = hueAngleTwo - hueAngleOne;
      if (Math.abs(deltaHueAngle) <= 180) {
        deltaHue = deltaHueAngle;
      } else if (deltaHueAngle > 180) {
        deltaHue = deltaHueAngle - 360;
      } else {
        deltaHue = deltaHueAngle + 360;
      }
    }
    return deltaHue;
  }

  private double getHueAngle(final LabColor labColor, final double correctedA) {
    if (labColor.getB() == 0 && correctedA == 0) {
      return 0;
    }
    double arcTan = Math.atan2(labColor.getB(), correctedA);
    if (arcTan < 0) {
      arcTan = arcTan + 2 * Math.PI;
    }
    return Math.toDegrees(arcTan);
  }
}
