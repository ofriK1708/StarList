package repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA converter that maps a {@code List<Integer>} (e.g. {@code [1, 3, 5]}) to a
 * comma-separated {@code VARCHAR} column value (e.g. {@code "1,3,5"}) and back.
 *
 * <p>Used for {@code scheduled_days_of_week} on the {@code habits} table.
 * The list is small (≤ 7 elements) and is never queried by individual element,
 * so a join table would add schema complexity with no benefit.
 */
@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        return attribute.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
