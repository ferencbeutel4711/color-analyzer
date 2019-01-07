package de.fbeutel.coloranalyzer.color.domain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Comparator;

@Data
@Builder(toBuilder = true)
public class BorderColorDeterminationResult {

    private final double upperEdgeUniformity;
    private final double lowerEdgeUniformity;
    private final double leftEdgeUniformity;
    private final double rightEdgeUniformity;

    private final RgbColor borderColor;
}
