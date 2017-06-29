package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.DataDefinition;
import com.jetbrains.jetpad.vclang.core.elimtree.Pattern;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.StripVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.SubstVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.normalization.EvalNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DataCallExpression extends DefCallExpression implements Type {
  private final Sort mySortArgument;
  private final List<Expression> myArguments;

  public DataCallExpression(DataDefinition definition, Sort sortArgument, List<Expression> arguments) {
    super(definition);
    assert definition.status().headerIsOK();
    mySortArgument = sortArgument;
    myArguments = arguments;
  }

  @Override
  public Sort getSortArgument() {
    return mySortArgument;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return myArguments;
  }

  @Override
  public DataDefinition getDefinition() {
    return (DataDefinition) super.getDefinition();
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitDataCall(this, params);
  }

  @Override
  public DataCallExpression toDataCall() {
    return this;
  }

  @Override
  public Expression getExpr() {
    return this;
  }

  @Override
  public Sort getSortOfType() {
    return getDefinition().getSort().subst(mySortArgument.toLevelSubstitution());
  }

  @Override
  public DataCallExpression subst(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    return new SubstVisitor(exprSubstitution, levelSubstitution).visitDataCall(this, null);
  }

  @Override
  public DataCallExpression strip(Set<Binding> bounds, LocalErrorReporter errorReporter) {
    return new StripVisitor(bounds, errorReporter).visitDataCall(this, null);
  }

  @Override
  public DataCallExpression normalize(NormalizeVisitor.Mode mode) {
    return new NormalizeVisitor(new EvalNormalizer()).visitDataCall(this, mode);
  }

  public List<ConCallExpression> getMatchedConstructors() {
    List<ConCallExpression> result = new ArrayList<>();
    for (Constructor constructor : getDefinition().getConstructors()) {
      if (!getMatchedConCall(constructor, result)) {
        return null;
      }
    }
    return result;
  }

  public boolean getMatchedConCall(Constructor constructor, List<ConCallExpression> conCalls) {
    if (!constructor.status().headerIsOK()) {
      return true;
    }

    List<Expression> matchedParameters;
    if (constructor.getPatterns() != null) {
      matchedParameters = new ArrayList<>();
      Pattern.MatchResult matchResult = constructor.getPatterns().match(myArguments, matchedParameters);
      if (matchResult == Pattern.MatchResult.MAYBE) {
        return false;
      }
      if (matchResult == Pattern.MatchResult.FAIL) {
        return true;
      }
    } else {
      matchedParameters = myArguments;
    }

    conCalls.add(new ConCallExpression(constructor, mySortArgument, new ArrayList<>(matchedParameters), new ArrayList<>()));
    return true;
  }
}
