package com.sergio.financial.budget;
import com.sergio.financial.category.Category; import com.sergio.financial.user.User; import jakarta.persistence.*; import java.math.BigDecimal; import java.time.LocalDate;
@Entity @Table(name="budgets") public class Budget {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne @JoinColumn(name="user_id") private User user; @ManyToOne @JoinColumn(name="category_id") private Category category;
 @Column(name="budget_month") private LocalDate month; @Column(name="amount_limit") private BigDecimal limit;
 protected Budget(){} public Budget(User u,Category c,LocalDate m,BigDecimal l){user=u;category=c;month=m;limit=l;} public void setLimit(BigDecimal l){limit=l;}
 public Category getCategory(){return category;} public BigDecimal getLimit(){return limit;}
}
