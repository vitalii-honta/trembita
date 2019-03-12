package trembita.operations

import cats.{~>, Monad, MonadError}
import trembita.{BiDataPipelineT, Environment}
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.control.NonFatal

trait MagnetlessOps[F[_], Er, A, E <: Environment] extends Any {
  def `this`: BiDataPipelineT[F, Er, A, E]

  def map[B: ClassTag](
      f: A => B
  )(implicit F: MonadError[F, Er]): BiDataPipelineT[F, Er, B, E] =
    `this`.mapImpl(f)

  def mapConcat[B: ClassTag](
      f: A => Iterable[B]
  )(implicit F: MonadError[F, Er]): BiDataPipelineT[F, Er, B, E] =
    `this`.mapConcatImpl(f)

  def filter(p: A => Boolean)(implicit F: MonadError[F, Er], A: ClassTag[A]): BiDataPipelineT[F, Er, A, E] =
    `this`.filterImpl(p)

  def collect[B: ClassTag](
      pf: PartialFunction[A, B]
  )(implicit F: MonadError[F, Er]): BiDataPipelineT[F, Er, B, E] =
    `this`.collectImpl(pf)

  def flatCollect[B: ClassTag](
      pf: PartialFunction[A, Iterable[B]]
  )(implicit F: MonadError[F, Er]): BiDataPipelineT[F, Er, B, E] =
    `this`.collectImpl(pf).flatten

  def handleError(f: Er => A)(implicit F: MonadError[F, Er], A: ClassTag[A]): BiDataPipelineT[F, Er, A, E] =
    `this`.handleErrorImpl[Er, A](f)

  def recover(pf: PartialFunction[Er, A])(
      implicit F: MonadError[F, Er],
      A: ClassTag[A]
  ): BiDataPipelineT[F, Er, A, E] =
    `this`.handleErrorWithImpl[Er, A](pf.andThen[F[A]](F.pure).applyOrElse[Er, F[A]](_, (e: Er) => F.raiseError[A](e)))

  def recoverNonFatal(f: Throwable => A)(
      implicit ev: Er <:< Throwable,
      F: MonadError[F, Er],
      A: ClassTag[A],
  ): BiDataPipelineT[F, Er, A, E] =
    recover {
      case NonFatal(e) => f(e)
    }

  def handleErrorWith(f: Er => F[A])(
      implicit F: MonadError[F, Er],
      A: ClassTag[A]
  ): BiDataPipelineT[F, Er, A, E] = `this`.handleErrorWithImpl[Er, A](f)

  def recoverWith(pf: PartialFunction[Er, F[A]])(
      implicit F: MonadError[F, Er],
      A: ClassTag[A]
  ): BiDataPipelineT[F, Er, A, E] =
    `this`.handleErrorWithImpl[Er, A](pf.applyOrElse(_, (e: Er) => F.raiseError[A](e)))

  def mapM[B: ClassTag](
      f: A => F[B]
  )(implicit F: MonadError[F, Er]): BiDataPipelineT[F, Er, B, E] =
    `this`.mapMImpl[A, B](f)

  def mapG[B: ClassTag, G[_]](
      f: A => G[B]
  )(implicit funcK: G ~> F, F: MonadError[F, Er]): BiDataPipelineT[F, Er, B, E] =
    `this`.mapMImpl[A, B] { a =>
      val gb = f(a)
      val fb = funcK(gb)
      fb
    }

  def transformError[Er2: ClassTag](f: Er => Er2)(
      implicit F0: MonadError[F, Er],
      F1: MonadError[F, Er2],
      Er: ClassTag[Er]
  ): BiDataPipelineT[F, Er2, A, E] =
    `this`.transformErrorImpl[Er, Er2](f)

  def mapError(f: Er => Er)(implicit F: MonadError[F, Er], Er: ClassTag[Er]): BiDataPipelineT[F, Er, A, E] =
    `this`.transformErrorImpl[Er, Er](f)

  def attempt(implicit F: MonadError[F, Er]): BiDataPipelineT[F, Er, Either[Er, A], E] =
    `this`.mapImpl[Either[Er, A]](Right(_)).handleErrorImpl(Left(_))
}
