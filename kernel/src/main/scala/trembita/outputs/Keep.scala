package trembita.outputs

import cats.{Monad, MonadError}
import trembita.outputs.internal.{KeepLeft, KeepRight, OutputT, OutputWithPropsT}
import trembita.{BiDataPipelineT, Environment}
import scala.language.higherKinds
import scala.reflect.ClassTag

object Keep {
  type Aux[Out0[_[_], _], Out1[_[_], _], OutX[_[_], _]] = Keep[Out0, Out1] { type Out[G[_], A] = OutX[G, A] }

  def left[Out0[_[_], _], Out1[_[_], _]](implicit KeepLeft: KeepLeft[Out0, Out1]): Keep.Aux[Out0, Out1, Out0] = new Keep[Out0, Out1] {
    type Out[G[_], A] = Out0[G, A]

    def apply[F[_], A](left: Out0[F, A], right: Out1[F, A])(implicit F: Monad[F]): Out0[F, A] = KeepLeft(left, right)
  }

  def right[Out0[_[_], _], Out1[_[_], _]](implicit KeepRight: KeepRight[Out0, Out1]): Keep.Aux[Out0, Out1, Out1] = new Keep[Out0, Out1] {
    type Out[G[_], A] = Out1[G, A]

    def apply[F[_], A](left: Out0[F, A], right: Out1[F, A])(implicit F: Monad[F]): Out1[F, A] = KeepRight(left, right)
  }

  def both[Out0[_[_], _], Out1[_[_], _]]: Keep.Aux[Out0, Out1, λ[(G[_], A) => (Out0[G, A], Out1[G, A])]] = new Keep[Out0, Out1] {
    type Out[G[_], A] = (Out0[G, A], Out1[G, A])

    def apply[F[_], A](left: Out0[F, A], right: Out1[F, A])(implicit F: Monad[F]): (Out0[F, A], Out1[F, A]) = (left, right)
  }
}

trait Keep[Out0[_[_], _], Out1[_[_], _]] extends Serializable { self =>
  type Out[G[_], A]

  def apply[F[_], A](left: Out0[F, A], right: Out1[F, A])(implicit F: Monad[F]): Out[F, A]

  def newOutputWithoutProps[F[_], Er, A, E <: Environment](
      left: OutputT.Aux[F, Er, A, E, Out0],
      right: OutputT.Aux[F, Er, A, E, Out1]
  ): OutputT.Aux[F, Er, A, E, Out] =
    new OutputT[F, Er, A, E] {
      type Out[G[_], β] = self.Out[G, β]

      def apply[Err >: Er, AA >: A](
          pipeline: BiDataPipelineT[F, Err, AA, E]
      )(implicit F: MonadError[F, Er], E: E, run: E#Run[F], A: ClassTag[AA]): Out[F, AA] = {
        val ppln     = pipeline.memoize()(F.asInstanceOf[MonadError[F, Err]], E, run, implicitly)
        val leftOut  = left(ppln)
        val rightOut = right(ppln)
        self(leftOut, rightOut)
      }
    }

  def newOutputWithProps[F[_], Er, E <: Environment, P0[_], P1[_]](
      left: OutputWithPropsT.Aux[F, Er, E, P0, Out0],
      right: OutputWithPropsT.Aux[F, Er, E, P1, Out1]
  ): OutputWithPropsT.Aux[F, Er, E, λ[a => (P0[a], P1[a])], Out] =
    new OutputWithPropsT[F, Er, E] {
      type Props[β]     = (P0[β], P1[β])
      type Out[G[_], β] = self.Out[G, β]

      def apply[Err >: Er, A: ClassTag](props: (P0[A], P1[A]))(
          pipeline: BiDataPipelineT[F, Err, A, E]
      )(implicit F: MonadError[F, Er], E: E, run: E#Run[F]): Keep.this.Out[F, A] = {
        val ppln     = pipeline.memoize()(F.asInstanceOf[MonadError[F, Err]], E, run, implicitly).asInstanceOf[BiDataPipelineT[F, Er, A, E]]
        val leftOut  = left(props._1)(ppln)
        val rightOut = right(props._2)(ppln)
        self(leftOut, rightOut)
      }
    }
}
