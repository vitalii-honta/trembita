package com.github.trembita.operations

import cats.Id

import scala.collection.parallel.immutable.ParVector
import scala.language.higherKinds

trait CanToVector[F[_]] {
  type Result[X]
  def apply[A](fa: F[A]): Result[Vector[A]]
}

object CanToVector {
  type Aux[F[_], R0[_]] = CanToVector[F] {type Result[X] = R0[X]}
  implicit val vectorToVector: CanToVector.Aux[Vector, Id] = new CanToVector[Vector] {
    final type Result[X] = X
    def apply[A](fa: Vector[A]): Vector[A] = fa
  }

  implicit val parVectorToVector: CanToVector.Aux[ParVector, Id] = new CanToVector[ParVector] {
    final type Result[X] = X
    def apply[A](fa: ParVector[A]): Vector[A] = fa.seq
  }
}