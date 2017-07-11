package com.jetbrains.jetpad.vclang.core.pattern.elimtree;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;

import java.util.List;

public abstract class ElimTreeNode {
  private Clause myParent = null;

  Clause getParent() {
    return myParent;
  }

  public abstract <P, R> R accept(ElimTreeNodeVisitor<P, R> visitor, P params);

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof ElimTreeNode;
  }

  public ElimTreeNode subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst) {
    if (exprSubst.isEmpty() && levelSubst.isEmpty()) {
      return this;
    }
    return accept(new SubstVisitor(exprSubst, levelSubst), null);
  }

  public ElimTreeNode subst(ExprSubstitution subst) {
    return subst(subst, LevelSubstitution.EMPTY);
  }

  public abstract ElimTreeNode matchUntilStuck(ExprSubstitution subst, boolean normalize);

  public ElimTreeNode matchUntilStuck(ExprSubstitution subst) {
    return matchUntilStuck(subst, true);
  }

  public abstract void updateLeavesMatched(List<Binding> context);

  public abstract LeafElimTreeNode match(List<Expression> expressions);

  void setParent(Clause parent) {
    myParent = parent;
  }
}
