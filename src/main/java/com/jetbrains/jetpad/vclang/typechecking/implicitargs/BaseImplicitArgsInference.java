package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

public class BaseImplicitArgsInference implements ImplicitArgsInference {
  protected final CheckTypeVisitor myVisitor;

  protected BaseImplicitArgsInference(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  @Override
  public CheckTypeVisitor.PreResult infer(Abstract.AppExpression expr, Expression expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.PreResult infer(Abstract.BinOpExpression expr, Expression expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result fun, Expression expectedType, Abstract.Expression expr) {
    return null;
  }
}
