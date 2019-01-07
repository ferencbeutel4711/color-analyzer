package de.fbeutel.coloranalyzer.color.service;

import de.fbeutel.coloranalyzer.color.domain.LabColor;
import de.fbeutel.coloranalyzer.color.domain.RgbColor;
import de.fbeutel.coloranalyzer.color.domain.XyzColor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.springframework.stereotype.Service;

import static de.fbeutel.coloranalyzer.color.domain.LabColor.CHROMA_CORRECTION;
import static de.fbeutel.coloranalyzer.color.domain.XyzColor.XYZ_D65;

@Slf4j
@Service
public class ColorConversionService {

  public LabColor toLab(final RgbColor rgbColor) {
    return toLab(toXyz(rgbColor));
  }

  public XyzColor toXyz(final RgbColor rgbColor) {
    final double[][] MUTATION_INPUT = {
            {0.4124564, 0.3575761, 0.1804375},
            {0.2126729, 0.7151522, 0.0721750},
            {0.0193339, 0.1191920, 0.9503041},
    };

    final double[][] rgbInput = {
            {invCompand(rgbColor.getR() / 255.0)},
            {invCompand(rgbColor.getG() / 255.0)},
            {invCompand(rgbColor.getB() / 255.0)},
    };

    final RealMatrix inputMatrix = MatrixUtils.createRealMatrix(rgbInput);
    final RealMatrix mutationMatrix = MatrixUtils.createRealMatrix(MUTATION_INPUT);

    final RealMatrix mutationResult = mutationMatrix.multiply(inputMatrix);

    return XyzColor.builder()
            .x(Math.round(mutationResult.getEntry(0, 0) * 100 * 100) / 100.0)
            .y(Math.round(mutationResult.getEntry(1, 0) * 100 * 100) / 100.0)
            .z(Math.round(mutationResult.getEntry(2, 0) * 100 * 100) / 100.0)
            .build();
  }

  private LabColor toLab(final XyzColor xyzColor) {
    final double lightnessCorrectedX = xyzColor.getX() / XYZ_D65[0];
    final double lightnessCorrectedY = xyzColor.getY() / XYZ_D65[1];
    final double lightnessCorrectedZ = xyzColor.getZ() / XYZ_D65[2];

    final double variableX;
    final double variableY;
    final double variableZ;

    if (lightnessCorrectedX > 0.008856) {
      variableX = Math.cbrt(lightnessCorrectedX);
    } else {
      variableX = (903.3 * lightnessCorrectedX + 16) / 116;
    }

    if (lightnessCorrectedY > 0.008856) {
      variableY = Math.cbrt(lightnessCorrectedY);
    } else {
      variableY = (903.3 * lightnessCorrectedY + 16) / 116;
    }

    if (lightnessCorrectedZ > 0.008856) {
      variableZ = Math.cbrt(lightnessCorrectedZ);
    } else {
      variableZ = (903.3 * lightnessCorrectedZ + 16) / 116;
    }

    return LabColor.builder()
            .l(Math.round((116.0 * variableY - 16) * 100) / 100.0)
            .a(Math.round((500.0 * (variableX - variableY)) * 100) / 100.0)
            .b(Math.round((200.0 * (variableY - variableZ)) * 100) / 100.0)
            .build();
  }

  private double invCompand(final double companded) {
    if (companded <= 0.04045) {
      return companded / 12.92;
    }
    return Math.pow((companded + 0.055) / 1.055, 2.4);
  }
}
