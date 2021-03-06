package de.fbeutel.coloranalyzer.product.service;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import de.fbeutel.coloranalyzer.color.domain.BorderColorDeterminationResult;
import de.fbeutel.coloranalyzer.color.domain.ColorDistribution;
import de.fbeutel.coloranalyzer.color.service.ColorDistributionService;
import de.fbeutel.coloranalyzer.color.service.ImageBorderService;
import de.fbeutel.coloranalyzer.color.service.ImageScoringService;
import de.fbeutel.coloranalyzer.product.domain.Product;
import de.fbeutel.coloranalyzer.product.domain.ProductData;
import de.fbeutel.coloranalyzer.product.domain.ProductImage;

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

  private static final List<String> SEARCH_WORDS = asList("hose", "kleid", "anzug", "hemd", "badehose", "t-shirt");

  @EventListener(ApplicationReadyEvent.class)
  public void startScraping() {
    log.info("starting to import product data");

    final ExecutorService productUrlsExecutorService = Executors.newFixedThreadPool(5);
    final List<Future<List<String>>> productUrlsFutures = new ArrayList<>();
    SEARCH_WORDS.forEach(searchWord -> productUrlsFutures.add(
      productUrlsExecutorService.submit(() -> scraperConnector.fetchProductUrls(searchWord))));

    final ExecutorService productDataExecutorService = Executors.newFixedThreadPool(5);
    final List<Future<ProductData>> productDataFutures = new ArrayList<>();
    final Set<String> productUrls = new HashSet<>();
    productUrlsFutures.forEach(productUrlsFuture -> {
      try {
        productUrls.addAll(productUrlsFuture.get());
      } catch (InterruptedException | ExecutionException exception) {
        log.error("error during resolving of a product urls future", exception);
      }
    });

    productUrls.forEach(productUrl -> productDataFutures.add(productDataExecutorService.submit(() -> scraperConnector
      .fetchProductData(productUrl))));

    final List<Long> performanceProbesDistribution = new ArrayList<>();
    final List<Long> performanceProbesScoring = new ArrayList<>();
    final AtomicInteger iterCount = new AtomicInteger();

    final long processingStartTime = System.currentTimeMillis();
    productDataFutures.stream().parallel()
      .forEach(productDataFuture -> {
        try {
          log.info("future #" + productDataFutures.indexOf(productDataFuture) + " of " + productDataFutures.size());
          iterCount.incrementAndGet();
          final ProductData productData = productDataFuture.get();

          if (!productService.findOne(productData.getId()).isPresent()) {
            boolean foundAcceptableImage = false;
            for (final String imageId : productData.getImages()) {
              final ProductImage rawImage = ProductImage.builder()
                .id(imageId)
                .url("https://i.otto.de/i/otto/" + imageId)
                .build();

              if (!rawImage.getUrl().contains("lh_platzhalter_ohne_abbildung")) {
                final BufferedImage image = imageServerConnector.fetchImage(rawImage);
                final BorderColorDeterminationResult borderColor = imageBorderService.determineBorderColor(image);
                if (acceptableBorderColorResult(borderColor)) {
                  final long startTime = System.currentTimeMillis();

                  final ColorDistribution distribution = colorDistributionService
                    .colorDistribution(image, borderColor.getBorderColor());

                  final long middleTime = System.currentTimeMillis();

                  final ProductImage enrichedImage = rawImage.toBuilder()
                    .imageScores(imageScoringService.calculateScores(distribution))
                    .colorDistribution(distribution)
                    .build();

                  final long endTime = System.currentTimeMillis();

                  performanceProbesDistribution.add(middleTime - startTime);
                  performanceProbesScoring.add(endTime - middleTime);

                  log.info("creating product: " + productData.getId());

                  productService.create(Product.builder()
                    .id(productData.getId())
                    .productImage(enrichedImage)
                    .build());
                  foundAcceptableImage = true;
                  break;
                }
              }
            }

            if (!foundAcceptableImage) {
              log.warn("could not determine acceptable image from images: " + productData.getImages() + " of product: " +
                productData.getId());
            }
          }
        } catch (InterruptedException | ExecutionException exception) {
          log.error("error during resolving of a product data future", exception);
        }
      });

    log.info("done importing");
    log.info("total products processed: " + iterCount.get());
    log.info("total time elapsed: " + (System.currentTimeMillis() - processingStartTime) / 1000.0 + " seconds");

    final double avgDistributionTime = performanceProbesDistribution.stream().mapToLong(i -> i).average().getAsDouble();
    final long minDistributionTime = performanceProbesDistribution.stream().mapToLong(i -> i).min().getAsLong();
    final long maxDistributionTime = performanceProbesDistribution.stream().mapToLong(i -> i).max().getAsLong();
    final int p99DistributionIndex = (int) (performanceProbesDistribution.size() / 100.0 * 99);
    final long p99DistributionTime = performanceProbesDistribution.stream().sorted().collect(toList()).get(p99DistributionIndex);
    log.info("average execution time color distribution: " + avgDistributionTime);
    log.info("min execution time color distribution: " + minDistributionTime);
    log.info("max execution time color distribution: " + maxDistributionTime);
    log.info("p99 execution time color distribution: " + p99DistributionTime);

    final double avgScoringTime = performanceProbesScoring.stream().mapToLong(i -> i).average().getAsDouble();
    final long minScoringTime = performanceProbesScoring.stream().mapToLong(i -> i).min().getAsLong();
    final long maxScoringTime = performanceProbesScoring.stream().mapToLong(i -> i).max().getAsLong();
    final int p99ScoringIndex = (int) (performanceProbesScoring.size() / 100.0 * 99);
    final long p99ScoringTime = performanceProbesScoring.stream().sorted().collect(toList()).get(p99ScoringIndex);

    log.info("average execution time scoring: " + avgScoringTime);
    log.info("min execution time scoring: " + minScoringTime);
    log.info("max execution time scoring: " + maxScoringTime);
    log.info("p99 execution time scoring: " + p99ScoringTime);
  }

  private boolean acceptableBorderColorResult(final BorderColorDeterminationResult result) {
    final double threshold = 0.9;
    return result.getUpperEdgeUniformity() >= threshold && result.getLeftEdgeUniformity() >= threshold && result
      .getRightEdgeUniformity() >= threshold && result.getLowerEdgeUniformity() >= threshold;
  }
}
