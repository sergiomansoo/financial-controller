package com.sergio.financial.rule;

import com.sergio.financial.category.Category;
import com.sergio.financial.category.CategoryRepository;
import com.sergio.financial.user.User;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CategorizationService {
    private final CategoryRuleRepository rules;
    private final CategoryRepository categories;

    public CategorizationService(CategoryRuleRepository rules, CategoryRepository categories) {
        this.rules = rules;
        this.categories = categories;
    }

    public Category categorize(User user, String normalizedDescription) {
        return rules.findByUserIdAndNormalizedDescription(user.getId(), normalizedDescription)
                .map(CategoryRule::getCategory)
                .orElseGet(() -> systemCategory(user.getId(), normalizedDescription));
    }

    public void learn(User user, String normalizedDescription, Category category) {
        rules.findByUserIdAndNormalizedDescription(user.getId(), normalizedDescription)
                .ifPresentOrElse(rule -> rule.updateCategory(category),
                        () -> rules.save(new CategoryRule(user, category, normalizedDescription)));
    }

    public String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private Category systemCategory(Long userId, String description) {
        String name;
        if (containsAny(description, "padaria", "lanches", "restaurante", "ifood")) {
            name = "Alimenta\u00e7\u00e3o";
        } else if (containsAny(description, "99", "uber", "combust\u00edvel")) {
            name = "Transporte";
        } else if (containsAny(description, "distribuidora", "mercado")) {
            name = "Mercado/Compras";
        } else if (containsAny(description, "cdb", "aplica\u00e7\u00e3o", "resgate")) {
            name = "Investimentos";
        } else {
            name = "Outros";
        }
        return categories.findAccessibleByUserId(userId).stream()
                .filter(category -> category.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
