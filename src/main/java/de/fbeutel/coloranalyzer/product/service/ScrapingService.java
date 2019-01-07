package de.fbeutel.coloranalyzer.product.service;

import de.fbeutel.coloranalyzer.color.domain.BorderColorDeterminationResult;
import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;
import de.fbeutel.coloranalyzer.color.service.ColorDistributionService;
import de.fbeutel.coloranalyzer.color.service.ImageBorderService;
import de.fbeutel.coloranalyzer.color.service.ImageScoringService;
import de.fbeutel.coloranalyzer.product.domain.Product;
import de.fbeutel.coloranalyzer.product.domain.ProductData;
import de.fbeutel.coloranalyzer.product.domain.ProductImage;
import de.fbeutel.coloranalyzer.product.domain.ProductUrls;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Arrays.asList;

@Slf4j
@Service
public class ScrapingService {

  private final ScraperConnector scraperConnector;
  private final ProductService productService;
  private final ColorDistributionService colorDistributionService;
  private final ImageServerConnector imageServerConnector;
  private final ImageBorderService imageBorderService;
  private final ImageScoringService imageScoringService;

  public ScrapingService(final ScraperConnector scraperConnector,
                         final ProductService productService,
                         final ColorDistributionService colorDistributionService,
                         final ImageServerConnector imageServerConnector, ImageBorderService imageBorderService,
                         ImageScoringService imageScoringService) {
    this.scraperConnector = scraperConnector;
    this.productService = productService;
    this.colorDistributionService = colorDistributionService;
    this.imageServerConnector = imageServerConnector;
    this.imageBorderService = imageBorderService;
    this.imageScoringService = imageScoringService;
  }

  private static final List<String> SEARCH_WORDS = asList("hose", "kleid", "anzug");

  @EventListener(ApplicationReadyEvent.class)
  public void startScraping() {
    log.info("starting to import product data");

    final ExecutorService productUrlsExecutorService = Executors.newFixedThreadPool(5);
    final List<Future<ProductUrls>> productUrlsFutures = new ArrayList<>();
    SEARCH_WORDS.forEach(searchWord -> productUrlsFutures.add(
            productUrlsExecutorService.submit(() -> scraperConnector.fetchProductUrls(searchWord))));

    final ExecutorService productDataExecutorService = Executors.newFixedThreadPool(10);
    final List<Future<ProductData>> productDataFutures = new ArrayList<>();
    productUrlsFutures.forEach(productUrlsFuture -> {
      try {
        productUrlsFuture.get()
                .getUrls()
                .forEach(productUrl -> productDataFutures.add(productDataExecutorService.submit(() -> scraperConnector
                        .fetchProductData(productUrl))));
      } catch (InterruptedException | ExecutionException exception) {
        log.error("error during resolving of a product urls future", exception);
      }
    });

    productDataFutures.forEach(productDataFuture -> {
      try {
        final ProductData productData = productDataFuture.get();
        boolean foundAcceptableImage = false;
        for (final String imageId : productData.getImages()) {
          final ProductImage rawImage = ProductImage.builder()
                  .id(imageId)
                  .url("https://i.otto.de/i/otto/" + imageId)
                  .build();

          final BufferedImage image = imageServerConnector.fetchImage(rawImage);
          final BorderColorDeterminationResult borderColor = imageBorderService.determineBorderColor(image);
          if (acceptableBorderColorResult(borderColor)) {
            final ColorDistribution distribution = colorDistributionService
                    .colorDistribution(image, borderColor.getBorderColor());

            final ProductImage enrichedImage = rawImage.toBuilder()
                    .imageScores(imageScoringService.calculateScores(distribution))
                    .colorDistribution(distribution)
                    .build();

            productService.create(Product.builder()
                    .id(productData.getId())
                    .productImage(enrichedImage)
                    .build());
            foundAcceptableImage = true;
            break;
          }
        }
        if (!foundAcceptableImage) {
          log.warn("could not determine acceptable image from images: " + productData.getImages());
        }
      } catch (InterruptedException | ExecutionException exception) {
        log.error("error during resolving of a product data future", exception);
      }
    });

    log.info("done importing");
  }

  private boolean acceptableBorderColorResult(final BorderColorDeterminationResult result) {
    final double threshold = 0.9;
    return result.getUpperEdgeUniformity() >= threshold && result.getLeftEdgeUniformity() >= threshold && result
            .getRightEdgeUniformity() >= threshold && result.getLowerEdgeUniformity() >= threshold;
  }
}
