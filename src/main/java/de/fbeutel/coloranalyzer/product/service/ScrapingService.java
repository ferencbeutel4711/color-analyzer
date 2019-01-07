package de.fbeutel.coloranalyzer.product.service;

import static java.util.Arrays.asList;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import de.fbeutel.coloranalyzer.color.domain.RgbColor;
import de.fbeutel.coloranalyzer.color.service.ColorDistributionService;
import de.fbeutel.coloranalyzer.product.domain.Product;
import de.fbeutel.coloranalyzer.product.domain.ProductData;
import de.fbeutel.coloranalyzer.product.domain.ProductImage;
import de.fbeutel.coloranalyzer.product.domain.ProductUrls;

@Slf4j
@Service
public class ScrapingService {

  private final ScraperConnector scraperConnector;
  private final ProductService productService;
  private final ColorDistributionService colorDistributionService;
  private final ImageServerConnector imageServerConnector;

  public ScrapingService(final ScraperConnector scraperConnector,
                         final ProductService productService,
                         final ColorDistributionService colorDistributionService,
                         final ImageServerConnector imageServerConnector) {
    this.scraperConnector = scraperConnector;
    this.productService = productService;
    this.colorDistributionService = colorDistributionService;
    this.imageServerConnector = imageServerConnector;
  }

  private static final List<String> SEARCH_WORDS = asList("hose", "kleid", "anzug");

  @PostConstruct
  public void startScraping() {
    final ExecutorService productUrlsExecutorService = Executors.newFixedThreadPool(5);
    final List<Future<ProductUrls>> productUrlsFutures = new ArrayList<>();
    SEARCH_WORDS.forEach(
      searchWord -> productUrlsFutures.add(
        productUrlsExecutorService.submit(() -> scraperConnector.fetchProductUrls(searchWord))));

    final ExecutorService productDataExecutorService = Executors.newFixedThreadPool(10);
    final List<Future<ProductData>> productDataFutures = new ArrayList<>();
    productUrlsFutures.forEach(productUrlsFuture -> {
      try {
        productUrlsFuture.get()
          .getUrls()
          .forEach(productUrl -> productDataFutures.add(
            productDataExecutorService.submit(() -> scraperConnector.fetchProductData(productUrl))));
      } catch (InterruptedException | ExecutionException exception) {
        log.error("error during resolving of a product urls future", exception);
      }
    });

    productDataFutures.forEach(productDataFuture -> {
      try {
        final ProductData productData = productDataFuture.get();
        // TODO: filter images to determine exempted image instead of picking the first
        final String filteredImage = productData.getImages().get(0);
        final ProductImage rawImage = ProductImage.builder()
          .id(filteredImage)
          .url("https://i.otto.de/i/otto/" + filteredImage)
          .build();

        final BufferedImage imageData = imageServerConnector.fetchImage(rawImage);

        final ProductImage enrichedImage = rawImage.toBuilder()
          // TODO: calculate image scores
          .imageScores(new ArrayList<>())
          // TODO: determine correct border color
          .colorDistribution(colorDistributionService.determineColorDistribution(imageData, RgbColor.builder()
            .r(255)
            .g(255)
            .b(255)
            .build()))
          .build();

        productService.create(Product.builder()
          .id(productData.getId())
          .productImage(enrichedImage)
          .build());
      } catch (InterruptedException | ExecutionException exception) {
        log.error("error during resolving of a product data future", exception);
      }
    });
  }
}
