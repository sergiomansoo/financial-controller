package com.sergio.financial.category;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class SystemCategorySeeder implements ApplicationRunner {
    private static final List<String> SYSTEM_CATEGORY_NAMES = List.of(
            "Alimenta\u00e7\u00e3o", "Transporte", "Mercado/Compras", "Investimentos", "Outros");

    private final CategoryRepository categories;

    SystemCategorySeeder(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        seed();
    }

    void seed() {
        if (categories.countBySystemCategoryTrue() == 0) {
            categories.saveAll(SYSTEM_CATEGORY_NAMES.stream().map(Category::system).toList());
        }
    }
}
