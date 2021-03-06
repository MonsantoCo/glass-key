package glasskey.resource.validation

import glasskey.model.validation.RBACAuthZData
import org.scalatest.{FunSpec, Matchers, FlatSpec}

import scala.concurrent.Future
import scala.util.parsing.combinator.RegexParsers

/**
  * Created by bkrodg on 3/8/16.
  */
class EntitlementAuthorizerSpec extends FunSpec with Matchers with RegexParsers {
  describe("One desired app with one desired entitlement") {
    val desired = Seq(RBACAuthZData("MyApp", Set("MyEntitlement")))

    describe("One matching user app with one user entitlement") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement")))
      val auth = new SimpleEntitlementAuthorizer(desired)

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be true on an AND") {
        auth.andAuthorized(user) should be(true)
      }
    }

    describe("one matching user app with one non-matching user entitlement") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlementOther")))
      val auth = new SimpleEntitlementAuthorizer(desired)
      it("should be false on an OR") {
        auth.orAuthorized(user) should be(false)
      }
      it("should be false on an AND") {
        auth.andAuthorized(user) should be(false)
      }
    }
  }
  describe("One desired app with two desired entitlements") {
    val desired = Seq(RBACAuthZData("MyApp", Set("MyEntitlement1", "MyEntitlement2")))
    val auth = new SimpleEntitlementAuthorizer(desired)

    describe("One user app with one of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement1")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be false on an AND") {
        auth.andAuthorized(user) should be(false)
      }
    }

    describe("One user app with both of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement1", "MyEntitlement2")))
      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be true on an AND") {
        auth.andAuthorized(user) should be(true)
      }
    }
  }

  describe("Two desired apps (same name) with two desired entitlements") {
    val desired = Seq(RBACAuthZData("MyApp", Set("MyEntitlement1")), RBACAuthZData("MyApp", Set("MyEntitlement2")))
    val auth = new SimpleEntitlementAuthorizer(desired)

    describe("One user app with the first of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement1")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be false on an AND") {
        auth.andAuthorized(user) should be(false)
      }
    }

    describe("One user app with the second of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement2")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be false on an AND") {
        auth.andAuthorized(user) should be(false)
      }
    }

    describe("One user app with both of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement1", "MyEntitlement2")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be true on an AND") {
        auth.andAuthorized(user) should be(true)
      }
    }

    describe("One user app with both of the entitlements, but a different order") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement2", "MyEntitlement1")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be true on an AND") {
        auth.andAuthorized(user) should be(true)
      }
    }


    describe("One user app with one other entitlement first, then both of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp", Set("OtherEntitlement", "MyEntitlement1", "MyEntitlement2")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be true on an AND") {
        auth.andAuthorized(user) should be(true)
      }
    }

    describe("One user app with both of the entitlements first, then one other entitlement") {
      val user = Seq(RBACAuthZData("MyApp", Set("MyEntitlement1", "MyEntitlement2", "OtherEntitlement")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be true on an AND") {
        auth.andAuthorized(user) should be(true)
      }
    }
  }

  describe("Two desired apps with one desired entitlement each") {
    val desired = Seq(RBACAuthZData("MyApp1", Set("MyEntitlement1")), RBACAuthZData("MyApp2", Set("MyEntitlement2")))
    val auth = new SimpleEntitlementAuthorizer(desired)

    describe("One user app with the first of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp1", Set("MyEntitlement1")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be false on an AND") {
        auth.andAuthorized(user) should be(false)
      }
    }
    describe("One user app with the second of the entitlements") {
      val user = Seq(RBACAuthZData("MyApp2", Set("MyEntitlement2")))

      it("should be true on an OR") {
        auth.orAuthorized(user) should be(true)
      }
      it("should be false on an AND") {
        auth.andAuthorized(user) should be(false)
      }
    }

  }
}
class SimpleEntitlementAuthorizer(val desiredAuth: Seq[RBACAuthZData]) extends EntitlementAuthorizer {
  override val entitlementUri: String = ""

  override def getAuth(accessToken: String, userId: String): Future[Seq[RBACAuthZData]] = ???

}
