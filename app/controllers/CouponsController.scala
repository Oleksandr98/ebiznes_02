package controllers

import models.repository.CouponsRepository
import models.{CouponStatuses, WSCouponData, WSUpdateCouponData}
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CouponsController @Inject()(val controllerComponents: ControllerComponents, cpnRepo: CouponsRepository) extends BaseController {

  def getCoupons() = Action.async { implicit request: Request[AnyContent] =>
    for {
      list <- cpnRepo.getAll()
    } yield {
      val json = JsObject(Seq(
        "coupons" -> Json.toJson(list)
      ))
      Ok(json)
    }
  }

  def addCoupon(): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSCouponData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { couponData =>
      cpnRepo.create(couponData.number, couponData.customerId).map(id =>
        Ok(Json.obj("status" -> "OK", "message" -> ("created: " + id))))
    })
  }

  def modifyCoupon(id: Long): Action[JsValue] = Action(parse.json).async { implicit request =>
    request.body.validate[WSUpdateCouponData].fold({ errors =>
      Future.successful(BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> "Bad JSON",
        "details" -> JsError.toJson(errors)
      )))
    }, { cnpData =>
      cpnRepo.updateById(id, cnpData).map {
        case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("updated: " + id)))
        case 0 => BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> s"Not found item by id: $id",
        ))
      }
    })
  }

  def removeCoupon(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    cpnRepo.removeById(id).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("removed: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def invalidateCoupon(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    cpnRepo.updateById(id, new WSUpdateCouponData(Option.empty, Option.apply(CouponStatuses.Used.toString), Option.empty)).map {
      case 1 => Ok(Json.obj("status" -> "OK", "message" -> ("used coupon: " + id)))
      case 0 => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

  def getCoupon(id: Long) = Action.async { implicit request: Request[AnyContent] =>
    cpnRepo.getById(id).map {
      case Some(p) => Ok(Json.toJson(p))
      case None => BadRequest(Json.obj(
        "status" -> "Error",
        "message" -> s"Not found item by id: $id",
      ))
    }
  }

}
