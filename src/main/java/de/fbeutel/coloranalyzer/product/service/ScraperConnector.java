package de.fbeutel.coloranalyzer.product.service;

import static org.springframework.http.RequestEntity.get;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import de.fbeutel.coloranalyzer.product.domain.ProductData;
import de.fbeutel.coloranalyzer.product.domain.ProductUrls;

@Slf4j
@Service
public class ScraperConnector {

  private final RestTemplate restTemplate;

  public ScraperConnector(final RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
  }

  public List<String> fetchProductUrls(final String searchWord) {
    final int numberOfPages = 8;
    final List<String> foundUrls = new ArrayList<>();

    IntStream.range(1, numberOfPages + 1).forEach(pageNumber -> {
      final String urlToScrape = "https://www.otto.de/suche/" + searchWord + "?p=" + pageNumber;
      final String scraperBaseUrl = "http://localhost:3000/scrape/productLinks";
      final URI scrapingUri = UriComponentsBuilder.fromUriString(scraperBaseUrl)
        .queryParam("url", urlToScrape)
        .build()
        .toUri();

      final ResponseEntity<ProductUrls> response = restTemplate.exchange(get(scrapingUri).build(), ProductUrls.class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.error("error during product url fetching! RC: " + response.getStatusCodeValue() + " body: " + response.getBody());
      } else {
        foundUrls.addAll(response.getBody().getUrls().stream()
          .filter(url -> !url.contains("lh_platzhalter_ohne_abbildung"))
          .collect(Collectors.toList()));
      }
    });

    return foundUrls;
  }

  public ProductData fetchProductData(final String urlToScrape) {
    final String scraperBaseUrl = "http://localhost:3000/scrape/productData";
    final URI scrapingUri = UriComponentsBuilder.fromUriString(scraperBaseUrl)
      .queryParam("url", urlToScrape)
      .build()
      .toUri();

    final ResponseEntity<ProductData> response = restTemplate.exchange(get(scrapingUri).build(), ProductData.class);

    if (!response.getStatusCode().is2xxSuccessful()) {
      log.error("error during product data fetching! RC: " + response.getStatusCodeValue() + " body: " + response.getBody());
    }

    return response.getBody();
  }
}
