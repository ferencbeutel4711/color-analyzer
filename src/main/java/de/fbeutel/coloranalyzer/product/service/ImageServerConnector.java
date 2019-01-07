package de.fbeutel.coloranalyzer.product.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import de.fbeutel.coloranalyzer.product.domain.ProductImage;

@Slf4j
@Service
public class ImageServerConnector {

  private static final Integer MAX_W = 500;
  private static final Integer MAX_H = 500;

  public BufferedImage fetchImage(final ProductImage imageToFetch) {
    try {
      final URL scrapingUri = UriComponentsBuilder.fromUriString(imageToFetch.getUrl())
        .queryParam("maxW", MAX_W)
        .queryParam("maxH", MAX_H)
        .build()
        .toUri()
        .toURL();
      return ImageIO.read(scrapingUri);
    } catch (IOException exception) {
      log.error("exception during image download", exception);
      throw new RuntimeException();
    }
  }
}
