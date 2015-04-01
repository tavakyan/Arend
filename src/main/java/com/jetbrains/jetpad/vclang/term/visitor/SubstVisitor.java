package com.jetbrains.jetpad.vclang.term.visitor;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.NameArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class SubstVisitor implements ExpressionVisitor<Expression> {
  private Expression mySubstExpr;
  private int myFrom;

  public SubstVisitor(Expression substExpr, int from) {
    mySubstExpr = substExpr;
    myFrom = from;
  }

  @Override
  public Expression visitApp(AppExpression expr) {
    return Apps(expr.getFunction().accept(this), expr.getArgument().accept(this));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr) {
    return expr;
  }

  @Override
  public Expression visitIndex(IndexExpression expr) {
    if (expr.getIndex() < myFrom) return Index(expr.getIndex());
    if (expr.getIndex() == myFrom) return mySubstExpr;
    return Index(expr.getIndex() - 1);
  }

  @Override
  public Expression visitLam(LamExpression expr) {
    List<Argument> arguments = new ArrayList<>();
    int on = 0;
    for (Argument argument : expr.getArguments()) {
      if (argument instanceof NameArgument) {
        arguments.add(argument);
        ++on;
      } else
      if (argument instanceof TelescopeArgument) {
        myFrom += on;
        TelescopeArgument teleArgument = (TelescopeArgument) argument;
        mySubstExpr = mySubstExpr.liftIndex(0, on);
        arguments.add(new TelescopeArgument(argument.getExplicit(), teleArgument.getNames(), teleArgument.getType().subst(mySubstExpr, myFrom)));
        on = teleArgument.getNames().size();
      } else {
        throw new IllegalStateException();
      }
    }
    return Lam(arguments, expr.getBody().subst(mySubstExpr.liftIndex(0, on), myFrom + on));
  }

  @Override
  public Expression visitNat(NatExpression expr) {
    return expr;
  }

  @Override
  public Expression visitNelim(NelimExpression expr) {
    return expr;
  }

  @Override
  public Expression visitPi(PiExpression expr) {
    List<TypeArgument> arguments = new ArrayList<>();
    for (TypeArgument argument : expr.getArguments()) {
      if (argument instanceof TelescopeArgument) {
        List<String> names = ((TelescopeArgument) argument).getNames();
        arguments.add(new TelescopeArgument(argument.getExplicit(), names, argument.getType().subst(mySubstExpr, myFrom)));
        mySubstExpr = mySubstExpr.liftIndex(0, names.size());
        myFrom += names.size();
      } else {
        arguments.add(new TypeArgument(argument.getExplicit(), argument.getType().subst(mySubstExpr, myFrom)));
        mySubstExpr = mySubstExpr.liftIndex(0, 1);
        ++myFrom;
      }
    }
    return Pi(arguments, expr.getCodomain().subst(mySubstExpr, myFrom));
  }

  @Override
  public Expression visitSuc(SucExpression expr) {
    return expr;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr) {
    return expr;
  }

  @Override
  public Expression visitVar(VarExpression expr) {
    return expr;
  }

  @Override
  public Expression visitZero(ZeroExpression expr) {
    return expr;
  }

  @Override
  public Expression visitHole(HoleExpression expr) {
    return expr.getInstance(expr.expression() == null ? null : expr.expression().accept(this));
  }
}
