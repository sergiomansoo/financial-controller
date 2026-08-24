package com.sergio.financial.category;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemCategorySeederTest {
    @Mock
    private CategoryRepository categories;

    @Test
    void createsTheFiveSystemCategoriesWhenTheExistingSchemaHasNone() {
        when(categories.countBySystemCategoryTrue()).thenReturn(0L);

        new SystemCategorySeeder(categories).seed();

        ArgumentCaptor<List<Category>> saved = ArgumentCaptor.forClass(List.class);
        verify(categories).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Category::getName)
                .containsExactly("Alimenta\u00e7\u00e3o", "Transporte", "Mercado/Compras", "Investimentos", "Outros");
        assertThat(saved.getValue()).allMatch(Category::isSystemCategory);
    }
}
