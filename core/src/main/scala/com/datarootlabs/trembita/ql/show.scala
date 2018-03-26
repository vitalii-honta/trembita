package com.datarootlabs.trembita.ql

import language.experimental.macros
import scala.reflect.macros.blackbox
import cats.Show
import cats.implicits._
import GroupingCriteria._
import Aggregation._
import ArbitraryGroupResult._
import shapeless.tag._


trait ShowPretty[A] {
  def spaces(count: Int): String = Seq.fill(count)(" ").mkString
  def appendPrint(a: A)(currSpaces: Int, requiredSpaces: Int): String
  def pretty(a: A)(requiredSpaces: Int): String = appendPrint(a)(0, requiredSpaces)
}
object ShowPretty {
  def apply[A](implicit S: ShowPretty[A]): ShowPretty[A] = S
}

trait show {
  def showTaggedImpl[A: c.WeakTypeTag, U: c.WeakTypeTag](c: blackbox.Context): c.Expr[Show[A ## U]] = {
    import c.universe._
    val A = weakTypeOf[A].dealias
    val u = weakTypeOf[U]
    val U = u.dealias
    val uName = u.typeSymbol.toString.dropWhile(_ != ' ').tail
    c.Expr[Show[A ## U]](q"""
      new Show[$A ## $U]{
        def show(v: $A ## $U): String = $uName + ": " + v.value
      }
    """)
  }
  def `showPretty-##@`[A: Show]: ShowPretty[##@[A]] = new ShowPretty[##@[A]] {
    def appendPrint(a: ##@[A])(currSpaces: Int, requiredSpaces: Int): String = {
      val ##@(records@_*) = a
      val defaultSpace: String = spaces(currSpaces)
      val totalSpaces: String = spaces(currSpaces + requiredSpaces)
      val mkStrSpaces: String = spaces(currSpaces + requiredSpaces * 2)
      s"##@(${
        records.map(_.show).mkString(s"\n$mkStrSpaces", s",\n$mkStrSpaces", "")
      }\n$defaultSpace)"
    }
  }
  def `showPretty-~::`[
  A: Show,
  KH <: ##[_, _] : Show,
  KT <: GroupingCriteria : Show,
  T <: Aggregation : Show
  ](implicit subGrShowPretty: ShowPretty[ArbitraryGroupResult[A, KT, T]]
   ): ShowPretty[~::[A, KH, KT, T]] = new ShowPretty[~::[A, KH, KT, T]] {
    def appendPrint(a: ~::[A, KH, KT, T])(currSpaces: Int, requiredSpaces: Int): String = {
      import a._

      val defaultSpace: String = spaces(currSpaces)
      val totalSpaces: String = spaces(currSpaces + requiredSpaces)
      s"""|~::(
          |${totalSpaces}key = ${key.show},
          |${totalSpaces}totals = ${totals.show}
          |${totalSpaces}subGroup = ${
        subGrShowPretty.appendPrint(subGroup)(
          currSpaces + requiredSpaces,
          requiredSpaces)
      }\n$defaultSpace)""".stripMargin
    }
  }
  def `showPretty-~**`[
  A: Show,
  K <: GroupingCriteria : Show,
  T <: Aggregation : Show
  ](grShowPretty: ShowPretty[ArbitraryGroupResult[A, K, T]]
   ): ShowPretty[~**[A, K, T]] = new ShowPretty[~**[A, K, T]] {
    def appendPrint(a: ~**[A, K, T])(currSpaces: Int, requiredSpaces: Int): String = {
      import a._

      val defaultSpace: String = spaces(currSpaces)
      val totalSpaces: String = spaces(currSpaces + requiredSpaces)
      val mkStrSpaces: String = spaces(currSpaces + requiredSpaces * 2)
      s"""|~**(
          |${totalSpaces}totals = ${totals.show}
          |${totalSpaces}records = ${
        records.map(grShowPretty.appendPrint(_)(
          currSpaces + requiredSpaces * 2,
          requiredSpaces))
          .mkString(s"{\n$mkStrSpaces", s",\n$mkStrSpaces", "}")
      }\n$defaultSpace)""".stripMargin
    }
  }

  def showPrettyArbGroupResultImpl[
  A: c.WeakTypeTag,
  K <: GroupingCriteria : c.WeakTypeTag,
  T <: Aggregation : c.WeakTypeTag
  ](c: blackbox.Context): c.Expr[ShowPretty[ArbitraryGroupResult[A, K, T]]] = {
    import c.universe._
    val A = weakTypeOf[A].dealias
    val K = weakTypeOf[K].dealias
    val T = weakTypeOf[T].dealias
    val gnil = typeOf[GNil].dealias
    val expr = K match {
      case `gnil` ⇒ q"`showPretty-##@`[$A]"
      case _      ⇒
        val List(kHx, kTx) = K.typeArgs
        val KH = kHx.dealias
        val KT = kTx.dealias
        q"""
          import ArbitraryGroupResult._
          new ShowPretty[ArbitraryGroupResult[$A, $K, $T]] {
            private val prettyCons   = `showPretty-~::`[$A, $KH, $KT, $T]
            private val prettyMul    = `showPretty-~**`[$A, $K, $T](this)
            override def appendPrint(a: ArbitraryGroupResult[$A, $K, $T])(currSpaces: Int, requiredSpaces: Int): String = {
              a match {
                 case cons: ~::[_,_,_,_] => prettyCons.appendPrint(cons.asInstanceOf[~::[$A, $KH, $KT, $T]])(
                   currSpaces, requiredSpaces
                 )
                 case mul: ~**[_,_,_]    => prettyMul.appendPrint(mul.asInstanceOf[~**[$A, $K, $T]])(
                   currSpaces, requiredSpaces
                 )
              }
            }
          }
        """
    }
    c.Expr[ShowPretty[ArbitraryGroupResult[A, K, T]]](q"$expr.asInstanceOf[ShowPretty[ArbitraryGroupResult[$A, $K, $T]]]")
  }
}

object show extends show {
  implicit def showTagged[A, U]: Show[A ## U] = macro showTaggedImpl[A, U]

  implicit object ShowGNil extends Show[GNil] {
    override def show(t: GNil): String = "∅"
  }
  implicit def showGroupCriteriaCons[
  GH <: ##[_, _] : Show,
  GT <: GroupingCriteria : Show
  ]: Show[GH &:: GT] = new Show[GH &:: GT] {
    override def show(t: GH &:: GT): String = {
      val tailStr = t.rest match {
        case GNil  ⇒ ""
        case other ⇒ s" & ${other.show}"
      }
      s"${t.first.show}$tailStr"
    }
  }
  implicit object ShowAgNil extends Show[AgNil] {
    override def show(t: AgNil): String = "∅"
  }
  implicit def showAggregationNameCons[
  AgH <: ##[_, _] : Show,
  AgT <: Aggregation : Show
  ]: Show[AgH %:: AgT] = new Show[AgH %:: AgT] {
    override def show(t: AgH %:: AgT): String = {
      val tailStr = t.agTail match {
        case AgNil ⇒ ""
        case other ⇒ s" & ${other.show}"
      }
      s"${t.agHead.show} $tailStr"
    }
  }
  implicit def showPrettyArbGroupResult[
  A,
  K <: GroupingCriteria,
  T <: Aggregation]: ShowPretty[ArbitraryGroupResult[A, K, T]] = macro showPrettyArbGroupResultImpl[A, K, T]

  implicit def prettySeq[A](implicit S: ShowPretty[A]): ShowPretty[Seq[A]] = new ShowPretty[Seq[A]] {
    override def appendPrint(a: Seq[A])(currSpaces: Int, requiredSpaces: Int): String = {
      val defaultSpaces = spaces(currSpaces)
      val accSpaces = spaces(currSpaces + requiredSpaces)
      a.mkString(s"$defaultSpaces[\n$accSpaces", s",\n$accSpaces", s"\n$defaultSpaces]")
    }
  }
  implicit class PrettyOps[A](val self: A) extends AnyVal {
    def pretty(requiredSpaces: Int = 2)(implicit showPretty: ShowPretty[A]): String = showPretty.pretty(self)(requiredSpaces)
  }
}