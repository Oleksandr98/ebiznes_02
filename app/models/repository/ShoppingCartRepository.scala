package models.repository

import models._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.sql.Date
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class ShoppingCartRepository @Inject()(dbConfigProvider: DatabaseConfigProvider, ordRepo: OrderRepository,
                                       cusRepo: CustomerRepository, shcPrdRepo: ShoppingCartProductsRepository,
                                       prdRepo: ProductRepository)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import cusRepo.CustomerTable
  import dbConfig._
  import ordRepo.OrdersTable
  import prdRepo.ProductTable
  import profile.api._
  import shcPrdRepo.ShoppingCartProductsTable

  class ShoppingCartTable(tag: Tag) extends Table[ShoppingCartData](tag, "SHOPPING_CART") {

    def id = column[Long]("SHC_ID", O.PrimaryKey, O.AutoInc)

    def modifyDate = column[Option[Date]]("SHC_MODIFY_DATE")

    def createDate = column[Option[Date]]("SHC_CREATE_DATE")

    def removeDate = column[Option[Date]]("SHC_REMOVE_DATE")

    def value = column[Double]("SHC_VALUE")

    def orderId = column[Option[Long]]("SHC_ORD_ID")

    def customerId = column[Long]("SHC_CTM_ID")

    private def order = foreignKey("ORDER_FK", orderId, orderData)(_.id)

    private def customer = foreignKey("CUSTOMER_FK", customerId, customerData)(_.id)

    def * = (id, createDate, modifyDate, removeDate, value, orderId, customerId) <> ((ShoppingCartData.apply _).tupled, ShoppingCartData.unapply)
  }

  private val orderData = TableQuery[OrdersTable]
  private val customerData = TableQuery[CustomerTable]
  private val shoppingCartData = TableQuery[ShoppingCartTable]
  private val shoppingCartProductsData = TableQuery[ShoppingCartProductsTable]
  private val productData = TableQuery[ProductTable]


  def create(shData: WSShoppingCartData): Future[Long] = db.run {
    val currentDate = Option.apply(new Date(new java.util.Date().getTime))
    (shoppingCartData.map(sData => (sData.createDate, sData.modifyDate, sData.removeDate, sData.value, sData.orderId, sData.customerId))
      returning shoppingCartData.map(_.id)
      ) += (currentDate, Option.empty, Option.empty, 0, Option.empty, shData.customerId)
  }

  def getAll(): Future[Seq[ShoppingCartData]] = db.run {
    shoppingCartData.result
  }

  def getById(id: Long): Future[Option[ShoppingCartData]] = db.run {
    shoppingCartData.filter(sc => sc.id === id && sc.removeDate.column.isEmpty).result.headOption
  }

  def removeById(id: Long): Future[Int] = db.run {
    val currentDate = Option.apply(new Date(new java.util.Date().getTime))
    shoppingCartData.filter(sc => sc.id === id && sc.removeDate.column.isEmpty).map(v => v.removeDate).update(currentDate)
  }

  def addProduct(shcId: Long, prdId: Long, quantity: Int): Future[Future[Int]] = {
    db.run(productData.filter(_.id === prdId).result.headOption).map {
      case Some(prd) =>
        val prdValues: Seq[ShoppingCartProductData] = Await.result(db.run(shoppingCartProductsData.filter(sc => sc.shcId === shcId).result), Duration.Inf)
        val currentPrdValue = prd.value * quantity
        var totalValue = currentPrdValue
        for (elem <- prdValues) {
          totalValue = totalValue + elem.value
        }
        val currentDate = Option.apply(new Date(new java.util.Date().getTime))
        db.run(shoppingCartData.filter(sc => sc.id === shcId && sc.removeDate.column.isEmpty).map(v => (v.modifyDate, v.value)).update(currentDate, totalValue)).map {
          case 1 =>
            shcPrdRepo.create(prdId, shcId, quantity, currentPrdValue)
            1
          case _ => 0
        }
      case None => Future.successful(0)
    }
  }

  def removeProduct(shcId: Long, prdId: Long): Future[Future[Int]] = {
    db.run(productData.filter(_.id === prdId).result.headOption).map {
      case Some(prd) =>
        val prdValues: Seq[ShoppingCartProductData] = Await.result(db.run(shoppingCartProductsData.filter(sc => sc.shcId === shcId).result), Duration.Inf)
        if (prdValues.nonEmpty) {
          var totalValue: Double = 0
          for (elem <- prdValues) {
            if (elem.prdId == prdId) {

            } else {
              totalValue = totalValue + elem.value
            }

          }
          val currentDate = Option.apply(new Date(new java.util.Date().getTime))
          db.run(shoppingCartData.filter(sc => sc.id === shcId && sc.removeDate.column.isEmpty).map(v => (v.modifyDate, v.value)).update(currentDate, totalValue)).map {
            case 1 =>
              shcPrdRepo.removeProductById(prdId, shcId)
              1
            case _ => 0
          }
        } else {
          Future.successful(0)
        }
      case None => Future.successful(0)
    }
  }

  def updateById(id: Long, sData: WSUpdateShoppingCartData): Future[Int] = {
    val currentDate = Option.apply(new Date(new java.util.Date().getTime))
    var updateQuery = "SHC_MODIFY_DATE = " + currentDate.get.getTime
    if (sData.value.isDefined) updateQuery += ", SHC_VALUE = '" + sData.value.get + "'"
    if (sData.orderId.isDefined) updateQuery += ", SHC_ORD_ID = '" + sData.orderId.get + "'"
    if (sData.customerId.isDefined) updateQuery += ", SHC_CTM_ID = '" + sData.customerId.get + "'"
    db.run(sql"UPDATE SHOPPING_CART SET #$updateQuery WHERE SHC_ID = $id".asUpdate)
  }

}
