package de.fbeutel.coloranalyzer.search.web;

import de.fbeutel.coloranalyzer.color.domain.ColorTile;
import de.fbeutel.coloranalyzer.product.domain.Product;
import de.fbeutel.coloranalyzer.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/search", produces = APPLICATION_JSON_VALUE)
public class SearchRestController {

  private final ProductService productService;

  public SearchRestController(ProductService productService) {
    this.productService = productService;
  }

  @GetMapping("/products")
  public ResponseEntity<List<Product>> findProducts(@RequestParam("colorTile") final ColorTile colorTile) {
    return ResponseEntity.ok(productService.findAllSortedByColor(colorTile));
  }
}
