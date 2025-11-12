package org.splitzy.common.criteria;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor
public class GenericCriteriaBuilder<T> {

    private final List<SearchCriteria> criteria;

    // Buid specification from search criteria
    public Specification<T> build(){
        if(criteria == null || criteria.isEmpty()){
            return null;
        }
        List<Specification<T>> specifications = new ArrayList<>();

        for(SearchCriteria searchCriteria : criteria){
            Specification<T> specification = createSpecification(searchCriteria);
            if(specification != null){
                specifications.add(specification);
            }
        }

        if(specifications.isEmpty()){
            return null;
        }
        //combining all specifications with AND
        Specification<T> result = specifications.get(0);
        for(int i = 1; i < specifications.size(); i++){
            result = Specification.where(result).and(specifications.get(i));
        }
        return result;
    }

    // create individual specification for a search criteria
    private Specification<T> createSpecification(SearchCriteria criteria){
        return (root, query, criteriaBuilder) -> {
            Path<Object> path = getPath(root, criteria.getKey());
            Object value = criteria.getValue();
            
            return switch (criteria.getOperation()){
                case EQUALITY -> criteriaBuilder.equal(path, value);
                case NEGATION -> criteriaBuilder.notEqual(path, value);
                case GREATER_THAN -> {
                    if(value instanceof Comparable){
                        yield criteriaBuilder.greaterThan(path.as(Comparable.class), (Comparable)value);
                    }
                    yield null;
                }
                case GREATER_THAN_OR_EQUAL -> {
                    if (value instanceof Comparable) {
                        yield criteriaBuilder.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
                    }
                    yield null;
                }

                case LESS_THAN -> {
                    if (value instanceof Comparable) {
                        yield criteriaBuilder.lessThan(path.as(Comparable.class), (Comparable) value);
                    }
                    yield null;
                }

                case LESS_THAN_OR_EQUAL -> {
                    if (value instanceof Comparable) {
                        yield criteriaBuilder.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
                    }
                    yield null;
                }

                case LIKE -> {
                    if (value instanceof String) {
                        yield criteriaBuilder.like(
                                criteriaBuilder.lower(path.as(String.class)),
                                "%" + ((String) value).toLowerCase() + "%"
                        );
                    }
                    yield null;
                }

                case STARTS_WITH -> {
                    if (value instanceof String) {
                        yield criteriaBuilder.like(
                                criteriaBuilder.lower(path.as(String.class)),
                                ((String) value).toLowerCase() + "%"
                        );
                    }
                    yield null;
                }

                case ENDS_WITH -> {
                    if (value instanceof String) {
                        yield criteriaBuilder.like(
                                criteriaBuilder.lower(path.as(String.class)),
                                "%" + ((String) value).toLowerCase()
                        );
                    }
                    yield null;
                }

                case IN -> {
                    if (value instanceof Collection) {
                        yield path.in((Collection<?>) value);
                    }
                    yield null;
                }

                case NOT_IN -> {
                    if (value instanceof Collection) {
                        yield criteriaBuilder.not(path.in((Collection<?>) value));
                    }
                    yield null;
                }

                case IS_NULL -> criteriaBuilder.isNull(path);

                case IS_NOT_NULL -> criteriaBuilder.isNotNull(path);

                case BETWEEN -> {
                    if (value instanceof List<?> list && list.size() == 2) {
                        Object start = list.get(0);
                        Object end = list.get(1);
                        if (start instanceof Comparable && end instanceof Comparable) {
                            yield criteriaBuilder.between(
                                    path.as(Comparable.class),
                                    (Comparable) start,
                                    (Comparable) end
                            );
                        }
                    }
                    yield null;
                }

                case JOIN -> {
                    // Handle join operations if needed
                    // This would require more complex implementation based on use case
                    yield null;
                }

                default -> null;
            };
        };
    }
    /**
     * Get path for nested properties (supports dot notation)
     */
    private Path<Object> getPath(Root<T> root, String propertyName) {
        if (propertyName.contains(".")) {
            String[] parts = propertyName.split("\\.");
            Path<Object> path = root.get(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                path = path.get(parts[i]);
            }
            return path;
        } else {
            return root.get(propertyName);
        }
    }

    /**
     * Static factory method for creating builder
     */
    public static <T> GenericCriteriaBuilder<T> of(List<SearchCriteria> criteria) {
        return new GenericCriteriaBuilder<>(criteria);
    }

    /**
     * Add search criteria and return new builder
     */
    public GenericCriteriaBuilder<T> with(String key, SearchOperation operation, Object value) {
        List<SearchCriteria> newCriteria = new ArrayList<>(this.criteria);
        newCriteria.add(new SearchCriteria(key, operation, value));
        return new GenericCriteriaBuilder<>(newCriteria);
    }

    /**
     * Add search criteria with join and return new builder
     */
    public GenericCriteriaBuilder<T> with(String key, SearchOperation operation, Object value, String joinKey) {
        List<SearchCriteria> newCriteria = new ArrayList<>(this.criteria);
        newCriteria.add(new SearchCriteria(key, operation, value, joinKey));
        return new GenericCriteriaBuilder<>(newCriteria);
    }
}
