package io.homemote.repository.postgres

import java.util.concurrent.Executors

import anorm.{Column, MetaDataItem, TypeDoesNotMatch}

import scala.concurrent.ExecutionContext
import scala.util.Try

trait PGRepository {

  implicit protected val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool())

  def parser[A](transformer: PartialFunction[Any, A]): Column[A] = Column.nonNull[A] {(value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    val left = Left(TypeDoesNotMatch(s"Cannot convert $value: $clazz to Set[String] for column $qualified"))
    Try(transformer(value)).map(Right.apply).getOrElse(left)
  }

}
