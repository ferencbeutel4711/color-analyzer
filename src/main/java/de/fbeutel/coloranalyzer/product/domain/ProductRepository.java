package de.fbeutel.coloranalyzer.product.domain;

import de.fbeutel.coloranalyzer.color.domain.ColorTile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import static org.springframework.data.domain.Sort.Order.*;

public interface ProductRepository extends MongoRepository<Product, String>, ProductRepositoryCustom {

}

interface ProductRepositoryCustom {
  List<Product> findAllSortedByColor(ColorTile colorTile);
}

class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

  private final MongoTemplate mongoTemplate;

  public ProductRepositoryCustomImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<Product> findAllSortedByColor(ColorTile colorTile) {
    final String fieldName = "imageScores." + colorTile.name();
    final Query query = new Query();
    query.with(Sort.by(asc(fieldName)));

    return mongoTemplate.find(query, Product.class);
  }
}