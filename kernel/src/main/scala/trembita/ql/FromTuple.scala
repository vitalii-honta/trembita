package trembita.ql

import shapeless._
import shapeless.ops.hlist._
import scala.annotation.implicitNotFound

trait FromTuple[T] extends DepFn1[T] with Serializable

object composePoly extends Poly2 with Serializable {
  implicit def case0[A, B, C](implicit ev: C <:!< HList): Case.Aux[A => B, A => C, A => B :: C :: HNil] =
    at((f1, f2) => a => f1(a) :: f2(a) :: HNil)

  implicit def case1[A, B, C <: HList]: Case.Aux[A => B, A => C, A => B :: C] =
    at((f1, f2) => a => f1(a) :: f2(a))
}

object FromTuple {
  @implicitNotFound("Unable to build ${Out0} from tuple ${T}")
  type Aux[T, Out0] = FromTuple[T] { type Out = Out0 }
  def apply[T](implicit ev: FromTuple[T]): FromTuple.Aux[T, ev.Out] = ev

  implicit def recCaseGroup[R, L <: HList, A, ROut <: HList, G <: GroupingCriteria](
      implicit gen: Generic.Aux[R, L],
      rightReducer: RightReducer.Aux[L, composePoly.type, A => ROut],
      fromHList: FromHList.Aux[ROut, G]
  ): FromTuple.Aux[R, A => G] = new FromTuple[R] {
    type Out = A => G
    def apply(t: R): Out = a => {
      val l        = gen.to(t)
      val routFunc = rightReducer(l)
      val rout     = routFunc(a)
      fromHList(rout)
    }
  }

  implicit def recCaseAggDecl[R, L <: HList, A, ROut <: HList, D <: AggDecl](
      implicit gen: Generic.Aux[R, L],
      rightReducer: RightReducer.Aux[L, composePoly.type, A => ROut],
      fromHList: FromHList.Aux[ROut, D]
  ): FromTuple.Aux[R, A => D] = new FromTuple[R] {
    type Out = A => D
    def apply(t: R): Out = a => {
      val l        = gen.to(t)
      val routFunc = rightReducer(l)
      val rout     = routFunc(a)
      fromHList(rout)
    }
  }
}
