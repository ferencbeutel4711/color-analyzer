package de.fbeutel.coloranalyzer.color.service;

import static org.springframework.http.RequestEntity.get;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import de.fbeutel.coloranalyzer.color.domain.ColorTile;

@Slf4j
@Service
public class ColorTileService {

  private final RestTemplate restTemplate;

  public ColorTileService(final RestTemplateBuilder restTemplateBuilder) {
    this.restTemplate = restTemplateBuilder.build();
  }

  public Map<String, List<ColorTile>> getColorTiles() {
    final String colorSearcherBaseUrl = "http://localhost:8090/tiles";
    final URI colorSearcherUri = UriComponentsBuilder.fromUriString(colorSearcherBaseUrl)
      .build()
      .toUri();

    final ResponseEntity<Map<String, List<ColorTile>>> response = restTemplate.exchange(get(colorSearcherUri).build(),
      new ParameterizedTypeReference<Map<String, List<ColorTile>>>() {
      });

    if (!response.getStatusCode().is2xxSuccessful()) {
      log.error("error during color tiles fetching! RC: " + response.getStatusCodeValue() + " body: " + response.getBody());
      return new ConcurrentHashMap<>();
    }

    return response.getBody();
  }
}
