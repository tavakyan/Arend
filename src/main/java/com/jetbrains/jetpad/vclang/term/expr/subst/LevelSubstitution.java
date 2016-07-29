package com.jetbrains.jetpad.vclang.term.expr.subst;

import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.expr.sort.Level;

import java.util.*;

public class LevelSubstitution {
  private Map<Binding, Level> mySubstExprs;

  public LevelSubstitution() {
    mySubstExprs = Collections.emptyMap();
  }

  public LevelSubstitution(Binding l, Level expr) {
    mySubstExprs = new HashMap<>();
    mySubstExprs.put(l, expr);
  }

  public LevelSubstitution(Binding lp, Level lpExpr, Binding lh, Level lhExpr) {
    mySubstExprs = new HashMap<>();
    mySubstExprs.put(lp, lpExpr);
    mySubstExprs.put(lh, lhExpr);
  }

  public LevelSubstitution(Binding lp, Binding lpNew, Binding lh, Binding lhNew) {
    mySubstExprs = new HashMap<>();
    mySubstExprs.put(lp, new Level(lpNew));
    mySubstExprs.put(lh, new Level(lhNew));
  }

  public Set<Binding> getDomain() {
    return mySubstExprs.keySet();
  }

  public Level get(Binding binding)  {
    return mySubstExprs.get(binding);
  }

  public void add(Binding binding, Level expr) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.put(binding, expr);
  }

  public void add(LevelSubstitution subst) {
    if (mySubstExprs.isEmpty()) {
      mySubstExprs = new HashMap<>();
    }
    mySubstExprs.putAll(subst.mySubstExprs);
  }

  public void subst(Binding binding, Level expr) {
    for (Map.Entry<Binding, Level> entry : mySubstExprs.entrySet()) {
      entry.setValue(entry.getValue().subst(binding, expr));
    }
  }

  public LevelSubstitution compose(LevelSubstitution subst, Collection<? extends Binding> params) {
    LevelSubstitution result = new LevelSubstitution();
    result.add(this);

    loop:
    for (Binding binding : subst.getDomain()) {
      if (mySubstExprs.containsKey(binding)) {
        result.add(binding, subst.get(binding));
        continue;
      }

      for (Map.Entry<Binding, Level> substExpr : mySubstExprs.entrySet()) {
        if (substExpr.getValue().getVar() == binding) {
          result.add(substExpr.getKey(), substExpr.getValue().subst(subst));
          continue loop;
        }
      }

      for (Binding myBinding : getDomain()) {
        if (myBinding.getType().toDataCall().getDefinition() == binding.getType().toDefCall().getDefinition()) {
          continue loop;
        }
      }

      if (params.contains(binding)) {
        result.add(binding, subst.get(binding));
      }
    }
    return result;
  }
}
