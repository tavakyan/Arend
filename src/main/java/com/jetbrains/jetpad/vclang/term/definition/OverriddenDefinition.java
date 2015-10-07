package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;

import java.util.List;

public class OverriddenDefinition extends FunctionDefinition {
  private FunctionDefinition myOverriddenFunction;

  public OverriddenDefinition(Namespace staticNamespace, Namespace dynamicNamespace, Precedence precedence, Arrow arrow) {
    super(staticNamespace, dynamicNamespace, precedence, arrow);
    myOverriddenFunction = null;
  }

  public OverriddenDefinition(Namespace staticNamespace, Namespace dynamicNamespace, Precedence precedence, List<Argument> arguments, Expression resultType, Arrow arrow, Expression term, FunctionDefinition overriddenFunction) {
    super(staticNamespace, dynamicNamespace, precedence, arguments, resultType, arrow, term);
    myOverriddenFunction = overriddenFunction;
  }

  public FunctionDefinition getOverriddenFunction() {
    return myOverriddenFunction;
  }

  public void setOverriddenFunction(FunctionDefinition overriddenFunction) {
    myOverriddenFunction = overriddenFunction;
  }

  @Override
  public boolean isOverridden() {
    return true;
  }

  @Override
  public Name getOriginalName() {
    return myOverriddenFunction.getName();
  }

  @Override
  public List<Argument> getArguments() {
    if (super.getArguments() == null) return myOverriddenFunction.getArguments();
    return super.getArguments();
  }

  @Override
  public Expression getResultType() {
    if (super.getResultType() == null) return myOverriddenFunction.getResultType();
    return super.getResultType();
  }

  @Override
  public Expression getType() {
    if (getArguments() == null || getResultType() == null) return myOverriddenFunction.getType();
    return super.getType();
  }
}
