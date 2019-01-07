package de.fbeutel.coloranalyzer.color.service;

import de.fbeutel.coloranalyzer.color.domain.RgbColor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

import static java.lang.Math.*;

@Slf4j
@Service
public class ImageService {

    public RgbColor getRgb(final byte[] imageData, final int coordinateX, final int coordinateY, final int width,
                           final int pixelLength) {
        final int pos = coordinateY * pixelLength * width + coordinateX * pixelLength;

        return RgbColor.builder()
                .r(imageData[pos + 2] & 0xff)
                .g(imageData[pos + 1] & 0xff)
                .b(imageData[pos] & 0xff)
                .build();
    }

    public RgbColor getMeanColor(final RgbColor... colors) {
        return RgbColor.builder()
                .r(round(Arrays.stream(colors).mapToLong(RgbColor::getR).sum() / (double) colors.length))
                .g(round(Arrays.stream(colors).mapToLong(RgbColor::getG).sum() / (double) colors.length))
                .b(round(Arrays.stream(colors).mapToLong(RgbColor::getB).sum() / (double) colors.length))
                .build();
    }
}
