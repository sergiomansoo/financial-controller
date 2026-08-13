package com.sergio.financial.goal;
import com.sergio.financial.transaction.TransactionNotFoundException; import com.sergio.financial.user.UserRepository; import java.math.*; import java.time.*; import java.util.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service public class SavingsGoalService {
 private final SavingsGoalRepository goals; private final SavingsGoalMonthRepository months; private final UserRepository users;
 public SavingsGoalService(SavingsGoalRepository goals,SavingsGoalMonthRepository months,UserRepository users){this.goals=goals;this.months=months;this.users=users;}
 @Transactional public SavingsGoalResponse create(Long userId,SavingsGoalRequest r){SavingsGoal g=goals.save(new SavingsGoal(users.getReferenceById(userId),r.name().trim(),r.targetAmount(),r.targetDate()));return response(userId,g,YearMonth.now(),null);}
 @Transactional(readOnly=true) public List<SavingsGoalResponse> list(Long userId,YearMonth month){return goals.findByUserIdOrderById(userId).stream().map(g->response(userId,g,month,months.findByUserIdAndGoalIdAndReferenceMonth(userId,g.getId(),month.atDay(1)).orElse(null))).toList();}
 @Transactional public SavingsGoalResponse update(Long userId,Long id,YearMonth month,SavingsGoalMonthRequest r){SavingsGoal g=goals.findByIdAndUserId(id,userId).orElseThrow(TransactionNotFoundException::new);SavingsGoalMonth m=months.findByUserIdAndGoalIdAndReferenceMonth(userId,id,month.atDay(1)).orElseGet(()->new SavingsGoalMonth(users.getReferenceById(userId),g,month.atDay(1),r.plannedAmount(),r.savedAmount()));m.update(r.plannedAmount(),r.savedAmount());months.save(m);return response(userId,g,month,m);}
 @Transactional public void delete(Long userId,Long id){
  SavingsGoal goal=goals.findByIdAndUserId(id,userId).orElseThrow(TransactionNotFoundException::new);
  months.deleteByUserIdAndGoalId(userId, goal.getId());
  goals.delete(goal);
 }
 private SavingsGoalResponse response(Long uid,SavingsGoal g,YearMonth month,SavingsGoalMonth m){BigDecimal planned=m==null?BigDecimal.ZERO:m.getPlannedAmount(),saved=m==null?BigDecimal.ZERO:m.getSavedAmount();BigDecimal overall=months.findByUserIdAndGoalId(uid,g.getId()).stream().map(SavingsGoalMonth::getSavedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);return new SavingsGoalResponse(g.getId(),g.getName(),g.getTargetAmount(),g.getTargetDate(),month.toString(),planned,saved,pct(saved,planned),overall,pct(overall,g.getTargetAmount()));}
 private BigDecimal pct(BigDecimal a,BigDecimal b){return b.signum()==0?BigDecimal.ZERO:a.multiply(BigDecimal.valueOf(100)).divide(b,2,RoundingMode.HALF_UP);}
}
